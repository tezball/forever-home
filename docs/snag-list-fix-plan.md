# Forever Home - Snag List Fix Plan

This document provides detailed fix plans for each item identified in the PM snag list review.

---

## Critical Issues (P0 - Blocking)

### Issue #1: Rescue Organization Dashboard Returns 500 Errors

**Priority:** P0 - Critical
**Estimated Effort:** 2-4 hours
**Files to Modify:**
- `src/main/java/com/example/foreverhome/controller/RescueDashboardController.java`
- `src/main/java/com/example/foreverhome/repository/AdopterRepository.java`

**Root Cause:**
The `getApplications()` endpoint (lines 102-128) performs N+1 queries when fetching adopter information for applications. If an adopter is deleted or not found, `adopterMap.get(app.getAdopterId())` returns null, causing serialization failures.

**Current Problematic Code:**
```java
// Line 117-122: Individual queries per adopter - N+1 problem
List<UUID> adopterIds = allApplications.stream().map(AdoptionApplication::getAdopterId).distinct().toList();
Map<UUID, Adopter> adopterMap = adopterIds.stream()
        .map(adopterRepository::findById)  // INDIVIDUAL QUERIES PER ADOPTER
        .filter(java.util.Optional::isPresent)
        .map(java.util.Optional::get)
        .collect(Collectors.toMap(Adopter::getId, a -> a));
```

**Fix Plan:**

1. **Add batch fetch method to AdopterRepository:**
```java
@Query("SELECT * FROM adopters WHERE id IN (:ids)")
List<Adopter> findAllByIds(@Param("ids") Collection<UUID> ids);
```

2. **Update RescueDashboardController.getApplications():**
```java
// Replace N+1 queries with single batch fetch
List<UUID> adopterIds = allApplications.stream()
    .map(AdoptionApplication::getAdopterId)
    .distinct()
    .toList();

Map<UUID, Adopter> adopterMap = adopterRepository.findAllByIds(adopterIds).stream()
    .collect(Collectors.toMap(Adopter::getId, a -> a));

// Handle missing adopters gracefully in response mapping
List<ApplicationResponse> responses = allApplications.stream()
    .map(app -> {
        Adopter adopter = adopterMap.get(app.getAdopterId());
        if (adopter == null) {
            // Return placeholder for deleted adopter
            return new ApplicationResponse(app, "[Deleted User]", "[Unknown]");
        }
        return new ApplicationResponse(app, adopter.getFirstName(), adopter.getLastName());
    })
    .toList();
```

3. **Add null-safe response DTO handling** to prevent serialization failures.

**Testing:**
- Login as rescue@test.com / password123
- Navigate to /rescue/dashboard
- Verify dashboard loads without 500 error
- Verify applications list displays correctly

---

### Issue #2: Vet Pet Lookup by Microchip Returns 404

**Priority:** P0 - Critical
**Estimated Effort:** 2-3 hours
**Files to Modify:**
- `src/main/java/com/example/foreverhome/service/PetService.java`
- `src/main/java/com/example/foreverhome/controller/VetController.java`

**Root Cause:**
The `findByMicrochipForVet()` method (PetService.java:307-329) throws an exception if the pet is not in `PENDING_VET` status. This is too restrictive - vets should be able to look up pets to see their status, even if they can't perform actions on them yet.

**Current Problematic Code:**
```java
// Line 323: Status check is TOO STRICT
if (pet.getStatus() != PetStatus.PENDING_VET) {
    throw new InvalidStatusTransitionException(
            "Pet is not pending vet verification. Current status: " + pet.getStatus());
}
```

**Fix Plan:**

