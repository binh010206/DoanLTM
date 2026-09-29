#define WIN32_LEAN_AND_MEAN
#include <winsock2.h>
#include <ws2tcpip.h>
#include <windows.h>
#include <iostream>
#include <fstream>
#include <string>
#include <vector>
#include <thread>
#include <mutex>
#include <sstream>
#include <ctime>
#include <algorithm>
#include <map>
#include <iomanip>
#pragma comment(lib, "Ws2_32.lib")

//  CAU TRUC DU LIEU
struct Slot {
    int         id;
    bool        occupied;
    std::string rfid;
    time_t      time_in;
    std::string client_ip;
    int         client_port;
};

struct LogEntry {
    std::string time_str;
    std::string event;
    std::string type;       // "entry" | "exit" | "reject"
    std::string client_ip;
    long        fee;
    int         duration_sec;
};

struct ClientInfo {
    std::string ip;
    int         port;
};

//  TRANG THAI TOAN CUC
std::mutex            g_mutex;
Slot                  g_slots[3] = {
    {1, false, "", 0, "", 0},
    {2, false, "", 0, "", 0},
    {3, false, "", 0, "", 0}
};
std::vector<LogEntry> g_log;

int     g_total_in       = 0;
int     g_total_out      = 0;
long    g_total_revenue  = 0;
int     g_active_clients = 0;
std::vector<ClientInfo> g_connected_clients;

const long RATE_PER_MINUTE = 5000;
const std::string DATA_FILE = "parking_data.json";

//  TIEN ICH THOI GIAN & DINH DANG

std::string get_time() {
    time_t now = time(nullptr);
    struct tm t;
    localtime_s(&t, &now);
    char buf[16];
    strftime(buf, sizeof(buf), "%H:%M:%S", &t);
    return std::string(buf);
}

std::string get_date_time() {
    time_t now = time(nullptr);
    struct tm t;
    localtime_s(&t, &now);
    char buf[32];
    strftime(buf, sizeof(buf), "%d/%m/%Y %H:%M:%S", &t);
    return std::string(buf);
}

void add_log(const std::string& event, const std::string& type,
             const std::string& client_ip, long fee = 0, int duration = 0) {
    LogEntry e;
    e.time_str     = get_time();
    e.event        = event;
    e.type         = type;
    e.client_ip    = client_ip;
    e.fee          = fee;
    e.duration_sec = duration;
    g_log.insert(g_log.begin(), e);
    if (g_log.size() > 100) g_log.pop_back();
}

long calc_fee(time_t time_in) {
    time_t now = time(nullptr);
    int seconds = (int)difftime(now, time_in);
    int minutes = (seconds + 59) / 60;
    if (minutes < 1) minutes = 1;
    return (long)minutes * RATE_PER_MINUTE;
}

std::string format_vnd(long amount) {
    std::string s = std::to_string(amount);
    int n = (int)s.length();
    std::string result;
    for (int i = 0; i < n; i++) {
        if (i > 0 && (n - i) % 3 == 0) result += '.';
        result += s[i];
    }
    return result + " VND";
}


//  LUU / TAI DU LIEU TU FILE JSON (DATA PERSISTENCE)
std::string json_escape(const std::string& s) {
    std::string out;
    for (char c : s) {
        if (c == '"') out += "\\\"";
        else if (c == '\\') out += "\\\\";
        else if (c == '\n') out += "\\n";
        else out += c;
    }
    return out;
}

