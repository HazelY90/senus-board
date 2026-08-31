"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import AppHeader from "@/components/AppHeader";
import ProfileMenu from "@/features/auth/components/ProfileMenu";
import colors from "@/public/colors.json";

const nav = [
  { href: "/dashboard", label: "Period Reports" },
  { href: "/dashboard/comparison", label: "Comparison" },
  { href: "/dashboard/documents", label: "Documents" },
] as const;

/** Renders brand, page navigation, and profile actions for Dashboard pages. */
export default function DashboardHeader() {
  const path = usePathname();

  return (
    <AppHeader
      actions={<ProfileMenu />}
      nav={
        <nav
          aria-label="Dashboard pages"
          className="flex min-w-0 items-center gap-1 overflow-x-auto pb-1"
        >
          {nav.map((item) => {
            const isActive = path === item.href;

            return (
              <Link
                aria-current={isActive ? "page" : undefined}
                className="shrink-0 rounded-lg px-3 py-2 text-sm font-semibold transition-colors"
                href={item.href}
                key={item.href}
                style={{
                  backgroundColor: isActive ? colors.light_theme : "transparent",
                  color: "white",
                }}
              >
                {item.label}
              </Link>
            );
          })}
        </nav>
      }
    />
  );
}
