import { apiClient } from "./apiClient";
import type { CreateUserReq, User } from "@/types/auth";

/** Groups authenticated backend requests used by Admin user management. */
class AdminApiService {
  /** Returns every account currently awaiting Admin review. */
  getPending() {
    return apiClient.get<User[]>("/admin/get-pending", true);
  }

  /** Finds one account by its exact email address. */
  searchUser(email: string) {
    const query = new URLSearchParams({ email });
    return apiClient.get<User>(`/admin/search-user?${query}`, true);
  }

  /** Creates one active ordinary account without registration review. */
  createUser(req: CreateUserReq) {
    return apiClient.post<User>("/admin/create-user", req, true);
  }

  /** Approves or rejects one pending account. */
  verifyUser(id: number, isApproved: boolean) {
    return apiClient.post<void>(
      `/admin/verify-user/${id}`,
      { isApproved },
      true,
    );
  }

  /** Disables one active ordinary account. */
  disableUser(id: number) {
    return apiClient.post<void>(`/admin/disable-user/${id}`, undefined, true);
  }

  /** Permanently deletes one ordinary account. */
  deleteUser(id: number) {
    return apiClient.delete<void>(`/admin/delete-user/${id}`, true);
  }
}

/** Shared Admin API service used by administrative features. */
export const adminApiService = new AdminApiService();
