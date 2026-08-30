"use client";

import { useContext } from "react";
import { UserContext } from "../UserContext";

/** Returns the active authentication context for a client component. */
export function useAuth() {
  const ctx = useContext(UserContext);

  if (!ctx) {
    throw new Error("useAuth must be used within UserProvider");
  }

  return ctx;
}
