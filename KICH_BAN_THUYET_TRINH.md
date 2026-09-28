# 🎓 KỊCH BẢN THUYẾT TRÌNH BẢO VỆ ĐỒ ÁN LẬP TRÌNH MẠNG
**Đề tài:** Xây dựng hệ thống quản lý bãi đỗ xe thông minh theo mô hình Client - Server phân tán qua giao thức TCP  
**Đơn vị:** Trường Đại học Công nghệ Thông tin & Truyền thông Việt - Hàn (VKU Đà Nẵng)

---

## 🕒 PHẦN 1: CÁC BƯỚC CHUẨN BỊ TRƯỚC KHI LÊN BÁO CÁO

Để không bị bối rối hay văng lỗi trước mặt thầy cô, bạn mở 4 tab terminal trong **VSCode** theo đúng thứ tự sau:

1. **Terminal 1 (Server C++):**
   ```powershell
   cd "D:\Lập trình mạng\DoanLTM\server"
   .\server.exe
   ```
   *(Thấy màn hình hiện thông báo Server v3.0, TCP:8888, HTTP:9999 là thành công)*

2. **Terminal 2 (Bridge WebSocket):**
   ```powershell
   cd "D:\Lập trình mạng\DoanLTM\bridge"
   node bridge.js
   ```

3. **Terminal 3 (Dashboard Web):**
   ```powershell
   cd "D:\Lập trình mạng\DoanLTM\dashboard"
   npm run dev
   ```
   *(Mở trình duyệt vào `http://localhost:3000`, chia màn hình: một nửa là VSCode Terminal, một nửa là trình duyệt Chrome)*

4. **Terminal 4 (Client mô phỏng quẹt thẻ):**
   ```powershell
   cd "D:\Lập trình mạng\DoanLTM\client"
   .\client.exe
   ```

---

## 🎤 PHẦN 2: LỜI THOẠI TRÌNH BÀY (KỊCH BẢN NÓI MẪU)

### 1. Mở đầu & Giới thiệu đề tài (1 phút)
> *"Kính thưa Thầy/Cô và các bạn, nhóm em xin phép báo cáo đề tài: **Xây dựng hệ thống bãi đỗ xe thông minh theo mô hình Client - Server phân tán qua giao thức TCP**.  
> Mục tiêu cốt lõi của đề tài là áp dụng kiến thức lập trình mạng hướng socket trong C++, quản lý đa luồng (multi-threading), đồng bộ dữ liệu và tích hợp giao diện giám sát thời gian thực."*

### 2. Trình bày kiến trúc phân tán & Luồng mạng (1.5 phút)
> *(Chuyển sang Tab **"🌐 Mô Hình 7 Tầng OSI"** trên trang web)*  
> *"Hệ thống của tụi em gồm 3 thành phần phân tán độc lập trên mạng:*  
> 1. ***Client (Trạm quẹt thẻ):*** *Viết bằng C++, mô phỏng vi điều khiển ESP32 và đầu đọc RFID. Kết nối tới Server qua TCP Socket cổng 8888.*  
> 2. ***Server trung tâm:*** *Viết bằng C++ nguyên bản sử dụng thư viện Winsock2. Đảm nhận việc mở cổng TCP 8888 lắng nghe các trạm quẹt thẻ, quản lý 3 ô đỗ xe bằng biến cấu trúc có khóa `std::mutex` để chống Race Condition, tự động tính phí gửi xe và lưu dữ liệu bền vững ra file `parking_data.json`.*  
> 3. ***Lớp Web Monitor & Cổng dịch vụ:*** *Gồm WebSocket Bridge trung gian (cổng 8080) và giao diện React giúp phản ánh trạng thái bãi xe tức thì (Zero-latency) mà không cần người dùng phải bấm F5 tải lại trang."*

### 3. Thực hành Demo Live trước mặt thầy cô (2 phút)
> *(Chuyển sang Tab **"📊 Quản Trị Bãi Xe"** trên web, song song với màn hình Client ở Terminal 4)*

- **Thao tác 1 (Xe vào):**  
  Ở Terminal Client, bạn nhập `1` (Quẹt xe vào) → chọn thẻ `1` (`A1B2C3D4` - Xe Honda AirBlade).  
  *Lời thoại:* *"Khi khách quẹt thẻ, Client đóng gói bản tin `ENTRY|A1B2C3D4\n` và gửi qua socket TCP. Server nhận được, kiểm tra ô 1 còn trống, cập nhật trạng thái và phản hồi `OK|SLOT_1`. Ngay lập tức, trên web ô số 1 chuyển sang màu đỏ báo Có Xe, hiển thị biển số xe, mã RFID và thời gian đỗ."*

- **Thao tác 2 (Quẹt thêm xe vào ô 2 và ô 3):**  
  Thao tác thêm xe `FF00AB12` và `11223344` để bãi xe đầy.  
  *Lời thoại:* *"Khi cả 3 ô đều có xe, bãi xe chuyển sang trạng thái ĐÃ ĐẦY. Nếu có xe thứ 4 cố tình quẹt thẻ, Server sẽ trả về `FULL` và ghi nhận sự kiện TỪ CHỐI vào bảng nhật ký."*

- **Thao tác 3 (Chuyển sang Tab Khách Hàng):**  
  Click vào Tab **"🚗 Cổng Khách Hàng"** trên web.  
  *Lời thoại:* *"Đây là cổng dịch vụ dành cho người gửi xe. Người dùng có thể bấm vào mã thẻ hoặc gõ mã thẻ để biết xe mình đang đỗ ở ô mấy, đỗ bao lâu và tiền gửi tạm tính. Đồng thời có thể bấm nút quét mã QR VietQR để thanh toán không chạm."*

