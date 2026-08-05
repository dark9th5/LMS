"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import { apiRequest, createIdempotencyKey } from "@/lib/api";
import { Icon } from "./Icon";
import { ErrorState, Toast } from "./Feedback";
import { PageHeader } from "./PageHeader";

type DetectedMapping = {
  codeColumn?: string | null;
  usernameColumn?: string | null;
  fullNameColumn?: string | null;
  emailColumn?: string | null;
  organizationUnitIdColumn?: string | null;
  roleCodesColumn?: string | null;
  passwordColumn?: string | null;
  statusColumn?: string | null;
};
type Inspection = {
  fileName: string;
  headers: string[];
  samples: Record<string, string>[];
  detectedMapping: DetectedMapping;
};
type Mapping = {
  codeColumn: string;
  usernameColumn: string;
  fullNameColumn: string;
  emailColumn: string | null;
  organizationUnitIdColumn: string | null;
  roleCodesColumn: string | null;
  passwordColumn: string | null;
  statusColumn: string | null;
  defaultRoleCodes: string[];
  defaultPassword: string | null;
  mode: "CREATE_ONLY" | "UPSERT";
  failurePolicy: "PARTIAL" | "ATOMIC";
  updatePasswordOnUpsert: boolean;
};
type PreviewRow = {
  rowNumber: number;
  code: string;
  username: string;
  fullName: string;
  roleCodes: string[];
  action: string;
  valid: boolean;
  errors: string[];
};
type Preview = {
  fileName: string;
  totalRows: number;
  validRows: number;
  invalidRows: number;
  creates: number;
  updates: number;
  rows: PreviewRow[];
};
type Commit = {
  operationId: string;
  totalRows: number;
  created: number;
  updated: number;
  skipped: number;
  failed: number;
  committed: boolean;
  results: Array<{
    rowNumber: number;
    code: string;
    username: string;
    action: string;
    success: boolean;
    errors: string[];
  }>;
};

const emptyMapping: Mapping = {
  codeColumn: "code",
  usernameColumn: "username",
  fullNameColumn: "fullName",
  emailColumn: null,
  organizationUnitIdColumn: null,
  roleCodesColumn: null,
  passwordColumn: null,
  statusColumn: null,
  defaultRoleCodes: ["STUDENT"],
  defaultPassword: null,
  mode: "CREATE_ONLY",
  failurePolicy: "PARTIAL",
  updatePasswordOnUpsert: false,
};

function jsonPart(value: unknown) {
  return new Blob([JSON.stringify(value)], { type: "application/json" });
}