void save_data() {
    std::ofstream f(DATA_FILE);
    if (!f.is_open()) return;

    f << "{\n";
    f << "  \"total_in\": " << g_total_in << ",\n";
    f << "  \"total_out\": " << g_total_out << ",\n";
    f << "  \"total_revenue\": " << g_total_revenue << ",\n";

    f << "  \"slots\": [\n";
    for (int i = 0; i < 3; i++) {
        f << "    {\"id\":" << g_slots[i].id
          << ",\"occupied\":" << (g_slots[i].occupied ? "true" : "false")
          << ",\"rfid\":\"" << json_escape(g_slots[i].rfid) << "\""
          << ",\"time_in\":" << g_slots[i].time_in
          << ",\"client_ip\":\"" << g_slots[i].client_ip << "\""
          << ",\"client_port\":" << g_slots[i].client_port
          << "}";
        if (i < 2) f << ",";
        f << "\n";
    }
    f << "  ],\n";

    int log_count = std::min((int)g_log.size(), 50);
    f << "  \"log\": [\n";
    for (int i = 0; i < log_count; i++) {
        f << "    {\"time\":\"" << g_log[i].time_str
          << "\",\"event\":\"" << json_escape(g_log[i].event)
          << "\",\"type\":\"" << g_log[i].type
          << "\",\"client_ip\":\"" << g_log[i].client_ip
          << "\",\"fee\":" << g_log[i].fee
          << ",\"duration_sec\":" << g_log[i].duration_sec
          << "}";
        if (i + 1 < log_count) f << ",";
        f << "\n";
    }
    f << "  ]\n";
    f << "}\n";
    f.close();
}

std::string extract_json_string(const std::string& json, const std::string& key) {
    std::string search = "\"" + key + "\":\"";
    size_t pos = json.find(search);
    if (pos == std::string::npos) return "";
    pos += search.size();
    size_t end = json.find("\"", pos);
    if (end == std::string::npos) return "";
    return json.substr(pos, end - pos);
}

long extract_json_long(const std::string& json, const std::string& key) {
    std::string search = "\"" + key + "\":";
    size_t pos = json.find(search);
    if (pos == std::string::npos) return 0;
    pos += search.size();
    while (pos < json.size() && (json[pos] == ' ' || json[pos] == '\t')) pos++;
    std::string num;
    while (pos < json.size() && (json[pos] >= '0' && json[pos] <= '9')) {
        num += json[pos++];
    }
    if (num.empty()) return 0;
    return std::stol(num);
}

bool extract_json_bool(const std::string& json, const std::string& key) {
    std::string search = "\"" + key + "\":";
    size_t pos = json.find(search);
    if (pos == std::string::npos) return false;
    pos += search.size();
    while (pos < json.size() && json[pos] == ' ') pos++;
    return (json.substr(pos, 4) == "true");
}

void load_data() {
    std::ifstream f(DATA_FILE);
    if (!f.is_open()) {
        std::cout << "  [DATA] Khoi tao co so du lieu moi.\n";
        return;
    }

    std::string content((std::istreambuf_iterator<char>(f)),
                         std::istreambuf_iterator<char>());
    f.close();

    if (content.empty()) return;

    g_total_in      = (int)extract_json_long(content, "total_in");
    g_total_out     = (int)extract_json_long(content, "total_out");
    g_total_revenue = extract_json_long(content, "total_revenue");

    size_t slots_pos = content.find("\"slots\"");
    if (slots_pos != std::string::npos) {
        size_t arr_start = content.find("[", slots_pos);
        if (arr_start != std::string::npos) {
            for (int i = 0; i < 3; i++) {
                size_t obj_start = content.find("{", arr_start + 1);
                size_t obj_end   = content.find("}", obj_start);
                if (obj_start == std::string::npos || obj_end == std::string::npos) break;

                std::string obj = content.substr(obj_start, obj_end - obj_start + 1);
                g_slots[i].occupied    = extract_json_bool(obj, "occupied");
                g_slots[i].rfid        = extract_json_string(obj, "rfid");
                g_slots[i].time_in     = (time_t)extract_json_long(obj, "time_in");
                g_slots[i].client_ip   = extract_json_string(obj, "client_ip");
                g_slots[i].client_port = (int)extract_json_long(obj, "client_port");

                arr_start = obj_end;
            }
        }
    }

    size_t log_pos = content.find("\"log\"");
    if (log_pos != std::string::npos) {
        size_t arr_start = content.find("[", log_pos);
        if (arr_start != std::string::npos) {
            size_t search_from = arr_start + 1;
            while (true) {
                size_t obj_start = content.find("{", search_from);
                size_t arr_end   = content.find("]", search_from);
                if (obj_start == std::string::npos || (arr_end != std::string::npos && obj_start > arr_end)) break;
                size_t obj_end = content.find("}", obj_start);
                if (obj_end == std::string::npos) break;

                std::string obj = content.substr(obj_start, obj_end - obj_start + 1);
                LogEntry e;
                e.time_str     = extract_json_string(obj, "time");
                e.event        = extract_json_string(obj, "event");
                e.type         = extract_json_string(obj, "type");
                e.client_ip    = extract_json_string(obj, "client_ip");
                e.fee          = extract_json_long(obj, "fee");
                e.duration_sec = (int)extract_json_long(obj, "duration_sec");
                g_log.push_back(e);

                search_from = obj_end + 1;
            }
        }
    }

    std::cout << "  [DATA] Da tai: " << g_total_in << " vao | "
              << g_total_out << " ra | Doanh thu: " << format_vnd(g_total_revenue) << "\n";
}

