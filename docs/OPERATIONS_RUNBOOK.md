# Operations runbook

## Mô hình an toàn

Web/API không được phép thực thi command tùy ý. `operations-service` chỉ ghi job và cấp claim có lease. `scripts/operations-agent.py` chạy trên host, chỉ nhận các loại thao tác cố định:

- `BACKUP` → `scripts/backup.sh`
- `RESTORE` → `scripts/restore.sh` với xác nhận cố định
- `MAINTENANCE ON/OFF` → dừng hoặc mở `web` và `api-gateway`
- `UPDATE/ROLLBACK` → fail-closed cho đến khi có signed-release adapter

Cổng agent của operations-service chỉ bind `127.0.0.1:8097`.

## Khởi động agent

Linux:

```bash
./scripts/start-operations-agent.sh
```

Windows PowerShell:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-operations-agent.ps1
```

Setup sẽ cố khởi động agent sau smoke test. Nếu máy chưa có Python 3, hệ thống học tập vẫn chạy nhưng job backup/restore sẽ ở trạng thái `REQUESTED`.

## Dừng agent

```bash
./scripts/stop-operations-agent.sh
```

hoặc:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stop-operations-agent.ps1
```

Log và PID nằm trong `.runtime/`, không được commit.

## Backup thủ công

```bash
./scripts/backup.sh
```

Backup gồm database, file storage, RabbitMQ definitions, cấu hình cần thiết và manifest phiên bản. Thư mục `backups/` bị loại khỏi Git.

## Restore

Chỉ thực hiện trong cửa sổ bảo trì và sau khi tạo thêm một bản backup hiện tại:

```bash
LMSPILOT_RESTORE_CONFIRMATION=RESTORE ./scripts/restore.sh backups/<timestamp>
```

Sau restore phải chạy:

```bash
./scripts/smoke-test.sh
```

Không mở lại thao tác ghi nếu smoke test/health check chưa đạt.
