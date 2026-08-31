/** Lists the ordinary and administrative roles returned by the backend. */
export type UserRole =
  | "MANAGEMENT"
  | "BOARD"
  | "EQUITY_INVESTOR"
  | "CREDIT_PROVIDER"
  | "ADMIN";

/** Lists the account lifecycle states returned by the backend. */
export type UserStatus = "PENDING" | "ACTIVE" | "REJECTED" | "DISABLED";

/** Describes the safe user profile returned by authentication endpoints. */
export type User = {
  id: number;
  name: string;
  email: string;
  organization: string;
  description: string | null;
  role: UserRole;
  status: UserStatus;
};

/** Describes the credentials submitted to the login endpoint. */
export type LoginReq = {
  email: string;
  password: string;
};

/** Describes an ordinary-user registration request. */
export type RegisterReq = {
  name: string;
  email: string;
  password: string;
  role: Exclude<UserRole, "ADMIN">;
  organization: string;
  description?: string;
};

/** Describes an Admin request that creates an active ordinary account. */
export type CreateUserReq = {
  name: string;
  email: string;
  password: string;
  role: Exclude<UserRole, "ADMIN">;
  organization: string;
};

/** Describes an authentication response containing a new access token. */
export type TokenRes = {
  accessToken: string;
};

/** Describes an authenticated password-change request. */
export type ChangePasswordReq = {
  currentPassword: string;
  newPassword: string;
};
