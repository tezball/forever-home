# Security Review - Forever Home

**Date:** 2025-12-20
**Status:** Under Construction Review
**Reviewer:** Claude Code Security Analysis

---

## Executive Summary

This security review identified **2 critical**, **2 high**, and **4 moderate** vulnerabilities that could harm the site or significantly increase AWS costs. The most severe issues are authorization bypasses (IDOR) that allow users to access or modify other users' data.

---

## Critical Vulnerabilities

### 1. Notification IDOR (Insecure Direct Object Reference)

**File:** `src/main/java/com/example/foreverhome/controller/NotificationController.java:40-43`
**Severity:** CRITICAL
**Impact:** Any authenticated user can mark ANY user's notifications as read

**Vulnerable Code:**
```java
@PutMapping("/{id}/read")
public ResponseEntity<Void> markAsRead(@PathVariable UUID id) {
    notificationService.markAsRead(id);
    return ResponseEntity.noContent().build();
}
```

**Issue:** No ownership verification. The endpoint accepts any notification UUID without checking if it belongs to the authenticated user.

**Fix Required:**
```java
@PutMapping("/{id}/read")
public ResponseEntity<Void> markAsRead(
        @PathVariable UUID id,
        @AuthenticationPrincipal UserPrincipal principal) {
    notificationService.markAsRead(id, principal.userId());
    return ResponseEntity.noContent().build();
}
```

Update `NotificationService.markAsRead()` to verify ownership before modifying.

---

### 2. Pet Access Authorization Bypass

**File:** `src/main/java/com/example/foreverhome/controller/PetController.java:151-161`
**Severity:** CRITICAL
**Impact:** Any RESCUE_ORG user can view pets belonging to ANY rescue organization

**Vulnerable Code:**
```java
@GetMapping("/rescue/{rescueOrgId}")
@PreAuthorize("hasRole('RESCUE_ORG') or hasRole('ADMIN')")
public ResponseEntity<List<PetDto>> getPetsByRescueOrg(@PathVariable UUID rescueOrgId) {
    return ResponseEntity.ok(petService.getPetsByRescueOrg(rescueOrgId));
}
```

**Issue:** Only checks role, not whether the user belongs to the specified rescue organization.

**Fix Options:**
1. Restrict to ADMIN only: `@PreAuthorize("hasRole('ADMIN')")`
2. Add ownership check in the method body
3. Remove endpoint entirely (use `/my-rescue-pets` instead)

---

## High Severity Vulnerabilities

### 3. No Rate Limiting on Pet/Image Creation

**Files:**
- `src/main/java/com/example/foreverhome/controller/PetController.java`
- `src/main/java/com/example/foreverhome/service/PetImageService.java`

**Severity:** HIGH
**Impact:** AWS S3 cost exploitation

**Current State:**
- Registration: 5 requests/minute ✓
- Login: 10 requests/minute ✓
- Password reset: 3 requests/minute ✓
- **Pet creation: UNLIMITED** ✗
- **Image uploads: UNLIMITED** ✗

**Attack Scenario:**
1. Attacker registers accounts (300/hour max)
2. Each account creates unlimited pets
3. Each pet allows 5 images × 5MB = 25MB
4. 1000 pets = 25GB storage + thousands of PUT requests
5. Significant S3 costs accumulated

**Fix Required:**
Add rate limits to `RateLimitFilter.java`:
- Pet creation: 10 pets/day per user
- Image uploads: 50 images/day per user

---

### 4. Content-Type Validation Bypass

**File:** `src/main/java/com/example/foreverhome/service/S3StorageService.java:214-227`
**Severity:** HIGH
**Impact:** Malicious file upload, potential XSS via SVG, or stored malware

**Vulnerable Code:**
```java
String contentType = file.getContentType();
if (contentType == null || !allowedContentTypes.contains(contentType)) {
    throw new StorageException("File type not allowed");
}
```

**Issue:** `file.getContentType()` returns the client-supplied MIME type, which can be spoofed.

**Fix Required:**
Validate file content using magic bytes:
```java
private String detectContentType(MultipartFile file) throws IOException {
    byte[] header = new byte[8];
    file.getInputStream().read(header);

    // JPEG: FF D8 FF
    if (header[0] == (byte)0xFF && header[1] == (byte)0xD8 && header[2] == (byte)0xFF) {
        return "image/jpeg";
    }
    // PNG: 89 50 4E 47 0D 0A 1A 0A
    if (header[0] == (byte)0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47) {
        return "image/png";
    }
    // ... etc
    return null; // Unknown/disallowed
}
```

