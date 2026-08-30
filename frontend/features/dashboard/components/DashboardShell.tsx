"use client";

import { useEffect, type ReactNode } from "react";
import { useRouter } from "next/navigation";
import DashboardHeader from "./DashboardHeader";
import { useAuth } from "@/features/auth/hooks/useAuth";
import colors from "@/public/colors.json";

/** Protects ordinary-user Dashboard routes and renders their shared header. */
export default function DashboardShell({ children }: { children: ReactNode }) {
  const { isReady, user } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!isReady) return;
    if (!user) router.replace("/");
    else if (user.role === "ADMIN") router.replace("/admin");
  }, [isReady, router, user]);

  if (!isReady || !user || user.role === "ADMIN") {
    return (
      <main className="flex min-h-screen items-center justify-center bg-slate-50">
        <p className="text-sm font-semibold" style={{ color: colors.main_theme }}>
          Loading Senus Board...
        </p>
      </main>
    );
  }

  return (
    <div className="min-h-screen bg-[#f4f7f6]">
      <DashboardHeader />
      {children}
    </div>
  );
}
