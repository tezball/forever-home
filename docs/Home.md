# Forever Home

> *Finding loving homes for pets through trusted rescue networks*

Forever Home is a pet adoption platform connecting pet owners (Fosters) with adopters through verified rescue organizations. All pets are health-verified by licensed veterinarians before becoming available for adoption.

---

## Folder Structure

```
docs/
├── Home.md                          # This file - main index
├── Roadmap.md                       # Project phases and status
│
├── architecture/                    # System design
│   ├── domain-model.md             # Entities, relationships, auth
│   ├── pet-status.md               # Pet lifecycle state machine
│   └── user-flow-diagrams.md       # Sequence diagrams
│
├── user-stories/                    # Requirements by role
│   ├── index.md                    # Stories overview
│   ├── visitor.md                  # Public browsing
│   ├── foster.md                   # Pet registration
│   ├── adopter.md                  # Adoption applications
│   ├── rescue-organization.md      # Pet management
│   ├── vet.md                      # Health verification
│   ├── admin.md                    # Platform management
│   └── notifications.md            # Notification system
│
├── guides/                          # How-to documentation
│   ├── deployment-guide.md         # AWS deployment
│   ├── testing-strategy.md         # Test approach
│   ├── manual-qa-guide.md          # QA test cases
│   └── ses-setup.md                # Email configuration
│
├── design/                          # UI/UX documentation
│   ├── ui-style-guide.md           # Design system
│   ├── style-guide.html            # Component library
│   └── ux-improvement-plan.md      # UX roadmap
│
├── services/                        # Service documentation
│   ├── moderation-service.md       # AI content moderation
│   └── notifications-map.md        # Notification triggers
│
└── reviews/                         # Audits and feedback
    ├── comprehensive-platform-review.md
    ├── security-review.md
    └── rescue-org-review-feedback.md
```

---

## Quick Navigation

### Core Documentation
| Document | Description |
|----------|-------------|
| [[Roadmap]] | Project roadmap and implementation status |
| [[architecture/domain-model\|Domain Model]] | Entity definitions, relationships, and authentication |
| [[architecture/pet-status\|Pet Status]] | Pet lifecycle state machine and transitions |
| [[design/ui-style-guide\|UI Style Guide]] | Design system, colors, typography, components |
| [[guides/deployment-guide\|Deployment Guide]] | Production deployment instructions |
| [[guides/testing-strategy\|Testing Strategy]] | Testing approach and coverage |

### User Stories by Role
| Role | Stories | Status |
|------|---------|--------|
| [[user-stories/visitor\|Visitor]] | Public browsing and discovery | Complete |
| [[user-stories/foster\|Foster]] | Pet registration and rehoming | Complete |
| [[user-stories/adopter\|Adopter]] | Browsing, favorites, applications, swipe mode | Complete |
| [[user-stories/rescue-organization\|Rescue Org]] | Pet intake, vet approval, adoptions | Complete |
| [[user-stories/vet\|Vet]] | Health verification and sign-off | Complete |
| [[user-stories/admin\|Admin]] | Approvals, moderation, analytics | Complete |
| [[user-stories/notifications\|Notifications]] | Email and in-app notifications | Complete |

### Additional Documentation
| Category | Documents |
|----------|-----------|
| **Services** | [[services/moderation-service\|Moderation Service]], [[services/notifications-map\|Notifications Map]] |
| **Guides** | [[guides/manual-qa-guide\|QA Guide]], [[guides/ses-setup\|SES Setup]] |
| **Reviews** | [[reviews/security-review\|Security]], [[reviews/comprehensive-platform-review\|Platform Review]] |
| **Design** | [[design/ux-improvement-plan\|UX Plan]], [[architecture/user-flow-diagrams\|Flow Diagrams]] |

---

## Platform Overview

```
Foster                    Rescue Org                 Vet                      Adopter
  │                          │                        │                          │
  │ 1. Register pet          │                        │                          │
  │─────────────────────────►│                        │                          │
  │                          │ 2. Accept/Review       │                          │
  │◄─────────────────────────│                        │                          │
  │ 3. Take pet to vet ──────┼───────────────────────►│                          │
  │                          │                        │ 4. Verify & sign off     │
  │                          │◄───────────────────────│                          │
  │                          │         Pet becomes Available                     │
  │                          │                        │    5. Browse & apply     │
  │                          │◄───────────────────────┼──────────────────────────│
  │                          │ 6. Review & approve    │                          │
  │                          │─────────────────────────────────────────────────►│
  │ 7. Handoff               │                        │        8. Adopt!         │
  │◄─────────────────────────┼────────────────────────┼─────────────────────────►│
```

---

## Implementation Status

