"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "../hooks/useAuth";

type DeleteAccountModalProps = {
  onClose: () => void;
};

/** Confirms and submits permanent deletion for an ordinary account. */
export default function DeleteAccountModal({
  onClose,
}: DeleteAccountModalProps) {
  const { deleteAccount } = useAuth();
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const [isSending, setIsSending] = useState(false);

  useEffect(() => {
    /** Closes the confirmation dialog with Escape when no request is active. */
    const closeKey = (event: KeyboardEvent) => {
      if (event.key === "Escape" && !isSending) onClose();
    };

    document.addEventListener("keydown", closeKey);
    document.body.style.overflow = "hidden";
    return () => {
      document.removeEventListener("keydown", closeKey);
      document.body.style.overflow = "";
    };
  }, [isSending, onClose]);

  /** Deletes the account and returns the browser to the Welcome page. */
  const confirmDelete = async () => {
    setError(null);
    setIsSending(true);

    try {
      await deleteAccount();
      router.replace("/");
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Request failed.");
      setIsSending(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/55 p-4 backdrop-blur-sm"
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !isSending) onClose();
      }}
    >
      <section
        aria-labelledby="delete-account-title"
        aria-modal="true"
        className="w-full max-w-md rounded-3xl bg-white p-7 shadow-2xl"
        role="dialog"
      >
        <p className="text-sm font-bold uppercase tracking-[0.16em] text-red-700">
          Permanent action
        </p>
        <h2
          className="mt-2 text-2xl font-bold text-slate-950"
          id="delete-account-title"
        >
          Delete account?
        </h2>
        <p className="mt-3 text-sm leading-6 text-slate-600">
          This permanently deletes your account and cannot be undone. You will
          immediately lose access to Senus Board.
        </p>

        {error && (
          <p
            className="mt-5 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700"
            role="alert"
          >
            {error}
          </p>
        )}

        <div className="mt-7 flex justify-end gap-3">
          <button
            className="h-11 cursor-pointer rounded-xl border border-slate-300 px-5 text-sm font-semibold text-slate-700 disabled:cursor-not-allowed disabled:opacity-60"
            disabled={isSending}
            type="button"
            onClick={onClose}
          >
            Cancel
          </button>
          <button
            className="h-11 cursor-pointer rounded-xl bg-red-700 px-5 text-sm font-semibold text-white transition-colors hover:bg-red-800 disabled:cursor-wait disabled:opacity-65"
            disabled={isSending}
            type="button"
            onClick={confirmDelete}
          >
            {isSending ? "Deleting..." : "Delete account"}
          </button>
        </div>
      </section>
    </div>
  );
}
