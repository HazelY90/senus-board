"use client";

import Link from "next/link";
import { useEffect, type ReactNode } from "react";
import { useRouter } from "next/navigation";
import AppHeader from "@/components/AppHeader";
import ProfileMenu from "@/features/auth/components/ProfileMenu";
import { useAuth } from "@/features/auth/hooks/useAuth";
import colors from "@/public/colors.json";

/** Protects Admin routes and provides their shared brand and Profile header. */
export default function AdminShell({ children }: { children: ReactNode }) {
  const { isReady, user } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!isReady) return;
    if (!user) router.replace("/");
    else if (user.role !== "ADMIN") router.replace("/dashboard");
  }, [isReady, router, user]);

  if (!isReady || !user || user.role !== "ADMIN") {
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
      <AppHeader
        actions={<ProfileMenu />}
        nav={
          <nav aria-label="Administration page">
            <Link
              aria-current="page"
              className="rounded-lg px-3 py-2 text-sm font-semibold text-white"
              href="/admin"
              style={{ backgroundColor: colors.light_theme }}
            >
              Administration
            </Link>
          </nav>
        }
      />
      {children}
    </div>
  );
}
