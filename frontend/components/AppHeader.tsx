import Link from "next/link";
import type { ReactNode } from "react";
import colors from "@/public/colors.json";

type AppHeaderProps = {
  actions: ReactNode;
  nav?: ReactNode;
};

/** Keeps brand placement, dimensions, and account actions identical across pages. */
export default function AppHeader({ actions, nav }: AppHeaderProps) {
  return (
    <header
      className="sticky top-0 z-40 min-h-24 border-b border-white/10 px-5 py-4 shadow-sm sm:px-8 lg:px-12"
      style={{ backgroundColor: colors.dark_theme }}
    >
      <div className="mx-auto flex min-h-16 max-w-[1600px] flex-wrap items-center justify-between gap-x-8 gap-y-4">
        <div className="flex min-w-0 flex-1 items-center gap-6 lg:gap-10">
          <Link
            className="shrink-0 text-2xl font-bold tracking-[-0.04em] text-white"
            href="/"
          >
            Senus Board
          </Link>
          {nav}
        </div>
        <div className="shrink-0">{actions}</div>
      </div>
    </header>
  );
}
