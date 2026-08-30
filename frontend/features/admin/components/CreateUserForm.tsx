"use client";

import { useState, type FormEvent } from "react";
import PasswordField from "@/features/auth/components/PasswordField";
import { isStrongPassword } from "@/features/auth/utils/password";
import { adminApiService } from "@/service/adminApiService";
import type { CreateUserReq, User } from "@/types/auth";
import colors from "@/public/colors.json";

type CreateUserFormProps = {
  onCreated: (user: User) => void;
};

const roles = [
  ["MANAGEMENT", "Management"],
  ["BOARD", "Board"],
  ["EQUITY_INVESTOR", "Equity Investor"],
  ["CREDIT_PROVIDER", "Credit Provider"],
] as const;

/** Creates an active ordinary account through the authenticated Admin API. */
export default function CreateUserForm({ onCreated }: CreateUserFormProps) {
  const [error, setError] = useState<string | null>(null);
  const [isSending, setIsSending] = useState(false);

  /** Validates the account fields and sends the create-user request. */
  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const form = event.currentTarget;
    const data = new FormData(form);
    const password = getText(data, "password", false);
    const confirm = getText(data, "passwordConfirm", false);
    const role = getText(data, "role");

    setError(null);

    if (password !== confirm) {
      setError("Passwords do not match.");
      return;
    }
    if (!isStrongPassword(password)) {
      setError(
        "Password must contain at least 10 characters, including uppercase, lowercase, number, and special characters.",
      );
      return;
    }
    if (!isRole(role)) {
      setError("Select a valid user type.");
      return;
    }

    const req: CreateUserReq = {
      email: getText(data, "email").toLowerCase(),
      name: getText(data, "name"),
      organization: getText(data, "organization"),
      password,
      role,
    };

    setIsSending(true);

    try {
      const user = await adminApiService.createUser(req);
      form.reset();
      onCreated(user);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Request failed.");
    } finally {
      setIsSending(false);
    }
  };

  return (
    <form
      className="mx-auto mt-5 grid max-w-3xl gap-5 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:grid-cols-2"
      onSubmit={submit}
    >
      {error && (
        <p
          className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 sm:col-span-2"
          role="alert"
        >
          {error}
        </p>
      )}

      <Field
        autoComplete="name"
        disabled={isSending}
        label="Name"
        maxLength={100}
        name="name"
      />
      <Field
        autoComplete="email"
        disabled={isSending}
        label="Email"
        maxLength={255}
        name="email"
        type="email"
      />
      <Field
        autoComplete="organization"
        disabled={isSending}
        label="Organisation"
        maxLength={255}
        name="organization"
      />
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
      <PasswordField
        autoComplete="new-password"
        disabled={isSending}
        label="Password"
        minLength={10}
        name="password"
      />
      <PasswordField
        autoComplete="new-password"
        disabled={isSending}
        label="Confirm password"
        minLength={10}
        name="passwordConfirm"
      />
      <p className="text-xs leading-5 text-slate-500 sm:col-span-2">
        Use at least 10 characters with uppercase, lowercase, number, and special
        characters. The new account is active immediately.
      </p>
      <button
        className="h-12 cursor-pointer rounded-xl text-sm font-semibold text-white transition-[filter] hover:brightness-110 disabled:cursor-wait disabled:opacity-65 sm:col-span-2"
        disabled={isSending}
        style={{ backgroundColor: colors.light_theme }}
        type="submit"
      >
        {isSending ? "Creating user..." : "Create user"}
      </button>
    </form>
  );
}

type FieldProps = {
  autoComplete: string;
  disabled: boolean;
  label: string;
  maxLength: number;
  name: string;
  type?: "email" | "text";
};

/** Renders one labelled create-user text field. */
function Field({
  autoComplete,
  disabled,
  label,
  maxLength,
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
        name={name}
        required
        style={{ "--tw-ring-color": colors.light_theme } as React.CSSProperties}
        type={type}
      />
    </label>
  );
}

/** Reads and trims one required form value by default. */
function getText(data: FormData, name: string, isTrimmed = true) {
  const value = data.get(name)?.toString() ?? "";
  return isTrimmed ? value.trim() : value;
}

/** Narrows a form value to an ordinary account role. */
function isRole(value: string): value is CreateUserReq["role"] {
  return roles.some(([role]) => role === value);
}
