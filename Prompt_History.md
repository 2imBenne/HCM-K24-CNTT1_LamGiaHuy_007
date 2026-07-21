# Lịch Sử Prompt (Prompt History)
### Prompt 1:
"Role: Bạn là một Senior Backend Developer chuyên về Java Spring Boot.
Context: Tôi có một dự án Base Code CoreBanking. Hiện dự án đã cấu hình kết nối Database và có sẵn các Entity cốt lõi là Customer và BankAccount.
Goal: Chúng ta cần bổ sung tính năng 'Kiểm soát hạn mức ngày (Daily Limit) và Cảnh báo' cho luồng chuyển khoản.
Constraint: Trong prompt này, bạn chưa cần viết code. Chỉ cần đọc hiểu base code, tóm tắt lại luồng chuyển khoản cơ bản và xác nhận bạn đã sẵn sàng.
Format: Trình bày câu trả lời dưới dạng gạch đầu dòng (bullet points)."

### Prompt 2:
"Bây giờ, hãy tạo file SRS.md để đặc tả yêu cầu cho tính năng Daily Limit này.
Hãy suy luận từng bước (Chain of Thought):
- Bước 1: Liệt kê các kịch bản ngoại lệ (What-if) có thể xảy ra khi giao dịch vượt hạn mức.
- Bước 2: Đề xuất cấu trúc lưu trữ (Database schema) cho bảng TransactionHistory để có thể truy vấn tổng tiền theo ngày. Đừng quên thêm biến dailyLimit vào tài khoản.
- Bước 3: Viết thuật toán bằng mã giả (Pseudo-code) mô tả logic cộng dồn số tiền giao dịch của khách hàng trong 24h của ngày hiện tại sao cho tối ưu nhất."

### Prompt 3:
"Tuyệt vời. Dựa trên bản thiết kế SRS vừa tạo, chúng ta bắt đầu lập trình.
Đầu tiên, hãy cập nhật Entity BankAccount để bổ sung trường dailyLimit (mặc định là 50,000,000 VNĐ). 
Sau đó, tạo mới Entity TransactionHistory bao gồm các trường: id, bankAccount, amount, transactionDate, và type."

### Prompt 4:
"Tiếp theo, hãy tạo TransactionHistoryRepository.
Yêu cầu bắt buộc: Để thuật toán chạy tối ưu và không kéo toàn bộ lịch sử giao dịch về RAM, hãy viết một hàm sử dụng @Query (JPQL) để tính tổng SUM(amount) của một tài khoản trong khoảng thời gian từ startOfDay đến endOfDay. 
Hãy chú ý xử lý trường hợp null (ví dụ dùng COALESCE) nếu khách hàng chưa có giao dịch nào trong ngày."

### Prompt 5:
"Hãy tạo các class DTO là TransferRequest (gồm toAccountNumber, amount) và UpdateLimitRequest (gồm newDailyLimit). Đồng thời tạo một Custom Exception tên là DailyLimitExceededException.

### Prompt 6:
Ví dụ: Giống như cách dự án đang dùng @NotBlank và @NotNull cho các request, hãy thêm các annotation validation tương tự cho số tiền (phải lớn hơn 0) và hạn mức mới."

### Prompt 7:
"Đóng vai là System Architect, hãy viết BankAccountService để xử lý logic chuyển tiền.
Ràng buộc cốt lõi (Constraints):
- Trước tiên phải kiểm tra số dư (balance) có đủ không.
- Phải gọi hàm tính sum() trong Repository mà bạn đã viết ở bước trước.
- Dùng cấu trúc If/Else: Nếu (tổng tiền đã chuyển trong ngày + số tiền giao dịch mới) > dailyLimit, bắt buộc phải throw DailyLimitExceededException.
- Nếu hợp lệ, tiến hành trừ tiền và lưu 2 bản ghi lịch sử giao dịch (TRANSFER_OUT và TRANSFER_IN).
Hãy vạch ra các bước logic dạng comment trong code trước khi viết logic thực sự."

### Prompt 8:
"Role: API Designer.
Goal: Tạo Controller để phơi bày tính năng ra ngoài.
Context: Service xử lý logic chuyển tiền và cập nhật hạn mức đã hoàn tất.
Constraint: Không phá vỡ cấu trúc RestController hiện tại.
Format: Viết mã nguồn Java cho BankAccountController chứa 2 endpoint: POST /api/v1/bankAccounts/{accountId}/transfer và PUT /api/v1/bankAccounts/{accountId}/limit. Gọi đúng các hàm từ Service."

### Prompt 9:
Dự án đã có sẵn GlobalExceptionHandler. 
Ràng buộc: Không được xóa bỏ các hàm xử lý lỗi cũ. Hãy bổ sung thêm một hàm @ExceptionHandler để bắt riêng DailyLimitExceededException. 
Đảm bảo rằng khi lỗi này xảy ra, hệ thống trả về mã HTTP Status Code 429 (Too Many Requests) cùng với thông báo chính xác là: 'Quý khách đã vượt hạn mức giao dịch trong ngày'."

### Prompt 10:
"Để trực quan hóa luồng dự án, hãy bổ sung thêm cho tôi sơ đồ Mermaid Flowchart vào file SRS.md để mô tả luồng thực thi nghiệp vụ (từ kiểm tra số dư đến bắt lỗi vượt hạn mức)."

### Prompt 11:
"Dự án đã xong tính năng, nhưng để đảm bảo độ tin cậy, hãy viết Unit Test cho tầng Service (BankAccountServiceTest.java).
Yêu cầu sử dụng JUnit 5, Mockito và tuân thủ chặt chẽ mô hình AAA.
Hãy giả lập (Mock) Repository để test đủ các kịch bản thực tế:
1. Giao dịch thành công.
2. Tổng tiền vừa bằng hạn mức.
3. Exception: Vượt hạn mức (Trả về lỗi DailyLimitExceededException).
4. Exception: Số dư không đủ (Insufficient Balance).
5. Exception: Không tìm thấy tài khoản (Account Not Found)."

### Prompt 12:
"Để chứng minh tầng Controller xử lý ngoại lệ HTTP 429 chính xác, hãy viết thêm BankAccountControllerTest.java.
Sử dụng MockMvcBuilders.standaloneSetup kết hợp với GlobalExceptionHandle.
Hãy viết test giả lập tình huống: Gửi request chuyển khoản vượt hạn mức, bắt Service ném lỗi `DailyLimitExceededException và Assert rằng Controller trả về đúng mã HTTP Status 429 Too Many Requests kèm câu thông báo chuẩn."