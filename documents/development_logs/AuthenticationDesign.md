# Authentication Design

## 1. Purpose

SenusBoard is a private platform for specific organisational and financial stakeholders. It is not a public consumer service and does not provide features for unrestricted individual accounts.

Authentication must therefore control both account creation and platform access. An ordinary user may access protected platform data while the registration is pending, but access ends immediately if an Admin rejects or disables the account.

## 2. User Types

The platform supports four ordinary user types defined by the product requirements:

- Management.
- Board.
- Equity Investors.
- Credit Providers.

The platform also supports an Admin user type. Admin users manage account access and user lifecycle operations but are not an additional financial-report audience.

An ordinary user must have exactly one of the four ordinary user types. The backend validates the selected type during registration, and the type applies while the account is pending. An Admin reviews the registration without delaying the user's initial access.

## 3. Account Creation

Ordinary accounts can be created through either of two controlled flows.

### 3.1 Registration and Approval

An ordinary user may submit a registration request only with an email address whose domain matches the configured enterprise email-domain allowlist. The frontend validates the email domain before submission, and the backend independently validates it before creating the account. Frontend validation improves feedback but is never treated as a security control.

A successful registration creates a `PENDING` account. A pending user may authenticate and access protected platform data immediately while waiting for review.

An Admin may approve or reject the request. Approval changes the account to `ACTIVE` without interrupting access. Rejection changes the account to `REJECTED`, blocks further authentication and data access, and invalidates the account's existing sessions and refresh tokens.

### 3.2 Admin-Created Account

An Admin may directly create an account for any of the four ordinary user types. This supports authorised users who are provisioned internally without completing the registration flow. The backend applies the same enterprise email-domain policy to these accounts so the registration restriction cannot be bypassed through administrative creation.

An Admin-created ordinary account is created with `ACTIVE` status and can authenticate with the supplied credential immediately.

## 4. Admin Provisioning

Admin registration is never exposed through the frontend or a public registration endpoint.

The initial Admin account must be created through a backend-controlled setup process. Additional Admin access must also require an existing trusted administrative process and must never be granted through ordinary self-registration.

The exact bootstrap mechanism, credential delivery method, and Admin management policy will be defined before implementation.

## 5. User Schema

Authentication uses one `users` table.

| Column | Type | Rule |
| --- | --- | --- |
| id | BIGINT | Primary key and generated identifier |
| name | VARCHAR(100) | Required display name |
| email | VARCHAR(255) | Required, unique, and stored in normalised lowercase form |
| password | VARCHAR(255) | Required password hash; plaintext passwords are never stored |
| role | VARCHAR(30) | Required user type |
| organization | VARCHAR(255) | Required organisation name |
| status | VARCHAR(20) | Required account lifecycle status |
| description | TEXT | Optional registration explanation for the Admin reviewer |
| created_at | TIMESTAMP | Required account creation time |

Supported `role` values are:

- `MANAGEMENT`.
- `BOARD`.
- `EQUITY_INVESTOR`.
- `CREDIT_PROVIDER`.
- `ADMIN`.

Supported `status` values are:

- `PENDING`: registration submitted, allowed to authenticate and access protected data while awaiting review.
- `ACTIVE`: registration approved, allowed to authenticate and access protected data.
- `REJECTED`: registration rejected, blocked from authentication and protected data.
- `DISABLED`: previously active account disabled by an Admin and blocked from authentication and protected data.

A registration request may select one of the four ordinary roles. Registration must reject the `ADMIN` role. Backend-provisioned Admin accounts are created with `ADMIN` and `ACTIVE` values.

## 6. Credential Requirements

### 6.1 Enterprise Email Domains

The system maintains a configured allowlist of enterprise email domains. Email addresses are trimmed and normalised to lowercase before comparison. The domain portion after the final `@` must exactly match an allowlisted domain; lookalike suffixes and unconfigured subdomains are rejected.

The frontend performs the same validation for immediate user feedback. The backend repeats the validation for every ordinary account creation request and remains authoritative. A request with a disallowed domain returns a validation error and does not create an account.

### 6.2 Password Strength