1. **Split lookup from action in PetService:**
```java
@Transactional(readOnly = true)
public PetDto findByMicrochipForVet(String microchipId, UUID vetUserId) {
    Pet pet = petRepository.findByMicrochipId(microchipId)
            .orElseThrow(() -> new ResourceNotFoundException("Pet", "microchip: " + microchipId));

    // Verify vet is approved by rescue org (if pet has one)
    if (pet.getRescueOrgId() != null) {
        boolean isApproved = vetApprovalService.isVetUserApprovedByRescueOrg(vetUserId, pet.getRescueOrgId());
        if (!isApproved) {
            throw new AccessDeniedException(
                "You are not approved by this rescue organization to view their pets");
        }
    }

    // CHANGED: Return pet info regardless of status
    // The vet can see the pet but can only sign off if status is PENDING_VET
    // Include a flag indicating if sign-off is available
    PetDto dto = mapToDto(pet);
    dto.setCanSignOff(pet.getStatus() == PetStatus.PENDING_VET);
    return dto;
}
```

2. **Add `canSignOff` field to PetDto:**
```java
public record PetDto(
    // ... existing fields
    boolean canSignOff  // Indicates if vet can perform sign-off action
) {}
```

3. **Keep strict status check in the actual sign-off endpoint:**
```java
public void vetSignOff(String microchipId, UUID vetUserId, VetSignOffRequest request) {
    Pet pet = petRepository.findByMicrochipId(microchipId)
            .orElseThrow(() -> new ResourceNotFoundException("Pet", "microchip: " + microchipId));

    // Status check only for ACTIONS, not lookups
    if (pet.getStatus() != PetStatus.PENDING_VET) {
        throw new InvalidStatusTransitionException(
                "Pet must be in PENDING_VET status for sign-off. Current: " + pet.getStatus());
    }
    // ... rest of sign-off logic
}
```

4. **Update frontend VetDashboard.tsx to remove mock data fallback** and display actual pet status.

**Testing:**
- Login as vet@test.com / password123
- Navigate to /vet/dashboard
- Enter microchip ID (e.g., "MC123456")
- Verify pet details are returned (not mock data)
- Verify sign-off button is only enabled when status is PENDING_VET

---

## High Priority (P1)

### Issue #3: Missing Approve/Reject Error Handling in Admin Panel

**Priority:** P1
**Estimated Effort:** 1-2 hours
**Files to Modify:**
- `frontend/src/pages/dashboards/AdminDashboard.tsx`

**Root Cause:**
The approve/reject handlers (lines 99-117) have empty catch blocks. Users don't see error messages when actions fail, and the UI optimistically removes items before server confirmation.

**Fix Plan:**

1. **Add error and success state:**
```typescript
const [actionError, setActionError] = useState<string | null>(null);
const [actionSuccess, setActionSuccess] = useState<string | null>(null);
const [processingId, setProcessingId] = useState<string | null>(null);
```

2. **Update handleApprove with proper error handling:**
```typescript
const handleApprove = async (type: string, id: string) => {
  setProcessingId(id);
  setActionError(null);
  try {
    await apiClient.put(`/admin/approvals/${type}/${id}/approve`);
    setActionSuccess(`Successfully approved ${type}`);
    await fetchApprovals(); // Refresh from server
  } catch (err: unknown) {
    if (err && typeof err === 'object' && 'response' in err) {
      const response = (err as { response?: { data?: { message?: string } } }).response;
      setActionError(response?.data?.message || `Failed to approve ${type}`);
    } else {
      setActionError(`Failed to approve ${type}`);
    }
  } finally {
    setProcessingId(null);
  }
};
```

3. **Update handleReject similarly** with error handling.

4. **Add loading state to buttons:**
```tsx
<Button
  variant="primary"
  size="sm"
  onClick={() => handleApprove(approval.type, approval.id)}
  disabled={processingId === approval.id}
>
  {processingId === approval.id ? 'Approving...' : 'Approve'}
</Button>
```

5. **Display error/success messages in UI:**
```tsx
{actionError && (
  <div className="bg-red-100 text-red-700 p-3 rounded mb-4">
    {actionError}
  </div>
)}
{actionSuccess && (
  <div className="bg-green-100 text-green-700 p-3 rounded mb-4">
    {actionSuccess}
  </div>
)}
```

