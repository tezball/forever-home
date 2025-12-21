# Moderation Service

AI-powered content moderation for Forever Home pet profiles using Spring AI with Ollama for local LLM inference.

## Features

- **Text Moderation**: Analyzes pet names, descriptions, and health notes for inappropriate content, spam, scams, and misleading information
- **Image Moderation**: Verifies images contain actual pets and are family-safe using vision AI (LLaVA)
- **Batch Processing**: Moderate multiple pets in one command
- **Review System**: Flag content for human review with detailed categorization

## Prerequisites

1. **Ollama** - Install from https://ollama.ai
2. **PostgreSQL** - For storing moderation results
3. **Forever Home API** - Running instance to fetch pet data

## Quick Start

### 1. Start Infrastructure

```bash
# Start PostgreSQL and Ollama using Docker Compose
docker compose up -d

# Pull required models
ollama pull llama3.2
ollama pull llava
```

### 2. Configure

Edit `src/main/resources/application.properties`:

```properties
# Forever Home API connection
foreverhome.api.base-url=http://localhost:8080
foreverhome.api.token=your-api-token

# Ollama (if not using default localhost:11434)
spring.ai.ollama.base-url=http://localhost:11434
```

### 3. Run

```bash
# Run with Maven
./mvnw spring-boot:run

# Or build and run native image
./mvnw -Pnative native:compile
./target/moderation-service
```

## CLI Commands

```bash
# Check if Forever Home API is available
moderate check-api

# Moderate a single pet
moderate pet --pet-id <UUID>

# Run batch moderation on available pets
moderate batch --limit 100

# Text-only or images-only moderation
moderate batch --limit 50 --text-only
moderate batch --limit 50 --images-only

# View flagged content
moderate flagged --limit 50
moderate flagged --category NOT_PET

# Review a flagged result
moderate review --result-id <UUID> --action approve
moderate review --result-id <UUID> --action reject --notes "Reason for rejection"

# View statistics
moderate stats

# View recent jobs
moderate jobs --limit 10
```

## Moderation Categories

### Text Categories
- `INAPPROPRIATE` - Profanity, offensive language
- `MISLEADING` - False claims about pet
- `SPAM` - Advertising, promotional content
- `SCAM` - Fraud indicators
- `HARMFUL` - Content promoting animal abuse

### Image Categories
- `NOT_PET` - Image doesn't contain a pet
- `NSFW` - Not family-safe content
- `UNCLEAR` - Image too low quality
- `MISLEADING` - Deceptive image
- `INAPPROPRIATE` - Other inappropriate content

## Severity Levels

- `LOW` - Minor issue, may be acceptable
- `MEDIUM` - Should be reviewed
- `HIGH` - Serious issue, likely requires action

## Architecture

```
moderation-service/
├── client/          # Forever Home API client
├── command/         # Spring Shell CLI commands
├── config/          # Spring configuration
├── domain/          # Domain entities
├── repository/      # Data access
└── service/         # Business logic
    ├── TextModerationService    # LLM text analysis
    ├── ImageModerationService   # Vision model analysis
    ├── PetModerationOrchestrator # Coordinates moderation
    └── ModerationResultService  # Results management
```

## Models Used

| Purpose | Model | Notes |
|---------|-------|-------|
| Text moderation | `llama3.2` | Fast, good instruction following |
| Image moderation | `llava` | Multimodal vision-language model |

## Configuration Options

| Property | Default | Description |
|----------|---------|-------------|
| `moderation.text-model` | `llama3.2` | Ollama model for text |
| `moderation.vision-model` | `llava` | Ollama model for images |
| `moderation.temperature` | `0.1` | LLM temperature (lower = more consistent) |
| `foreverhome.api.base-url` | `http://localhost:8080` | Forever Home API URL |
| `foreverhome.api.token` | | API authentication token |

## Building Native Image

```bash
# Requires GraalVM 25+
./mvnw -Pnative native:compile

# Run native executable
./target/moderation-service moderate stats
```