//  XU LY GOI TIN TCP TU TRAM QUET THE (ENTRY / EXIT)
std::string handle_message(const std::string& msg, const ClientInfo& ci) {
    size_t sep = msg.find('|');
    if (sep == std::string::npos) return "ERROR|BAD_FORMAT\n";

    std::string cmd  = msg.substr(0, sep);
    std::string rfid = msg.substr(sep + 1);
    rfid.erase(std::remove_if(rfid.begin(), rfid.end(),
        [](char c){ return c == '\r' || c == '\n' || c == ' '; }), rfid.end());

    if (rfid.empty()) return "ERROR|EMPTY_RFID\n";

    std::string client_addr = ci.ip + ":" + std::to_string(ci.port);
    std::lock_guard<std::mutex> lock(g_mutex);

    if (cmd == "ENTRY") {
        // 1. Kiem tra xe da trong bai chua (chong quet 2 lan)
        for (auto& s : g_slots) {
            if (s.occupied && s.rfid == rfid) {
                std::cout << "  [!] The " << rfid << " DA TRONG BAI (O " << s.id << ")\n";
                add_log("TU CHOI - THE DA CO TRONG BAI - RFID: " + rfid, "reject", client_addr);
                save_data();
                return "ERROR|ALREADY_IN|SLOT_" + std::to_string(s.id) + "\n";
            }
        }

        // 2. Tim o trong dau tien de cap phat
        for (auto& s : g_slots) {
            if (!s.occupied) {
                s.occupied    = true;
                s.rfid        = rfid;
                s.time_in     = time(nullptr);
                s.client_ip   = ci.ip;
                s.client_port = ci.port;
                g_total_in++;

                std::string ev = "Xe VAO - O " + std::to_string(s.id) + " - RFID: " + rfid;
                add_log(ev, "entry", client_addr);

                std::cout << "  [VAO] " << get_time()
                          << " | RFID=" << rfid
                          << " | Cap O " << s.id
                          << " | Client=" << client_addr << std::endl;

                save_data();
                return "OK|SLOT_" + std::to_string(s.id) + "\n";
            }
        }

        // 3. Het cho
        std::cout << "  [CANH BAO] BAI DAY - Tu choi the " << rfid << std::endl;
        add_log("TU CHOI - BAI DAY - RFID: " + rfid, "reject", client_addr);
        save_data();
        return "FULL\n";
    }
    else if (cmd == "EXIT") {
        for (auto& s : g_slots) {
            if (s.occupied && s.rfid == rfid) {
                int id = s.id;
                int duration = (int)difftime(time(nullptr), s.time_in);
                long fee = calc_fee(s.time_in);

                s.occupied    = false;
                s.rfid        = "";
                s.time_in     = 0;
                s.client_ip   = "";
                s.client_port = 0;

                g_total_out++;
                g_total_revenue += fee;

                int dur_min = duration / 60;
                int dur_sec = duration % 60;

                std::string ev = "Xe RA - O " + std::to_string(id) 
                    + " - RFID: " + rfid
                    + " - " + format_vnd(fee);
                add_log(ev, "exit", client_addr, fee, duration);

                std::cout << "  [RA ] " << get_time()
                          << " | RFID=" << rfid
                          << " | Giai phong O " << id
                          << " | Do: " << dur_min << "p" << dur_sec << "s"
                          << " | Thu: " << format_vnd(fee) << std::endl;

                save_data();

                return "OK|FREED_" + std::to_string(id)
                    + "|FEE:" + std::to_string(fee)
                    + "|DURATION:" + std::to_string(duration) + "s\n";
            }
        }
        std::cout << "  [?] The " << rfid << " KHONG TIM THAY TRONG BAI!" << std::endl;
        add_log("TU CHOI - THE CHUA VAO BAI - RFID: " + rfid, "reject", client_addr);
        save_data();
        return "NOT_FOUND\n";
    }

    return "ERROR|UNKNOWN_CMD\n";
}
//  THREAD CLIENT TCP
void client_thread(SOCKET client_sock, ClientInfo ci) {
    {
        std::lock_guard<std::mutex> lock(g_mutex);
        g_connected_clients.push_back(ci);
        g_active_clients = (int)g_connected_clients.size();
    }
    std::cout << "\n=========================================================\n"
              << "  [+] CO MAY TRAM (CLIENT) KET NOI DEN!\n"
              << "      -> Dia chi Socket Client : " << ci.ip << ":" << ci.port << "\n"
              << "      -> Tong so Client online : " << g_active_clients << " may tram\n"
              << "=========================================================" << std::endl;

    char buf[1024];
    while (true) {
        int n = recv(client_sock, buf, sizeof(buf) - 1, 0);
        if (n <= 0) break;
        buf[n] = '\0';

        std::string data(buf);
        std::istringstream stream(data);
        std::string line;
        while (std::getline(stream, line)) {
            if (line.empty() || line == "\r") continue;

            std::cout << "\n[" << get_time() << "] [TCP RECV] Nhan tu Client [" << ci.ip << ":" << ci.port << "]: \"" << line << "\"" << std::endl;

            std::string response = handle_message(line, ci);

            std::string resp_print = response;
            if (!resp_print.empty() && resp_print.back() == '\n') resp_print.pop_back();

            std::cout << "[" << get_time() << "] [TCP SEND] Phan hoi cho Client [" << ci.ip << ":" << ci.port << "]: \"" << resp_print << "\"" << std::endl;

            send(client_sock, response.c_str(), (int)response.size(), 0);
        }
    }

    {
        std::lock_guard<std::mutex> lock(g_mutex);
        for (auto it = g_connected_clients.begin(); it != g_connected_clients.end(); ++it) {
            if (it->ip == ci.ip && it->port == ci.port) {
                g_connected_clients.erase(it);
                break;
            }
        }
        g_active_clients = (int)g_connected_clients.size();
    }
    std::cout << "\n[-] Client ngat ket noi: " << ci.ip << ":" << ci.port << " (Con lai: " << g_active_clients << " client)" << std::endl;
    closesocket(client_sock);
}

