# Final Frontend Design

## 1. Scope and Page Count

The initial frontend contains **five content pages**:

| Page | Route | Access | Purpose |
|---|---|---|---|
| Welcome | `/` | Public | Introduce SenusBoard and provide login and registration entry points. |
| Dashboard | `/dashboard` | Authenticated `PENDING` or `ACTIVE` ordinary users | Display all four financial categories for a reporting period returned by the backend. |
| Comparison | `/dashboard/comparison` | Authenticated `PENDING` or `ACTIVE` ordinary users | Compare one of the two supported equivalent-period pairs. |
| Documents | `/dashboard/documents` | Authenticated `PENDING` or `ACTIVE` ordinary users | List and download available source documents. |
| Admin | `/admin` | Authenticated Admin users | Review pending registrations, find users by email address, and create ordinary accounts. |

Login and registration are modal dialogs on the Welcome page and are not separate pages. The Dashboard uses backend-provided in-page period tabs and selects the period marked `isDefault`, falling back to the first returned period. Dashboard, Comparison, and Documents remain separate routes. The four financial categories remain sections within Dashboard and Comparison pages.

## 2. Shared Application Rules

### 2.1 Access and Routing

- Unauthenticated visitors are directed to the Welcome page.
- An authenticated ordinary user with `PENDING` or `ACTIVE` status is directed to the Dashboard.
- An authenticated Admin is directed to the Admin page.
- `REJECTED` and `DISABLED` users cannot access protected pages.
- Initial session restoration validates the status returned by `GET /api/v1/auth/me`. Only `PENDING` and `ACTIVE` profiles enter authenticated frontend state; `REJECTED` or `DISABLED` profiles trigger the same access-denied flow as an SSE revocation event.
- A direct visit to a page without the required permission redirects to the appropriate accessible page.
- The frontend keeps an authenticated connection to `GET /api/v1/auth/events`. An `account-access-revoked` event with `REJECTED` or `DISABLED` clears the in-memory token and current profile, expires the refresh cookie through logout, displays an `Access denied` dialog, and returns the user to the Welcome page. The stream reconnects after timeout or transient failure while the user remains authenticated.

### 2.2 Visual Language

- Use a restrained executive-report style with strong numeric hierarchy, clear spacing, and accessible contrast.
- Use consistent category colours across navigation, cards, charts, and section headings.
- Use colour together with text or an icon; colour alone must not communicate performance or status.
- Preserve accounting signs. A larger loss, expense, or cash outflow must not be presented as a positive result.
- Format monetary values directly in EUR base units while preserving their accounting signs.
- Format percentage and ratio values consistently and identify their units.
- Display a null value as `Unavailable`, never as zero.

### 2.3 Data Classification

- Values returned directly in `growth`, `profitability`, `liquidity`, and `capital` are labelled `Reported`.
- Values inside each category's `calculated` object are labelled `Calculated`.
- Calculated values are identified with a visible `Calculated` classification badge.
- The initial fixed schema contains no estimated values. The frontend must not infer or display estimates.
- AI commentary is labelled `AI-generated analysis` and is visually separated from reported and calculated facts.

### 2.4 Loading, Empty, and Error States

- Show concise inline loading messages while profile, period, financial, Admin, or document data is loading.
- Keep the selected period and available category navigation visible when one category contains unavailable values.
- If a request fails, show its error inline without replacing cached data from another successful request. Admin pending-user loading also provides a manual refresh action.
- Authentication failures use a generic message and do not reveal whether an account exists.

## 3. Welcome Page

### 3.1 Page Content

The Welcome page contains:

- `Senus Board` brand text without a logo.
- A concise statement describing the platform as a private executive view of Senus PLC financial and operating performance.
- Supporting text explaining that the platform contains reported results, calculated metrics, analysis, and multiple reporting periods.
- A `Login` action.
- A `Sign Up` action.

The page must not expose financial data, pending registrations, or administrative controls.

### 3.2 Login Modal

The login modal contains:

- Email.
- Password with show or hide control.
- Submit action.
- Link to switch to the registration modal.

On success, the frontend retrieves the current profile. Ordinary users continue to the Dashboard and Admin users continue to the Admin page. Both `PENDING` and `ACTIVE` ordinary accounts may log in. A failed login displays the backend's generic authentication error.

### 3.3 Registration Modal

The registration modal contains:

- Name.
- Enterprise email.
- Organisation.
- User type: Management, Board, Equity Investor, or Credit Provider.
- Password and password confirmation.
- Optional description for the Admin reviewer.
- Submit action.
- Link to switch to the login modal.

The frontend validates the email format and ordinary-user password rules before submission. The backend remains authoritative for the configured enterprise email-domain allowlist. The frontend must not offer `ADMIN` as a registration type.

After successful registration, the modal explains that the account is pending review but can be used immediately. It then offers a direct login action.

## 4. Dashboard Page

### 4.1 Overall Layout

The Dashboard uses three persistent regions on desktop:

