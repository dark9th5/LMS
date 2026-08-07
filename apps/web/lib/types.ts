import type { PortalRole } from "./role";

export type PortalUser = {
  id: string;
  code?: string;
  username: string;
  fullName: string;
  email?: string | null;
  organizationUnitId?: string | null;
  status?: string;
  accountType?: "SYSTEM_ADMIN" | "USER";
  protectedAccount?: boolean;
  roles: PortalRole[] | string[];
  primaryRole?: PortalRole;
  permissions: string[];
  lastLoginAt?: string | null;
  mustChangePassword?: boolean;
};

export type IconName =
  | "dashboard"
  | "book"
  | "users"
  | "building"
  | "learn"
  | "exam"
  | "grade"
  | "report"
  | "certificate"
  | "operations"
  | "settings"
  | "bell"
  | "search"
  | "arrow"
  | "plus"
  | "menu"
  | "close"
  | "clock"
  | "check"
  | "edit"
  | "play"
  | "upload"
  | "chevron"
  | "logout"
  | "eye"
  | "calendar"
  | "more"
  | "warning"
  | "back"
  | "refresh"
  | "file"
  | "download"
  | "filter"
  | "trash"
  | "lock"
  | "unlock"
  | "list"
  | "target"
  | "question"
  | "save"
  | "link";
