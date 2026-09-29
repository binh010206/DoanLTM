# Hệ Thống Bãi Đỗ Xe Thông Minh (Smart Parking System)
**Học phần**: Lập Trình Mạng (Client - Server Phân Tán qua giao thức TCP Socket)  
**Trường**: Đại học Công nghệ Thông tin & Truyền thông Việt - Hàn (VKU Đà Nẵng)

---

## 📁 Cấu Trúc Đồ Án
```
DoanLTM/
├── server/
│   ├── server.cpp              ← Core Server C++ (TCP Port 8888 đa luồng + HTTP Port 9999 REST API)
│   ├── server.exe              ← File thực thi Server
│   └── parking_data.json       ← File lưu trữ dữ liệu bền vững (Persistence)
├── client/
│   ├── client.cpp              ← Client C++ (Máy trạm quẹt thẻ RFID 5 thẻ khách)
│   └── client.exe              ← File thực thi Client
├── monitor/
│   ├── ParkingMonitor.java     ← Giao diện Desktop Giám sát trung tâm (Java Swing Dark Theme)
│   └── ParkingMonitor.class    ← Đã biên dịch Java 21
├── esp32/
│   └── esp32_gate/
│       └── esp32_gate.ino      ← Mã nguồn Arduino cho mạch ESP32 + RFID RC522 + Servo Barie
├── run-server.bat              ← Khởi động Server C++
├── run-gui.bat                 ← Khởi động Giao diện Desktop Monitor (Java Swing)
├── run-client.bat              ← Khởi động Máy trạm Client C++
└── README.md
```

---

## 🚀 Hướng Dẫn Vận Hành

### 1. Máy Chủ (Laptop Server)
1. Chạy `run-server.bat` để bật Server C++ (TCP Port `8888`, HTTP Port `9999`).
2. Chạy `run-gui.bat` để mở màn hình giám sát trung tâm Desktop GUI (Java Swing).

### 2. Máy Trạm Cổng Ra (Laptop Client)
1. Kết nối cùng mạng Wi-Fi với Server.
2. Chạy `run-client.bat`, nhập IP Server hiển thị trên thanh tiêu đề GUI (ví dụ: `10.187.149.136`).
3. Chọn thẻ để thực hiện quẹt xe ra, tính cước phí và mở barie.

### 3. Trạm Phần Cứng IoT (ESP32 Cổng Vào)
1. Mở file `esp32/esp32_gate/esp32_gate.ino` bằng **Arduino IDE**.
2. Cài 2 thư viện: `MFRC522` và `ESP32Servo`.
3. Điền thông tin Wi-Fi (`Redmi 13`, mật khẩu `binh010206`) và IP Server.
4. Nạp code vào ESP32. Khi quẹt thẻ RFID thật, ESP32 sẽ gửi gói tin TCP đến Server và mở barie.
