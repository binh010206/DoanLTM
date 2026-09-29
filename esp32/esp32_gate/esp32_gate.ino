/*
 * =============================================================================
 *   DO AN LAP TRINH MANG - VKU DA NANG
 *   HE THONG BAI DO XE THONG MINH (SMART PARKING SYSTEM)
 *   HARDWARE CLIENT: ESP32 + RFID RC522 + SERVO BARRIER
 * =============================================================================
 * 
 * SO DO NOI DAY (PINOUT):
 *   RC522 VCC  --> ESP32 3V3 (BAT BUOC 3.3V, KHONG CAM 5V SE CHAY MODULE)
 *   RC522 RST  --> ESP32 GPIO 22
 *   RC522 GND  --> ESP32 GND
 *   RC522 MISO --> ESP32 GPIO 19
 *   RC522 MOSI --> ESP32 GPIO 23
 *   RC522 SCK  --> ESP32 GPIO 18
 *   RC522 SDA  --> ESP32 GPIO 5 (SS)
 *   SERVO PWM  --> ESP32 GPIO 13 (Day cam tin hieu)
 *   BUILTIN LED--> ESP32 GPIO 2
 *
 * THU VIEN ARDUINO IDE CAN CAI:
 *   1. MFRC522 (by GithubCommunity)
 *   2. ESP32Servo (by Kevin Harrington)
 */

#include <WiFi.h>
#include <SPI.h>
#include <MFRC522.h>
#include <ESP32Servo.h>

// =============================================================================
// 1. CAU HINH WI-FI & TCP SERVER
// =============================================================================
const char* WIFI_SSID     = "Redmi 13";       // SSID phat tu dien thoai
const char* WIFI_PASSWORD = "binh010206";      // Mat khau Wi-Fi
const char* SERVER_HOST   = "10.187.149.136"; // IP Server tren Laptop
const uint16_t SERVER_PORT = 8888;            // Port TCP cua Server C++

// =============================================================================
// 2. CHE DO CONG (GATE MODE)
//    1 = CONG VAO (ENTRY) - Quet the de vao bai, mo barie vao
//    2 = CONG RA  (EXIT)  - Quet the de tinh tien va ra bai, mo barie ra
// =============================================================================
#define GATE_MODE 1

// =============================================================================
// 3. KHAI BAO PHAN CUNG
// =============================================================================
#define SS_PIN    5
#define RST_PIN   22
#define SERVO_PIN 13
#define LED_PIN   2

MFRC522 rfid(SS_PIN, RST_PIN);
Servo barrierServo;
WiFiClient tcpClient;

unsigned long lastScanTime = 0;

void connectWiFi();
void connectServer();

void setup() {
    Serial.begin(115200);
    delay(1000);
    Serial.println("\n\n===============================================");
    Serial.println("   VKU - HE THONG BAI DO XE THONG MINH");
    Serial.println("   MODULE PHAN CUNG ESP32 GATEWAY");
    Serial.printf("   CHUC NANG: %s\n", (GATE_MODE == 1) ? "TRAM CONG VAO (ENTRY)" : "TRAM CONG RA (EXIT)");
    Serial.println("===============================================");

    pinMode(LED_PIN, OUTPUT);
    digitalWrite(LED_PIN, LOW);

    // Khoi tao Servo
    barrierServo.attach(SERVO_PIN);
    barrierServo.write(0); // Barie dong o goc 0 do

    // Khoi tao RFID RC522
    SPI.begin(18, 19, 23, 5); // SCK, MISO, MOSI, SS
    rfid.PCD_Init();
    Serial.println("[RFID] Khoi tao module RC522 thanh cong.");

    // Ket noi Wi-Fi
    connectWiFi();

    // Ket noi TCP Socket toi Server C++
    connectServer();
}

