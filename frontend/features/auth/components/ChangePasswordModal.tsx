"use client";

import { useState, type FormEvent } from "react";
import PasswordField from "./PasswordField";
import { isStrongPassword } from "../utils/password";
import { useAuth } from "../hooks/useAuth";
import { authApiService } from "@/service/authApiService";
import colors from "@/public/colors.json";

type ChangePasswordModalProps = {
  onClose: () => void;
};

/** Submits an authenticated password change from the Profile menu. */
export default function ChangePasswordModal({
  onClose,
}: ChangePasswordModalProps) {
  const { user } = useAuth();
  const [error, setError] = useState<string | null>(null);
  const [isDone, setIsDone] = useState(false);
  const [isSending, setIsSending] = useState(false);
  const minLength = user?.role === "ADMIN" ? 16 : 10;

  /** Validates the new password and sends the backend change request. */
  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    const currentPassword = data.get("currentPassword")?.toString() ?? "";
    const newPassword = data.get("newPassword")?.toString() ?? "";
    const confirm = data.get("confirm")?.toString() ?? "";

    setError(null);
    setIsDone(false);

    if (newPassword !== confirm) {
      setError("Passwords do not match.");
      return;
    }
    if (!isStrongPassword(newPassword, minLength)) {
      setError(
        `Password must contain at least ${minLength} characters, including uppercase, lowercase, number, and special characters.`,
      );
      return;
    }

    setIsSending(true);

    try {
      await authApiService.changePassword({ currentPassword, newPassword });
      form.reset();
      setIsDone(true);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Request failed.");
    } finally {
      setIsSending(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/55 p-4 backdrop-blur-sm"
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
    >
      <section
        aria-labelledby="password-title"
        aria-modal="true"
        className="relative w-full max-w-md rounded-3xl bg-white p-7 shadow-2xl"
        role="dialog"
      >
        <button
          aria-label="Close"
          className="absolute right-5 top-4 cursor-pointer text-3xl leading-none text-slate-500 hover:text-slate-900"
          type="button"
          onClick={onClose}
        >
          ×
        </button>
        <h2
          className="text-2xl font-bold"
          id="password-title"
          style={{ color: colors.dark_theme }}
        >
          Change password
        </h2>
        <p className="mt-2 text-sm leading-6 text-slate-600">
          Enter your current password and choose a new secure password.
        </p>

        {error && (
          <p
            className="mt-5 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700"
            role="alert"
          >
            {error}
          </p>
        )}
        {isDone && (
          <p
            className="mt-5 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800"
            role="status"
          >
            Password changed successfully.
          </p>
        )}

        <form className="mt-6 grid gap-5" onSubmit={submit}>
          <PasswordField
            autoComplete="current-password"
            disabled={isSending}
            label="Current password"
            name="currentPassword"
          />
          <PasswordField
            autoComplete="new-password"
            disabled={isSending}
            label="New password"
            minLength={minLength}
            name="newPassword"
          />
          <PasswordField
            autoComplete="new-password"
            disabled={isSending}
            label="Confirm new password"
            minLength={minLength}
            name="confirm"
          />
          <button
            className="h-12 cursor-pointer rounded-xl font-semibold text-white disabled:cursor-wait disabled:opacity-65"
            disabled={isSending}
            style={{ backgroundColor: colors.light_theme }}
            type="submit"
          >
            {isSending ? "Saving..." : "Save password"}
          </button>
        </form>
      </section>
    </div>
  );
}
