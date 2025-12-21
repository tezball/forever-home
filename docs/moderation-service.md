# Moderation Service

The moderation service is an AI-powered content moderation system that automatically screens pet profiles for inappropriate content, spam, scams, and ensures uploaded images actually contain pets.

## Overview

The service runs as a **standalone Spring Boot application** separate from the main Forever Home API. It uses local LLM inference via Ollama to analyze text and images, storing results in its own PostgreSQL database.

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                           MODERATION SERVICE                                  │
│                           (Port 8081)                                         │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────┐                                                     │
│  │   Spring Shell CLI  │  Commands: moderate pet, batch, flagged, review    │
│  └──────────┬──────────┘                                                     │
│             │                                                                │
│             ▼                                                                │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │              PetModerationOrchestrator                               │    │
│  │  - Coordinates text + image moderation                               │    │
│  │  - Manages batch jobs with progress tracking                         │    │
│  │  - Aggregates results and determines final status                    │    │
│  └───────────────────┬─────────────────────┬───────────────────────────┘    │
│                      │                     │                                 │
│          ┌───────────▼───────┐  ┌──────────▼────────┐                       │
│          │TextModerationService│  │ImageModerationService│                       │
│          │  - Name analysis    │  │  - Pet detection     │                       │
│          │  - Description scan │  │  - Safety check      │                       │
│          │  - Health notes     │  │  - Family-safe       │                       │
│          └─────────┬─────────┘  └──────────┬────────┘                       │
│                    │                       │                                 │
│                    └───────────┬───────────┘                                │
│                                │                                             │
│                    ┌───────────▼───────────┐                                │
│                    │   Ollama LLM Server   │                                │
│                    │   (Port 11434)        │                                │
│                    │   - llama3.2 (text)   │                                │
│                    │   - llava (vision)    │                                │
│                    └───────────────────────┘                                │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                    Moderation Database (Port 5433)                   │    │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────────┐    │    │
│  │  │moderation_results│  │ flagged_content │  │ moderation_jobs  │    │    │
│  │  │ - pet_id        │  │ - result_id     │  │ - status         │    │    │
│  │  │ - content_type  │  │ - category      │  │ - total_pets     │    │    │
│  │  │ - status        │  │ - severity      │  │ - processed      │    │    │
│  │  │ - confidence    │  │ - description   │  │ - flagged_count  │    │    │
│  │  └─────────────────┘  └─────────────────┘  └──────────────────┘    │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
                                     │
                                     │ REST API calls
                                     ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                         FOREVER HOME API (Port 8080)                         │
│                                                                              │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐         │
│  │  GET /api/pets  │    │GET /api/pets/{id}│    │   Pet Images    │         │
│  │  (paginated)    │    │                 │    │   (S3 URLs)     │         │
│  └─────────────────┘    └─────────────────┘    └─────────────────┘         │
│                                                                              │
│  Pet entity has: moderationStatus (PENDING → APPROVED/FLAGGED/REJECTED)     │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

## End-to-End Flow

### 1. Pet Creation (Main App)

When a foster creates a pet profile in the main application:
- Pet is saved with `moderationStatus = PENDING`
- Pet remains hidden from public listings until approved

### 2. Moderation Trigger (Moderation Service)

An operator runs moderation via CLI:

```bash
# Single pet
moderate pet --pet-id <UUID>

# Batch process pending pets
moderate batch --limit 100
```

### 3. Data Fetching

The `ForeverHomeApiClient` fetches pet data from the main API:

```
GET /api/pets/{id}
→ Returns: name, description, healthNotes, imageUrls[]
```

### 4. Text Moderation

`TextModerationService` analyzes text content using `llama3.2`:

```
┌────────────────────────────────────────────────────────────┐
│                    LLM Prompt                              │
├────────────────────────────────────────────────────────────┤
│ You are a content moderator for a pet adoption platform.  │
│                                                            │
│ Analyze this pet profile for:                              │
│ - Inappropriate language                                   │
│ - Misleading information                                   │
│ - Spam or promotional content                              │
│ - Scam indicators                                          │
│ - Harmful content                                          │
│                                                            │
│ Pet Name: {name}                                           │
│ Description: {description}                                 │
│ Health Notes: {healthNotes}                                │
│                                                            │
│ Respond with:                                              │
│ SCORE: 0.0-1.0 (confidence this is legitimate)            │
│ FLAG: CATEGORY|SEVERITY|description (if issues found)     │
└────────────────────────────────────────────────────────────┘
```