**Testing:**
- Login as admin
- Navigate to approvals section
- Click approve/reject on pending items
- Verify success messages appear
- Disconnect network and retry to verify error handling

---

### Issue #4: No Email Verification Implementation

**Priority:** P1
**Estimated Effort:** 6-8 hours
**Files to Modify:**
- `src/main/resources/application.properties`
- `src/main/java/com/example/foreverhome/service/AuthService.java`
- `src/main/java/com/example/foreverhome/controller/AuthController.java`
- `frontend/src/pages/auth/VerifyEmailPage.tsx` (new)
- `frontend/src/App.tsx`

**Root Cause:**
Configuration has `app.email.verification.auto-activate=true` which bypasses email verification entirely. No frontend page exists to handle verification links.

**Fix Plan:**

1. **Create production-ready configuration profile:**
```properties
# application-prod.properties
app.email.verification.auto-activate=false
app.email.verification.use-console=false
```

2. **Create VerifyEmailPage.tsx:**
```tsx
import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { apiClient } from '../api/client';

export function VerifyEmailPage() {
  const { token } = useParams<{ token: string }>();
  const navigate = useNavigate();
  const [status, setStatus] = useState<'verifying' | 'success' | 'error'>('verifying');
  const [message, setMessage] = useState('');

  useEffect(() => {
    if (token) {
      verifyEmail(token);
    }
  }, [token]);

  const verifyEmail = async (token: string) => {
    try {
      await apiClient.post(`/auth/verify-email/${token}`);
      setStatus('success');
      setMessage('Email verified successfully! You can now log in.');
      setTimeout(() => navigate('/login'), 3000);
    } catch (err) {
      setStatus('error');
      setMessage('Invalid or expired verification link.');
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center">
      {status === 'verifying' && <p>Verifying your email...</p>}
      {status === 'success' && <p className="text-green-600">{message}</p>}
      {status === 'error' && <p className="text-red-600">{message}</p>}
    </div>
  );
}
```

3. **Add route to App.tsx:**
```tsx
<Route path="/verify-email/:token" element={<VerifyEmailPage />} />
```

4. **Update RegisterResponse to indicate verification needed:**
```java
public record RegisterResponse(
    boolean success,
    String email,
    boolean verificationRequired,
    String message
) {
    public static RegisterResponse success(String email, boolean verificationRequired) {
        String msg = verificationRequired
            ? "Please check your email to verify your account"
            : "Registration successful";
        return new RegisterResponse(true, email, verificationRequired, msg);
    }
}
```

5. **Add resend verification endpoint:**
```java
@PostMapping("/resend-verification")
public ResponseEntity<Map<String, String>> resendVerification(@RequestBody ResendVerificationRequest request) {
    authService.resendVerificationEmail(request.email());
    return ResponseEntity.ok(Map.of("message", "Verification email sent"));
}
```

**Testing:**
- Set `auto-activate=false` in properties
- Register new user
- Check console/logs for verification email
- Click verification link
- Verify user can now log in

---

### Issue #5: No Pagination in Admin User Management

**Priority:** P1
**Estimated Effort:** 2-3 hours
**Files to Modify:**
- `frontend/src/pages/dashboards/AdminDashboard.tsx`

**Root Cause:**
Backend supports pagination but frontend displays all users in a single table without pagination controls.

**Fix Plan:**

1. **Add pagination state (already partially exists):**
```typescript
const [currentPage, setCurrentPage] = useState(1);
const [totalPages, setTotalPages] = useState(1);
const [pageSize] = useState(20);
```

