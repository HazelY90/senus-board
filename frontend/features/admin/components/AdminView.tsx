"use client";

import {
  useCallback,
  useEffect,
  useState,
  type FormEvent,
  type ReactNode,
} from "react";
import CreateUserForm from "./CreateUserForm";
import { adminApiService } from "@/service/adminApiService";
import { ApiError } from "@/service/apiClient";
import type { User } from "@/types/auth";
import colors from "@/public/colors.json";

type Tab = "pending" | "search" | "create";
type ActionKind = "verify" | "reject" | "disable" | "delete";
type UserAction = { kind: ActionKind; user: User };

/** Provides account review, search, and creation tools for Admin users. */
export default function AdminView() {
  const [tab, setTab] = useState<Tab>("pending");
  const [pending, setPending] = useState<User[] | null>(null);
  const [pendingError, setPendingError] = useState<string | null>(null);
  const [isPendingLoading, setIsPendingLoading] = useState(true);
  const [email, setEmail] = useState("");
  const [result, setResult] = useState<User | null>(null);
  const [searchError, setSearchError] = useState<string | null>(null);
  const [isSearching, setIsSearching] = useState(false);
  const [created, setCreated] = useState<User | null>(null);
  const [action, setAction] = useState<UserAction | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [isActing, setIsActing] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);

  /** Loads the latest pending-user list from the backend. */
  const loadPending = useCallback(async () => {
    setPendingError(null);
    setIsPendingLoading(true);

    try {
      setPending(await adminApiService.getPending());
    } catch (reason) {
      setPendingError(getError(reason));
    } finally {
      setIsPendingLoading(false);
    }
  }, []);

  useEffect(() => {
    let isLive = true;

    // Load pending accounts immediately after the Admin page becomes available.
    adminApiService
      .getPending()
      .then((users) => {
        if (isLive) setPending(users);
      })
      .catch((reason) => {
        if (isLive) setPendingError(getError(reason));
      })
      .finally(() => {
        if (isLive) setIsPendingLoading(false);
      });

    return () => {
      isLive = false;
    };
  }, []);

  /** Searches for one user using a trimmed, normalised email address. */
  const search = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const query = email.trim().toLowerCase();

    setEmail(query);
    setResult(null);
    setSearchError(null);
    setNotice(null);
    setIsSearching(true);

    try {
      setResult(await adminApiService.searchUser(query));
    } catch (reason) {
      setSearchError(
        reason instanceof ApiError && reason.status === 404
          ? "No user found."
          : getError(reason),
      );
    } finally {
      setIsSearching(false);
    }
  };

  /** Opens a confirmation dialog for one administrative account action. */
  const openAction = (kind: ActionKind, user: User) => {
    setActionError(null);
    setAction({ kind, user });
  };

  /** Sends the confirmed action and updates the visible user data. */
  const runAction = async () => {
    if (!action) return;

    setActionError(null);
    setIsActing(true);

    try {
      if (action.kind === "verify" || action.kind === "reject") {
        await adminApiService.verifyUser(
          action.user.id,
          action.kind === "verify",
        );
        setPending((users) =>
          users?.filter((user) => user.id !== action.user.id) ?? [],
        );
        setResult((user) =>
          user?.id === action.user.id
            ? {
                ...user,
                status: action.kind === "verify" ? "ACTIVE" : "REJECTED",
              }
            : user,
        );
      } else if (action.kind === "disable") {
        await adminApiService.disableUser(action.user.id);
        setResult((user) =>
          user?.id === action.user.id ? { ...user, status: "DISABLED" } : user,
        );
      } else {
        if (action.user.status === "ACTIVE") {
          await adminApiService.disableUser(action.user.id);
          setResult((user) =>
            user?.id === action.user.id
              ? { ...user, status: "DISABLED" }
              : user,
          );
        }

        await adminApiService.deleteUser(action.user.id);
        setResult((user) => (user?.id === action.user.id ? null : user));
      }

      setNotice(getSuccess(action));
      setAction(null);
    } catch (reason) {
      setActionError(getError(reason));
    } finally {
      setIsActing(false);
    }
  };

  return (
    <main className="mx-auto max-w-[1600px] px-5 py-8 sm:px-8 lg:px-12 lg:py-10">
      <header className="max-w-4xl">
        <p
          className="text-xs font-bold uppercase tracking-[0.2em]"
          style={{ color: colors.light_theme }}
        >
          Account management
        </p>
        <h1
          className="mt-3 text-4xl font-bold tracking-[-0.035em] sm:text-5xl"
          style={{ color: colors.dark_theme }}
        >
          Administration
        </h1>
        <p className="mt-4 text-base leading-7 text-slate-600 sm:text-lg">
          Review registrations, manage existing accounts, or create a new user.
        </p>
      </header>

      <div className="mt-8 flex flex-wrap gap-3 rounded-2xl border border-slate-200 bg-white p-3 shadow-sm">
        <TabButton
          isActive={tab === "pending"}
          label="Pending users"
          onClick={() => {
            setNotice(null);
            setTab("pending");
          }}
        />
        <TabButton
          isActive={tab === "search"}
          label="User search"
          onClick={() => {
            setNotice(null);
            setTab("search");
          }}
        />
        <TabButton
          isActive={tab === "create"}
          label="Create user"
          onClick={() => {
            setNotice(null);
            setTab("create");
          }}
        />
      </div>

      {notice && (
        <p
          className="mt-5 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-800"
          role="status"
        >
          {notice}
        </p>
      )}

      {tab === "pending" ? (
        <section className="mt-6">
          <div className="mb-4 flex items-center justify-between gap-4">
            <div>
              <h2 className="text-xl font-bold text-slate-900">Pending users</h2>
              <p className="mt-1 text-sm text-slate-600">
                Verify or reject registration requests awaiting review.
              </p>
            </div>
            <button
              className="h-10 cursor-pointer rounded-xl border border-slate-300 bg-white px-4 text-sm font-semibold text-slate-700 disabled:cursor-wait disabled:opacity-60"
              disabled={isPendingLoading}
              type="button"
              onClick={loadPending}
            >
              {isPendingLoading ? "Refreshing..." : "Refresh"}
            </button>
          </div>

          {pendingError && <ErrorMessage message={pendingError} />}
          {pending === null && !pendingError ? (
            <LoadingMessage message="Loading pending users..." />
          ) : pending?.length === 0 ? (
            <EmptyMessage message="No pending users." />
          ) : pending ? (
            <UserTable
              actions={(user) => (
                <>
                  <ActionButton
                    label="Verify"
                    onClick={() => openAction("verify", user)}
                  />
                  <ActionButton
                    isDanger
                    label="Reject"
                    onClick={() => openAction("reject", user)}
                  />
                </>
              )}
              users={pending}
            />
          ) : null}
        </section>
      ) : tab === "search" ? (
        <section className="mt-6">
          <h2 className="text-xl font-bold text-slate-900">User search</h2>
          <p className="mt-1 text-sm text-slate-600">
            Enter an exact email address to manage an existing account.
          </p>

          <form className="mt-5 flex max-w-2xl gap-3" onSubmit={search}>
            <input
              className="h-12 min-w-0 flex-1 rounded-xl border border-slate-300 bg-white px-4 text-base outline-none transition focus:border-transparent focus:ring-2"
              disabled={isSearching}
              name="email"
              placeholder="name@company.com"
              required
              style={{ "--tw-ring-color": colors.light_theme } as React.CSSProperties}
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
            />
            <button
              className="h-12 shrink-0 cursor-pointer rounded-xl px-6 text-sm font-semibold text-white disabled:cursor-wait disabled:opacity-65"
              disabled={isSearching}
              style={{ backgroundColor: colors.light_theme }}
              type="submit"
            >
              {isSearching ? "Searching..." : "Search"}
            </button>
          </form>

          {searchError && <ErrorMessage message={searchError} />}
          {result && (
            <div className="mt-6">
              <UserTable
                actions={(user) => {
                  if (user.status === "PENDING") {
                    return (
                      <>
                        <ActionButton
                          label="Verify"
                          onClick={() => openAction("verify", user)}
                        />
                        <ActionButton
                          isDanger
                          label="Reject"
                          onClick={() => openAction("reject", user)}
                        />
                      </>
                    );
                  }

                  if (user.status === "ACTIVE") {
                    return (
                      <ActionButton
                        disabled={user.role === "ADMIN"}
                        label="Disable"
                        onClick={() => openAction("disable", user)}
                      />
                    );
                  }

                  return (
                    <ActionButton
                      disabled={user.role === "ADMIN"}
                      isDanger
                      label="Delete"
                      onClick={() => openAction("delete", user)}
                    />
                  );
                }}
                users={[result]}
              />
            </div>
          )}
        </section>
      ) : (
        <section className="mt-6">
          <div className="text-center">
            <h2 className="text-xl font-bold text-slate-900">Create user</h2>
            <p className="mt-1 text-sm text-slate-600">
              Create an active ordinary account without registration review.
            </p>
          </div>
          <CreateUserForm
            onCreated={(user) => {
              setCreated(user);
              setNotice(`${user.email} was created.`);
            }}
          />

          {created && (
            <div className="mt-6">
              <UserTable
                actions={(user) => (
                  <ActionButton
                    label="View user"
                    onClick={() => {
                      setEmail(user.email);
                      setResult(user);
                      setNotice(null);
                      setTab("search");
                    }}
                  />
                )}
                users={[created]}
              />
            </div>
          )}
        </section>
      )}

      {action && (
        <ActionModal
          action={action}
          error={actionError}
          isSending={isActing}
          onClose={() => setAction(null)}
          onConfirm={runAction}
        />
      )}
    </main>
  );
}