//  JSON API /status
std::string make_status_json() {
    std::lock_guard<std::mutex> lock(g_mutex);
    time_t now = time(nullptr);
    std::ostringstream ss;
    ss << "{\"slots\":[";
    for (int i = 0; i < 3; i++) {
        int dur = g_slots[i].occupied ? (int)difftime(now, g_slots[i].time_in) : 0;
        long cur_fee = g_slots[i].occupied ? calc_fee(g_slots[i].time_in) : 0;
        struct tm t_in = {};
        char time_in_buf[16] = "";
        if (g_slots[i].occupied && g_slots[i].time_in > 0) {
            localtime_s(&t_in, &g_slots[i].time_in);
            strftime(time_in_buf, sizeof(time_in_buf), "%H:%M:%S", &t_in);
        }

        ss << "{\"id\":" << g_slots[i].id
           << ",\"occupied\":" << (g_slots[i].occupied ? "true" : "false")
           << ",\"rfid\":\"" << g_slots[i].rfid << "\""
           << ",\"time_in\":\"" << time_in_buf << "\""
           << ",\"duration_sec\":" << dur
           << ",\"current_fee\":" << cur_fee
           << ",\"client_ip\":\"" << g_slots[i].client_ip << "\""
           << ",\"client_port\":" << g_slots[i].client_port
           << "}";
        if (i < 2) ss << ",";
    }
    ss << "],\"log\":[";
    for (size_t i = 0; i < g_log.size(); i++) {
        ss << "{\"time\":\"" << g_log[i].time_str
           << "\",\"event\":\"" << json_escape(g_log[i].event)
           << "\",\"type\":\"" << g_log[i].type
           << "\",\"client_ip\":\"" << g_log[i].client_ip
           << "\",\"fee\":" << g_log[i].fee
           << ",\"duration_sec\":" << g_log[i].duration_sec
           << "}";
        if (i + 1 < g_log.size()) ss << ",";
    }
    ss << "],\"clients\":[";
    for (size_t i = 0; i < g_connected_clients.size(); i++) {
        ss << "{\"ip\":\"" << g_connected_clients[i].ip << "\""
           << ",\"port\":" << g_connected_clients[i].port
           << ",\"addr\":\"" << g_connected_clients[i].ip << ":" << g_connected_clients[i].port << "\"}";
        if (i + 1 < g_connected_clients.size()) ss << ",";
    }
    ss << "],\"stats\":{"
       << "\"total_in\":" << g_total_in
       << ",\"total_out\":" << g_total_out
       << ",\"revenue\":" << g_total_revenue
       << ",\"active_clients\":" << g_active_clients
       << ",\"occupancy\":" << (g_slots[0].occupied + g_slots[1].occupied + g_slots[2].occupied)
       << ",\"capacity\":3"
       << ",\"server_time\":\"" << get_date_time() << "\""
       << ",\"rate_per_minute\":" << RATE_PER_MINUTE
       << "}}";
    return ss.str();
}