Passwords for the four ordinary user types must contain at least 10 characters. Admin passwords must contain at least 16 characters. Every password must include at least one uppercase letter, one lowercase letter, one number, and one special character.

These requirements apply to registration, Admin-created accounts, initial Admin provisioning, password changes, and credential resets. The frontend may mirror the rules for immediate feedback, but the backend must enforce them before accepting a password.

## 7. Authentication and Admin APIs

Authentication endpoints use the `/api/v1/auth` base path. Administrative user-management endpoints use the `/api/v1/admin` base path.

| Method | Endpoint | Authentication | Purpose |
| --- | --- | --- | --- |
| POST | `/api/v1/auth/register` | Public | Submit an ordinary account registration for Admin review |
| POST | `/api/v1/auth/login` | Public | Authenticate a pending or active account and issue access and refresh tokens |
| GET | `/api/v1/auth/me` | Access token | Return the authenticated user's profile |
| POST | `/api/v1/auth/refresh` | Refresh token | Issue a new access token from a valid refresh token |
| POST | `/api/v1/auth/logout` | Public | Expire the browser refresh-token cookie |
| DELETE | `/api/v1/auth/delete` | Access token | Permanently delete the authenticated non-Admin account |
| POST | `/api/v1/auth/change-password` | Access token | Replace the authenticated user's password after verifying the current password |
| GET | `/api/v1/admin/search-user?email={email}` | Admin access token | Find a user by email address |
| GET | `/api/v1/admin/get-pending` | Admin access token | Return all users awaiting Admin review |
| POST | `/api/v1/admin/create-user` | Admin access token | Create an active ordinary user without registration review |
| POST | `/api/v1/admin/verify-user/{id}` | Admin access token | Approve a pending registration |
| POST | `/api/v1/admin/disable-user/{id}` | Admin access token | Disable an active ordinary user and terminate access |
| DELETE | `/api/v1/admin/delete-user/{id}` | Admin access token | Permanently delete an ordinary user account |

### 7.1 Register

The registration request contains `name`, `email`, `password`, `role`, `organization`, and optional `description`. The role must be one of the four ordinary roles, the email must match the configured enterprise email-domain allowlist, and the password must satisfy the ordinary-user password requirements.

A successful registration creates a `PENDING` user and returns HTTP 201. The user may then authenticate immediately; registration does not depend on Admin review.

### 7.2 Login

The login request contains `email` and `password`. `PENDING` and `ACTIVE` accounts can authenticate successfully. `REJECTED` and `DISABLED` accounts cannot authenticate.

A successful login returns the access token as an `accessToken` JSON field. The refresh token is returned only as an HTTP-only cookie scoped to `/api/v1/auth`; it is not included in the JSON body. The login response does not include the authenticated user's profile or the stored password hash.

Authentication failure returns a generic response that does not disclose whether the email, password, or account status caused the failure.

### 7.3 Current User

The current-user endpoint returns `id`, `name`, `email`, `organization`, `description`, `role`, and `status` for the authenticated user. It never returns `password`.

### 7.4 Refresh

The refresh request reads the refresh token from the HTTP-only `refreshToken` cookie. A valid refresh token for a `PENDING` or `ACTIVE` account returns a new access token as an `accessToken` JSON field. Refresh requests for `REJECTED` or `DISABLED` accounts are denied.

### 7.5 Change Password

The change-password request contains `currentPassword` and `newPassword`. The backend verifies the current password, applies the password requirements for the authenticated user's role, hashes the new password, and replaces the stored hash. A successful change returns HTTP 204.

### 7.6 Logout

The logout endpoint accepts no request body and does not require a valid access token. It returns HTTP 204 with an empty HTTP-only `refreshToken` cookie using the same `/api/v1/auth` path and an immediate expiry. This overwrites the browser cookie even when the access token has already expired. Because access tokens are stateless, the client must also discard its local access token.

### 7.7 Delete Account

The delete endpoint permanently deletes the account identified by the authenticated principal. It accepts no path ID or request body, so an authenticated account cannot select another account for deletion. Admin accounts cannot use this endpoint and receive HTTP 403. A successful deletion returns HTTP 204 and expires the refresh-token cookie. Existing access and refresh tokens can no longer authenticate because their user ID no longer exists.