### Backend (Spring Boot)
| Feature | Status | Details |
|---------|--------|---------|
| Authentication | Complete | JWT access (15min) + refresh tokens (7 days), token rotation, email verification, password reset, Google OAuth |
| User Management | Complete | Multi-role registration, profile completion, account lockout (5 failed attempts) |
| Pet Lifecycle | Complete | Full state machine: Draft → Pending Rescue → Pending Vet → Available → In Progress → Adopted |
| Dual Pet Workflows | Complete | Foster-initiated (standard) and Rescue-direct (skips foster submission) |
| Adoption Flow | Complete | Applications (max 3 active), review, approval, finalization with notifications |
| Vet Verification | Complete | Microchip lookup, bilateral approval system, sign-off with health checks |
| Notifications | Complete | In-app + email notifications, user preferences, 6 notification types |
| Image Storage | Complete | S3 integration (LocalStack dev / AWS prod), up to 5 images per pet |
| Email Service | Complete | AWS SES, SMTP, and console backends with branded HTML templates |
| Admin Tools | Complete | User management, rescue org verification, audit logs |
| Content Moderation | Complete | AI-powered via separate moderation service (Ollama LLMs for text + images) |
| Analytics | Complete | MetricsService tracking registrations, adoptions, sign-offs |

### Frontend (React + TypeScript)
| Feature | Status | Details |
|---------|--------|---------|
| Authentication | Complete | Login, register, email verification, password reset, Google OAuth |
| Pet Browsing | Complete | Filters (species, size, sex), pagination, featured pets |
| Swipe Mode | Complete | Tinder-style pet discovery with swipe gestures, keyboard support, lazy loading |
| Pet Detail | Complete | Image carousel, status timeline, favorites, adoption form |
| Foster Dashboard | Complete | Pet management by status, submit to rescue, withdraw |
| Adopter Dashboard | Complete | Favorites, application tracking with status, liked pets |
| Rescue Dashboard | Complete | Accept/decline pets, manage applications, vet approvals, pet hold |
| Vet Dashboard | Complete | Microchip lookup, pending queue, sign-off/decline forms |
| Admin Dashboard | Complete | Analytics, user management, rescue approvals |
| Notifications | Complete | Preferences, notification bell with count |
| Image Upload | Complete | Multi-image upload with drag-drop, preview and reordering |

---

## Tech Stack

### Backend
- **Framework**: Spring Boot 4.0.0 with Java 25
- **Database**: PostgreSQL 16 with Spring Data JDBC
- **Auth**: JWT with stateless access + refresh token rotation
- **Storage**: AWS S3 (LocalStack for dev)
- **Email**: AWS SES with HTML templates
- **Monitoring**: Spring Boot Actuator
- **Build**: Maven, GraalVM Native Image support

### Frontend
- **Framework**: React 19.2 with TypeScript 5.9
- **Build**: Vite 7.2
- **Styling**: Tailwind CSS 4.1
- **HTTP Client**: Axios with interceptors
- **Testing**: Playwright E2E

### Infrastructure
- **Container**: Docker with ECS Fargate
- **Database**: AWS RDS PostgreSQL
- **CDN/Storage**: AWS S3
- **DNS**: Route53 (optional)
- **IaC**: Terraform

---

## Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| **Microchip Required** | All pets must have a microchip for vet lookup and ownership tracking |
| **Bilateral Vet Approval** | Vets request approval from rescues OR rescues proactively approve vets |
| **Microchip-Based Vet Lookup** | Vets find pets by microchip number - no push assignment needed |
| **JWT Authentication** | 15-min access tokens + 7-day refresh tokens with rotation for security |
| **Google OAuth** | Alternative login method with automatic email verification |
| **Token Rotation** | Refresh tokens rotated on each use - old tokens immediately revoked |
| **Auto Profile Creation** | Profiles created at registration, marked complete immediately |
| **Environment-Agnostic Storage** | S3 keys stored in DB, URLs generated dynamically |
| **Comprehensive Notifications** | Every state change triggers notifications based on user preferences |
| **AI Content Moderation** | Pets must pass AI moderation before becoming publicly visible |
| **Dual Pet Workflows** | Foster-initiated (standard) or Rescue-direct (skips foster submission) |

See [[architecture/domain-model|Domain Model]] for full entity definitions.

---

## API Endpoints Summary

| Controller | Base Path | Key Endpoints |
|------------|-----------|---------------|
| Auth | `/api/auth` | register, login, refresh, verify-email, forgot-password, google |
| Profile | `/api/profile` | status, foster, adopter, vet, rescue-org |
| Pet | `/api/pets` | CRUD, submit, accept, decline, lookup, hold |
| Vet | `/api/vet` | pending queue, sign-off, decline, approvals |
| Rescue | `/api/rescue-org` | vet management, approval requests |
| Adoption | `/api/applications` | submit, review, approve, reject, finalize |
| Favorites | `/api/favorites` | add, remove, list |
| Notifications | `/api/notifications` | list, read, preferences |
| Admin | `/api/admin` | analytics, users, approvals, moderation |
| Images | `/api/pets/{id}/images` | upload, delete, reorder |

---

## Development

### Quick Start
```bash
# Start all services (PostgreSQL, LocalStack, Mailpit)
./dev.sh start

# App runs at http://localhost:8080
# Frontend at http://localhost:5173
# Mailpit at http://localhost:8025
```

### Test Accounts (Dev Mode)
| Role | Email | Password |
|------|-------|----------|
| Admin | admin@test.com | password123 |
| Foster | foster@test.com | password123 |
| Adopter | adopter@test.com | password123 |
| Vet | vet@test.com | password123 |
| Rescue Org | rescue@test.com | password123 |

---

*Last updated: December 2025*
