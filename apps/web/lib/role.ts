import type { PortalUser } from "./types";
import { landingForUser } from "./authorization";

/** @deprecated Quyền hiệu lực, không phải tên role, quyết định trang bắt đầu. */
export function landingForRoles(_roles: string[] | undefined): string {
  return "/news";
}

export function landingForSession(user: PortalUser): string {
  return landingForUser(user);
}
