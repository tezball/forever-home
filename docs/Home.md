# Forever Home

> *Finding loving homes for pets through trusted rescue networks*

Forever Home is a pet adoption platform connecting pet owners (Fosters) with adopters through verified rescue organizations. All pets are health-verified by licensed veterinarians before becoming available for adoption.

---

## Quick Navigation

### Core Documentation
| Document | Description |
|----------|-------------|
| [[Roadmap]] | **Start Here** - Project roadmap and implementation status |
| [[domain-model]] | Entity definitions, relationships, and authentication |
| [[pet-status]] | Pet lifecycle state machine and transitions |
| [[ui-style-guide]] | Design system, colors, typography, components |

### User Stories by Role
| Role | Stories | Priority Focus |
|------|---------|----------------|
| [[user-stories/visitor\|Visitor]] | Public browsing and discovery | P0/P3 |
| [[user-stories/foster\|Foster]] | Pet registration and rehoming | P0/P1 |
| [[user-stories/adopter\|Adopter]] | Browsing, favorites, applications | P0/P1/P2 |
| [[user-stories/rescue-organization\|Rescue Org]] | Pet intake, vet approval, adoptions | P0/P1 |
| [[user-stories/vet\|Vet]] | Health verification and sign-off | P0/P2 |
| [[user-stories/admin\|Admin]] | Approvals, moderation, analytics | P0/P2/P3 |
| [[user-stories/notifications\|Notifications]] | Email and in-app notifications | P2 |

### Project Management
| Document | Description |
|----------|-------------|
| [[gaps-and-decisions]] | Architectural decisions and requirement gaps |
| [[e2e-review]] | End-to-end testing status and known issues |

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

## Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| **Microchip Required** | All pets must have a microchip for vet lookup and ownership tracking |
| **Vets Verified by Rescues** | Rescue organizations approve vets (not admins) for organization-specific trust |
| **Microchip-Based Vet Lookup** | Vets find pets by microchip number - no pre-assignment needed |
| **JWT Authentication** | 15-min access tokens + 7-30 day refresh tokens in httpOnly cookies |
| **AWS Infrastructure** | S3 for images, SES for email |

See [[gaps-and-decisions]] for full decision log.

---

## Tech Stack

- **Backend**: Spring Boot 4.0.0 with Java 25
- **Database**: PostgreSQL with Spring Data JDBC
- **Auth**: JWT (access + refresh tokens)
- **Storage**: AWS S3 (images), AWS SES (email)
- **Monitoring**: Spring Boot Actuator
- **Build**: GraalVM Native Image support
- **Dev**: Docker Compose for local PostgreSQL

---

## Status Summary

> Based on [[e2e-review]] (December 8, 2025)

| Area | Status |
|------|--------|
| Authentication | Working |
| Pet Browsing | Working |
| Adopter Dashboard | Fixed |
| Foster Dashboard | Working |
| Rescue Org Dashboard | **500 Errors** - High Priority |
| Vet Dashboard | Working (mock data) |
| Admin Dashboard | Working |

---

## Tags Used in This Vault

- `#mvp` - MVP (P0) priority features
- `#core` - Core (P1) features
- `#enhanced` - Enhanced (P2) features
- `#polish` - Polish (P3) features
- `#decision` - Architectural decisions
- `#bug` - Known bugs
- `#fixed` - Recently fixed issues
- `#blocked` - Blocked items

---

*Last updated: December 2025*
