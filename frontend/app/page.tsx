"use client";

import { useState } from "react";
import Link from "next/link";
import AppHeader from "@/components/AppHeader";
import AuthModal, {
  type AuthMode,
} from "@/features/auth/components/AuthModal";
import ProfileMenu from "@/features/auth/components/ProfileMenu";
import { useAuth } from "@/features/auth/hooks/useAuth";
import colors from "@/public/colors.json";

/** Renders the public Welcome page and controls the authentication dialogs. */
export default function Home() {
  const { isReady, user } = useAuth();
  const [mode, setMode] = useState<AuthMode | null>(null);

  return (
    <main className="flex min-h-dvh flex-col bg-white">
      <AppHeader
        actions={
          !isReady ? (
            <div className="h-12 w-48" aria-hidden="true" />
          ) : user ? (
            <ProfileMenu />
          ) : (
            <div className="flex items-center gap-3 sm:gap-4">
              <button
                className="min-w-24 cursor-pointer rounded-xl border-2 px-4 py-2.5 text-sm font-semibold transition-colors hover:bg-white/10 focus-visible:outline-2 focus-visible:outline-offset-4 sm:min-w-28 sm:px-6 sm:py-3 sm:text-base"
                style={{
                  borderColor: colors.light_theme,
                  color: colors.light_theme,
                }}
                type="button"
                onClick={() => setMode("signup")}
              >
                Sign Up
              </button>
              <button
                className="min-w-24 cursor-pointer rounded-xl px-4 py-3 text-sm font-semibold text-white transition-[filter] hover:brightness-110 focus-visible:outline-2 focus-visible:outline-offset-4 sm:min-w-28 sm:px-6 sm:py-3.5 sm:text-base"
                style={{ backgroundColor: colors.light_theme }}
                type="button"
                onClick={() => setMode("login")}
              >
                Login
              </button>
            </div>
          )
        }
        nav={
          isReady && user ? (
            <nav aria-label="Account destination">
              <Link
                className="rounded-lg px-3 py-2 text-sm font-semibold text-white transition-colors hover:bg-white/10"
                href={user.role === "ADMIN" ? "/admin" : "/dashboard"}
              >
                {user.role === "ADMIN" ? "Administration" : "Dashboard"}
              </Link>
            </nav>
          ) : undefined
        }
      />

      <section className="mx-auto flex min-h-0 w-full max-w-[1500px] flex-1 items-center justify-center px-6 py-12 sm:px-10 sm:py-16 lg:px-14">
        <div className="max-w-6xl text-center">
          <p
            className="mb-5 text-sm font-bold uppercase tracking-[0.22em] sm:text-base"
            style={{ color: colors.light_theme }}
          >
            Financial intelligence for Senus
          </p>
          <h1
            className="text-[clamp(2.5rem,5vw,5.25rem)] font-bold leading-[1.16] tracking-[-0.045em]"
            style={{ color: colors.dark_theme }}
          >
            A clear executive view of{" "}
            <span style={{ color: colors.light_theme }}>
              financial and operating performance.
            </span>
          </h1>
          <p
            className="mx-auto mt-8 max-w-3xl text-lg leading-8 sm:text-xl"
            style={{ color: colors.main_theme }}
          >
            Review reported results, calculated metrics, and focused analysis
            across every available reporting period.
          </p>
        </div>
      </section>

      {mode && (
        <AuthModal
          key={mode}
          mode={mode}
          onClose={() => setMode(null)}
          onMode={setMode}
        />
      )}
    </main>
  );
}