**Categories detected:**
- `INAPPROPRIATE` - Offensive language, adult content
- `MISLEADING` - False claims, breed misrepresentation
- `SPAM` - Promotional content, links
- `SCAM` - Payment requests, suspicious contact info
- `HARMFUL` - Dangerous advice, illegal content

### 5. Image Moderation

`ImageModerationService` analyzes images using `llava` (vision model):

```
┌────────────────────────────────────────────────────────────┐
│                 Vision Model Prompt                        │
├────────────────────────────────────────────────────────────┤
│ Analyze this image for a pet adoption platform.           │
│                                                            │
│ Check:                                                     │
│ 1. IS_PET: Does this image contain a dog or cat?          │
│ 2. FAMILY_SAFE: Is this image appropriate for all ages?   │
│                                                            │
│ [Image bytes attached]                                     │
│                                                            │
│ Respond with:                                              │
│ IS_PET: yes/no                                             │
│ FAMILY_SAFE: yes/no                                        │
│ ISSUES: description of problems (if any)                   │
└────────────────────────────────────────────────────────────┘
```

**Image flags:**
- `NOT_PET` - Image doesn't contain a dog/cat
- `NSFW` - Adult or inappropriate content
- `UNCLEAR` - Can't determine image content

### 6. Result Aggregation

The orchestrator combines all results:

```java
// Determine final status
if (anyResult.hasFlags()) {
    if (flags.maxSeverity() == HIGH) → REJECTED
    else → FLAGGED (needs human review)
} else {
    → APPROVED
}
```

### 7. Storage

Results are persisted to the moderation database:

```sql
-- moderation_results
INSERT INTO moderation_results (
    pet_id, content_type, status, confidence_score,
    model_used, raw_response, reviewed_by, review_notes
);

-- flagged_content (for each flag)
INSERT INTO flagged_content (
    result_id, category, severity, description, suggested_action
);
```

### 8. Human Review

Flagged content requires human review:

```bash
# View flagged items
moderate flagged --limit 50

# Approve after review
moderate review --result-id <UUID> --action approve --notes "False positive"

# Reject
moderate review --result-id <UUID> --action reject --notes "Confirmed spam"
```

## Sequence Diagram

```
┌─────────┐    ┌────────────┐    ┌─────────┐    ┌───────┐    ┌──────┐
│Operator │    │Orchestrator│    │TextSvc  │    │ImgSvc │    │Ollama│
└────┬────┘    └─────┬──────┘    └────┬────┘    └───┬───┘    └──┬───┘
     │               │                │             │           │
     │ moderate pet  │                │             │           │
     │──────────────>│                │             │           │
     │               │                │             │           │
     │               │ fetch pet data │             │           │
     │               │───────────────────────────────────────────────> Forever Home API
     │               │<──────────────────────────────────────────────  {name, desc, images}
     │               │                │             │           │
     │               │ moderate(text) │             │           │
     │               │───────────────>│             │           │
     │               │                │   llama3.2  │           │
     │               │                │────────────────────────>│
     │               │                │<────────────────────────│ {score, flags}
     │               │<───────────────│             │           │
     │               │   TextResult   │             │           │
     │               │                │             │           │
     │               │ moderate(image)│             │           │
     │               │───────────────────────────────>          │
     │               │                │             │   llava   │
     │               │                │             │──────────>│
     │               │                │             │<──────────│ {is_pet, safe}
     │               │<───────────────────────────────          │
     │               │   ImageResult  │             │           │
     │               │                │             │           │
     │               │ aggregate & save              │           │
     │               │────────────────────────────────────────────────> Moderation DB
     │               │                │             │           │
     │<──────────────│                │             │           │
     │ APPROVED/FLAGGED               │             │           │
     │               │                │             │           │
```

## CLI Commands Reference

| Command | Description |
|---------|-------------|
| `moderate pet --pet-id <UUID>` | Moderate a single pet profile |
| `moderate batch --limit N` | Batch process N pending pets |
| `moderate batch --text-only` | Only check text content |
| `moderate batch --images-only` | Only check images |
| `moderate status --pet-id <UUID>` | Check moderation status |
| `moderate flagged --limit N` | List flagged content |
| `moderate flagged --category SPAM` | Filter by category |
| `moderate review --result-id <UUID> --action approve` | Approve flagged item |
| `moderate review --result-id <UUID> --action reject` | Reject flagged item |
| `moderate stats` | Show moderation statistics |
| `moderate jobs --limit N` | List recent batch jobs |
| `moderate check-api` | Test API connectivity |
| `moderate config` | Show current configuration |

