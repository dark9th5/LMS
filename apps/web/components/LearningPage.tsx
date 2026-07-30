"use client";

import Link from "next/link";
import { useCallback, useEffect, useMemo, useState } from "react";
import { apiRequest, unwrapItems } from "@/lib/api";
import type { Course, CourseProgress, PageResponse } from "@/lib/models";
import { formatDate, formatDuration } from "@/lib/models";
import type { PortalUser } from "@/lib/types";
import { Icon } from "./Icon";
import { PageHeader } from "./PageHeader";
import { EmptyState, ErrorState, LoadingState } from "./Feedback";
import { ProgressBar } from "./ProgressBar";
import { StatusBadge } from "./StatusBadge";

export function LearningPage({ user }: { user: PortalUser }) {
  const [rows, setRows] = useState<CourseProgress[]>([]);
  const [courses, setCourses] = useState<Course[]>([]);
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    setLoading(true); setError("");
    try {
      const [progress, coursePayload] = await Promise.all([
        apiRequest<unknown>("/api/v1/learning/me"),
        apiRequest<unknown>("/api/v1/courses?size=100"),
      ]);
      setRows(unwrapItems<CourseProgress>(progress as any));
      setCourses(unwrapItems<Course>(coursePayload as any));
    } catch (caught) { setError(caught instanceof Error ? caught.message : "Không thể tải dữ liệu học tập"); }
    finally { setLoading(false); }
  }, []);
  useEffect(() => { void load(); }, [load]);

  const courseList = useMemo(() => Array.isArray(courses) ? courses : unwrapItems<Course>(courses as any), [courses]);
  const rowList = useMemo(() => Array.isArray(rows) ? rows : unwrapItems<CourseProgress>(rows as any), [rows]);
  const map = useMemo(() => new Map(courseList.map((course) => [course.id, course])), [courseList]);
  const filtered = useMemo(() => rowList.filter((row) => {
    const course = map.get(row.courseId);
    const matchesQuery = !query.trim() || `${course?.name ?? ""} ${course?.code ?? ""}`.toLowerCase().includes(query.trim().toLowerCase());
    return matchesQuery && (!status || row.status === status);
  }), [map, query, rowList, status]);
  const completed = rowList.filter((row) => row.status === "COMPLETED").length;
  const average = rowList.length ? Math.round(rowList.reduce((sum, row) => sum + row.progressPercent, 0) / rowList.length) : 0;

  return <>
    <PageHeader eyebrow="KHÔNG GIAN HỌC TẬP" title={`Xin chào, ${user.fullName}`} description="Tiếp tục các khóa học được giao và theo dõi tiến độ cá nhân." icon="learn"/>
    <section className="learner-hero">
      <div><span>Tiến độ tổng quan</span><strong>{average}%</strong><p>{completed}/{rows.length} khóa học đã hoàn thành</p></div>
      <div className="hero-progress"><ProgressBar value={average}/><small>Tiến độ được lưu trên máy chủ và có thể tiếp tục từ thiết bị khác trong cùng mạng LAN.</small></div>
    </section>
    <section className="toolbar-card">
      <label className="search-field"><Icon name="search"/><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Tìm khóa học của bạn"/></label>
      <label className="select-field"><Icon name="filter"/><select value={status} onChange={(event) => setStatus(event.target.value)}><option value="">Tất cả trạng thái</option><option value="NOT_STARTED">Chưa bắt đầu</option><option value="IN_PROGRESS">Đang học</option><option value="COMPLETED">Đã hoàn thành</option><option value="OVERDUE">Quá hạn</option></select></label>
      <button className="button secondary" onClick={() => void load()}><Icon name="refresh"/>Làm mới</button>
    </section>
    {loading ? <LoadingState/> : error ? <ErrorState message={error} onRetry={() => void load()}/> : filtered.length === 0 ? <EmptyState title="Chưa có khóa học phù hợp" description="Khóa học sẽ xuất hiện khi quản trị viên ghi danh bạn vào một lớp đào tạo."/> :
      <section className="learning-grid">{filtered.map((row, index) => {
        const course = map.get(row.courseId);
        return <Link className="learning-card" href={`/learning/${row.enrollmentId}`} key={row.enrollmentId}>
          <div className={`learning-cover cover-${index % 5}`}><span>{course?.code ?? "COURSE"}</span><Icon name="learn" size={34}/></div>
          <div className="learning-card-body"><div className="learning-card-status"><StatusBadge value={row.status}/><span>{formatDate(row.lastAccessedAt)}</span></div><h2>{course?.name ?? "Khóa học"}</h2><p>{course?.description ?? "Mở khóa học để xem nội dung và tiếp tục học."}</p><ProgressBar value={row.progressPercent} label="Tiến độ"/><div className="course-meta"><span><Icon name="clock"/>{formatDuration(course?.durationMinutes)}</span><span><Icon name="calendar"/>Cập nhật {formatDate(row.lastAccessedAt)}</span></div><div className="learning-action"><span>{row.status === "NOT_STARTED" ? "Bắt đầu học" : row.status === "COMPLETED" ? "Xem lại khóa học" : "Tiếp tục học"}</span><Icon name="play"/></div></div>
        </Link>;
      })}</section>}
  </>;
}
