"use client";

import { useEffect, useState } from "react";
import { useData } from "../hooks/useData";
import type { DataDoc } from "@/types/data";
import { formatDate } from "@/utils/format";
import colors from "@/public/colors.json";

/** Loads and displays source documents without the financial-category sidebar. */
export default function DocumentsView() {
  const { documents, downloadDoc, loadDocuments } = useData();
  const [downloadName, setDownloadName] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let isLive = true;
    if (documents) return;

    loadDocuments().catch((reason) => {
      if (isLive) {
        setError(reason instanceof Error ? reason.message : "Request failed.");
      }
    });

    return () => {
      isLive = false;
    };
  }, [documents, loadDocuments]);

  /** Downloads one protected document and reports request failures inline. */
  const download = async (doc: DataDoc) => {
    setError(null);
    setDownloadName(doc.name);

    try {
      await downloadDoc(doc);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Download failed.");
    } finally {
      setDownloadName(null);
    }
  };

  return (
    <main className="mx-auto max-w-[1500px] px-5 py-8 sm:px-8 lg:px-12 lg:py-10">
      <header className="max-w-4xl">
        <p
          className="text-xs font-bold uppercase tracking-[0.2em]"
          style={{ color: colors.light_theme }}
        >
          Source registry
        </p>
        <h1
          className="mt-3 text-4xl font-bold tracking-[-0.035em] sm:text-5xl"
          style={{ color: colors.dark_theme }}
        >
          Documents
        </h1>
        <p className="mt-4 text-base leading-7 text-slate-600 sm:text-lg">
          Review the disclosures supporting reported financial information and
          download available source files.
        </p>
      </header>

      {error && (
        <p className="mt-7 rounded-2xl border border-red-200 bg-red-50 px-5 py-4 text-sm text-red-700">
          {error}
        </p>
      )}

      {!documents && !error && (
        <p
          className="mt-10 rounded-2xl border border-slate-200 bg-white px-6 py-5 text-sm font-semibold shadow-sm"
          style={{ color: colors.main_theme }}
        >
          Loading source documents...
        </p>
      )}

      {documents?.length === 0 && (
        <p className="mt-10 rounded-2xl border border-slate-200 bg-white px-6 py-8 text-center text-slate-600 shadow-sm">
          No source documents are available.
        </p>
      )}

      {documents && documents.length > 0 && (
        <div className="mt-10 flex flex-col gap-5">
          {documents.map((doc) => (
            <article
              className="flex w-full min-w-0 flex-col rounded-3xl border border-slate-200 bg-white p-6 shadow-sm"
              key={`${doc.name}:${doc.publicationDate ?? "unknown"}`}
            >
              <div className="flex min-w-0 flex-wrap items-start justify-between gap-4">
                <span
                  className="max-w-full break-words rounded-full bg-teal-50 px-3 py-1 text-xs font-bold uppercase tracking-wide"
                  style={{ color: colors.main_theme }}
                >
                  {doc.type.replaceAll("_", " ")}
                </span>
                <time className="shrink-0 text-xs font-medium text-slate-500">
                  {formatDate(doc.publicationDate)}
                </time>
              </div>
              <div className="mt-5 flex min-w-0 items-center justify-between gap-5">
                <h2
                  className="min-w-0 break-words text-xl font-bold leading-7"
                  style={{ color: colors.dark_theme }}
                >
                  {doc.name}
                </h2>
                <button
                  className="h-10 shrink-0 cursor-pointer rounded-xl border-2 px-5 text-sm font-semibold transition-colors disabled:cursor-not-allowed disabled:border-slate-200 disabled:text-slate-400"
                  disabled={!doc.downloadUrl || downloadName === doc.name}
                  style={
                    doc.downloadUrl
                      ? { borderColor: colors.light_theme, color: colors.main_theme }
                      : undefined
                  }
                  type="button"
                  onClick={() => download(doc)}
                >
                  {!doc.downloadUrl
                    ? "Unavailable"
                    : downloadName === doc.name
                      ? "Downloading..."
                      : "Download"}
                </button>
              </div>
              <p className="mt-4 whitespace-pre-wrap break-words text-sm leading-6 text-slate-600">
                {doc.aiSummary ?? "Unavailable"}
              </p>
            </article>
          ))}
        </div>
      )}
    </main>
  );
}