void loop() {
    // 1. Duy tri ket noi Wi-Fi
    if (WiFi.status() != WL_CONNECTED) {
        connectWiFi();
    }

    // 2. Duy tri ket noi TCP Socket
    if (!tcpClient.connected()) {
        connectServer();
        delay(1000);
        return;
    }

    // 3. Kiem tra the RFID
    if (!rfid.PICC_IsNewCardPresent() || !rfid.PICC_ReadCardSerial()) {
        return;
    }

    // Chong quet trung lap trong 3 giay
    if (millis() - lastScanTime < 3000) {
        rfid.PICC_HaltA();
        return;
    }
    lastScanTime = millis();

    // Doc ma the UID dang Hex in hoa
    String uidStr = "";
    for (byte i = 0; i < rfid.uid.size; i++) {
        if (rfid.uid.uidByte[i] < 0x10) uidStr += "0";
        uidStr += String(rfid.uid.uidByte[i], HEX);
    }
    uidStr.toUpperCase();

    Serial.printf("\n[RFID SCAN] Phat hien the: %s\n", uidStr.c_str());

    // 4. Gui lenh qua TCP Socket toi C++ Server
    String cmd = (GATE_MODE == 1) ? "ENTRY" : "EXIT";
    String packet = cmd + "|" + uidStr + "\n";

    Serial.printf("[TCP SEND] >> %s", packet.c_str());
    tcpClient.print(packet);

    // 5. Cho phan hoi tu Server
    unsigned long timeout = millis();
    while (!tcpClient.available() && millis() - timeout < 2500) {
        delay(10);
    }

    if (tcpClient.available()) {
        String resp = tcpClient.readStringUntil('\n');
        resp.trim();
        Serial.printf("[TCP RECV] << %s\n", resp.c_str());

        // Xu ly logic mo Barie
        if (resp.startsWith("OK|")) {
            Serial.println("[SUCCESS] Quet the hop le! DANG MO BARIE...");
            digitalWrite(LED_PIN, HIGH);

            // Mo Barie len 90 do
            barrierServo.write(90);
            delay(3000); // Giu 3 giay de xe di qua

            // Ha Barie ve 0 do
            barrierServo.write(0);
            digitalWrite(LED_PIN, LOW);
            Serial.println("[BARIE] Da dong barie an toan.\n");
        } else if (resp.indexOf("FULL") != -1) {
            Serial.println("[WARNING] Bai do xe da DAY! Khong the vao.");
            for (int k = 0; k < 3; k++) {
                digitalWrite(LED_PIN, HIGH); delay(150);
                digitalWrite(LED_PIN, LOW); delay(150);
            }
        } else if (resp.indexOf("ALREADY_IN") != -1) {
            Serial.println("[WARNING] The nay dang o trong bai!");
            for (int k = 0; k < 2; k++) {
                digitalWrite(LED_PIN, HIGH); delay(200);
                digitalWrite(LED_PIN, LOW); delay(200);
            }
        } else {
            Serial.println("[REJECT] Giao dich bi tu choi boi Server.");
            for (int k = 0; k < 4; k++) {
                digitalWrite(LED_PIN, HIGH); delay(100);
                digitalWrite(LED_PIN, LOW); delay(100);
            }
        }
    } else {
        Serial.println("[ERROR] Timeout - Server khong phan hoi kip thoi!");
    }

    rfid.PICC_HaltA();
    rfid.PCD_StopCrypto1();
}

void connectWiFi() {
    Serial.printf("[WIFI] Dang ket noi mang: %s ...\n", WIFI_SSID);
    WiFi.mode(WIFI_STA);
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
    int retry = 0;
    while (WiFi.status() != WL_CONNECTED && retry < 25) {
        delay(500);
        Serial.print(".");
        retry++;
    }
    if (WiFi.status() == WL_CONNECTED) {
        Serial.printf("\n[WIFI] Ket noi thanh cong! IP ESP32: %s\n", WiFi.localIP().toString().c_str());
    } else {
        Serial.println("\n[WIFI] Ket noi that bai! Vui long kiem tra ten va mat khau Wi-Fi.");
    }
}

void connectServer() {
    Serial.printf("[TCP] Dang ket noi toi Server %s:%d ...\n", SERVER_HOST, SERVER_PORT);
    if (tcpClient.connect(SERVER_HOST, SERVER_PORT)) {
        Serial.println("[TCP] >> KET NOI SERVER THANH CONG!");
    } else {
        Serial.println("[TCP] >> Ket noi Server that bai, se thu lai...");
    }
}
