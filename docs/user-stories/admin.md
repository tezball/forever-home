# Admin User Stories

> **User Type:** Platform administrators who manage users, approvals, content, and analytics

## Overview

Admins are platform administrators responsible for maintaining the integrity and safety of Forever Home. They verify rescue organizations and veterinarians, manage user accounts, moderate content, and monitor platform analytics.

The first admin account is bootstrapped via the `ADMIN_EMAIL` environment variable on startup. Additional admins can be created by existing admins.

## Related Documentation

- [Index](index.md) - Platform overview and all user types
- [Rescue Organization Stories](rescue-organization.md) - Organizations requiring approval
- [Vet Stories](vet.md) - Vets requiring license verification
- [Domain Model](../domain-model.md) - Entity definitions
- [UI Style Guide](../ui-style-guide.md) - Component specifications

---

## User Journey

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│    Login    │────►│   Review    │────►│   Manage    │────►│   Monitor   │
│             │     │  Approvals  │     │    Users    │     │  Analytics  │
└─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
                           │                   │
                           ▼                   ▼
                    ┌─────────────┐     ┌─────────────┐
                    │   Approve   │     │   Suspend   │
                    │  or Reject  │     │  Accounts   │
                    └─────────────┘     └─────────────┘
                                               │
                                               ▼
                                        ┌─────────────┐
                                        │  Moderate   │
                                        │   Content   │
                                        └─────────────┘