1. A left sidebar containing the four category links.
2. A shared header containing page navigation and account controls.
3. A main area containing all four category sections for the selected Dashboard period or comparison pair.

Selecting a sidebar item scrolls to its section and updates the active item as the user scrolls. It does not navigate to another page or hide the other categories.

The Documents page does not display the category sidebar because documents are not part of the four financial categories.

On narrow screens, the sidebar becomes a compact category drawer or horizontal section navigator. KPI cards and charts stack vertically, and tables may scroll horizontally while keeping metric names visible.

### 4.2 Sidebar

The sidebar contains exactly these financial category anchors:

- Growth.
- Profitability.
- Liquidity.
- Capital.

Their order is determined by the authenticated ordinary user's type. Every type can see all four categories.

| User Type | First | Second | Third | Fourth |
|---|---|---|---|---|
| Management | Growth | Profitability | Liquidity | Capital |
| Board | Capital | Liquidity | Growth | Profitability |
| Equity Investor | Capital | Growth | Profitability | Liquidity |
| Credit Provider | Liquidity | Capital | Profitability | Growth |

The main category sections use the same order as the sidebar. User type changes only order and emphasis; it never changes the available dataset.

### 4.3 Header and Page Navigation

The top bar contains:

- `Senus Board` brand text.
- Separate navigation links for Period Reports, Comparison, and Documents.
- A reusable Profile component displaying `Welcome, {name}`.
- Profile actions for Change Password, Delete Account, and Logout.

Logout calls the public backend logout endpoint, which expires the HttpOnly refresh cookie. After a successful response, the frontend clears its in-memory access token and current profile, then returns to the Welcome page.

Delete Account is available only to ordinary users because the self-delete endpoint rejects Admin accounts. It opens a destructive-action confirmation dialog before sending the authenticated delete request. After successful deletion, the frontend clears local authentication state and returns to the Welcome page.

Change Password validates a minimum of 10 characters for ordinary users and 16 characters for Admin users, together with the shared uppercase, lowercase, number, and special-character rules.

Welcome, Dashboard, and Admin use the same shared header shell. Header height, content width, horizontal padding, `Senus Board` position and typography, and Profile placement remain identical across route changes; only the navigation content changes by page and user role.

Each header navigation label opens its own route. The header remains mounted through the shared Dashboard layout while the page content changes through Next.js navigation.

The Dashboard page requests `GET /api/v1/data/reporting-periods` and creates its period-tab control from the returned periods in backend-defined order. Each tab uses the returned code as its request key and the returned label as its visible text. The period marked `isDefault` is selected initially; if no period is marked, the first returned period is selected.

Selecting a period replaces the financial dataset within the same `/dashboard` page. It does not create a new route.

The `Compare` control opens a compact selector containing exactly two valid comparison pairs:

- `FY2024 vs FY2025`.
- `HY2025 vs HY2026`.

Selecting a pair on the Comparison page loads both complete period responses and the stored comparison response. Every metric is displayed in a comparison card with two horizontal signed bars, formatted values, period codes, and `Reported` or `Calculated` classification. Comparison cards use three columns on desktop, two columns at medium widths, and one column on narrow screens. All four category sections also retain their AI comparison panels.

The frontend must not allow a custom pair. Full-year and half-year performance figures must never appear in the same comparison.

### 4.4 Shared Category Structure

Each category section contains:

1. A category heading and concise purpose statement.
2. A metric-card grid containing every displayed reported and calculated field in the fixed schema.
3. Two-bar metric cards for every field while comparison mode is active.
4. Category-specific AI analysis when available.

Reported and calculated fields remain visibly distinct. Comparison cards always include the applicable period codes, and the page description shows the selected period's start and end dates.

### 4.5 Growth Section

The Growth section displays:

- Reported revenue.
- Calculated revenue growth when an equivalent prior period exists.
- `growthAnalytics` as AI-generated analysis.

Revenue growth uses the backend-calculated value and is not recalculated independently for display.

### 4.6 Profitability Section

The Profitability section displays:

- Gross profit, gross margin, operating loss, cost of sales, and administrative expenses.
- Calculated operating margin, cost-of-sales ratio, and administrative-expense ratio.
- `profitabilityAnalytics` as AI-generated analysis.

Display one `Gross margin` metric. Use the reported gross margin when available and classify it as `Reported`; otherwise, use the calculated gross margin and classify it as `Calculated`. Losses and expenses retain their negative accounting signs.

### 4.7 Liquidity Section

The Liquidity section displays:

- Cash balance, operating cash flow, working-capital movement, current assets, current liabilities, net current position, and capital expenditure.
- Calculated operating-cash-flow margin, free cash flow, free-cash-flow margin, current ratio, and cash ratio.
- `liquidityAnalytics` as AI-generated analysis.

### 4.8 Capital Section

The Capital section displays:

- Bank debt, loan movement, interest expense, and net asset position.
- Calculated net cash.
- `capitalAnalytics` as AI-generated analysis.