### 7.8 Search User

The search-user endpoint is available only to an authenticated `ADMIN` user. The required `email` query parameter is trimmed and normalised to lowercase before an exact lookup.

A successful lookup returns HTTP 200 with the user's safe profile: `id`, `name`, `email`, `organization`, `description`, `role`, and `status`. It never returns `password`. If no user matches the email address, the endpoint returns HTTP 404.

### 7.9 Get Pending Users

The get-pending endpoint is available only to an authenticated `ADMIN` user. It accepts no request body or query parameters.

A successful request returns HTTP 200 with all users whose status is `PENDING`. Each result uses `UserDto` and contains `id`, `name`, `email`, `organization`, `description`, `role`, and `status`. The response is an empty list when no pending users exist and never includes password hashes.

### 7.10 Create User

The create-user endpoint is available only to an authenticated `ADMIN` user. The request contains `name`, `email`, `password`, `role`, and `organization`. The role must be one of the four ordinary roles and must not be `ADMIN`. The backend validates the enterprise email domain and ordinary-user password strength.

A successful request creates the user with `ACTIVE` status and returns HTTP 201. The account can authenticate immediately and does not enter the registration review flow.

### 7.11 Verify User

The verify-user endpoint is available only to an authenticated `ADMIN` user. The `{id}` path parameter identifies the target user. The request body contains the required Boolean field `isApproved`.

The target user must have `PENDING` status. When `isApproved` is `true`, the backend changes the status to `ACTIVE`. When it is `false`, the backend changes the status to `REJECTED`. A successful review returns HTTP 204.

An account that is already active, rejected, or disabled cannot be processed through the registration verification flow again.

### 7.12 Disable User

The disable-user endpoint is available only to an authenticated `ADMIN` user. The `{id}` path parameter identifies the target user, which must be an ordinary user with `ACTIVE` status. No request body is required.

A successful request changes the target account to `DISABLED`, invalidates its existing sessions and refresh tokens, and returns HTTP 204. A disabled account cannot authenticate or access protected data.

### 7.13 Delete User

The delete-user endpoint is available only to an authenticated `ADMIN` user. The `{id}` path parameter identifies the target ordinary account, and no request body is required. An `ADMIN` account cannot be deleted through this endpoint; attempting to do so returns HTTP 403.

A successful request permanently deletes the target account and returns HTTP 204. Existing access and refresh tokens can no longer authenticate because the user ID no longer exists.

## 8. Admin Permissions

An Admin may:

- Search for a user by email address.
- List all users awaiting review.
- Create a new ordinary user.
- Approve a pending registration.
- Disable an active ordinary user.
- Permanently delete an ordinary user.

Admin creation and privilege delegation are not exposed through the ordinary user-management API.

## 9. Access Principles

- Unauthenticated users cannot access protected financial data.
- Pending and active ordinary users can access protected financial data according to their role.
- Rejected and disabled users cannot access protected financial data.
- Ordinary users cannot approve registrations, create other users, or grant Admin access.
- Admin users can search for users, list pending registrations, create ordinary accounts, review pending registrations, and disable active ordinary users.
- The backend remains authoritative for account status and user type.
- Every protected request must check the current account status so a rejection or disable action takes effect immediately, even if an access token has not yet expired.
- Authentication failures must not reveal whether a specific account exists.

### 9.1 Real-Time Account Status Feedback

The backend uses Server-Sent Events (SSE) to notify the connected frontend when an ordinary user's status changes to `REJECTED` or `DISABLED`. The frontend refreshes immediately after receiving the status event so the updated access state is reflected without waiting for the user's next navigation or request.

SSE provides real-time user-interface feedback only. Backend account-status validation remains authoritative for every protected request.

## 10. Deferred Design

The following details will be defined separately in this document before implementation:

- Access-token and refresh-token lifecycle.
- Admin bootstrap and recovery procedures.
- Account reactivation policy.
- Audit requirements for administrative actions.