2. **Create Pagination component:**
```tsx
function Pagination({
  currentPage,
  totalPages,
  onPageChange
}: {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}) {
  return (
    <div className="flex items-center justify-center gap-2 mt-4">
      <Button
        variant="outline"
        size="sm"
        onClick={() => onPageChange(currentPage - 1)}
        disabled={currentPage <= 1}
      >
        Previous
      </Button>
      <span className="text-sm text-gray-600">
        Page {currentPage} of {totalPages}
      </span>
      <Button
        variant="outline"
        size="sm"
        onClick={() => onPageChange(currentPage + 1)}
        disabled={currentPage >= totalPages}
      >
        Next
      </Button>
    </div>
  );
}
```

3. **Add Pagination below user table:**
```tsx
<Pagination
  currentPage={currentPage}
  totalPages={totalPages}
  onPageChange={(page) => {
    setCurrentPage(page);
    fetchUsers(page);
  }}
/>
```

4. **Fix edge case in AdminController.java** - validate page >= 1:
```java
int page = Math.max(1, requestedPage);
int offset = (page - 1) * limit;
```

**Testing:**
- Create 50+ test users
- Navigate to admin user management
- Verify pagination controls appear
- Verify page navigation works correctly

---

## Medium Priority (P2)

### Issue #6: Pet Registration Flow Untested End-to-End

**Priority:** P2
**Estimated Effort:** 3-4 hours (testing + fixes)
**Files to Review/Test:**
- `frontend/src/pages/foster/PetFormPage.tsx`
- `src/main/java/com/example/foreverhome/controller/PetController.java`
- `src/main/java/com/example/foreverhome/service/PetService.java`

**Fix Plan:**

1. **Create E2E test checklist:**
   - [ ] Login as foster
   - [ ] Navigate to Add Pet form
   - [ ] Fill all required fields
   - [ ] Upload pet image(s)
   - [ ] Select rescue organization
   - [ ] Submit form
   - [ ] Verify pet created in draft status
   - [ ] Submit pet for review
   - [ ] Verify status changes to PENDING_RESCUE

2. **Document any issues found during testing.**

3. **Fix image upload if broken:**
   - Verify S3 LocalStack is running
   - Check PetImageController endpoints
   - Test presigned URL generation

**Testing:**
Execute full checklist above and document failures.

---

### Issue #7: Password Reset Email Doesn't Send

**Priority:** P2
**Estimated Effort:** 4-6 hours
**Files to Modify:**
- `src/main/java/com/example/foreverhome/service/AuthService.java`
- `src/main/java/com/example/foreverhome/service/email/SesEmailService.java`
- `frontend/src/pages/auth/ResetPasswordPage.tsx`

**Fix Plan:**

1. **Verify AuthService.forgotPassword() implementation:**
```java
public void forgotPassword(String email) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("User", "email: " + email));

    String resetToken = UUID.randomUUID().toString();
    user.setPasswordResetToken(resetToken);
    user.setPasswordResetExpiry(LocalDateTime.now().plusHours(24));
    userRepository.save(user);

    emailService.sendPasswordResetEmail(email, resetToken);
}
```

2. **Implement sendPasswordResetEmail in EmailService:**
```java
public void sendPasswordResetEmail(String email, String token) {
    String resetUrl = frontendUrl + "/reset-password/" + token;
    String body = "Click here to reset your password: " + resetUrl;
    sendEmail(email, "Password Reset Request", body);
}
```

3. **Update ResetPasswordPage.tsx** to handle token from URL:
```tsx
const { token } = useParams<{ token: string }>();

const handleSubmit = async (e: FormEvent) => {
  e.preventDefault();
  await apiClient.post('/auth/reset-password', {
    token,
    newPassword: password
  });
  navigate('/login');
};
```

4. **For development**, use ConsoleEmailService to log reset links.

**Testing:**
- Go to /forgot-password
- Enter registered email
- Check console for reset link
- Click link and reset password
- Verify login with new password

---

### Issue #8: Notification System Not Connected

**Priority:** P2
**Estimated Effort:** 8-10 hours
**Files to Modify:**
- `src/main/java/com/example/foreverhome/service/NotificationService.java`
- `src/main/java/com/example/foreverhome/service/PetService.java`
- `frontend/src/components/NotificationBell.tsx`

