"use client";

import { useEffect, useRef, useState } from "react";
import ChangePasswordModal from "./ChangePasswordModal";
import { useAuth } from "../hooks/useAuth";
import colors from "@/public/colors.json";

/** Displays the current user and reusable account actions in a dropdown menu. */
export default function ProfileMenu() {
  const { user } = useAuth();
  const [isOpen, setIsOpen] = useState(false);
  const [isPasswordOpen, setIsPasswordOpen] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const rootRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    /** Closes the dropdown when the user interacts with another page area. */
    const closeOutside = (event: PointerEvent) => {
      if (!rootRef.current?.contains(event.target as Node)) setIsOpen(false);
    };

    /** Closes the dropdown when the Escape key is pressed. */
    const closeKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") setIsOpen(false);
    };

    document.addEventListener("pointerdown", closeOutside);
    document.addEventListener("keydown", closeKey);
    return () => {
      document.removeEventListener("pointerdown", closeOutside);
      document.removeEventListener("keydown", closeKey);
    };
  }, []);

  if (!user) return null;

  const initial = user.name.trim().charAt(0).toUpperCase() || "U";

  return (
    <>
      <div className="relative" ref={rootRef}>
        <button
          aria-expanded={isOpen}
          aria-haspopup="menu"
          className="flex cursor-pointer items-center gap-3 rounded-xl border border-white/15 px-3 py-2 text-left text-sm font-semibold text-white transition-colors hover:bg-white/10"
          type="button"
          onClick={() => {
            setIsOpen((value) => !value);
            setNotice(null);
          }}
        >
          <span
            className="flex size-8 items-center justify-center rounded-full text-sm font-bold text-white"
            style={{ backgroundColor: colors.light_theme }}
          >
            {initial}
          </span>
          <span className="max-w-44 truncate">Welcome, {user.name}</span>
          <svg
            aria-hidden="true"
            className={`size-4 transition-transform ${isOpen ? "rotate-180" : ""}`}
            fill="none"
            viewBox="0 0 20 20"
          >
            <path
              d="m5 7.5 5 5 5-5"
              stroke="currentColor"
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth="1.7"
            />
          </svg>
        </button>

        {isOpen && (
          <div
            className="absolute right-0 top-[calc(100%+0.65rem)] w-60 rounded-2xl border border-slate-200 bg-white p-2 text-slate-700 shadow-xl"
            role="menu"
          >
            <MenuButton
              label="Change password"
              onClick={() => {
                setIsOpen(false);
                setIsPasswordOpen(true);
              }}
            />
            <MenuButton
              label="Delete account"
              onClick={() =>
                setNotice("Delete Account requires a backend endpoint.")
              }
            />
            <MenuButton
              label="Logout"
              onClick={() =>
                setNotice("Logout is deferred until the backend endpoint is available.")
              }
            />
            {notice && (
              <p className="m-2 rounded-lg bg-amber-50 p-2 text-xs leading-5 text-amber-800">
                {notice}
              </p>
            )}
          </div>
        )}
      </div>

      {isPasswordOpen && (
        <ChangePasswordModal onClose={() => setIsPasswordOpen(false)} />
      )}
    </>
  );
}

/** Renders one consistent action inside the Profile dropdown. */
function MenuButton({ label, onClick }: { label: string; onClick: () => void }) {
  return (
    <button
      className="w-full cursor-pointer rounded-xl px-3 py-2.5 text-left text-sm font-medium transition-colors hover:bg-slate-100"
      role="menuitem"
      type="button"
      onClick={onClick}
    >
      {label}
    </button>
  );
}
