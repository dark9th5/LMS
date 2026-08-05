"use client";

import type { PortalUser } from "@/lib/types";
import { CoursesPage } from "./CoursesPage";

/** Khóa học là không gian đào tạo duy nhất của sản phẩm. */
export function CourseWorkspace({ user }: { user: PortalUser }) {
  return <CoursesPage user={user} />;
}
