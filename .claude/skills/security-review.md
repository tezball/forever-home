---
description: Security audit for auth, injection, and access control issues
user-invocable: true
args: "[scope: auth|api|uploads|all]"
---

Perform a security review of the Forever Home codebase based on scope (default: all).

## Scopes

### auth
- JWT token handling in `SecurityConfig.java` and `JwtService`
- Refresh token rotation and expiration
- httpOnly and secure cookie settings
- Password hashing (bcrypt strength)
- Password reset token expiration and single-use
- Session fixation prevention
- CORS configuration

### api
- SQL injection in repository queries (check for raw queries)
- Input validation on all controller endpoints
- Missing `@Valid` annotations on request bodies
- Authorization checks (`@PreAuthorize`, role validation)
- Rate limiting coverage on sensitive endpoints
- CSRF protection configuration
- Error message information leakage

### uploads
- File upload validation in `S3StorageService`
- Content-type spoofing (magic bytes validation)
- File size limits
- Allowed file extensions
- Path traversal in filenames
- Presigned URL expiration

### all
Run all scope checks above.

## Cross-Reference

Check against:
- `docs/security-review.md` if it exists
- OWASP Top 10 2021 guidelines
- Recent commits mentioning "security" or "vulnerability"

## Output Format

```
[CRITICAL] SQL Injection vulnerability
  Location: PetRepository.java:87
  Issue: Raw query with string concatenation
  Fix: Use parameterized query with @Param

[HIGH] Missing rate limit on password reset
  Location: AuthController.java:156
  Issue: Endpoint allows unlimited reset requests
  Fix: Add @RateLimit annotation

[MEDIUM] Verbose error messages expose stack trace
  Location: GlobalExceptionHandler.java:42
  Issue: Stack trace returned in production
  Fix: Return generic message, log details server-side

[LOW] JWT expiration could be shorter
  Location: application.properties:23
  Issue: Access token valid for 15min (acceptable but could be 5min)
  Fix: Consider reducing for sensitive operations
```
