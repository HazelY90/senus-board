import type { ReactNode } from "react";
import CategorySidebar from "./CategorySidebar";
import colors from "@/public/colors.json";

type DataPageProps = {
  children: ReactNode;
  description: string;
  eyebrow: string;
  title: string;
};

/** Provides the common heading and category-sidebar layout for financial pages. */
export default function DataPage({
  children,
  description,
  eyebrow,
  title,
}: DataPageProps) {
  return (
    <main className="mx-auto max-w-[1600px] px-5 py-8 sm:px-8 lg:px-12 lg:py-10">
      <header className="mb-8 max-w-4xl">
        <p
          className="text-xs font-bold uppercase tracking-[0.2em]"
          style={{ color: colors.light_theme }}
        >
          {eyebrow}
        </p>
        <h1
          className="mt-3 text-4xl font-bold tracking-[-0.035em] sm:text-5xl"
          style={{ color: colors.dark_theme }}
        >
          {title}
        </h1>
        <p className="mt-4 text-base leading-7 text-slate-600 sm:text-lg">
          {description}
        </p>
      </header>

      <div className="grid gap-6 lg:grid-cols-[14rem_minmax(0,1fr)] lg:items-start">
        <CategorySidebar />
        <div className="grid min-w-0 gap-7">{children}</div>
      </div>
    </main>
  );
}
