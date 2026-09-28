#define WIN32_LEAN_AND_MEAN
#include <winsock2.h>
#include <ws2tcpip.h>
#include <windows.h>
#include <iostream>
#include <string>
#include <vector>
#pragma comment(lib, "Ws2_32.lib")

// Danh sach the chip RFID vat ly phat cho khach vang lai
struct RFIDCard {
    std::string id;
    std::string label;
};

const RFIDCard CARDS[] = {
    {"A1B2C3D4", "The so 01 (Chip ID: A1B2C3D4)"},
    {"FF00AB12", "The so 02 (Chip ID: FF00AB12)"},
    {"11223344", "The so 03 (Chip ID: 11223344)"},
    {"DEADBEEF", "The so 04 (Chip ID: DEADBEEF)"},
    {"CAFE0001", "The so 05 (Chip ID: CAFE0001)"}
};
const int CARD_COUNT = 5;

void print_header() {
    std::cout << "\n";
    std::cout << "   TRAM QUET THE RFID - BAI DO XE THONG MINH (VKU)      \n";
    std::cout << "   Giao thuc: TCP Socket | Server: 127.0.0.1:8888       \n";
    std::cout << "   -----------------------------------------------------\n";
}

void print_cards() {
    std::cout << "\n  --- DANH SACH THE RFID PHAT CHO KHACH VANG LAI ---\n";
    for (int i = 0; i < CARD_COUNT; i++) {
        std::cout << "    [" << (i + 1) << "] " << CARDS[i].label << "\n";
    }
    std::cout << "    [0] Quet the khac (Nhap ma tay)\n";
}

std::string pick_rfid() {
    print_cards();
    std::cout << "\n  Chon the de quet (1-" << CARD_COUNT << ") hoac 0 de nhap ma tuy y: ";
    int choice;
    if (!(std::cin >> choice)) {
        std::cin.clear();
        std::cin.ignore(1000, '\n');
        return "CARD_01";
    }

    if (choice >= 1 && choice <= CARD_COUNT) {
        std::cout << "  >> Da quet: " << CARDS[choice - 1].label << "\n";
        return CARDS[choice - 1].id;
    }

    std::cout << "  Nhap ma the RFID thu cong: ";
    std::cin.ignore();
    std::string rfid;
    std::getline(std::cin, rfid);
    if (rfid.empty()) rfid = "CARD_CUSTOM";
    return rfid;
}

void parse_response(const std::string& resp) {
    if (resp.find("OK|SLOT_") != std::string::npos) {
        std::string slot = resp.substr(resp.find("SLOT_") + 5, 1);
        std::cout << "\n  +---------------------------------------------------+\n";
        std::cout << "  | [CONG VAO] >> QUET THE THANH CONG! MO BARIE       |\n";
        std::cout << "  | [HE THONG] >> Chi dinh xe do vao O SO: " << slot << "        |\n";
        std::cout << "  +---------------------------------------------------+\n";
    }
    else if (resp.find("OK|FREED_") != std::string::npos) {
        size_t fee_pos = resp.find("FEE:");
        size_t dur_pos = resp.find("DURATION:");
        std::string fee_str = "0", dur_str = "0";
        if (fee_pos != std::string::npos) {
            size_t end = resp.find('|', fee_pos);
            if (end == std::string::npos) end = resp.find('\n', fee_pos);
            if (end == std::string::npos) end = resp.size();
            fee_str = resp.substr(fee_pos + 4, end - fee_pos - 4);
        }
        if (dur_pos != std::string::npos) {
            size_t end = resp.find('|', dur_pos);
            if (end == std::string::npos) end = resp.find('\n', dur_pos);
            if (end == std::string::npos) end = resp.size();
            dur_str = resp.substr(dur_pos + 9, end - dur_pos - 9);
        }
        std::string slot = resp.substr(resp.find("FREED_") + 6, 1);

        std::cout << "\n  +---------------------------------------------------+\n";
        std::cout << "  | [CONG RA]  >> THU HOI THE - MO BARIE CHO XE RA    |\n";
        std::cout << "  | [GIAI PHONG] >> Tra O SO: " << slot << "                          |\n";
        std::cout << "  | [THOI GIAN]  >> Thoi gian do: " << dur_str << "\n";
        std::cout << "  | [PHI PHAI THU] >> " << fee_str << " VND                     |\n";
        std::cout << "  +---------------------------------------------------+\n";
    }
    else if (resp.find("FULL") != std::string::npos) {
        std::cout << "\n  [CANH BAO] BAI DA DAY! Tat ca cac o deu da co xe.\n";
    }
    else if (resp.find("NOT_FOUND") != std::string::npos) {
        std::cout << "\n  [LOI] THE NAY CHUA QUET VAO BAI!\n";
    }
    else if (resp.find("ALREADY_IN") != std::string::npos) {
        std::cout << "\n  [LOI] THE NAY DANG TRONG BAI (Chua quet ra)!\n";
    }
    else {
        std::cout << "\n  [Server phan hoi] " << resp;
    }
}

