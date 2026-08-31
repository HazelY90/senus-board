"use client";

import { useContext } from "react";
import { DataContext } from "../DataContext";

/** Returns the shared Dashboard data context for a client component. */
export function useData() {
  const ctx = useContext(DataContext);

  if (!ctx) {
    throw new Error("useData must be used within DataProvider");
  }

  return ctx;
}