## Configuration

```properties
# moderation-service/src/main/resources/application.properties

# Service
server.port=8081

# Database (separate from main app)
spring.datasource.url=jdbc:postgresql://localhost:5433/moderation

# Ollama LLM
spring.ai.ollama.base-url=http://localhost:11434
moderation.text-model=llama3.2
moderation.vision-model=llava
moderation.temperature=0.1

# Feature Flags - enable/disable moderation types
moderation.text-enabled=true      # Enable text moderation (name, description, health notes)
moderation.image-enabled=true     # Enable image moderation (pet photos)

# Debug Mode - enables verbose logging of prompts and LLM responses
moderation.debug=false

# Forever Home API
foreverhome.api.base-url=http://localhost:8080
foreverhome.api.token=${FOREVERHOME_API_TOKEN:}
```

### Feature Flags

The moderation service supports feature flags to control which types of moderation are performed:

| Flag | Default | Description |
|------|---------|-------------|
| `moderation.text-enabled` | `true` | Enable/disable text content moderation |
| `moderation.image-enabled` | `true` | Enable/disable image moderation |
| `moderation.debug` | `false` | Enable verbose logging of prompts and LLM responses |

When a moderation type is disabled:
- The service logs that moderation is disabled
- Content is auto-approved with `modelUsed = "disabled"`
- No LLM calls are made for that content type

### Debug Mode

When `moderation.debug=true`, the service logs:
- Full LLM prompts before sending
- Complete raw LLM responses
- Image metadata (size, MIME type, URL)
- Timing breakdown for each operation

Example debug output:
```
[TEXT][DEBUG] ========== PROMPT START ==========
[TEXT][DEBUG] Pet: 123e4567-e89b-12d3-a456-426614174000
[TEXT][DEBUG] Field: description
[TEXT][DEBUG] Content: A friendly golden retriever...
[TEXT][DEBUG] Full prompt: You are a content moderator...
[TEXT][DEBUG] ========== PROMPT END ==========
```

### Log Prefixes

The service uses structured log prefixes to make filtering and debugging easier:

| Prefix | Component | Description |
|--------|-----------|-------------|
| `[ORCHESTRATOR]` | PetModerationOrchestrator | Overall coordination and workflow |
| `[BATCH]` | Batch processing | Batch job progress and statistics |
| `[TEXT]` | TextModerationService | Text content analysis |
| `[IMAGE]` | ImageModerationService | Image analysis |
| `[SAVE]` | Result persistence | Database operations |
| `[DEBUG]` | Debug mode | Verbose prompt/response logging |

Example log output:
```
[ORCHESTRATOR] ========== MODERATING PET: Buddy (abc-123) ==========
[ORCHESTRATOR] Config: textEnabled=true, imageEnabled=true
[TEXT] ===== Starting text moderation for pet=abc-123 =====
[TEXT] Starting moderation for pet=abc-123 field=name
[TEXT] LLM responded in 245ms for pet=abc-123 field=name
[TEXT] Moderation complete: pet=abc-123 field=name status=APPROVED confidence=0.95 flags=0 duration=245ms
[IMAGE] ===== Starting image moderation for pet=abc-123 imageCount=2 =====
[IMAGE] Fetched image: pet=abc-123 size=156KB mimeType=image/jpeg fetchTime=89ms
[IMAGE] Vision LLM responded in 1250ms for pet=abc-123
[ORCHESTRATOR] ========== COMPLETED PET: abc-123 ==========
[ORCHESTRATOR] Results: 5 total, 0 flagged, 2890ms duration
```

## Running the Service

```bash
# Start infrastructure (PostgreSQL on 5433, Ollama on 11434)
cd moderation-service
docker compose up -d

# Pull required models
docker exec ollama ollama pull llama3.2
docker exec ollama ollama pull llava

# Run the service
./mvnw spring-boot:run
```

## Integration with Main Application

The main Forever Home application has a `moderationStatus` field on `Pet`:

```java
public enum ModerationStatus {
    PENDING,   // Awaiting moderation
    APPROVED,  // Passed moderation
    FLAGGED,   // Needs human review
    REJECTED   // Failed moderation
}
```

Pets with non-APPROVED status are hidden from public listings. The admin panel (`/admin/moderation`) allows manual review of flagged content and user-reported issues.
