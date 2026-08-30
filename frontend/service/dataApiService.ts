import { apiClient } from "./apiClient";
import type {
  Comparison,
  DocumentsRes,
  PeriodData,
  PeriodsRes,
} from "@/types/data";

/** Groups protected backend requests for financial and source-document data. */
class DataApiService {
  /** Returns all reporting periods available to the dashboard. */
  getPeriods() {
    return apiClient.get<PeriodsRes>("/data/reporting-periods", true);
  }

  /** Returns the complete dataset for one canonical reporting-period code. */
  getPeriod(code: string) {
    return apiClient.get<PeriodData>(`/data/${encodeURIComponent(code)}`, true);
  }

  /** Returns stored comparison analysis for one supported ordered pair. */
  getComparison(base: string, target: string) {
    const query = new URLSearchParams({ basePeriod: base, targetPeriod: target });
    return apiClient.get<Comparison>(`/data/comparisons?${query}`, true);
  }

  /** Returns source-document metadata in backend-defined order. */
  getDocuments() {
    return apiClient.get<DocumentsRes>("/data/documents", true);
  }

  /** Downloads a protected source document as a browser Blob. */
  download(path: string) {
    return apiClient.get<Blob>(path, true, { responseType: "blob" });
  }
}

/** Shared financial-data service used by the Dashboard data context. */
export const dataApiService = new DataApiService();