**Fix Plan:**

1. **Create NotificationService event methods:**
```java
@Service
public class NotificationService {
    public void notifyPetStatusChange(Pet pet, PetStatus oldStatus, PetStatus newStatus) {
        // Notify foster owner
        createNotification(pet.getFosterId(),
            "Pet Status Update",
            "Your pet " + pet.getName() + " status changed to " + newStatus);

        // Notify rescue org if applicable
        if (pet.getRescueOrgId() != null) {
            createNotification(pet.getRescueOrgId(),
                "Pet Status Update",
                pet.getName() + " status changed to " + newStatus);
        }
    }

    public void notifyNewApplication(AdoptionApplication app) {
        // Notify rescue org of new application
    }
}
```

2. **Add event triggers in PetService:**
```java
public void updatePetStatus(UUID petId, PetStatus newStatus) {
    Pet pet = findById(petId);
    PetStatus oldStatus = pet.getStatus();
    pet.setStatus(newStatus);
    petRepository.save(pet);

    // Trigger notification
    notificationService.notifyPetStatusChange(pet, oldStatus, newStatus);
}
```

3. **Create notification fetch endpoint:**
```java
@GetMapping("/notifications")
public ResponseEntity<List<NotificationDto>> getNotifications(
        @AuthenticationPrincipal UserPrincipal principal) {
    return ResponseEntity.ok(notificationService.getUnreadNotifications(principal.userId()));
}
```

4. **Update frontend NotificationBell to fetch real data:**
```tsx
useEffect(() => {
  const fetchNotifications = async () => {
    const res = await apiClient.get<Notification[]>('/notifications');
    setNotifications(res.data);
  };
  fetchNotifications();
  const interval = setInterval(fetchNotifications, 30000); // Poll every 30s
  return () => clearInterval(interval);
}, []);
```

**Testing:**
- Trigger a pet status change
- Verify notification appears in database
- Verify notification bell shows count
- Click bell to see notification list

---

### Issue #9: Pet Status Shows Raw Enum Names

**Priority:** P2
**Estimated Effort:** 1-2 hours
**Files to Modify:**
- `frontend/src/utils/statusDisplay.ts` (new)
- `frontend/src/components/PetCard.tsx`
- `frontend/src/pages/PetDetailPage.tsx`

**Fix Plan:**

1. **Create status display utility:**
```typescript
// src/utils/statusDisplay.ts
export const PET_STATUS_DISPLAY: Record<string, string> = {
  'DRAFT': 'Draft',
  'PENDING_RESCUE': 'Pending Review',
  'PENDING_VET': 'Vet Review',
  'AVAILABLE': 'Available',
  'IN_PROGRESS': 'Adoption In Progress',
  'ADOPTED': 'Adopted',
  'WITHDRAWN': 'Withdrawn',
  'ON_HOLD': 'On Hold',
};

export function formatPetStatus(status: string): string {
  return PET_STATUS_DISPLAY[status] || status;
}

export const STATUS_COLORS: Record<string, string> = {
  'DRAFT': 'bg-gray-100 text-gray-800',
  'PENDING_RESCUE': 'bg-yellow-100 text-yellow-800',
  'PENDING_VET': 'bg-blue-100 text-blue-800',
  'AVAILABLE': 'bg-green-100 text-green-800',
  'IN_PROGRESS': 'bg-purple-100 text-purple-800',
  'ADOPTED': 'bg-teal-100 text-teal-800',
  'WITHDRAWN': 'bg-red-100 text-red-800',
  'ON_HOLD': 'bg-orange-100 text-orange-800',
};
```

2. **Update components to use utility:**
```tsx
import { formatPetStatus, STATUS_COLORS } from '../utils/statusDisplay';

<span className={`px-2 py-1 rounded text-sm ${STATUS_COLORS[pet.status]}`}>
  {formatPetStatus(pet.status)}
</span>
```