/** Renders a reusable Admin subsection tab. */
function TabButton({
  isActive,
  label,
  onClick,
}: {
  isActive: boolean;
  label: string;
  onClick: () => void;
}) {
  return (
    <button
      aria-pressed={isActive}
      className="cursor-pointer rounded-xl px-4 py-2.5 text-sm font-semibold transition-colors"
      style={{
        backgroundColor: isActive ? colors.light_theme : "#f1f5f4",
        color: isActive ? "white" : colors.main_theme,
      }}
      type="button"
      onClick={onClick}
    >
      {label}
    </button>
  );
}

/** Displays users in a consistent horizontal management table. */
function UserTable({
  actions,
  users,
}: {
  actions: (user: User) => ReactNode;
  users: User[];
}) {
  return (
    <div className="overflow-x-auto rounded-2xl border border-slate-200 bg-white shadow-sm">
      <table className="w-full min-w-[72rem] border-collapse text-left text-sm">
        <thead className="bg-slate-50 text-slate-600">
          <tr>
            <TableHead label="Name" />
            <TableHead label="Email" />
            <TableHead label="Organisation" />
            <TableHead label="User type" />
            <TableHead label="Description" />
            <TableHead label="Status" />
            <TableHead label="Actions" />
          </tr>
        </thead>
        <tbody>
          {users.map((user) => (
            <tr className="border-t border-slate-100" key={user.id}>
              <TableCell value={user.name} />
              <TableCell value={user.email} />
              <TableCell value={user.organization} />
              <TableCell value={formatRole(user.role)} />
              <TableCell value={user.description || "Unavailable"} />
              <td className="whitespace-nowrap px-4 py-4 align-top">
                <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-bold text-slate-700">
                  {user.status}
                </span>
              </td>
              <td className="px-4 py-3 align-top">
                <div className="flex gap-2">{actions(user)}</div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

/** Renders one table heading with consistent spacing. */
function TableHead({ label }: { label: string }) {
  return <th className="px-4 py-3 font-semibold">{label}</th>;
}

/** Renders one wrap-safe text cell. */
function TableCell({ value }: { value: string }) {
  return (
    <td className="max-w-64 break-words px-4 py-4 align-top text-slate-700">
      {value}
    </td>
  );
}

/** Renders one compact user-management action. */
function ActionButton({
  disabled = false,
  isDanger = false,
  label,
  onClick,
}: {
  disabled?: boolean;
  isDanger?: boolean;
  label: string;
  onClick: () => void;
}) {
  return (
    <button
      className={`h-9 cursor-pointer rounded-lg border px-3 text-xs font-bold transition-colors disabled:cursor-not-allowed disabled:border-slate-200 disabled:text-slate-400 ${
        isDanger
          ? "border-red-200 text-red-700 hover:bg-red-50"
          : "border-teal-200 text-teal-800 hover:bg-teal-50"
      }`}
      disabled={disabled}
      type="button"
      onClick={onClick}
    >
      {label}
    </button>
  );
}

/** Confirms one Admin action before sending a state-changing request. */
function ActionModal({
  action,
  error,
  isSending,
  onClose,
  onConfirm,
}: {
  action: UserAction;
  error: string | null;
  isSending: boolean;
  onClose: () => void;
  onConfirm: () => void;
}) {
  const labels: Record<ActionKind, string> = {
    verify: "Verify user",
    reject: "Reject user",
    disable: "Disable user",
    delete: "Delete user",
  };
  const isDanger = action.kind !== "verify";
  const title =
    action.kind === "delete" && action.user.status === "ACTIVE"
      ? "Delete active user"
      : labels[action.kind];

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/55 p-4 backdrop-blur-sm"
      role="presentation"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !isSending) onClose();
      }}
    >
      <section
        aria-labelledby="admin-action-title"
        aria-modal="true"
        className="w-full max-w-md rounded-3xl bg-white p-7 shadow-2xl"
        role="dialog"
      >
        <h2 className="text-2xl font-bold text-slate-950" id="admin-action-title">
          {title}?
        </h2>
        <p className="mt-3 text-sm leading-6 text-slate-600">
          {getPrompt(action)}
        </p>
        <p className="mt-3 break-words text-sm font-semibold text-slate-800">
          {action.user.email}
        </p>

        {error && <ErrorMessage message={error} />}

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
            className={`h-11 cursor-pointer rounded-xl px-5 text-sm font-semibold text-white disabled:cursor-wait disabled:opacity-65 ${
              isDanger ? "bg-red-700 hover:bg-red-800" : "bg-teal-700 hover:bg-teal-800"
            }`}
            disabled={isSending}
            type="button"
            onClick={onConfirm}
          >
            {isSending ? "Processing..." : labels[action.kind]}
          </button>
        </div>
      </section>
    </div>
  );
}

/** Displays a backend or validation error in the current section. */
function ErrorMessage({ message }: { message: string }) {
  return (
    <p
      className="mt-5 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700"
      role="alert"
    >
      {message}
    </p>
  );
}

/** Displays a neutral loading state. */
function LoadingMessage({ message }: { message: string }) {
  return (
    <p className="rounded-2xl border border-slate-200 bg-white px-6 py-8 text-center text-sm font-semibold text-slate-600 shadow-sm">
      {message}
    </p>
  );
}

/** Displays an empty collection state. */
function EmptyMessage({ message }: { message: string }) {
  return (
    <p className="rounded-2xl border border-slate-200 bg-white px-6 py-8 text-center text-slate-600 shadow-sm">
      {message}
    </p>
  );
}

/** Formats a backend role code as a readable table value. */
function formatRole(role: User["role"]) {
  return role
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ");
}

/** Returns a concise success message for one completed action. */
function getSuccess(action: UserAction) {
  const verbs: Record<ActionKind, string> = {
    verify: "verified",
    reject: "rejected",
    disable: "disabled",
    delete: "deleted",
  };
  return `${action.user.email} was ${verbs[action.kind]}.`;
}

/** Returns a confirmation explanation for one Admin action. */
function getPrompt(action: UserAction) {
  if (action.kind === "verify") return "This grants the pending account active status.";
  if (action.kind === "reject") return "This rejects the pending registration.";
  if (action.kind === "disable") return "This immediately prevents the account from signing in.";
  if (action.user.status === "ACTIVE") {
    return "This account is active. Continuing will disable it first and then permanently delete it. This cannot be undone.";
  }
  return "This permanently deletes the account and cannot be undone.";
}

/** Converts an unknown request failure into a display-safe message. */
function getError(reason: unknown) {
  return reason instanceof Error ? reason.message : "Request failed.";
}
