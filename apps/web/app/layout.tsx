import type { Metadata } from "next";
import "./globals.css";
export const metadata:Metadata={title:"LMSPilot – Hệ thống quản lý học tập",description:"LMS On-Premise cho tổ chức"};
export default function RootLayout({children}:{children:React.ReactNode}){return <html lang="vi"><body>{children}</body></html>}
