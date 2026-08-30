"use client";

import {
  createContext,
  useCallback,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import AccessDeniedModal from "./components/AccessDeniedModal";
import { apiClient, authExpiredEvent } from "@/service/apiClient";
import { authApiService } from "@/service/authApiService";
import { authEventService } from "@/service/authEventService";
import type { LoginReq, RegisterReq, User } from "@/types/auth";

type UserCtx = {
  deleteAccount: () => Promise<void>;
  isReady: boolean;
  login: (req: LoginReq) => Promise<User>;
  logout: () => Promise<void>;
  register: (req: RegisterReq) => Promise<User>;
  user: User | null;
};

export const UserContext = createContext<UserCtx | null>(null);

/** Restores authentication and exposes account actions to client components. */
export function UserProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isReady, setIsReady] = useState(false);
  const [isDenied, setIsDenied] = useState(false);
  const userId = user?.id;

  /** Clears authentication and displays the global access-denied message. */
  const denyAccess = useCallback(() => {
    apiClient.clearToken();
    setUser(null);
    setIsDenied(true);

    // Expire the HttpOnly refresh cookie after access is denied.
    authApiService.logout().catch(() => undefined);
  }, []);

  const login = useCallback(async (req: LoginReq) => {
    await authApiService.login(req);
    const profile = await authApiService.getMe();
    setIsDenied(false);
    setUser(profile);
    return profile;
  }, []);

  useEffect(() => {
    if (!userId) return;

    const ctrl = new AbortController();
    let isLive = true;
    let retry: number | undefined;

    /** Clears access immediately after a rejected or disabled status event. */
    const revoke = () => {
      if (!isLive) return;
      isLive = false;
      denyAccess();
    };

    /** Reconnects the long-lived event stream after normal timeout or failure. */
    const connect = async () => {
      try {
        await authEventService.listen(revoke, ctrl.signal);
      } catch (reason) {
        if (reason instanceof DOMException && reason.name === "AbortError") return;
      }

      if (isLive) retry = window.setTimeout(connect, 3000);
    };

    connect();

    return () => {
      isLive = false;
      ctrl.abort();
      if (retry !== undefined) window.clearTimeout(retry);
    };
  }, [denyAccess, userId]);

  const register = useCallback((req: RegisterReq) => {
    return authApiService.register(req);
  }, []);

  /** Ends the backend session and clears all local authentication state. */
  const logout = useCallback(async () => {
    await authApiService.logout();
    apiClient.clearToken();
    setUser(null);
  }, []);

  /** Deletes the current account and clears all local authentication state. */
  const deleteAccount = useCallback(async () => {
    await authApiService.deleteAccount();
    apiClient.clearToken();
    setUser(null);
  }, []);

  useEffect(() => {
    let isLive = true;

    /** Clears the current profile after token refresh is no longer possible. */
    const expire = () => {
      if (isLive) setUser(null);
    };

    window.addEventListener(authExpiredEvent, expire);

    // Restore a session from the HttpOnly refresh cookie after a page reload.
    authApiService
      .getMe()
      .then((profile) => {
        if (!isLive) return;

        if (isDeniedStatus(profile.status)) {
          denyAccess();
          return;
        }

        setUser(profile);
      })
      .catch(() => {
        if (isLive) setUser(null);
      })
      .finally(() => {
        if (isLive) setIsReady(true);
      });

    return () => {
      isLive = false;
      window.removeEventListener(authExpiredEvent, expire);
    };
  }, [denyAccess]);

  const value = useMemo(
    () => ({ deleteAccount, isReady, login, logout, register, user }),
    [deleteAccount, isReady, login, logout, register, user],
  );

  return (
    <UserContext.Provider value={value}>
      {children}
      {isDenied && <AccessDeniedModal onClose={() => setIsDenied(false)} />}
    </UserContext.Provider>
  );
}

/** Identifies account statuses that must never restore frontend access. */
function isDeniedStatus(status: User["status"]) {
  return status === "DISABLED" || status === "REJECTED";
}
