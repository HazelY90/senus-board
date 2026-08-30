import type { Metadata } from "next";
import AdminShell from "@/features/admin/components/AdminShell";

export const metadata: Metadata = {
  title: "Admin | SenusBoard",
  description: "SenusBoard account administration",
};

/** Provides the shared full-height container for Admin routes. */
export default function AdminLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return <AdminShell>{children}</AdminShell>;
}
