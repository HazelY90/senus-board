"use client";

import {
  createContext,
  useCallback,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { authExpiredEvent } from "@/service/apiClient";
import { authApiService } from "@/service/authApiService";
import type { LoginReq, RegisterReq, User } from "@/types/auth";

type UserCtx = {
  isReady: boolean;
  login: (req: LoginReq) => Promise<User>;
  register: (req: RegisterReq) => Promise<User>;
  user: User | null;
};

export const UserContext = createContext<UserCtx | null>(null);

/** Restores authentication and exposes account actions to client components. */
export function UserProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [isReady, setIsReady] = useState(false);

  const login = useCallback(async (req: LoginReq) => {
    await authApiService.login(req);
    const profile = await authApiService.getMe();
    setUser(profile);
    return profile;
  }, []);

  const register = useCallback((req: RegisterReq) => {
    return authApiService.register(req);
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
        if (isLive) setUser(profile);
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
  }, []);

  const value = useMemo(
    () => ({ isReady, login, register, user }),
    [isReady, login, register, user],
  );

  return <UserContext.Provider value={value}>{children}</UserContext.Provider>;
}
