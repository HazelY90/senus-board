"use client";

import { useAuth } from "@/features/auth/hooks/useAuth";
import { categoryMeta, getCategoryOrder } from "../config";
import colors from "@/public/colors.json";

/** Links to the four category sections in the current financial-data page. */
export default function CategorySidebar() {
  const { user } = useAuth();
  const order = getCategoryOrder(user?.role);

  return (
    <aside className="lg:sticky lg:top-28 lg:self-start">
      <nav
        aria-label="Financial categories"
        className="flex gap-2 overflow-x-auto rounded-2xl border border-slate-200 bg-white p-2 shadow-sm lg:grid lg:w-56 lg:gap-1 lg:p-3"
      >
        {order.map((id, index) => (
          <a
            className="group flex shrink-0 items-center gap-3 rounded-xl px-3 py-3 text-sm font-semibold text-slate-600 transition-colors hover:bg-slate-50"
            href={`#${id}`}
            key={id}
          >
            <span
              className="flex size-7 items-center justify-center rounded-lg text-xs font-bold text-white"
              style={{ backgroundColor: colors.light_theme }}
            >
              {index + 1}
            </span>
            <span className="group-hover:text-slate-950">{categoryMeta[id].label}</span>
          </a>
        ))}
      </nav>
    </aside>
  );
}
