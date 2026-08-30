"use client";

import {
  createContext,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { dataApiService } from "@/service/dataApiService";
import type {
  Comparison,
  DataDoc,
  Period,
  PeriodData,
} from "@/types/data";

type DataCtx = {
  comparisons: Record<string, Comparison>;
  documents: DataDoc[] | null;
  downloadDoc: (doc: DataDoc) => Promise<void>;
  loadComparison: (base: string, target: string) => Promise<Comparison>;
  loadDocuments: () => Promise<DataDoc[]>;
  loadPeriod: (code: string) => Promise<PeriodData>;
  periodData: Record<string, PeriodData>;
  periods: Period[];
};

export const DataContext = createContext<DataCtx | null>(null);

/** Caches Dashboard API data across navigation between Dashboard pages. */
export function DataProvider({ children }: { children: ReactNode }) {
  const [periods, setPeriods] = useState<Period[]>([]);
  const [periodData, setPeriodData] = useState<Record<string, PeriodData>>({});
  const [comparisons, setComparisons] = useState<Record<string, Comparison>>(
    {},
  );
  const [documents, setDocuments] = useState<DataDoc[] | null>(null);
  const periodRef = useRef(new Map<string, PeriodData>());
  const comparisonRef = useRef(new Map<string, Comparison>());
  const docRef = useRef<DataDoc[] | null>(null);
  const reqRef = useRef(new Map<string, Promise<unknown>>());

  const loadPeriods = useCallback(async () => {
    const key = "periods";
    const active = reqRef.current.get(key) as Promise<Period[]> | undefined;
    if (active) return active;

    const req = dataApiService
      .getPeriods()
      .then(({ periods: values }) => {
        setPeriods(values);
        return values;
      })
      .finally(() => reqRef.current.delete(key));

    reqRef.current.set(key, req);
    return req;
  }, []);

  const loadPeriod = useCallback(async (code: string) => {
    const cached = periodRef.current.get(code);
    if (cached) return cached;

    const key = `period:${code}`;
    const active = reqRef.current.get(key) as Promise<PeriodData> | undefined;
    if (active) return active;

    const req = dataApiService
      .getPeriod(code)
      .then((data) => {
        periodRef.current.set(code, data);
        setPeriodData((current) => ({ ...current, [code]: data }));
        return data;
      })
      .finally(() => reqRef.current.delete(key));

    reqRef.current.set(key, req);
    return req;
  }, []);

  const loadComparison = useCallback(async (base: string, target: string) => {
    const pair = `${base}:${target}`;
    const cached = comparisonRef.current.get(pair);
    if (cached) return cached;

    const key = `comparison:${pair}`;
    const active = reqRef.current.get(key) as Promise<Comparison> | undefined;
    if (active) return active;

    const req = dataApiService
      .getComparison(base, target)
      .then((data) => {
        comparisonRef.current.set(pair, data);
        setComparisons((current) => ({ ...current, [pair]: data }));
        return data;
      })
      .finally(() => reqRef.current.delete(key));

    reqRef.current.set(key, req);
    return req;
  }, []);

  const loadDocuments = useCallback(async () => {
    if (docRef.current) return docRef.current;

    const key = "documents";
    const active = reqRef.current.get(key) as Promise<DataDoc[]> | undefined;
    if (active) return active;

    const req = dataApiService
      .getDocuments()
      .then(({ documents: values }) => {
        docRef.current = values;
        setDocuments(values);
        return values;
      })
      .finally(() => reqRef.current.delete(key));

    reqRef.current.set(key, req);
    return req;
  }, []);

  const downloadDoc = useCallback(async (doc: DataDoc) => {
    if (!doc.downloadUrl) return;

    const blob = await dataApiService.download(doc.downloadUrl);
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = doc.name;
    document.body.appendChild(link);
    link.click();
    link.remove();

    // Release the temporary object URL after the browser starts the download.
    window.setTimeout(() => URL.revokeObjectURL(url), 0);
  }, []);

  useEffect(() => {
    // Load navigation metadata once after authenticated Dashboard entry.
    loadPeriods().catch(() => undefined);
  }, [loadPeriods]);

  const value = useMemo(
    () => ({
      comparisons,
      documents,
      downloadDoc,
      loadComparison,
      loadDocuments,
      loadPeriod,
      periodData,
      periods,
    }),
    [
      comparisons,
      documents,
      downloadDoc,
      loadComparison,
      loadDocuments,
      loadPeriod,
      periodData,
      periods,
    ],
  );

  return <DataContext.Provider value={value}>{children}</DataContext.Provider>;
}