Or use Apache Tika for robust detection.

---

## Moderate Severity Vulnerabilities

### 5. Refresh Token Not Rotated

**File:** `src/main/java/com/example/foreverhome/service/AuthService.java:209-229`
**Severity:** MODERATE
**Impact:** Stolen refresh tokens remain valid for 7-30 days

**Issue:** When `refreshAccessToken()` is called, the same refresh token is reused. If compromised, attacker has persistent access.

**Fix Required:**
Implement refresh token rotation - issue new refresh token on each use and invalidate the old one.

---

### 6. X-Forwarded-For Header Spoofing

**File:** `src/main/java/com/example/foreverhome/config/RateLimitFilter.java:115`
**Severity:** MODERATE
**Impact:** Rate limiting bypass via header manipulation

**Vulnerable Code:**
```java
String xForwardedFor = request.getHeader("X-Forwarded-For");
if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
    return xForwardedFor.split(",")[0].trim();
}
```

**Issue:** Takes first IP from header without verifying request came through trusted proxy.

**Fix Required:**
Only parse `X-Forwarded-For` when behind a trusted reverse proxy (configured via allowlist).

---

### 7. JWT Secret Has Hardcoded Default

**File:** `src/main/resources/application.properties:21`
**Severity:** MODERATE
**Impact:** Token forgery if default is used in production

**Current:**
```properties
jwt.secret=${JWT_SECRET:mySecretKeyForJWTTokenGenerationWhichMustBeLongEnough256BitsForHS256}
```

**Fix Required:**
Remove default value for production:
```properties
jwt.secret=${JWT_SECRET}
```

Add startup validation that fails if `JWT_SECRET` is not set.

---

### 8. Microchip ID Exposed in Public API

**File:** `src/main/java/com/example/foreverhome/controller/PublicController.java:236-258`
**Severity:** MODERATE
**Impact:** Information disclosure - microchip IDs could be used for fraudulent vet lookups

**Fix Required:**
Remove `microchipId` from `PetPublicResponse` record or mask it (show last 4 digits only).

---

## AWS Cost Attack Vectors Summary

| Attack Vector | Current Protection | Risk Level | Potential Cost Impact |
|--------------|-------------------|------------|----------------------|
| S3 Storage Abuse | 5 images/pet, 5MB/image, but unlimited pets | HIGH | $100s-$1000s |
| S3 PUT Requests | None | HIGH | $10s-$100s |
| SES Email Spam | 3/min password reset | MODERATE | $10s (plus reputation damage) |
| S3 Bandwidth/Egress | None | LOW | Depends on traffic |

---

## Tasks Checklist

### Critical Priority (Fix Before Launch)

- [ ] **Fix Notification IDOR** - Add ownership verification to `markAsRead` endpoint
- [ ] **Fix Pet Access Bypass** - Restrict `/rescue/{rescueOrgId}` endpoints or add ownership checks

### High Priority (Fix Within 1 Week of Launch)

- [ ] **Add Pet Creation Rate Limit** - Implement 10 pets/day per user limit
- [ ] **Add Image Upload Rate Limit** - Implement 50 images/day per user limit
- [ ] **Fix Content-Type Validation** - Use magic bytes instead of client-supplied MIME type

### Moderate Priority (Fix Within 1 Month)

- [ ] **Implement Refresh Token Rotation** - Issue new token on each refresh
- [ ] **Fix X-Forwarded-For Handling** - Only trust header from configured proxies
- [ ] **Remove JWT Default Secret** - Require environment variable, fail startup if missing
- [ ] **Hide Microchip IDs** - Remove or mask in public API responses

### Low Priority (Improvements)

- [ ] Add virus/malware scanning for uploaded images (AWS Lambda + ClamAV)
- [ ] Implement global API rate limiting (beyond auth endpoints)
- [ ] Add audit logging for admin actions (password resets, user suspension)
- [ ] Consider email validation for disposable email providers

---

## Testing Recommendations

1. **IDOR Testing:** Use two accounts to verify users cannot access each other's notifications/pets
2. **Rate Limit Testing:** Script to verify rate limits are enforced correctly
3. **File Upload Testing:** Attempt to upload renamed executables, SVGs with scripts
4. **Token Security Testing:** Verify refresh token rotation works correctly

---

## References

- OWASP Top 10: https://owasp.org/www-project-top-ten/
- OWASP API Security Top 10: https://owasp.org/www-project-api-security/
- AWS S3 Security Best Practices: https://docs.aws.amazon.com/AmazonS3/latest/userguide/security-best-practices.html
