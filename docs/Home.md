# Forever Home

> *Finding loving homes for pets through trusted rescue networks*

Forever Home is a pet adoption platform connecting pet owners (Fosters) with adopters through verified rescue organizations. All pets are health-verified by licensed veterinarians before becoming available for adoption.

---

## Quick Navigation

### Core Documentation
| Document | Description |
|----------|-------------|
| [[Roadmap]] | Project roadmap and implementation status |
| [[domain-model]] | Entity definitions, relationships, and authentication |
| [[pet-status]] | Pet lifecycle state machine and transitions |
| [[ui-style-guide]] | Design system, colors, typography, components |
| [[deployment-guide]] | Production deployment instructions |
| [[testing-strategy]] | Testing approach and coverage |

### User Stories by Role
| Role | Stories | Status |
|------|---------|--------|
| [[user-stories/visitor\|Visitor]] | Public browsing and discovery | Complete |
| [[user-stories/foster\|Foster]] | Pet registration and rehoming | Complete |
| [[user-stories/adopter\|Adopter]] | Browsing, favorites, applications | Complete |
| [[user-stories/rescue-organization\|Rescue Org]] | Pet intake, vet approval, adoptions | Complete |
| [[user-stories/vet\|Vet]] | Health verification and sign-off | Complete |
| [[user-stories/admin\|Admin]] | Approvals, moderation, analytics | Complete |
| [[user-stories/notifications\|Notifications]] | Email and in-app notifications | Complete |

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
| Authentication | Complete | JWT access (15min) + refresh tokens (7 days), email verification, password reset |
| User Management | Complete | Multi-role registration, profile completion, account lockout (5 failed attempts) |
| Pet Lifecycle | Complete | Full state machine: Draft → Pending Rescue → Pending Vet → Available → In Progress → Adopted |
| Adoption Flow | Complete | Applications, review, approval, finalization with notifications |
| Vet Verification | Complete | Microchip lookup, bilateral approval system, sign-off with health checks |
| Notifications | Complete | In-app + email notifications, user preferences, 6 notification types |
| Image Storage | Complete | S3 integration (LocalStack dev / AWS prod), up to 5 images per pet |
| Email Service | Complete | AWS SES, SMTP, and console backends with branded HTML templates |
| Admin Tools | Complete | User management, rescue org verification, content moderation, audit logs |
| Analytics | Complete | MetricsService tracking registrations, adoptions, sign-offs |

### Frontend (React + TypeScript)
| Feature | Status | Details |
|---------|--------|---------|
| Authentication | Complete | Login, register, email verification, password reset |
| Pet Browsing | Complete | Filters (species, size, sex), pagination, featured pets |
| Pet Detail | Complete | Image carousel, status timeline, favorites, adoption form |
| Foster Dashboard | Complete | Pet management by status, submit to rescue, withdraw |
| Adopter Dashboard | Complete | Favorites, application tracking with status |
| Rescue Dashboard | Complete | Accept/decline pets, manage applications, vet approvals |
| Vet Dashboard | Complete | Microchip lookup, pending queue, sign-off/decline forms |
| Admin Dashboard | Complete | Analytics, user management, rescue approvals |
| Notifications | Complete | Preferences, notification bell with count |
| Image Upload | Complete | Multi-image upload with preview and reordering |

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
| **JWT Authentication** | 15-min access tokens + 7-day refresh tokens for security |
| **Auto Profile Creation** | Profiles created at registration, marked complete immediately |
| **Environment-Agnostic Storage** | S3 keys stored in DB, URLs generated dynamically |
| **Comprehensive Notifications** | Every state change triggers notifications based on user preferences |

See [[domain-model]] for full entity definitions.

---

## API Endpoints Summary

| Controller | Base Path | Key Endpoints |
|------------|-----------|---------------|
| Auth | `/api/auth` | register, login, refresh, verify-email, forgot-password |
| Profile | `/api/profile` | status, foster, adopter, vet, rescue-org |
| Pet | `/api/pets` | CRUD, submit, accept, decline, lookup |
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
