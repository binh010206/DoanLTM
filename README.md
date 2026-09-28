#  Hệ Thống Bãi Đỗ Xe Thông Minh (Smart Parking System)
Học phần: Lập Trình Mạng (Client - Server Phân Tán qua giao thức TCP)
Trường Đại học Công nghệ Thông tin & Truyền thông Việt - Hàn (VKU Đà Nẵng)


#  Cấu Trúc Đồ Án
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