std::string get_http_path(const char* req) {
    std::string s(req);
    if (s.substr(0, 7) == "OPTIONS") return "OPTIONS";
    if (s.substr(0, 3) != "GET") return "";
    size_t start = 4;
    size_t end = s.find(' ', start);
    if (end == std::string::npos) return "";
    return s.substr(start, end - start);
}

std::string make_http_response(const std::string& body, int code = 200) {
    std::string status = (code == 200) ? "200 OK" : "404 Not Found";
    std::ostringstream resp;
    resp << "HTTP/1.1 " << status << "\r\n"
         << "Content-Type: application/json; charset=utf-8\r\n"
         << "Access-Control-Allow-Origin: *\r\n"
         << "Access-Control-Allow-Methods: GET, OPTIONS\r\n"
         << "Access-Control-Allow-Headers: Content-Type\r\n"
         << "Content-Length: " << body.size() << "\r\n"
         << "Connection: close\r\n\r\n"
         << body;
    return resp.str();
}

void http_server_thread() {
    SOCKET srv = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
    int opt = 1;
    setsockopt(srv, SOL_SOCKET, SO_REUSEADDR, (char*)&opt, sizeof(opt));

    sockaddr_in addr{};
    addr.sin_family      = AF_INET;
    addr.sin_port        = htons(9999);
    addr.sin_addr.s_addr = INADDR_ANY;

    if (bind(srv, (sockaddr*)&addr, sizeof(addr)) != 0) {
        std::cerr << "[LOI] Khong bind duoc HTTP port 9999!\n";
        return;
    }
    listen(srv, 32);

    while (true) {
        SOCKET cli = accept(srv, nullptr, nullptr);
        if (cli == INVALID_SOCKET) continue;

        char req[4096] = {};
        recv(cli, req, sizeof(req) - 1, 0);

        std::string path = get_http_path(req);
        std::string response;

        if (path == "OPTIONS") {
            response = make_http_response("", 200);
        }
        else if (path == "/status") {
            response = make_http_response(make_status_json());
        }
        else if (path == "/stats") {
            std::lock_guard<std::mutex> lock(g_mutex);
            int occ = g_slots[0].occupied + g_slots[1].occupied + g_slots[2].occupied;
            std::ostringstream ss;
            ss << "{\"total_in\":" << g_total_in
               << ",\"total_out\":" << g_total_out
               << ",\"revenue\":" << g_total_revenue
               << ",\"revenue_formatted\":\"" << format_vnd(g_total_revenue) << "\""
               << ",\"occupancy\":" << occ
               << ",\"capacity\":3"
               << ",\"active_clients\":" << g_active_clients
               << ",\"server_time\":\"" << get_date_time() << "\""
               << "}";
            response = make_http_response(ss.str());
        }
        else {
            response = make_http_response("{\"error\":\"Not Found\"}", 404);
        }

        send(cli, response.c_str(), (int)response.size(), 0);
        closesocket(cli);
    }
}