**Testing:**
- View pet cards on browse page
- View pet detail page
- Verify all statuses display human-readable labels

---

### Issue #10: Vet Public Profile Page Has No Data Endpoint

**Priority:** P2
**Estimated Effort:** 2-3 hours
**Files to Modify:**
- `src/main/java/com/example/foreverhome/controller/VetController.java`
- `src/main/java/com/example/foreverhome/dto/VetPublicProfileDto.java` (new)

**Fix Plan:**

1. **Create VetPublicProfileDto:**
```java
public record VetPublicProfileDto(
    UUID id,
    String clinicName,
    String city,
    String state,
    String licenseNumber,
    boolean verified,
    String specializations
) {}
```

2. **Add public profile endpoint:**
```java
@GetMapping("/public/{id}")
public ResponseEntity<VetPublicProfileDto> getPublicProfile(@PathVariable UUID id) {
    Vet vet = vetRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Vet", id));

    return ResponseEntity.ok(new VetPublicProfileDto(
        vet.getId(),
        vet.getClinicName(),
        vet.getCity(),
        vet.getState(),
        vet.getLicenseNumber(),
        vet.isVerified(),
        vet.getSpecializations()
    ));
}
```

3. **Update VetProfilePage.tsx** to fetch from this endpoint.

**Testing:**
- Navigate to /vets/:id
- Verify vet profile displays correctly

---

### Issue #11: Pet Image Upload to S3 Untested

**Priority:** P2
**Estimated Effort:** 3-4 hours
**Files to Review:**
- `src/main/java/com/example/foreverhome/controller/PetImageController.java`
- `src/main/java/com/example/foreverhome/service/S3StorageService.java`
- `frontend/src/components/ImageUpload.tsx`

**Fix Plan:**

1. **Verify LocalStack S3 is configured correctly:**
```yaml
# compose.yaml
localstack:
  image: localstack/localstack
  ports:
    - "4566:4566"
  environment:
    - SERVICES=s3,ses
```

2. **Test presigned URL generation:**
```java
@GetMapping("/pets/{petId}/images/upload-url")
public ResponseEntity<Map<String, String>> getUploadUrl(
    @PathVariable UUID petId,
    @RequestParam String filename,
    @RequestParam String contentType) {

    String presignedUrl = s3StorageService.generatePresignedUploadUrl(
        "pet-images/" + petId + "/" + filename,
        contentType
    );
    return ResponseEntity.ok(Map.of("uploadUrl", presignedUrl));
}
```

3. **Create E2E test:**
   - Request presigned URL
   - Upload image using presigned URL
   - Verify image accessible
   - Save image reference to pet

**Testing:**
- Start LocalStack
- Upload image through pet form
- Verify image displays in pet detail

---

## Security Concerns

### Issue #12: Default JWT Secret in Config

**Priority:** P2
**Estimated Effort:** 1 hour
**Files to Modify:**
- `src/main/resources/application.properties`
- `src/main/resources/application-prod.properties` (new)

**Fix Plan:**

1. **Create production profile:**
```properties
# application-prod.properties
jwt.secret=${JWT_SECRET}
jwt.access-token-expiration=900000
jwt.refresh-token-expiration=604800000
```

2. **Document required environment variables:**
```markdown
## Required Environment Variables for Production
- JWT_SECRET: Minimum 256-bit secret for HS256 signing
- DATABASE_URL: PostgreSQL connection string
- AWS_ACCESS_KEY_ID: AWS credentials
- AWS_SECRET_ACCESS_KEY: AWS credentials
```

3. **Add startup validation** to fail fast if secret is default:
```java
@PostConstruct
public void validateJwtSecret() {
    if (jwtSecret.contains("mySecretKey") && !environment.acceptsProfiles("dev", "test")) {
        throw new IllegalStateException("Production requires custom JWT_SECRET");
    }
}
```

---

### Issue #13: Test Credentials in application.properties

