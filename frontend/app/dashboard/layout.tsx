import type { Metadata } from "next";
import DashboardShell from "@/features/dashboard/components/DashboardShell";
import { DataProvider } from "@/features/dashboard/DataContext";

export const metadata: Metadata = {
  title: "Dashboard | SenusBoard",
  description: "SenusBoard financial performance dashboard",
};

/** Provides the shared full-height container for Dashboard routes. */
export default function DashboardLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <DashboardShell>
      <DataProvider>{children}</DataProvider>
    </DashboardShell>
  );
}
