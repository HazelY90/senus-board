"use client";

import { useEffect, useState, type FormEvent } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "../hooks/useAuth";
import { isStrongPassword } from "../utils/password";
import PasswordField from "./PasswordField";
import type { RegisterReq } from "@/types/auth";
import colors from "@/public/colors.json";

export type AuthMode = "login" | "signup";

type AuthModalProps = {
  mode: AuthMode;
  onClose: () => void;
  onMode: (mode: AuthMode) => void;
};

const roles = [
  ["MANAGEMENT", "Management"],
  ["BOARD", "Board"],
  ["EQUITY_INVESTOR", "Equity Investor"],
  ["CREDIT_PROVIDER", "Credit Provider"],
] as const;

/** Displays and submits either the login or ordinary-user registration form. */
export default function AuthModal({ mode, onClose, onMode }: AuthModalProps) {
  const { login, register } = useAuth();
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const [isDone, setIsDone] = useState(false);
  const [isSending, setIsSending] = useState(false);
  const isLogin = mode === "login";

  useEffect(() => {
    // Allow keyboard users to dismiss the active dialog with Escape.
    const onKey = (event: KeyboardEvent) => {
      if (event.key === "Escape") onClose();
    };

    // Prevent the Welcome page from scrolling behind the open dialog.
    document.addEventListener("keydown", onKey);
    document.body.style.overflow = "hidden";

    return () => {
      document.removeEventListener("keydown", onKey);
      document.body.style.overflow = "";
    };
  }, [onClose]);

  /** Validates form-only fields and sends the matching backend request. */
  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    setIsDone(false);

    const form = event.currentTarget;
    const data = new FormData(form);
    const email = getText(data, "email").toLowerCase();
    const password = getText(data, "password", false);

    setIsSending(true);

    try {
      if (isLogin) {
        const user = await login({ email, password });
        onClose();
        router.push(user.role === "ADMIN" ? "/admin" : "/dashboard");
        return;
      }

      const confirm = getText(data, "passwordConfirm", false);
      if (password !== confirm) {
        throw new Error("Passwords do not match.");
      }
      if (!isStrongPassword(password)) {
        throw new Error(
          "Password must contain at least 10 characters, including uppercase, lowercase, number, and special characters.",
        );
      }

      const role = getText(data, "role");
      if (!isRole(role)) {
        throw new Error("Select a valid user type.");
      }

      const description = getText(data, "description");
      const req: RegisterReq = {
        email,
        name: getText(data, "name"),
        organization: getText(data, "organization"),
        password,
        role,
        ...(description ? { description } : {}),
      };

      await register(req);
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
      className="fixed inset-0 z-50 flex items-center justify-center overflow-y-auto bg-black/55 p-4 backdrop-blur-sm"
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
    >
      <section
        aria-labelledby="auth-title"
        aria-modal="true"
        className="relative my-auto max-h-[calc(100vh-2rem)] w-full max-w-lg overflow-y-auto rounded-3xl bg-white p-6 shadow-2xl sm:p-9"
        role="dialog"
      >
        <button
          aria-label="Close"
          className="absolute right-5 top-4 cursor-pointer text-3xl leading-none text-slate-500 transition-colors hover:text-slate-900"
          type="button"
          onClick={onClose}
        >
          ×
        </button>

        <p
          className="mb-2 text-sm font-bold uppercase tracking-[0.18em]"
          style={{ color: colors.light_theme }}
        >
          Senus Board
        </p>
        <h2
          className="text-3xl font-bold tracking-tight"
          id="auth-title"
          style={{ color: colors.dark_theme }}
        >
          {isLogin ? "Welcome back" : "Create your account"}
        </h2>
        <p className="mt-2 text-sm leading-6 text-slate-600">
          {isLogin
            ? "Log in to access the financial dashboard."
            : "Register with your approved enterprise email address."}
        </p>

        {error && (
          <p
            aria-live="polite"
            className="mt-5 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700"
            role="alert"
          >
            {error}
          </p>
        )}

        {isDone && (
          <p
            aria-live="polite"
            className="mt-5 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm leading-6 text-emerald-800"
            role="status"
          >
            Registration submitted. Your account is pending review, but you can
            log in immediately.
          </p>
        )}

        <form className="mt-7 grid gap-5" onSubmit={submit}>
          {!isLogin && (
            <>
              <Field
                autoComplete="name"
                disabled={isSending}
                label="Name"
                maxLength={100}
                name="name"
              />
              <Field
                autoComplete="organization"
                disabled={isSending}
                label="Organisation"
                maxLength={255}
                name="organization"
              />
            </>
          )}

          <Field
            autoComplete="email"
            disabled={isSending}
            label="Enterprise email"
            maxLength={255}
            name="email"
            type="email"
          />

          {!isLogin && (
            <label className="grid gap-2 text-sm font-semibold text-slate-800">
              User type
              <select
                className="h-12 rounded-xl border border-slate-300 bg-white px-4 text-base font-normal outline-none transition focus:border-transparent focus:ring-2 disabled:cursor-not-allowed disabled:bg-slate-100"
                defaultValue="MANAGEMENT"
                disabled={isSending}
                name="role"
                style={{ "--tw-ring-color": colors.light_theme } as React.CSSProperties}
              >
                {roles.map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </select>
            </label>
          )}

          <PasswordField
            autoComplete={isLogin ? "current-password" : "new-password"}
            disabled={isSending}
            label="Password"
            minLength={isLogin ? undefined : 10}
            name="password"
          />

          {!isLogin && (
            <>
              <p className="-mt-3 text-xs leading-5 text-slate-500">
                Use at least 10 characters with uppercase, lowercase, number,
                and special characters.
              </p>
              <PasswordField
                autoComplete="new-password"
                disabled={isSending}
                label="Confirm password"
                minLength={10}
                name="passwordConfirm"
              />
              <label className="grid gap-2 text-sm font-semibold text-slate-800">
                Description <span className="font-normal text-slate-500">(optional)</span>
                <textarea
                  className="min-h-24 resize-y rounded-xl border border-slate-300 px-4 py-3 text-base font-normal outline-none transition focus:border-transparent focus:ring-2 disabled:cursor-not-allowed disabled:bg-slate-100"
                  disabled={isSending}
                  name="description"
                  style={{ "--tw-ring-color": colors.light_theme } as React.CSSProperties}
                />
              </label>
            </>
          )}

          <button
            className="mt-2 h-12 cursor-pointer rounded-xl text-base font-semibold text-white transition-[filter] hover:brightness-110 focus-visible:outline-2 focus-visible:outline-offset-2 disabled:cursor-wait disabled:opacity-65"
            disabled={isSending}
            style={{ backgroundColor: colors.light_theme }}
            type="submit"
          >
            {isSending
              ? isLogin
                ? "Logging in..."
                : "Creating account..."
              : isLogin
                ? "Login"
                : "Sign Up"}
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-slate-600">
          {isLogin ? "Need an account?" : "Already have an account?"}{" "}
          <button
            className="cursor-pointer font-semibold underline-offset-4 hover:underline"
            disabled={isSending}
            style={{ color: colors.light_theme }}
            type="button"
            onClick={() => onMode(isLogin ? "signup" : "login")}
          >
            {isLogin ? "Sign Up" : "Login"}
          </button>
        </p>
      </section>
    </div>
  );
}

type FieldProps = {
  autoComplete: string;
  disabled: boolean;
  label: string;
  maxLength?: number;
  minLength?: number;
  name: string;
  type?: "email" | "text";
};

/** Renders a consistently styled and accessible authentication input. */
function Field({
  autoComplete,
  disabled,
  label,
  maxLength,
  minLength,
  name,
  type = "text",
}: FieldProps) {
  return (
    <label className="grid gap-2 text-sm font-semibold text-slate-800">
      {label}
      <input
        autoComplete={autoComplete}
        className="h-12 rounded-xl border border-slate-300 px-4 text-base font-normal outline-none transition focus:border-transparent focus:ring-2 disabled:cursor-not-allowed disabled:bg-slate-100"
        disabled={disabled}
        maxLength={maxLength}
        minLength={minLength}
        name={name}
        required
        style={{ "--tw-ring-color": colors.light_theme } as React.CSSProperties}
        type={type}
      />
    </label>
  );
}

/** Reads and optionally trims a string value from submitted form data. */
function getText(data: FormData, name: string, trim = true) {
  const value = data.get(name)?.toString() ?? "";
  return trim ? value.trim() : value;
}

/** Confirms that the selected value is an ordinary account role. */
function isRole(role: string): role is RegisterReq["role"] {
  return roles.some(([value]) => value === role);
}
