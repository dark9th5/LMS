export type PortalUser = {
  id: string;
  code?: string;
  username: string;
  fullName: string;
  email?: string | null;
  organizationUnitId?: string | null;
  status?: string;
  roles: string[];
  permissions: string[];
  lastLoginAt?: string | null;
};

export type IconName =
  | "dashboard" | "book" | "users" | "building" | "class" | "learn" | "exam"
  | "grade" | "report" | "certificate" | "operations" | "settings" | "bell"
  | "search" | "arrow" | "plus" | "menu" | "close" | "clock" | "check"
  | "edit" | "play" | "upload" | "chevron" | "logout" | "eye" | "calendar"
  | "more" | "warning" | "back" | "refresh" | "file" | "download" | "filter"
  | "trash" | "lock" | "unlock" | "list" | "target" | "question" | "save";
