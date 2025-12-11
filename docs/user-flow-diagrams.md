# Forever Home - User Flow Diagrams

This document contains comprehensive diagrams of all core user flows in the Forever Home pet adoption platform.

## Table of Contents

1. [Authentication Flow](#1-authentication-flow)
2. [Pet Registration Flow](#2-pet-registration-flow-foster)
3. [Adoption Flow](#3-adoption-flow-adopter)
4. [Vet Verification Flow](#4-vet-verification-flow)
5. [Rescue Organization Flow](#5-rescue-organization-flow)
6. [Pet Status Lifecycle](#6-pet-status-lifecycle)

---

## 1. Authentication Flow

### 1.1 User Registration Flow

```mermaid
flowchart TD
    A[User Visits Site] --> B{Has Account?}
    B -->|No| C[Click Register]
    B -->|Yes| L[Go to Login]

    C --> D[Select Role]
    D --> D1[Foster]
    D --> D2[Adopter]
    D --> D3[Vet]
    D --> D4[Rescue Organization]

    D1 & D2 & D3 & D4 --> E[Enter Email & Password]
    E --> F[Accept Terms & Conditions]
    F --> G[POST /api/auth/register]
    G --> H[Create User Entity<br/>Status: PENDING]
    H --> I[Send Verification Email<br/>24-hour expiry]
    I --> J[User Clicks Email Link]
    J --> K[POST /api/auth/verify-email]
    K --> M[User Status: ACTIVE]
    M --> N[Return JWT Tokens]
    N --> O{Profile Complete?}
    O -->|No| P[Redirect to Profile Completion]
    O -->|Yes| Q[Redirect to Dashboard]

    style A fill:#E8F5E9
    style Q fill:#C8E6C9
    style P fill:#FFF3E0
    style H fill:#E3F2FD
    style N fill:#FCE4EC
```

### 1.2 User Login Flow

```mermaid
flowchart TD
    A[User Visits Login Page] --> B[Enter Email & Password]
    B --> C{Remember Me?}
    C -->|Yes| D[Set 30-day refresh token]
    C -->|No| E[Set 7-day refresh token]

    D & E --> F[POST /api/auth/login]
    F --> G{Credentials Valid?}

    G -->|No| H{Attempts >= 5?}
    H -->|Yes| I[Account Locked<br/>15 minutes]
    H -->|No| J[Show Error<br/>Increment Attempts]
    J --> B

    G -->|Yes| K{User Status?}
    K -->|PENDING| L[Show: Verify Email First]
    K -->|LOCKED| M[Show: Account Locked]
    K -->|ACTIVE| N[Update lastLoginAt]

    N --> O[Return Access Token<br/>15 min expiry]
    O --> P[Set Refresh Token<br/>httpOnly Cookie]
    P --> Q{Profile Complete?}
    Q -->|No| R[Redirect to<br/>Profile Completion]
    Q -->|Yes| S[Redirect to<br/>Role Dashboard]

    style A fill:#E8F5E9
    style S fill:#C8E6C9
    style R fill:#FFF3E0
    style I fill:#FFCDD2
    style L fill:#FFCDD2
    style M fill:#FFCDD2
```

### 1.3 Token Refresh Flow

```mermaid
sequenceDiagram
    participant C as Client
    participant API as API Server
    participant DB as Database

    Note over C: Access token expires (15 min)

    C->>API: POST /api/auth/refresh<br/>(refresh token in cookie)
    API->>DB: Validate refresh token

    alt Token Valid
        DB-->>API: Token valid
        API->>API: Generate new access token
        API-->>C: 200 OK + New Access Token
        C->>C: Store in memory
    else Token Invalid/Expired
        DB-->>API: Token invalid
        API-->>C: 401 Unauthorized
        C->>C: Redirect to login
    end
```

### 1.4 Password Recovery Flow

```mermaid
flowchart TD
    A[User Clicks<br/>Forgot Password] --> B[Enter Email Address]
    B --> C[POST /api/auth/forgot-password]
    C --> D{Email Exists?}

    D -->|No| E[Show Generic Success<br/>Security measure]
    D -->|Yes| F[Generate Reset Token<br/>24-hour expiry]

    F --> G[Send Reset Email via SES]
    E & G --> H[User Checks Email]
    H --> I[Click Reset Link]
    I --> J[Enter New Password]
    J --> K{Password Valid?}

    K -->|No| L[Show Requirements:<br/>8+ chars, mixed case, number]
    L --> J

    K -->|Yes| M[POST /api/auth/reset-password]
    M --> N[Update Password Hash]
    N --> O[Invalidate All Sessions]
    O --> P[Redirect to Login]
    P --> Q[Login with New Password]

    style A fill:#E8F5E9
    style Q fill:#C8E6C9
    style E fill:#FFF3E0
```

### 1.5 Logout Flow

```mermaid
flowchart TD
    A[User Clicks Logout] --> B[POST /api/auth/logout]
    B --> C[Invalidate Refresh Token<br/>in Database]
    C --> D[Clear httpOnly Cookie]
    D --> E[Client Clears<br/>Access Token from Memory]
    E --> F[Redirect to Home Page]

    G[Access Token Expires<br/>After 15 Minutes] -.-> H[Token Unusable]

    style A fill:#E8F5E9
    style F fill:#C8E6C9
```

---

## 2. Pet Registration Flow (Foster)

### 2.1 Complete Foster Profile

```mermaid
flowchart TD
    A[Foster Logs In<br/>First Time] --> B{Profile Complete?}
    B -->|Yes| C[Go to Dashboard]
    B -->|No| D[Redirect to<br/>Profile Completion]

    D --> E[Enter Required Fields]
    E --> F[First Name]
    E --> G[Last Name]
    E --> H[Phone Number]
    E --> I[Location/Address]

    F & G & H & I --> J[POST /api/profile/foster]
    J --> K[Create Foster Entity]
    K --> L[Set User.profileComplete = true]
    L --> M[Redirect to<br/>Foster Dashboard]

    style A fill:#E8F5E9
    style M fill:#C8E6C9
    style D fill:#FFF3E0
```

### 2.2 Pet Registration Process

```mermaid
flowchart TD
    A[Foster Dashboard] --> B[Click 'Register Pet']
    B --> C[Fill Pet Information]

    subgraph PetDetails[Pet Details Form]
        C1[Name]
        C2[Species: Dog/Cat/etc]
        C3[Breed]
        C4[Age: months/years]
        C5[Sex: Male/Female]
        C6[Size: S/M/L]
        C7[Microchip ID<br/>REQUIRED & IMMUTABLE]
        C8[Description<br/>max 500 chars]
    end

    C --> PetDetails
    PetDetails --> D[Upload Images<br/>Up to 5, set primary]
    D --> E[POST /api/pets]
    E --> F[Create Pet<br/>Status: DRAFT]
    F --> G[Pet Saved<br/>Not Public]

    G --> H{Ready to Submit?}
    H -->|No| I[Edit Pet Details]
    I --> H
    H -->|Yes| J[Select Rescue Organization]
    J --> K[View Rescue List<br/>GET /api/rescue-org/list]
    K --> L[Choose Organization]
    L --> M[POST /api/pets/id/submit]
    M --> N[Status: PENDING_RESCUE]
    N --> O[Rescue Org Notified]

    style A fill:#E8F5E9
    style O fill:#C8E6C9
    style F fill:#E3F2FD
    style N fill:#FFF3E0
```

### 2.3 Full Pet Journey (Foster Perspective)

```mermaid
flowchart TD
    A[Create Pet Draft] --> B[Submit to Rescue]
    B --> C{Rescue Decision}

    C -->|Declined| D[Return to Draft<br/>Foster Notified]
    D --> E[Edit & Resubmit]
    E --> B

    C -->|Accepted| F[Status: PENDING_VET]
    F --> G[Foster Takes Pet<br/>to Any Approved Vet]

    G --> H{Vet Decision}
    H -->|Declined| I[Return to PENDING_RESCUE<br/>with Reason]
    I --> J[Rescue Reviews]
    J --> C

    H -->|Signed Off| K[Status: AVAILABLE]
    K --> L[Pet Listed Publicly]

    L --> M[Adopters Apply]
    M --> N{Application Approved?}
    N -->|No| M
    N -->|Yes| O[Status: IN_PROGRESS]

    O --> P{Adoption Finalized?}
    P -->|No| Q[Falls Through]
    Q --> L
    P -->|Yes| R[Status: ADOPTED]
    R --> S[Foster Notified<br/>Success!]

    subgraph Withdraw[Withdraw Option]
        W1[Foster Can Withdraw]
        W2[Status: WITHDRAWN]
        W3[Can Resubmit Later]
    end

    L -.-> W1
    W1 --> W2
    W2 --> W3
    W3 --> B

    style A fill:#E8F5E9
    style S fill:#C8E6C9
    style K fill:#81C784
    style R fill:#4CAF50
    style D fill:#FFCDD2
    style I fill:#FFCDD2
```

---

## 3. Adoption Flow (Adopter)

### 3.1 Browse & Search Pets (Public)

```mermaid
flowchart TD
    A[Visitor/Adopter<br/>Visits Site] --> B[Browse Available Pets<br/>GET /api/pets]

    B --> C[View Pet Grid/List]
    C --> D{Apply Filters?}

    D -->|Yes| E[Filter Options]
    subgraph Filters[Available Filters]
        E1[Species: Dog/Cat/etc]
        E2[Breed: Multi-select]
        E3[Size: S/M/L]
        E4[Age Range: Slider]
        E5[Location]
    end
    E --> Filters
    Filters --> F[Apply Filters<br/>AND Logic]
    F --> G[Update Results<br/>Debounced 300ms]
    G --> C

    D -->|No| H[Click Pet Card]
    C --> H
    H --> I[View Pet Profile<br/>GET /api/pets/id]

    I --> J[See Full Details]
    subgraph Details[Pet Profile]
        J1[Image Gallery<br/>Up to 5 photos]
        J2[Name, Age, Breed]
        J3[Size, Sex, Species]
        J4[Full Description]
        J5[Vet Verification Badge]
        J6[Rescue Org Info]
    end
    J --> Details

    Details --> K{Logged In?}
    K -->|No| L[Click Favorite/Apply]
    L --> M[Redirect to Login/Register]
    K -->|Yes| N[Actions Available]

    style A fill:#E8F5E9
    style C fill:#E3F2FD
    style I fill:#FFF3E0
```

### 3.2 Adopter Registration & Profile

```mermaid
flowchart TD
    A[Visitor Wants to Adopt] --> B[Register as Adopter]
    B --> C[Email Verification]
    C --> D[First Login]

    D --> E{Profile Complete?}
    E -->|No| F[Complete Adopter Profile]

    subgraph Profile[Required Fields]
        F1[First Name]
        F2[Last Name]
        F3[Phone Number]
        F4[Location/Address]
        F5[Living Situation<br/>House/Apartment/etc]
        F6[Pet Experience<br/>Previous ownership]
    end

    F --> Profile
    Profile --> G[POST /api/profile/adopter]
    G --> H[Create Adopter Entity]
    H --> I[profileComplete = true]

    E -->|Yes| J[Adopter Dashboard]
    I --> J

    J --> K[Browse Pets]
    J --> L[View Favorites]
    J --> M[Track Applications]
    J --> N[View Adoptions]

    style A fill:#E8F5E9
    style J fill:#C8E6C9
    style F fill:#FFF3E0
```

### 3.3 Submit Adoption Application

```mermaid
flowchart TD
    A[View Pet Profile] --> B[Click 'Apply to Adopt']
    B --> C[POST /api/applications/check/petId]

    C --> D{Already Applied?}
    D -->|Yes| E[Show: Application Already Submitted]

    D -->|No| F{Active Applications >= 3?}
    F -->|Yes| G[Show: Maximum Applications Reached]

    F -->|No| H[Show Application Form]

    subgraph Form[Application Form]
        H1[Living Situation<br/>Pre-filled from profile]
        H2[Pet Experience<br/>Pre-filled from profile]
        H3[Why This Pet?<br/>Min 50 chars, required]
    end

    H --> Form
    Form --> I[Review Application]
    I --> J[POST /api/applications]

    J --> K[Create AdoptionApplication<br/>Status: SUBMITTED]
    K --> L[Adopter Gets Confirmation]
    L --> M[Rescue Org Notified]

    M --> N[Application in Queue]

    style A fill:#E8F5E9
    style N fill:#C8E6C9
    style E fill:#FFCDD2
    style G fill:#FFCDD2
    style K fill:#E3F2FD
```

### 3.4 Application Review & Adoption

```mermaid
sequenceDiagram
    participant A as Adopter
    participant R as Rescue Org
    participant F as Foster
    participant S as System

    Note over A: Application Submitted

    A->>S: POST /api/applications
    S->>R: Notification: New Application

    R->>S: GET /api/pets/petId/applications
    S-->>R: List of Applications

    R->>R: Review Adopter Profile<br/>& Application

    alt Approved
        R->>S: PUT /api/applications/id/approve
        S->>S: Application Status: APPROVED<br/>Pet Status: IN_PROGRESS
        S->>A: Notification: Approved!
        S->>F: Notification: Application Approved

        Note over R,F: Coordinate Handoff
        R->>R: Arrange Pet Transfer

        R->>S: POST /api/adoptions<br/>(finalize)
        S->>S: Create Adoption Record<br/>Pet Status: ADOPTED
        S->>A: Confirmation Email
        S->>F: Confirmation Email
        S->>R: Confirmation Email

    else Rejected
        R->>S: PUT /api/applications/{id}/reject<br/>with reason
        S->>S: Application Status: REJECTED
        S->>A: Notification with Feedback
        Note over A: Can apply to other pets
    end
```

### 3.5 Track Application Status

```mermaid
flowchart TD
    A[Adopter Dashboard] --> B[My Applications<br/>GET /api/applications]

    B --> C[View Application List]

    subgraph Status[Application Statuses]
        S1[SUBMITTED<br/>Gray - Awaiting Review]
        S2[UNDER_REVIEW<br/>Gold - Being Reviewed]
        S3[APPROVED<br/>Green - Adoption Pending]
        S4[REJECTED<br/>Red - With Feedback]
    end

    C --> D[Click Application]
    D --> E[View Details]
    E --> F{Status?}

    F -->|SUBMITTED| G[Can Withdraw<br/>DELETE /api/applications/id]
    F -->|APPROVED| H[Await Rescue Contact<br/>for Pet Transfer]
    F -->|REJECTED| I[View Feedback<br/>Apply to Other Pets]

    H --> J[Adoption Finalized]
    J --> K[Pet in My Adoptions<br/>GET /api/adoptions]

    style A fill:#E8F5E9
    style K fill:#C8E6C9
    style S1 fill:#E0E0E0
    style S2 fill:#FFF176
    style S3 fill:#81C784
    style S4 fill:#EF9A9A
```

---

## 4. Vet Verification Flow

### 4.1 Vet Registration & Approval

```mermaid
flowchart TD
    A[Vet Registers] --> B[Email Verification]
    B --> C[Complete Vet Profile]

    subgraph Profile[Vet Profile Fields]
        C1[Clinic Name]
        C2[License Number]
        C3[Phone & Website]
        C4[Address]
        C5[Description]
    end

    C --> Profile
    Profile --> D[POST /api/profile/vet]
    D --> E[Create Vet Entity<br/>verified = false]
    E --> F[Admin Verification Required]

    F --> G{Admin Approves?}
    G -->|No| H[Vet Remains Unverified]
    G -->|Yes| I[verified = true]

    I --> J[Rescue Orgs Can Approve]
    J --> K[GET /api/rescue-org/vets/pending]
    K --> L[Rescue Reviews Vet]
    L --> M[POST /api/rescue-org/vets/vetId/approve]
    M --> N[VetApproval Created<br/>Many-to-Many Relationship]
    N --> O[Vet Can Sign Off<br/>on This Rescue's Pets]

    style A fill:#E8F5E9
    style O fill:#C8E6C9
    style E fill:#FFF3E0
    style I fill:#81C784
```

### 4.2 Pet Lookup & Verification

```mermaid
flowchart TD
    A[Foster Brings Pet<br/>to Vet Clinic] --> B[Vet Logs In]
    B --> C[Enter Microchip Number]
    C --> D[GET /api/vet/pets/lookup?microchip=XXX]

    D --> E{Pet Found?}
    E -->|No| F[Show: Pet Not Found]

    E -->|Yes| G{Pet Status = PENDING_VET?}
    G -->|No| H[Show: Pet Not Available<br/>for Verification]

    G -->|Yes| I{Vet Approved by<br/>Pet's Rescue Org?}
    I -->|No| J[Show: Not Authorized<br/>for This Rescue]

    I -->|Yes| K[Display Pet Details]
    K --> L[Perform Examination]

    subgraph Exam[Verification Checklist]
        L1[Neutered/Spayed?<br/>Confirm with Date]
        L2[Vaccinations Current?<br/>Review Records]
        L3[Health Assessment<br/>Good/Known Conditions]
    end

    L --> Exam
    Exam --> M{All Checks Pass?}

    M -->|Yes| N[POST /api/vet/pets/petId/sign-off]
    N --> O[Create VetSignOff<br/>Immutable Record]
    O --> P[Pet Status: AVAILABLE]
    P --> Q[Foster & Rescue Notified]

    M -->|No| R[POST /api/vet/pets/petId/decline<br/>with Reason]
    R --> S[Pet Status: PENDING_RESCUE]
    S --> T[Foster & Rescue Notified<br/>with Decline Reason]

    style A fill:#E8F5E9
    style Q fill:#C8E6C9
    style T fill:#FFCDD2
    style O fill:#E3F2FD
    style F fill:#FFCDD2
    style H fill:#FFCDD2
    style J fill:#FFCDD2
```

### 4.3 Vet Sign-Off Details

```mermaid
flowchart TD
    A[Vet Completes Exam] --> B[Submit Sign-Off Form]

    subgraph SignOff[VetSignOff Entity]
        B1[vetId: Vet Reference]
        B2[petId: Pet Reference]
        B3[isNeutered: boolean]
        B4[neuterDate: Date]
        B5[isVaccinated: boolean]
        B6[vaccinations: List]
        B7[isHealthy: boolean]
        B8[healthStatus: Good/Conditions]
        B9[healthNotes: Text]
        B10[signedOffAt: Timestamp]
    end

    B --> SignOff
    SignOff --> C[POST /api/vet/pets/petId/sign-off]
    C --> D[Validation]

    D --> E{All Required<br/>Fields Complete?}
    E -->|No| F[Return Validation Errors]

    E -->|Yes| G[Create VetSignOff<br/>IMMUTABLE RECORD]
    G --> H[Update Pet Status<br/>PENDING_VET → AVAILABLE]
    H --> I[Pet Now Publicly Listed]

    J[GET /api/vet/sign-offs] --> K[View Sign-Off History]
    K --> L[Shows All Verified Pets]

    style A fill:#E8F5E9
    style I fill:#C8E6C9
    style G fill:#E3F2FD
```

---

## 5. Rescue Organization Flow

### 5.1 Rescue Organization Setup

```mermaid
flowchart TD
    A[Register as<br/>Rescue Organization] --> B[Email Verification]
    B --> C[Complete Organization Profile]

    subgraph Profile[Organization Profile]
        C1[Organization Name]
        C2[Phone & Website]
        C3[Description]
        C4[Contact Person]
        C5[Address]
        C6[Social Media Links]
        C7[Upload Logo]
    end

    C --> Profile
    Profile --> D[POST /api/profile/rescue-org]
    D --> E[Create RescueOrganization<br/>verified = false]

    E --> F[Admin Reviews]
    F --> G{Admin Approves?}
    G -->|No| H[Organization Remains<br/>Unverified]
    G -->|Yes| I[verified = true]
    I --> J[Organization Visible<br/>to Fosters]

    style A fill:#E8F5E9
    style J fill:#C8E6C9
    style E fill:#FFF3E0
    style I fill:#81C784
```

### 5.2 Pet Management (Rescue Perspective)

```mermaid
flowchart TD
    A[Rescue Dashboard] --> B[Pending Pets<br/>GET /api/pets/rescue/id/pending]

    B --> C[View Pending Submissions]
    C --> D[Click Pet to Review]

    D --> E[Review Pet Details]
    E --> F[Review Foster Profile]

    F --> G{Accept Pet?}

    G -->|Yes| H[POST /api/pets/id/accept]
    H --> I[Pet Status: PENDING_VET]
    I --> J[Foster Notified:<br/>Take Pet to Approved Vet]

    G -->|No| K[POST /api/pets/id/decline<br/>with Reason]
    K --> L[Pet Status: DRAFT]
    L --> M[Foster Notified<br/>with Feedback]

    A --> N[Available Pets<br/>GET /api/pets/rescue/id/available]
    N --> O[View Listed Pets]
    O --> P[Monitor Applications]

    A --> Q[Manage Vets]
    Q --> R[View Approved Vets<br/>GET /api/rescue-org/vets/approved]
    Q --> S[Approve New Vets<br/>POST /api/rescue-org/vets/id/approve]
    Q --> T[Revoke Approval<br/>DELETE /api/rescue-org/vets/id/approve]

    style A fill:#E8F5E9
    style J fill:#C8E6C9
    style M fill:#FFCDD2
```

### 5.3 Application & Adoption Management

```mermaid
flowchart TD
    A[Available Pet] --> B[Adopters Apply]
    B --> C[GET /api/pets/petId/applications]

    C --> D[View All Applications]
    D --> E[Review Application]

    subgraph Review[Application Review]
        E1[Adopter Profile]
        E2[Living Situation]
        E3[Pet Experience]
        E4[Application Message]
    end

    E --> Review
    Review --> F{Decision}

    F -->|Approve| G[PUT /api/applications/id/approve]
    G --> H[Application: APPROVED]
    H --> I[Pet Status: IN_PROGRESS]
    I --> J[Notify Adopter & Foster]

    J --> K[Coordinate Handoff]
    K --> L[Pet Transfer Occurs]
    L --> M[POST /api/adoptions]
    M --> N[Create Adoption Record]
    N --> O[Pet Status: ADOPTED]
    O --> P[All Parties Notified]

    F -->|Reject| Q[PUT /api/applications/id/reject<br/>with Reason]
    Q --> R[Application: REJECTED]
    R --> S[Adopter Notified<br/>with Feedback]

    style A fill:#E8F5E9
    style P fill:#C8E6C9
    style S fill:#FFCDD2
```

---

## 6. Pet Status Lifecycle

### 6.1 Complete State Machine

```mermaid
stateDiagram-v2
    [*] --> DRAFT: Foster Creates Pet

    DRAFT --> PENDING_RESCUE: Foster Submits<br/>to Rescue Org

    PENDING_RESCUE --> DRAFT: Rescue Declines<br/>(with reason)
    PENDING_RESCUE --> PENDING_VET: Rescue Accepts

    PENDING_VET --> PENDING_RESCUE: Vet Declines<br/>(with reason)
    PENDING_VET --> AVAILABLE: Vet Signs Off

    AVAILABLE --> ON_HOLD: Rescue Places Hold
    ON_HOLD --> AVAILABLE: Hold Released

    AVAILABLE --> IN_PROGRESS: Application Approved
    AVAILABLE --> WITHDRAWN: Foster Withdraws

    IN_PROGRESS --> AVAILABLE: Adoption Falls Through
    IN_PROGRESS --> WITHDRAWN: Foster Withdraws<br/>(needs rescue approval)
    IN_PROGRESS --> ADOPTED: Adoption Finalized

    WITHDRAWN --> PENDING_RESCUE: Foster Resubmits

    ADOPTED --> [*]: Terminal State

    note right of DRAFT
        Only Foster can edit
        Not public
    end note

    note right of AVAILABLE
        Public listing
        Adopters can apply
    end note

    note right of ADOPTED
        Immutable
        Success!
    end note
```

### 6.2 Status Transition Matrix

```mermaid
flowchart LR
    subgraph Private[Private States]
        DRAFT[DRAFT<br/>Foster editable]
        PENDING_RESCUE[PENDING_RESCUE<br/>Awaiting review]
        PENDING_VET[PENDING_VET<br/>Awaiting verification]
    end

    subgraph Public[Public States]
        AVAILABLE[AVAILABLE<br/>Listed for adoption]
        ON_HOLD[ON_HOLD<br/>Temporarily unavailable]
        IN_PROGRESS[IN_PROGRESS<br/>Adoption pending]
    end

    subgraph Terminal[Terminal States]
        ADOPTED[ADOPTED<br/>Successfully rehomed]
        WITHDRAWN[WITHDRAWN<br/>Can resubmit]
    end

    DRAFT --> PENDING_RESCUE
    PENDING_RESCUE --> DRAFT
    PENDING_RESCUE --> PENDING_VET
    PENDING_VET --> PENDING_RESCUE
    PENDING_VET --> AVAILABLE
    AVAILABLE --> ON_HOLD
    ON_HOLD --> AVAILABLE
    AVAILABLE --> IN_PROGRESS
    IN_PROGRESS --> AVAILABLE
    AVAILABLE --> WITHDRAWN
    IN_PROGRESS --> WITHDRAWN
    IN_PROGRESS --> ADOPTED
    WITHDRAWN --> PENDING_RESCUE

    style DRAFT fill:#E0E0E0
    style PENDING_RESCUE fill:#FFF9C4
    style PENDING_VET fill:#FFE0B2
    style AVAILABLE fill:#C8E6C9
    style ON_HOLD fill:#B3E5FC
    style IN_PROGRESS fill:#CE93D8
    style ADOPTED fill:#4CAF50,color:#fff
    style WITHDRAWN fill:#FFCDD2
```

### 6.3 Who Can Do What

```mermaid
flowchart TD
    subgraph Roles[User Roles & Permissions]
        F[FOSTER]
        R[RESCUE ORG]
        V[VET]
        A[ADOPTER]
        AD[ADMIN]
    end

    subgraph FosterActions[Foster Actions]
        F1[Create Pet Draft]
        F2[Edit Draft/PendingRescue]
        F3[Submit to Rescue]
        F4[Withdraw Pet]
        F5[View Pet Status]
    end

    subgraph RescueActions[Rescue Actions]
        R1[Accept/Decline Pets]
        R2[Approve/Revoke Vets]
        R3[Review Applications]
        R4[Approve/Reject Applications]
        R5[Finalize Adoptions]
        R6[Place Pet on Hold]
    end

    subgraph VetActions[Vet Actions]
        V1[Lookup Pet by Microchip]
        V2[Sign Off on Pet]
        V3[Decline Pet]
        V4[View Sign-Off History]
    end

    subgraph AdopterActions[Adopter Actions]
        A1[Browse Available Pets]
        A2[Save Favorites]
        A3[Submit Applications]
        A4[Track Applications]
        A5[View Adoptions]
    end

    subgraph AdminActions[Admin Actions]
        AD1[Verify Vets]
        AD2[Verify Rescue Orgs]
        AD3[View Analytics]
        AD4[Moderate Content]
    end

    F --> FosterActions
    R --> RescueActions
    V --> VetActions
    A --> AdopterActions
    AD --> AdminActions

    style F fill:#81D4FA
    style R fill:#A5D6A7
    style V fill:#FFCC80
    style A fill:#CE93D8
    style AD fill:#EF9A9A
```

---

## 7. Notification Flow

### 7.1 System Notifications

```mermaid
flowchart TD
    subgraph Triggers[Notification Triggers]
        T1[Pet Status Change]
        T2[Application Received]
        T3[Application Status Change]
        T4[Vet Sign-Off]
        T5[Adoption Finalized]
    end

    subgraph Recipients[Notification Recipients]
        R1[Foster]
        R2[Adopter]
        R3[Rescue Org]
        R4[Vet]
    end

    T1 --> |Pet accepted| R1
    T1 --> |Pet goes available| R1
    T1 --> |Favorited pet changes| R2

    T2 --> R3

    T3 --> |Approved| R2
    T3 --> |Approved| R1
    T3 --> |Rejected| R2

    T4 --> R1
    T4 --> R3

    T5 --> R1
    T5 --> R2
    T5 --> R3
    T5 --> R4

    subgraph Channels[Delivery Channels]
        C1[In-App Notification]
        C2[Email via AWS SES]
    end

    R1 & R2 & R3 & R4 --> Channels

    style T1 fill:#E3F2FD
    style T2 fill:#E8F5E9
    style T3 fill:#FFF3E0
    style T4 fill:#FCE4EC
    style T5 fill:#F3E5F5
```

---

## Quick Reference: API Endpoints

### Authentication
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/auth/register` | POST | Create new account |
| `/api/auth/login` | POST | Authenticate user |
| `/api/auth/refresh` | POST | Refresh access token |
| `/api/auth/logout` | POST | Invalidate session |
| `/api/auth/verify-email/{token}` | POST | Verify email |
| `/api/auth/forgot-password` | POST | Request password reset |
| `/api/auth/reset-password` | POST | Reset password |

### Pets
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/pets` | GET | List available pets (public) |
| `/api/pets` | POST | Create pet (Foster) |
| `/api/pets/{id}` | GET | Get pet details |
| `/api/pets/{id}` | PUT | Update pet (Foster) |
| `/api/pets/{id}/submit` | POST | Submit to rescue |
| `/api/pets/{id}/accept` | POST | Accept pet (Rescue) |
| `/api/pets/{id}/decline` | POST | Decline pet (Rescue) |
| `/api/pets/{id}/withdraw` | POST | Withdraw pet (Foster) |
| `/api/pets/my` | GET | Foster's pets |
| `/api/pets/lookup` | GET | Lookup by microchip (Vet) |

### Applications
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/applications` | GET | My applications (Adopter) |
| `/api/applications` | POST | Submit application |
| `/api/applications/{id}` | DELETE | Withdraw application |
| `/api/applications/{id}/approve` | PUT | Approve (Rescue) |
| `/api/applications/{id}/reject` | PUT | Reject (Rescue) |
| `/api/pets/{petId}/applications` | GET | Pet's applications (Rescue) |

### Vet
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/vet/pets/{petId}/sign-off` | POST | Sign off on pet |
| `/api/vet/pets/{petId}/decline` | POST | Decline pet |
| `/api/vet/sign-offs` | GET | Sign-off history |
| `/api/vet/rescue-orgs` | GET | Approved rescues |

### Adoptions
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/adoptions` | GET | My adoptions |
| `/api/adoptions` | POST | Finalize adoption (Rescue) |

---

## Color Legend

| Color | Meaning |
|-------|---------|
| Green (#E8F5E9, #C8E6C9) | Start/Success states |
| Blue (#E3F2FD) | Data creation/storage |
| Orange (#FFF3E0) | Pending/In-progress states |
| Red (#FFCDD2) | Error/Declined states |
| Purple (#FCE4EC, #F3E5F5) | Token/Security related |
| Gray (#E0E0E0) | Neutral/Initial states |

---

*Generated for Forever Home Pet Adoption Platform*