- **Thao tác 4 (Xe ra & Tính tiền):**  
  Ở Terminal Client, nhập `2` (Quẹt xe ra) → chọn thẻ `1` (`A1B2C3D4`).  
  *Lời thoại:* *"Khi xe ra, Client gửi lệnh `EXIT|A1B2C3D4`. Server tính toán thời gian đỗ, áp dụng biểu phí 5.000đ/phút, giải phóng ô số 1, cộng doanh thu và trả về thông tin tiền phí. Trên web, ô số 1 lập tức chuyển về màu xanh TRỐNG sẵn sàng đón xe mới."*

- **Thao tác 5 (Chứng minh tính bền vững dữ liệu - Điểm cộng lớn):**  
  *Lời thoại:* *"Đặc biệt, hệ thống có cơ chế Data Persistence. Kể cả khi Server bị sập nguồn hoặc tắt đột ngột, toàn bộ trạng thái xe đang đỗ và doanh thu đã được lưu vào `parking_data.json`. Khi bật server lại, mọi thông tin được khôi phục 100%."*

---

## 🎯 PHẦN 3: BỘ CÂU HỎI VẤN ĐÁP THẦY CÔ HAY HỎI & CÁCH TRẢ LỜI

### ❓ Câu 1: Tại sao em dùng TCP mà không dùng UDP cho trạm quẹt thẻ RFID?
> **Trả lời:**  
> *"Dạ thưa Thầy/Cô, việc quẹt thẻ xe và mở cổng barie đòi hỏi tính **chính xác và toàn vẹn 100%**. Giao thức TCP có cơ chế Bắt tay 3 bước (3-way handshake), kiểm soát luồng (flow control) và tự động truyền lại gói tin khi bị mất (retransmission). Nếu dùng UDP, gói tin quẹt thẻ có thể bị rơi rớt trên đường truyền mạng dẫn tới việc xe đã quẹt thẻ nhưng cổng không mở hoặc sai lệch tiền phí."*

### ❓ Câu 2: Tính "Phân tán" (Distributed) trong đồ án này thể hiện ở chỗ nào?
> **Trả lời:**  
> *"Dạ, tính phân tán thể hiện ở việc các thành phần tách biệt hoàn toàn về tiến trình (Process) và địa chỉ mạng:  
> - Các trạm Client (cổng vào/cổng ra) có thể đặt ở các cổng vật lý khác nhau chạy trên vi điều khiển ESP32 độc lập.  
> - Server trung tâm đặt tại phòng điều hành quản lý cơ sở dữ liệu chung.  
> - Web dashboard có thể được mở bởi bảo vệ hoặc người quản lý trên bất kỳ máy tính nào qua mạng LAN/Internet.  
> Các nút mạng này giao tiếp phân tán với nhau qua giao thức TCP Socket cổng 8888 và WebSocket cổng 8080."*

### ❓ Câu 3: Làm sao giải quyết vấn đề khi có 2 xe cùng quẹt thẻ vào bãi tại cùng 1 tích tắc?
> **Trả lời:**  
> *"Dạ, Server C++ của tụi em tạo mỗi kết nối Client trên một Thread riêng biệt (`std::thread`). Để tránh hiện tượng **Race Condition** (hai luồng cùng tranh chấp cấp phát 1 ô đỗ còn trống), tụi em sử dụng `std::mutex` với `std::lock_guard<std::mutex> lock(g_mutex)` để bảo vệ vùng tài nguyên chung `g_slots`. Nhờ đó, thao tác kiểm tra và cấp phát ô được thực hiện tuần tự và an toàn luồng (Thread-safe)."*

### ❓ Câu 4: WebSocket khác gì so với HTTP thông thường trong việc cập nhật web?
> **Trả lời:**  
> *"Dạ, HTTP truyền thống hoạt động theo mô hình Request/Response bán song công, Client phải liên tục hỏi server (Polling) gây lãng phí băng thông. Trong khi đó, **WebSocket (RFC 6455)** là kênh giao tiếp song công toàn phần (Full-duplex) duy trì trên một kết nối TCP duy nhất. Khi Server C++ có sự kiện xe vào/ra, WebSocket Bridge sẽ chủ động `push` dữ liệu xuống Web ngay lập tức, độ trễ gần như bằng 0."*

### ❓ Câu 5: Em ánh xạ 7 tầng OSI vào đồ án này như thế nào?
> **Trả lời:**  
> *(Chỉ tay vào Tab **Mô hình 7 tầng OSI** trên Dashboard Web)*  
> *"Dạ:  
> - **Tầng 7 (Application):** Giao thức bản tin tự định nghĩa `ENTRY|RFID` và REST API JSON.  
> - **Tầng 6 (Presentation):** Định dạng JSON và mã hóa chuỗi ASCII.  
> - **Tầng 5 (Session):** Phiên làm việc duy trì Socket File Descriptor giữa Client và Server.  
> - **Tầng 4 (Transport):** Giao thức TCP tin cậy cổng 8888 và 9999.  
> - **Tầng 3 (Network):** Địa chỉ IP nguồn và đích (IPv4).  
> - **Tầng 2 (Data Link):** Đóng gói Ethernet/Wi-Fi frame mang địa chỉ MAC.  
> - **Tầng 1 (Physical):** Tín hiệu sóng Radio RFID 13.56MHz và cáp mạng LAN."*
