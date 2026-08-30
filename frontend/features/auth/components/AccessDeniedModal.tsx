"use client";

import colors from "@/public/colors.json";

type AccessDeniedModalProps = {
  onClose: () => void;
};

/** Informs the user that account access was revoked by an Admin. */
export default function AccessDeniedModal({ onClose }: AccessDeniedModalProps) {
  return (
    <div className="fixed inset-0 z-[60] flex items-center justify-center bg-black/55 p-4 backdrop-blur-sm">
      <section
        aria-labelledby="access-denied-title"
        aria-modal="true"
        className="w-full max-w-md rounded-3xl bg-white p-7 text-center shadow-2xl"
        role="alertdialog"
      >
        <p className="text-sm font-bold uppercase tracking-[0.16em] text-red-700">
          Account access
        </p>
        <h2
          className="mt-2 text-3xl font-bold"
          id="access-denied-title"
          style={{ color: colors.dark_theme }}
        >
          Access denied
        </h2>
        <p className="mt-4 text-sm leading-6 text-slate-600">
          Your account no longer has access to Senus Board. You have been logged
          out.
        </p>
        <button
          className="mt-7 h-12 w-full cursor-pointer rounded-xl text-sm font-semibold text-white"
          style={{ backgroundColor: colors.light_theme }}
          type="button"
          onClick={onClose}
        >
          Return to Welcome
        </button>
      </section>
    </div>
  );
}