**Priority:** P2
**Estimated Effort:** 30 minutes
**Files to Modify:**
- `src/main/resources/application.properties`

**Fix Plan:**

1. **Move test credentials to application-dev.properties:**
```properties
# application-dev.properties
aws.access-key=test
aws.secret-key=test
aws.s3.endpoint=http://localhost:4566
```

2. **Use environment variables in main properties:**
```properties
# application.properties
aws.access-key=${AWS_ACCESS_KEY_ID:}
aws.secret-key=${AWS_SECRET_ACCESS_KEY:}
```

---

### Issue #14: No Rate Limiting on Auth Endpoints

**Priority:** P2
**Estimated Effort:** 3-4 hours
**Files to Modify:**
- `pom.xml`
- `src/main/java/com/example/foreverhome/config/RateLimitConfig.java` (new)

**Fix Plan:**

1. **Add bucket4j dependency:**
```xml
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.7.0</version>
</dependency>
```

2. **Create rate limit filter:**
```java
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain chain) {

        if (request.getRequestURI().startsWith("/api/auth")) {
            String key = request.getRemoteAddr();
            Bucket bucket = buckets.computeIfAbsent(key, this::createBucket);

            if (!bucket.tryConsume(1)) {
                response.setStatus(429);
                response.getWriter().write("Too many requests");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private Bucket createBucket(String key) {
        return Bucket.builder()
            .addLimit(Bandwidth.simple(10, Duration.ofMinutes(1)))
            .build();
    }
}
```

---

### Issue #15: Email Verification Auto-Bypassed

**Priority:** P2
**Estimated Effort:** 30 minutes

**Fix Plan:**
See Issue #4. Ensure `auto-activate=false` in production profile.

---

## UX/Design Issues

### Issue #16: Mock Data Fallback Masks Real Errors

**Priority:** P3
**Estimated Effort:** 1-2 hours
**Files to Modify:**
- `frontend/src/pages/dashboards/VetDashboard.tsx`

**Fix Plan:**

1. **Remove mock data fallback:**
```typescript
// REMOVE this fallback
const mockPet = {
  id: crypto.randomUUID(),
  name: 'Max',
  // ...
};
```

2. **Show proper error messages instead:**
```typescript
try {
  const res = await apiClient.get(`/vet/pets/lookup?microchip=${microchip}`);
  setPetResult(res.data);
} catch (err) {
  if (axios.isAxiosError(err) && err.response?.status === 404) {
    setError('No pet found with this microchip ID');
  } else if (axios.isAxiosError(err) && err.response?.status === 403) {
    setError('You are not approved to view pets from this rescue organization');
  } else {
    setError('Failed to lookup pet. Please try again.');
  }
}
```

---

### Issue #17: No Global Error Boundary

**Priority:** P3
**Estimated Effort:** 2 hours
**Files to Modify:**
- `frontend/src/components/ErrorBoundary.tsx` (new)
- `frontend/src/App.tsx`

**Fix Plan:**

1. **Create ErrorBoundary component:**
```tsx
import { Component, ErrorInfo, ReactNode } from 'react';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
  error?: Error;
}

export class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false };

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('Uncaught error:', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="min-h-screen flex items-center justify-center">
          <div className="text-center">
            <h1 className="text-2xl font-bold text-red-600">Something went wrong</h1>
            <p className="text-gray-600 mt-2">Please refresh the page and try again.</p>
            <button
              onClick={() => window.location.reload()}
              className="mt-4 px-4 py-2 bg-primary text-white rounded"
            >
              Refresh Page
            </button>
          </div>
        </div>
      );
    }
    return this.props.children;
  }
}
```

2. **Wrap App with ErrorBoundary:**
```tsx
<ErrorBoundary>
  <Router>
    <App />
  </Router>
</ErrorBoundary>
```

---

### Issue #18: Missing Loading States

**Priority:** P3
**Estimated Effort:** 2-3 hours

