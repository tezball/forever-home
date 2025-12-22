# Custom Ollama image with pre-loaded models for Forever Home moderation
FROM ollama/ollama:latest

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copy the model pull script
COPY pull-models.sh /pull-models.sh
RUN chmod +x /pull-models.sh

# Pull models during build
# Ollama needs to be running to pull models, so we start it in the background
RUN /pull-models.sh

# Default command (same as base image)
ENTRYPOINT ["/bin/ollama"]
CMD ["serve"]
