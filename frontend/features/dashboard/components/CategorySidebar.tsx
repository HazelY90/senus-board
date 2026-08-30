"use client";

import { useEffect, useState } from "react";
import { useAuth } from "@/features/auth/hooks/useAuth";
import { categoryMeta, getCategoryOrder } from "../config";
import colors from "@/public/colors.json";

/** Links to the four category sections in the current financial-data page. */
export default function CategorySidebar() {
  const { user } = useAuth();
  const role = user?.role;
  const order = getCategoryOrder(role);
  const [active, setActive] = useState(order[0]);

  useEffect(() => {
    const ids = getCategoryOrder(role);
    let frame: number | null = null;

    /** Selects the last category heading that has crossed the header offset. */
    const update = () => {
      const offset = 144;
      let current = ids[0];

      for (const id of ids) {
        const section = document.getElementById(id);
        if (section && section.getBoundingClientRect().top <= offset) current = id;
      }

      // Keep the final category active when the page bottom prevents its heading crossing the offset.
      const isBottom =
        window.innerHeight + window.scrollY >=
        document.documentElement.scrollHeight - 2;
      if (isBottom) current = ids[ids.length - 1];

      setActive(current);
      frame = null;
    };

    /** Limits scroll calculations to one update per animation frame. */
    const schedule = () => {
      if (frame === null) frame = window.requestAnimationFrame(update);
    };

    schedule();
    window.addEventListener("scroll", schedule, { passive: true });
    window.addEventListener("resize", schedule);

    return () => {
      window.removeEventListener("scroll", schedule);
      window.removeEventListener("resize", schedule);
      if (frame !== null) window.cancelAnimationFrame(frame);
    };
  }, [role]);

  return (
    <aside className="lg:sticky lg:top-28 lg:self-start">
      <nav
        aria-label="Financial categories"
        className="flex gap-2 overflow-x-auto rounded-2xl border border-slate-200 bg-white p-2 shadow-sm lg:grid lg:w-56 lg:gap-1 lg:p-3"
      >
        {order.map((id, index) => (
          <a
            aria-current={active === id ? "location" : undefined}
            className={`group flex shrink-0 items-center gap-3 rounded-xl px-3 py-3 text-sm font-semibold transition-colors ${
              active === id
                ? "text-white shadow-sm"
                : "text-slate-600 hover:bg-slate-50"
            }`}
            href={`#${id}`}
            key={id}
            style={
              active === id ? { backgroundColor: colors.light_theme } : undefined
            }
            onClick={() => setActive(id)}
          >
            <span
              className="flex size-7 items-center justify-center rounded-lg text-xs font-bold text-white"
              style={{
                backgroundColor:
                  active === id ? colors.dark_theme : colors.main_theme,
              }}
            >
              {index + 1}
            </span>
            <span className={active === id ? "text-white" : "group-hover:text-slate-950"}>
              {categoryMeta[id].label}
            </span>
          </a>
        ))}
      </nav>
    </aside>
  );
}