**Fix Plan:**

1. **Create LoadingSpinner component:**
```tsx
export function LoadingSpinner({ message = 'Loading...' }: { message?: string }) {
  return (
    <div className="flex items-center justify-center p-8">
      <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
      <span className="ml-2 text-gray-600">{message}</span>
    </div>
  );
}
```

2. **Add to all data-fetching components.**

---

### Issue #19: No Human Status Labels

**Priority:** P3

**Fix Plan:**
See Issue #9 above.

---

## Documentation Gaps

### Issue #20: No OpenAPI/Swagger Documentation

**Priority:** P3
**Estimated Effort:** 2-3 hours
**Files to Modify:**
- `pom.xml`
- `src/main/java/com/example/foreverhome/config/OpenApiConfig.java` (new)

**Fix Plan:**

1. **Add springdoc dependency:**
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

2. **Create OpenAPI configuration:**
```java
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Forever Home API")
                .version("1.0")
                .description("Pet Adoption Platform API"));
    }
}
```

3. **Access at:** `/swagger-ui.html`

---

### Issue #21: No Production Deployment Guide

**Priority:** P3
**Estimated Effort:** 4-6 hours
**Files to Create:**
- `docs/deployment-guide.md`

**Fix Plan:**
Create comprehensive deployment guide covering:
- AWS infrastructure setup (RDS, S3, SES)
- Environment variables
- Database migration procedure
- Monitoring setup
- SSL/HTTPS configuration
- Health check endpoints

---

### Issue #22: No Testing Strategy Document

**Priority:** P3
**Estimated Effort:** 2-3 hours
**Files to Create:**
- `docs/testing-strategy.md`

**Fix Plan:**
Document:
- Unit test coverage goals (80%+)
- Integration test requirements
- E2E test scenarios
- Load testing procedures
- Test data management

---

## Technical Debt

### Issue #23-26: Test Coverage, E2E Tests, Logging, Monitoring

**Priority:** P3
**Estimated Effort:** Ongoing

**Fix Plan:**

1. **Increase test coverage:**
   - Add integration tests for all controllers
   - Add service layer unit tests
   - Target 80% code coverage

2. **Add E2E browser tests:**
   - Set up Playwright or Cypress
   - Create tests for critical user flows

3. **Improve logging:**
   - Add structured logging with correlation IDs
   - Log all status transitions
   - Log authentication events

4. **Add monitoring:**
   - Configure Actuator endpoints
   - Add Micrometer metrics
   - Set up health check dashboard

---

## Implementation Timeline Recommendation

### Sprint 1 (Week 1-2): Critical Fixes
- Issue #1: Rescue Org Dashboard (2-4h)
- Issue #2: Vet Pet Lookup (2-3h)
- Issue #3: Admin Approval Buttons (1-2h)
- Issue #6: Test Pet Registration (3-4h)

### Sprint 2 (Week 3-4): High Priority
- Issue #4: Email Verification (6-8h)
- Issue #5: Admin Pagination (2-3h)
- Issue #7: Password Reset (4-6h)

### Sprint 3 (Week 5-6): Medium Priority
- Issue #8: Notifications (8-10h)
- Issue #9: Status Display (1-2h)
- Issue #10: Vet Public Profile (2-3h)
- Issue #11: Image Upload (3-4h)

### Sprint 4 (Week 7-8): Security & Polish
- Issues #12-15: Security hardening (6-8h)
- Issues #16-19: UX improvements (6-8h)

### Ongoing: Documentation & Technical Debt
- Issues #20-22: Documentation (8-12h)
- Issues #23-26: Testing & monitoring (ongoing)

---

## Appendix: Test Accounts

| Role | Email | Password |
|------|-------|----------|
| Foster | foster@test.com | password123 |
| Adopter | adopter@test.com | password123 |
| Rescue Org | rescue@test.com | password123 |
| Vet | vet@test.com | password123 |
| Admin | admin@test.com | password123 |