export function UserImportWizard() {
  const [file, setFile] = useState<File | null>(null);
  const [inspection, setInspection] = useState<Inspection | null>(null);
  const [mapping, setMapping] = useState<Mapping>(emptyMapping);
  const [preview, setPreview] = useState<Preview | null>(null);
  const [result, setResult] = useState<Commit | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [toast, setToast] = useState("");

  const requiredMappingReady = Boolean(
    mapping.codeColumn && mapping.usernameColumn && mapping.fullNameColumn,
  );
  const columns = useMemo(() => inspection?.headers ?? [], [inspection]);

  async function inspect(selected: File | null) {
    setFile(selected);
    setInspection(null);
    setPreview(null);
    setResult(null);
    setError("");
    if (!selected) return;
    setBusy(true);
    try {
      const form = new FormData();
      form.append("file", selected);
      const response = await apiRequest<Inspection>(
        "/api/v1/users/import/inspect",
        { method: "POST", body: form },
      );
      setInspection(response);
      const detected = response.detectedMapping;
      setMapping((current) => ({
        ...current,
        codeColumn: detected.codeColumn ?? current.codeColumn,
        usernameColumn: detected.usernameColumn ?? current.usernameColumn,
        fullNameColumn: detected.fullNameColumn ?? current.fullNameColumn,
        emailColumn: detected.emailColumn ?? null,
        organizationUnitIdColumn: detected.organizationUnitIdColumn ?? null,
        roleCodesColumn: detected.roleCodesColumn ?? null,
        passwordColumn: detected.passwordColumn ?? null,
        statusColumn: detected.statusColumn ?? null,
      }));
    } catch (caught) {
      setError(
        caught instanceof Error ? caught.message : "Không thể đọc tệp nhập",
      );
    } finally {
      setBusy(false);
    }
  }

  function buildForm() {
    if (!file) throw new Error("Vui lòng chọn tệp CSV hoặc XLSX");
    const form = new FormData();
    form.append("file", file);
    form.append("mapping", jsonPart(mapping));
    return form;
  }

  async function runPreview() {
    setBusy(true);
    setError("");
    setResult(null);
    try {
      setPreview(
        await apiRequest<Preview>("/api/v1/users/import/preview", {
          method: "POST",
          body: buildForm(),
        }),
      );
    } catch (caught) {
      setError(
        caught instanceof Error
          ? caught.message
          : "Không thể xem trước dữ liệu",
      );
    } finally {
      setBusy(false);
    }
  }

  async function commit() {
    setBusy(true);
    setError("");
    try {
      const form = buildForm();
      form.append("operationId", createIdempotencyKey("user-import"));
      const response = await apiRequest<Commit>("/api/v1/users/import/commit", {
        method: "POST",
        body: form,
      });
      setResult(response);
      setToast(
        response.committed
          ? "Đã hoàn tất nhập tài khoản."
          : "Tệp chưa được nhập vì còn lỗi.",
      );
    } catch (caught) {
      setError(
        caught instanceof Error ? caught.message : "Không thể nhập tài khoản",
      );
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="import-wizard">
      <PageHeader
        backHref="/users"
        eyebrow="Quản lý người dùng"
        title="Nhập tài khoản hàng loạt"
        description="Đọc CSV/XLSX, ánh xạ cột, kiểm tra từng dòng và nhập idempotent theo chính sách đã chọn."
      />
      <section className="workspace-panel import-step">
        <header>
          <span>1</span>
          <div>
            <h2>Chọn tệp nguồn</h2>
            <p>Tối đa 8 MB và 5.000 dòng. Dòng đầu tiên phải là tiêu đề.</p>
          </div>
        </header>
        <div className="import-template-link">
          <a
            className="button secondary"
            href="/templates/user-import-template.csv"
            download
          >
            <Icon name="download" />
            Tải tệp CSV mẫu
          </a>
          <small>
            Tệp mẫu minh họa cả một người có nhiều vai trò. Hãy thay mật khẩu
            mẫu trước khi nhập.
          </small>
        </div>
        <label className="assignment-drop">
          <input
            type="file"
            accept=".csv,.xlsx,text/csv,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            onChange={(event) => void inspect(event.target.files?.[0] ?? null)}
          />
          <Icon name="upload" size={28} />
          <span>
            <strong>{file?.name ?? "Chọn tệp CSV hoặc XLSX"}</strong>
            <small>
              Hệ thống không thực thi macro hay công thức trong tệp.
            </small>
          </span>
        </label>
      </section>
      {inspection && (
        <section className="workspace-panel import-step">
          <header>
            <span>2</span>
            <div>
              <h2>Ánh xạ dữ liệu</h2>
              <p>
                Hệ thống đã tự nhận diện các cột phổ biến; bạn có thể điều chỉnh
                trước khi xem trước.
              </p>
            </div>
          </header>
          <div className="import-grid">
            <ColumnSelect
              label="Mã người dùng *"
              value={mapping.codeColumn}
              columns={columns}
              onChange={(value) =>
                setMapping({ ...mapping, codeColumn: value ?? "" })
              }
            />
            <ColumnSelect
              label="Tên đăng nhập *"
              value={mapping.usernameColumn}
              columns={columns}
              onChange={(value) =>
                setMapping({ ...mapping, usernameColumn: value ?? "" })
              }
            />
            <ColumnSelect
              label="Họ và tên *"
              value={mapping.fullNameColumn}
              columns={columns}
              onChange={(value) =>
                setMapping({ ...mapping, fullNameColumn: value ?? "" })
              }
            />
            <ColumnSelect
              label="Email"
              value={mapping.emailColumn}
              columns={columns}
              optional
              onChange={(value) =>
                setMapping({ ...mapping, emailColumn: value })
              }
            />
            <ColumnSelect
              label="Mã đơn vị tổ chức"
              value={mapping.organizationUnitIdColumn}
              columns={columns}
              optional
              onChange={(value) =>
                setMapping({ ...mapping, organizationUnitIdColumn: value })
              }
            />
            <ColumnSelect
              label="Gói quyền"
              value={mapping.roleCodesColumn}
              columns={columns}
              optional
              onChange={(value) =>
                setMapping({ ...mapping, roleCodesColumn: value })
              }
            />
            <ColumnSelect
              label="Mật khẩu"
              value={mapping.passwordColumn}
              columns={columns}
              optional
              onChange={(value) =>
                setMapping({ ...mapping, passwordColumn: value })
              }
            />
            <ColumnSelect
              label="Trạng thái"
              value={mapping.statusColumn}
              columns={columns}
              optional
              onChange={(value) =>
                setMapping({ ...mapping, statusColumn: value })
              }
            />
            <label>
              <span>Vai trò mặc định</span>
              <select
                value={mapping.defaultRoleCodes[0] ?? "STUDENT"}
                onChange={(event) =>
                  setMapping({ ...mapping, defaultRoleCodes: [event.target.value] })
                }
              >
                <option value="ADMIN">Quản trị viên</option>
                <option value="INSTRUCTOR">Giảng viên</option>
                <option value="STUDENT">Học viên</option>
              </select>
              <small>Mỗi tài khoản chỉ có đúng một vai trò.</small>
            </label>
            <label>
              <span>Mật khẩu mặc định</span>
              <input
                type="password"
                minLength={12}
                value={mapping.defaultPassword ?? ""}
                onChange={(event) =>
                  setMapping({
                    ...mapping,
                    defaultPassword: event.target.value || null,
                  })
                }
                placeholder="Ít nhất 12 ký tự"
              />
            </label>
            <label>
              <span>Chế độ</span>
              <select
                value={mapping.mode}
                onChange={(event) =>
                  setMapping({
                    ...mapping,
                    mode: event.target.value as Mapping["mode"],
                  })
                }
              >
                <option value="CREATE_ONLY">Chỉ tạo mới</option>
                <option value="UPSERT">Tạo mới hoặc cập nhật</option>
              </select>
            </label>
            <label>
              <span>Khi có dòng lỗi</span>
              <select
                value={mapping.failurePolicy}
                onChange={(event) =>
                  setMapping({
                    ...mapping,
                    failurePolicy: event.target
                      .value as Mapping["failurePolicy"],
                  })
                }
              >
                <option value="PARTIAL">Nhập các dòng hợp lệ</option>
                <option value="ATOMIC">Không nhập toàn bộ tệp</option>
              </select>
            </label>
            {mapping.mode === "UPSERT" && (
              <label className="check-row">
                <input
                  type="checkbox"
                  checked={mapping.updatePasswordOnUpsert}
                  onChange={(event) =>
                    setMapping({
                      ...mapping,
                      updatePasswordOnUpsert: event.target.checked,
                    })
                  }
                />
                <span>
                  Cập nhật mật khẩu của tài khoản đã tồn tại khi cột mật khẩu có
                  giá trị
                </span>
              </label>
            )}
          </div>
          <div className="import-actions">
            <button
              className="button primary"
              disabled={busy || !requiredMappingReady}
              onClick={() => void runPreview()}
            >
              <Icon name="search" />
              Kiểm tra và xem trước
            </button>
          </div>
        </section>
      )}
      {preview && (
        <section className="workspace-panel import-step">
          <header>
            <span>3</span>
            <div>
              <h2>Xác nhận kết quả kiểm tra</h2>
              <p>
                {preview.validRows} dòng hợp lệ · {preview.invalidRows} dòng lỗi
                · {preview.creates} tạo mới · {preview.updates} cập nhật.
              </p>
            </div>
          </header>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Dòng</th>
                  <th>Tài khoản</th>
                  <th>Họ tên</th>
                  <th>Vai trò</th>
                  <th>Hành động</th>
                  <th>Kết quả</th>
                </tr>
              </thead>
              <tbody>
                {preview.rows.map((row) => (
                  <tr key={row.rowNumber}>
                    <td>{row.rowNumber}</td>
                    <td>
                      <b>{row.username}</b>
                      <small>{row.code}</small>
                    </td>
                    <td>{row.fullName}</td>
                    <td>{row.roleCodes.join(", ")}</td>
                    <td>{row.action}</td>
                    <td>
                      {row.valid ? (
                        <span className="status-pill success">Hợp lệ</span>
                      ) : (
                        <span className="import-errors">
                          {row.errors.join(" · ")}
                        </span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="import-actions">
            <button
              className="button primary"
              disabled={
                busy ||
                preview.validRows === 0 ||
                (mapping.failurePolicy === "ATOMIC" && preview.invalidRows > 0)
              }
              onClick={() => void commit()}
            >
              <Icon name="check" />
              {busy ? "Đang nhập..." : "Xác nhận nhập dữ liệu"}
            </button>
          </div>
        </section>
      )}
      {result && (
        <section className="workspace-panel import-step">
          <header>
            <span>4</span>
            <div>
              <h2>Kết quả nhập</h2>
              <p>
                Tạo {result.created} · cập nhật {result.updated} · bỏ qua{" "}
                {result.skipped} · lỗi {result.failed}.
              </p>
            </div>
          </header>
          {result.failed > 0 && (
            <div className="import-result-errors">
              {result.results
                .filter((row) => !row.success)
                .map((row) => (
                  <p key={row.rowNumber}>
                    <b>
                      Dòng {row.rowNumber} ({row.username || row.code}):
                    </b>{" "}
                    {row.errors.join(" · ")}
                  </p>
                ))}
            </div>
          )}
          <div className="import-actions">
            <Link className="button primary" href="/admin/users">
              Quay lại danh sách người dùng
            </Link>
          </div>
        </section>
      )}
      {error && <ErrorState message={error} />}{" "}
      {toast && <Toast message={toast} onClose={() => setToast("")} />}
    </div>
  );
}

function ColumnSelect({
  label,
  value,
  columns,
  optional = false,
  onChange,
}: {
  label: string;
  value?: string | null;
  columns: string[];
  optional?: boolean;
  onChange: (value: string | null) => void;
}) {
  return (
    <label>
      <span>{label}</span>
      <select
        value={value ?? ""}
        onChange={(event) => onChange(event.target.value || null)}
      >
        <option value="">{optional ? "Không sử dụng" : "Chọn cột"}</option>
        {columns.map((column) => (
          <option key={column} value={column}>
            {column}
          </option>
        ))}
      </select>
    </label>
  );
}
