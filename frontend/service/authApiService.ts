import { apiClient } from "./apiClient";
import type {
  ChangePasswordReq,
  LoginReq,
  RegisterReq,
  TokenRes,
  User,
} from "@/types/auth";

/** Groups backend requests related to authentication and account access. */
class AuthApiService {
  /** Authenticates credentials without attaching an existing access token. */
  login(req: LoginReq) {
    return apiClient.post<TokenRes>("/auth/login", req, false);
  }

  /** Registers an ordinary account without attaching an access token. */
  register(req: RegisterReq) {
    return apiClient.post<User>("/auth/register", req, false);
  }

  /** Returns the latest profile for the authenticated account. */
  getMe() {
    return apiClient.get<User>("/auth/me", true);
  }

  /** Changes the password for the authenticated account. */
  changePassword(req: ChangePasswordReq) {
    return apiClient.post<void>("/auth/change-password", req, true);
  }

  /** Expires the refresh-token cookie without requiring an access token. */
  logout() {
    return apiClient.post<void>("/auth/logout", undefined, false);
  }

  /** Permanently deletes the authenticated ordinary account. */
  deleteAccount() {
    return apiClient.delete<void>("/auth/delete", true);
  }
}

/** Shared authentication API service used by authentication features. */
export const authApiService = new AuthApiService();