A missing bank-debt value is `Unavailable` unless the source explicitly reports zero. Net cash is displayed only when both cash balance and bank debt are available.

### 4.9 Period Overview and Sources

Directly below the period or comparison selector, and before the four category sections, the Dashboard and Comparison pages display `totalAnalytics` in a clearly labelled overall AI-analysis panel.

The dedicated Documents page loads `GET /api/v1/data/documents`. Each source-document card shows its name, type, publication date, AI summary, and a download action when `downloadUrl` is available. The frontend uses the server-provided download URL and never constructs or exposes a local filesystem path.

## 5. Admin Page

### 5.1 Page Content and Initial Load

The Admin page contains:

- Page title and Admin account menu.
- A `Pending users` tab for registration review.
- A `User search` tab for existing-account management.
- A `Create user` tab for direct ordinary-account creation.

`Pending users` is the default tab. On every page load, the frontend automatically requests `GET /api/v1/admin/get-pending`. No search or manual refresh is required to obtain the initial pending list. A visible refresh action supports retry and manual synchronisation.

### 5.2 Pending-User Review

Each pending-user row or card displays:

- Name.
- Email.
- Organisation.
- Requested user type.
- Description, or `Unavailable`.
- Current status.
- `Verify` and `Reject` actions.

Both review actions require confirmation and call `POST /api/v1/admin/verify-user/{id}` with the relevant `isApproved` value. After a successful action, the user is removed from the pending list and a concise success message is shown. If there are no pending users, the section displays a clear empty state.

### 5.3 Email Search

The search section contains:

- One email input.
- A `Search` action.
- An inline validation or request-error area.
- A single user-result panel.

The frontend trims and normalises the email before calling `GET /api/v1/admin/search-user?email={email}`. Search uses an exact email address rather than partial matching. The result uses the same horizontal table layout as the pending-user list and displays name, email, organisation, description, user type, and status. A 404 response displays `No user found` without affecting the pending list.

The search-result actions depend on the current account status. A `PENDING` user shows `Verify` and `Reject`; a successful review immediately updates the result to `ACTIVE` or `REJECTED`. An `ACTIVE` ordinary user shows `Disable` and calls `POST /api/v1/admin/disable-user/{id}`. A `DISABLED` or `REJECTED` ordinary user shows `Delete` and calls `DELETE /api/v1/admin/delete-user/{id}`. Every state-changing action requires confirmation. Admin results do not expose an enabled management action.

### 5.4 Create User

The Create user tab contains name, email, organisation, user type, password, and password-confirmation fields. User type is limited to Management, Board, Equity Investor, and Credit Provider; Admin is never offered. The frontend mirrors the ordinary-user password rules before sending `POST /api/v1/admin/create-user`. A successfully created account is active immediately and its safe profile is shown in the same horizontal user-table layout.

## 6. Frontend Data Requests

| Screen or Component | Request | Trigger |
|---|---|---|
| Login modal | `POST /api/v1/auth/login` and `GET /api/v1/auth/me` | Login submission |
| Registration modal | `POST /api/v1/auth/register` | Registration submission |
| Account status listener | `GET /api/v1/auth/events` | Authenticated session start and SSE reconnection |
| Dashboard period tabs | `GET /api/v1/data/reporting-periods` | Dashboard entry |
| Dashboard categories | `GET /api/v1/data/{period}` | Initial load and period change |
| Dashboard comparison | Two `GET /api/v1/data/{period}` requests and `GET /api/v1/data/comparisons?basePeriod={base}&targetPeriod={target}` | Initial comparison load and compare-pair selection |
| Dashboard sources | `GET /api/v1/data/documents` | Documents page entry |
| Pending-user review | `GET /api/v1/admin/get-pending` | Admin page entry and manual refresh |
| Email search | `GET /api/v1/admin/search-user?email={email}` | Search submission |
| Create user | `POST /api/v1/admin/create-user` | Create-user form submission |
| Registration review | `POST /api/v1/admin/verify-user/{id}` | Verify or reject confirmation |
| Disable user | `POST /api/v1/admin/disable-user/{id}` | Disable confirmation |
| Delete user | `DELETE /api/v1/admin/delete-user/{id}` | Delete confirmation |

Period and comparison requests may run in parallel. A reporting-period change must update the four dashboard categories as one coherent view so values from different selected periods are never mixed.

## 7. Out of Scope for the Initial Frontend

- Separate pages for the four financial categories.
- A separate login page or registration page.
- Monthly reporting or month-on-month revenue growth.
- Historical EBITDA, EBITDA margin, an EBITDA-to-free-cash-flow bridge, debt-service coverage ratio, ROCE, or cash runway.
- User-defined formulas, forecasts, or frontend-generated estimates.
- Admin self-registration or Admin privilege delegation.
- Displaying metrics excluded from the final fixed schema, including customer mix, geographic mix, pipeline, contingent consideration, and Senus 2030 target progress.