//  MAIN
int main() {
    SetConsoleOutputCP(CP_UTF8);
    WSADATA wsa;
    WSAStartup(MAKEWORD(2, 2), &wsa);

    std::cout << "\n";
    std::cout << "   SERVER HE THONG BAI DO XE THONG MINH  \n";
    std::cout << "   Giao thuc: TCP Socket (8888) + HTTP REST API (9999) \n";
    std::cout << "  -----------------------------------------------------\n";

    load_data();

    // Khoi dong HTTP Thread
    std::thread(http_server_thread).detach();

    // Khoi dong TCP Server
    SOCKET srv = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
    int opt = 1;
    setsockopt(srv, SOL_SOCKET, SO_REUSEADDR, (char*)&opt, sizeof(opt));

    sockaddr_in addr{};
    addr.sin_family      = AF_INET;
    addr.sin_port        = htons(8888);
    addr.sin_addr.s_addr = INADDR_ANY;

    if (bind(srv, (sockaddr*)&addr, sizeof(addr)) != 0) {
        std::cerr << "  [LOI] Khong bind duoc TCP port 8888!\n";
        WSACleanup();
        return 1;
    }
    listen(srv, 10);

    std::cout << "  [OK] TCP Port 8888  : Dang lang nghe Client RFID\n";
    std::cout << "  [OK] HTTP Port 9999 : Dang phuc vu Dashboard REST API\n";
    std::cout << "  [OK] Du lieu        : " << DATA_FILE << " ()\n";
    std::cout << "  -----------------------------------------------------\n\n";

    while (true) {
        sockaddr_in client_addr{};
        int addr_len = sizeof(client_addr);
        SOCKET cli = accept(srv, (sockaddr*)&client_addr, &addr_len);
        if (cli == INVALID_SOCKET) continue;

        char ip_buf[INET_ADDRSTRLEN];
        inet_ntop(AF_INET, &client_addr.sin_addr, ip_buf, sizeof(ip_buf));
        ClientInfo ci;
        ci.ip   = std::string(ip_buf);
        ci.port = ntohs(client_addr.sin_port);

        std::thread(client_thread, cli, ci).detach();
    }

    WSACleanup();
    return 0;
}