```

**Typical Flow:**
1. Login to admin dashboard
2. Review pending approval queue (vets and rescue organizations)
3. Verify credentials and approve or reject
4. Manage user accounts as needed
5. Monitor platform analytics
6. Moderate flagged content

---

## Admin Bootstrap

The first admin account is created automatically on application startup:

**Configuration:**
```properties
ADMIN_EMAIL=admin@foreverhome.com
```

**Bootstrap Process:**
1. Application starts
2. Checks if any admin exists
3. If no admin exists and `ADMIN_EMAIL` is set:
   - Creates `User` with `UserRole = Admin`
   - Sets `User.status = Active`
   - Sends password setup email to `ADMIN_EMAIL`
4. Admin completes password setup via email link

**Note:** Admin accounts do not require email verification or additional approval - they are immediately active.

---

## User Approval

### US-6.1: Approve User Registrations

**As an** admin
**I want to** approve rescue organization and vet registrations
**So that** only legitimate entities operate on the platform

**Acceptance Criteria:**
- Queue shows pending registrations (Vets and Rescue Orgs with `verified: false`)
- View submitted profile details and credentials
- For vets: Shows license number for verification
- For rescues: Shows organization details and contact info
- Approve (`verified: true`) or reject with reason
- Rejection sets `User.status = Suspended` with reason (or allows resubmission)
- User is notified of decision via email

**Domain Notes:**
- Query: `Vet WHERE verified = false` UNION `RescueOrganization WHERE verified = false`
- Approve: Sets `verified = true`
- Reject: Sets `User.status = Suspended` with reason

**Approval Queue Display:**
| Field | Vet | Rescue Org |
|-------|-----|------------|
| Name | Clinic Name | Organization Name |
| Contact | Phone, Email | Contact Name, Email |
| Location | Address | Address |
| Credential | License Number | N/A |
| Website | Optional | Optional |
| Submitted | Date | Date |

**UI Components:**
- Approval queue: List with entity type badge (Vet/Rescue)
- Detail panel: Slide-out or modal with full profile
- Action buttons: "Approve" (primary) | "Reject" (destructive)
- Reject modal: Reason textarea (required)
- Filter tabs: All | Vets | Rescue Orgs

**Verification Steps:**
1. **For Vets:**
   - Verify license number with state licensing board
   - Confirm clinic address exists
   - Check for any disciplinary actions

2. **For Rescue Organizations:**
   - Verify organization exists (website, social media)
   - Confirm contact information is valid
   - Check for any red flags or complaints

**Priority:** P0 - MVP

---

## User Management

### US-6.2: Manage All Users

**As an** admin
**I want to** view and manage all platform users
**So that** I can maintain platform integrity

**Acceptance Criteria:**
- Search users by name or email
- Filter users by role (Foster, Adopter, Vet, RescueOrg, Admin)
- Filter by status (Active, Pending, Suspended)
- Suspend or reactivate accounts
- Trigger password reset for any user
- View user activity summary

**Domain Notes:**
- Query: `User` with filters
- Suspend: Sets `User.status = Suspended`
- Reactivate: Sets `User.status = Active`
- Password reset: Generates reset token, sends email

**User Table Columns:**
| Column | Description |
|--------|-------------|
| Name | First + Last name (or Org name) |
| Email | User email address |
| Role | Foster, Adopter, Vet, Rescue Org |
| Status | Active, Pending, Suspended |
| Verified | Yes/No (for Vet/Rescue only) |
| Joined | Registration date |
| Last Active | Last login timestamp |
| Actions | Menu with available actions |

**UI Components:**
- Search input: Top of page
- Filter dropdowns: Role, Status
- User table: Sortable columns
- Actions menu: Suspend, Reactivate, Reset Password, View Activity
- Bulk actions: Select multiple users for batch operations

**User Actions:**
| Action | Effect | Notification |
|--------|--------|--------------|
| Suspend | Sets status to Suspended, logs out user | Email sent with reason |
| Reactivate | Sets status to Active | Email confirmation |
| Reset Password | Generates reset link | Email with reset link |
| View Activity | Opens activity log modal | None |

**Priority:** P2 - Enhanced

---

## Analytics

### US-6.3: Platform Analytics

**As an** admin
**I want to** view platform statistics
**So that** I can understand platform usage and success

**Acceptance Criteria:**
- Dashboard shows key metrics:
  - Total users by role
  - Total pets by status
  - Adoption rate (completed / total listed)
  - Average time to adoption
- Charts show trends over time (30/60/90 days)
- Export reports as CSV

**Metrics Dashboard:**

**User Metrics:**
| Metric | Description |
|--------|-------------|
| Total Users | Count by role (Foster, Adopter, Vet, Rescue) |
| New Users | Registrations in selected period |
| Active Users | Users who logged in during period |
| Verification Queue | Pending Vet + Rescue approvals |

**Pet Metrics:**
| Metric | Description |
|--------|-------------|
| Total Pets | All registered pets |
| By Status | Draft, PendingRescue, PendingVet, Available, InProgress, Adopted |
| Adoption Rate | Adopted / (Adopted + Withdrawn) |
| Avg. Time to Adopt | Days from Available to Adopted |

**Trend Charts:**
- User registrations over time (line chart)
- Pet registrations over time (line chart)
- Adoptions over time (bar chart)
- Pets by status (pie chart)

**UI Components:**
- Stat cards: Large number + label + trend indicator (up/down arrow)
- Line charts: Registrations, adoptions over time
- Pie chart: Pets by status
- Date range selector: Last 30/60/90 days, custom range
- Export button: Download CSV

**Priority:** P3 - Polish

---

## Content Moderation

### US-6.4: Content Moderation

**As an** admin
**I want to** review flagged content
**So that** I can maintain appropriate platform content

**Acceptance Criteria:**
- Queue shows flagged pet profiles and user reports
- View full content and flag reason
- Dismiss flag (false positive)
- Remove content (hide pet profile)
- Warn user via email
- Suspend repeat offenders
- Audit log of all moderation actions

**Domain Notes:**
- Flagged content stored with reason and reporter
- Actions logged with admin ID and timestamp
- Moderation history preserved for patterns

**Flag Types:**
| Type | Description | Severity |
|------|-------------|----------|
| Inappropriate Image | Pet image contains inappropriate content | High |
| Misleading Info | Pet description is inaccurate or deceptive | Medium |
| Spam/Scam | Suspected fraudulent listing | High |
| Abuse Concern | Suspected animal abuse | Critical |
| Other | General concern | Low |

**Moderation Actions:**
| Action | Effect | Notification |
|--------|--------|--------------|
| Dismiss | Removes flag, no action taken | None |
| Remove Content | Hides pet profile, status to `OnHold` | Email to foster |
| Warn User | Records warning, sends email | Warning email |
| Suspend User | Suspends account | Suspension email |

**UI Components:**
- Moderation queue: Card per flagged item
- Flag reason highlighted with severity badge
- Full content preview (pet profile, images)
- Reporter info (if available)
- Actions: Dismiss, Remove, Warn User, Suspend User
- Moderation history: List of past actions on this user/pet

**Audit Log Entry:**
```
{
  adminId: UUID,
  action: "removed_content" | "warned_user" | "suspended_user" | "dismissed",
  targetType: "Pet" | "User",
  targetId: UUID,
  reason: String,
  timestamp: DateTime
}
```

**Priority:** P3 - Polish

---

## Admin Dashboard

The Admin dashboard provides a command center for platform management:

### Dashboard Layout
```
┌─────────────────────────────────────────────────────────┐
│  Forever Home Admin                          [Logout]   │
├─────────────────────────────────────────────────────────┤
│  Quick Stats                                            │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐   │
│  │  1,234   │ │    456   │ │     23   │ │     5    │   │
│  │  Users   │ │   Pets   │ │ Adopted  │ │ Pending  │   │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘   │
├─────────────────────────────────────────────────────────┤
│  Approval Queue (5)                            [View All]│
│  ┌──────────────────────────────────────────────────┐   │
│  │ [Vet] Austin Veterinary Clinic                  │   │
│  │       License: TX-12345 | Submitted 2h ago      │   │
│  │       [View] [Approve] [Reject]                 │   │
│  └──────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────┐   │
│  │ [Rescue] Happy Tails Rescue                     │   │
│  │       Austin, TX | Submitted 1d ago             │   │
│  │       [View] [Approve] [Reject]                 │   │
│  └──────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────┤
│  Moderation Queue (2)                          [View All]│
│  ┌──────────────────────────────────────────────────┐   │
│  │ [!] Flagged: Misleading pet info                │   │
│  │     Pet: Max | Reporter: user@email.com         │   │
│  │     [Review]                                    │   │
│  └──────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────┤
│  Recent Activity                                         │
│  - Approved: Austin Vet Clinic (2h ago)                 │
│  - Suspended: spam_user@email.com (1d ago)              │
│  - Dismissed flag: Pet #1234 (2d ago)                   │
└─────────────────────────────────────────────────────────┘
```

### Navigation
- **Dashboard**: Overview with queues and stats
- **Approvals**: Full approval queue management
- **Users**: User search and management
- **Moderation**: Content moderation queue
- **Analytics**: Platform statistics and reports
- **Settings**: Platform configuration

---

## Admin Permissions

All admin accounts have full platform access:

| Capability | Description |
|------------|-------------|
| Approve Users | Verify vets and rescue organizations |
| Manage Users | Suspend, reactivate, password reset |
| Moderate Content | Review and act on flagged content |
| View Analytics | Access all platform statistics |
| Create Admins | Add new admin accounts |

**Security Notes:**
- Admin actions are logged with timestamps
- Sensitive actions require confirmation
- Cannot suspend own account
- Cannot demote last remaining admin
