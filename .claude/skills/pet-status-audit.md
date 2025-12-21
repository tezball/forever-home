---
description: Audit pet status transitions for correctness
user-invocable: true
---

Analyze pet status state machine implementation for this codebase.

## Steps

1. Read `docs/pet-status.md` for the canonical status flow and transition rules

2. Search for all status transition logic in:
   - `src/main/java/com/example/foreverhome/service/PetService.java`
   - `src/main/java/com/example/foreverhome/domain/pet/PetStatus.java`
   - Any controllers that modify pet status

3. Verify transitions follow the valid flow:
   ```
   Draft → PendingRescue → PendingVet → Available → InProgress → Adopted
   ```

4. Check for:
   - Invalid backward transitions (e.g., Available → Draft)
   - Missing authorization checks (who can trigger each transition)
   - Edge cases: rejected pets, withdrawn listings, vet rejection
   - Race conditions in concurrent status updates

5. Verify test coverage in `src/test/java/` for:
   - All valid transitions
   - Rejection of invalid transitions
   - Authorization enforcement

## Output Format

Report findings with severity and file:line references:

```
[CRITICAL] Invalid transition allowed: Available → Draft
  Location: PetService.java:142
  Fix: Add validation in updateStatus() method

[MEDIUM] Missing test for PendingVet → rejected flow
  Location: PetServiceTest.java
  Fix: Add test case for vet rejection scenario
```
