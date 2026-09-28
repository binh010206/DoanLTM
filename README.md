# 🅿️ Hệ Thống Bãi Đỗ Xe Thông Minh (Smart Parking System)
**Học phần: Lập Trình Mạng (Client - Server Phân Tán qua giao thức TCP)**  
**Trường Đại học Công nghệ Thông tin & Truyền thông Việt - Hàn (VKU Đà Nẵng)**

---

## 📁 Cấu Trúc Đồ Án
```
DoanLTM/
├── server/
│   ├── server.cpp          ← Core Server C++ (TCP:8888 + HTTP:9999 + Lưu JSON)
│   └── parking_data.json   ← File lưu trữ dữ liệu bền vững (Persistence)
├── client/
│   └── client.cpp          ← Mock Client C++ (Giả lập trạm quẹt thẻ RFID - 1 thẻ 1 mã)
├── bridge/
│   ├── bridge.js           ← WebSocket Bridge (Port 8080, đồng bộ thời gian thực)
│   └── package.json
└── dashboard/              ← Web Monitor (React/Vite - Giao diện Dark Cyber Glassmorphism)
    ├── src/App.jsx         ← Tab Admin, Tab Khách hàng, Tab Mô hình 7 tầng OSI
    └── src/App.css
```

---

## 🚀 Cách Chạy Đồ Án Trong VSCode (Mở 4 Tab Terminal song song)

> **Mẹo:** Trong VSCode, bấm dấu `+` ở góc Terminal để mở 4 tab hoặc dùng tính năng Split Terminal để xem cạnh nhau rất chuyên nghiệp, không cần bung popup ra màn hình ngoài.

### Tab 1: Khởi động Server C++ (Máy chủ trung tâm)
```powershell
cd "D:\Lập trình mạng\DoanLTM\server"
.\server.exe
```
*(Nếu sửa code C++, biên dịch lại bằng: `g++ -o server.exe server.cpp -lws2_32 -std=c++17`)*

### Tab 2: Khởi động WebSocket Bridge
```powershell
cd "D:\Lập trình mạng\DoanLTM\bridge"
node bridge.js
```

### Tab 3: Khởi động Web Dashboard
```powershell
cd "D:\Lập trình mạng\DoanLTM\dashboard"
npm run dev
```
👉 Mở trình duyệt tại: **http://localhost:3000**

### Tab 4: Chạy Client mô phỏng quẹt thẻ RFID
```powershell
cd "D:\Lập trình mạng\DoanLTM\client"
.\client.exe
```
*(Nếu sửa code C++, biên dịch lại bằng: `g++ -o client.exe client.cpp -lws2_32 -std=c++17`)*

---

## 🎯 Danh Sách Thẻ Định Danh (1 Thẻ = 1 Mã Xe Cố Định)
Hệ thống sử dụng các mã thẻ định danh duy nhất tương ứng với từng xe:
1. `A1B2C3D4`: Xe Honda AirBlade (Biển số: `43-C1 123.45`)
2. `FF00AB12`: Xe Honda Vision (Biển số: `43-D1 678.90`)
3. `11223344`: Xe Honda SH Mode (Biển số: `43-E1 999.88`)
4. `DEADBEEF`: Xe VinFast VF5 (Biển số: `43-A 555.66`)
5. `CAFE0001`: Xe Yamaha Exciter (Biển số: `43-K1 246.80`)

---

## 🌟 Các Tính Năng Đạt Điểm Cao Trong Đồ Án

1. **Lập trình Mạng C++ nguyên bản (Winsock2):**
   - Socket TCP truyền nhận dữ liệu qua cổng 8888.
   - Cơ chế đa luồng (`std::thread`) và khóa đồng bộ (`std::mutex`) tránh xung đột tài nguyên khi nhiều xe vào cùng lúc.
   - Socket HTTP tích hợp trên cổng 9999 phục vụ REST API `/status` và `/stats`.
2. **Lưu trữ dữ liệu bền vững (Data Persistence):**
   - Tự động lưu mọi trạng thái ô đỗ, lượt vào/ra, doanh thu vào file `server/parking_data.json`.
   - Khi tắt server và bật lại, toàn bộ dữ liệu xe đang đỗ vẫn được giữ nguyên vẹn.
3. **Cổng dịch vụ Khách Hàng (User Portal):**
   - Xem số chỗ trống thời gian thực.
   - Nhập hoặc bấm chọn mã thẻ RFID để tra cứu xe của mình đang đỗ ở ô nào, đỗ bao lâu và tiền gửi tạm tính.
   - Hỗ trợ mô phỏng quét mã QR thanh toán không chạm.
4. **Ánh xạ 7 tầng OSI tích hợp trực tiếp trên Web:**
   - Tab "Mô Hình 7 Tầng OSI" ngay trên trang web giúp trình bày và trả lời vấn đáp trực tiếp với giảng viên.
