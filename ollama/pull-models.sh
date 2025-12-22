#!/bin/bash
set -e

echo "Starting Ollama server in background..."
ollama serve &
OLLAMA_PID=$!

# Wait for Ollama to be ready
echo "Waiting for Ollama to be ready..."
for i in {1..30}; do
    if curl -s http://localhost:11434/api/tags > /dev/null 2>&1; then
        echo "Ollama is ready!"
        break
    fi
    echo "Waiting... ($i/30)"
    sleep 2
done

# Pull models
echo "Pulling llama3.2 model..."
ollama pull llama3.2

echo "Pulling llava model..."
ollama pull llava

echo "Models pulled successfully!"
ollama list

# Stop Ollama server
echo "Stopping Ollama server..."
kill $OLLAMA_PID 2>/dev/null || true
wait $OLLAMA_PID 2>/dev/null || true

echo "Done!"