bool send_cmd(SOCKET sock, const std::string& cmd, const std::string& rfid) {
    std::string msg = cmd + "|" + rfid + "\n";
    std::cout << "\n  --> [TCP SEND] " << cmd << "|" << rfid << "\n";
    send(sock, msg.c_str(), (int)msg.size(), 0);

    char buf[512] = {};
    int n = recv(sock, buf, sizeof(buf) - 1, 0);
    if (n <= 0) return false;
    std::string resp(buf, n);
    std::cout << "  <-- [TCP RECV] " << resp;
    parse_response(resp);
    return true;
}

int main() {
    SetConsoleOutputCP(CP_UTF8);
    WSADATA wsa;
    WSAStartup(MAKEWORD(2, 2), &wsa);

    SOCKET sock = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
    sockaddr_in srv{};
    srv.sin_family = AF_INET;
    srv.sin_port   = htons(8888);
    inet_pton(AF_INET, "127.0.0.1", &srv.sin_addr);

    std::cout << "\n  Dang ket noi toi Server TCP 127.0.0.1:8888...\n";

    if (connect(sock, (sockaddr*)&srv, sizeof(srv)) != 0) {
        std::cerr << "  [LOI] Khong the ket noi! Hay chac chan server.exe dang chay.\n";
        system("pause");
        return 1;
    }

    print_header();
    std::cout << "   [OK] Da ket noi toi Server TCP: 127.0.0.1:8888\n";

    while (true) {
        std::cout << "\n  - MENU DIEU KHIEN -\n";
        std::cout << "  [1] Quet the XE VAO (ENTRY)\n";
        std::cout << "  [2] Quet the XE RA  (EXIT & Tinh tien)\n";
        std::cout << "  [0] Thoat chuong trinh\n";
        std::cout << "  -----------------------------------------------------\n";
        std::cout << "  Nhap lua chon cua ban (0, 1, 2): ";

        int choice;
        if (!(std::cin >> choice)) {
            std::cin.clear();
            std::cin.ignore(1000, '\n');
            continue;
        }

        if (choice == 0) break;

        if (choice != 1 && choice != 2) {
            std::cout << "  [!] Lua chon khong hop le! Vui long chon 1, 2 hoac 0.\n";
            continue;
        }

        std::string rfid = pick_rfid();
        std::string cmd = (choice == 1) ? "ENTRY" : "EXIT";

        if (!send_cmd(sock, cmd, rfid)) {
            std::cout << "  [LOI] Mat ket noi voi Server!\n";
            break;
        }
    }

    closesocket(sock);
    WSACleanup();
    std::cout << "\n  Da ngat ket noi toi Server. Ket thuc chuong trinh.\n";
    return 0;
}
