"use client";

import { useState } from "react";
import colors from "@/public/colors.json";

type PasswordFieldProps = {
  autoComplete: "current-password" | "new-password";
  disabled: boolean;
  label: string;
  minLength?: number;
  name: string;
};

/** Renders a password input with an accessible visibility toggle. */
export default function PasswordField({
  autoComplete,
  disabled,
  label,
  minLength,
  name,
}: PasswordFieldProps) {
  const [isVisible, setIsVisible] = useState(false);

  return (
    <label className="grid gap-2 text-sm font-semibold text-slate-800">
      {label}
      <span className="relative block">
        <input
          autoComplete={autoComplete}
          className="h-12 w-full rounded-xl border border-slate-300 px-4 pr-12 text-base font-normal outline-none transition focus:border-transparent focus:ring-2 disabled:cursor-not-allowed disabled:bg-slate-100"
          disabled={disabled}
          minLength={minLength}
          name={name}
          required
          style={{ "--tw-ring-color": colors.light_theme } as React.CSSProperties}
          type={isVisible ? "text" : "password"}
        />
        <button
          aria-label={isVisible ? `Hide ${label.toLowerCase()}` : `Show ${label.toLowerCase()}`}
          aria-pressed={isVisible}
          className="absolute inset-y-0 right-0 flex w-12 cursor-pointer items-center justify-center rounded-r-xl text-slate-500 transition-colors hover:text-slate-800 focus-visible:outline-2 focus-visible:outline-offset-[-4px] disabled:cursor-not-allowed disabled:text-slate-300"
          disabled={disabled}
          type="button"
          onClick={() => setIsVisible((value) => !value)}
        >
          {isVisible ? <HiddenIcon /> : <VisibleIcon />}
        </button>
      </span>
    </label>
  );
}

/** Displays the icon used when the current password value is hidden. */
function VisibleIcon() {
  return (
    <svg
      aria-hidden="true"
      className="size-5"
      fill="none"
      viewBox="0 0 24 24"
    >
      <path
        d="M2.5 12s3.5-6 9.5-6 9.5 6 9.5 6-3.5 6-9.5 6-9.5-6-9.5-6Z"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.8"
      />
      <circle cx="12" cy="12" r="2.75" stroke="currentColor" strokeWidth="1.8" />
    </svg>
  );
}

/** Displays the icon used when the current password value is visible. */
function HiddenIcon() {
  return (
    <svg
      aria-hidden="true"
      className="size-5"
      fill="none"
      viewBox="0 0 24 24"
    >
      <path
        d="m4 4 16 16M9.8 6.3A9.8 9.8 0 0 1 12 6c6 0 9.5 6 9.5 6a15.6 15.6 0 0 1-2.1 2.7M6.2 7.4A16.1 16.1 0 0 0 2.5 12s3.5 6 9.5 6a9.7 9.7 0 0 0 3-.5M10.1 10.1a2.7 2.7 0 0 0 3.8 3.8"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.8"
      />
    </svg>
  );
}
