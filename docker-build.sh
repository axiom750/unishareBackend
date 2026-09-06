#!/bin/bash

# UniShare Backend - Docker Build Script

echo "🚀 Building UniShare Backend Docker Image..."
echo ""

# Build the Docker image
docker build -t unishare-backend:latest .

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Docker image built successfully!"
    echo ""
    echo "To run the container locally:"
    echo "  docker run -p 7500:7500 --env-file .env unishare-backend:latest"
    echo ""
    echo "Or use docker-compose:"
    echo "  docker-compose up"
else
    echo ""
    echo "❌ Docker build failed. Check the errors above."
    exit 1
fi
