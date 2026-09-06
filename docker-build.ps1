# UniShare Backend - Docker Build Script (PowerShell)

Write-Host "🚀 Building UniShare Backend Docker Image..." -ForegroundColor Cyan
Write-Host ""

# Build the Docker image
docker build -t unishare-backend:latest .

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "✅ Docker image built successfully!" -ForegroundColor Green
    Write-Host ""
    Write-Host "To run the container locally:" -ForegroundColor Yellow
    Write-Host "  docker run -p 7500:7500 --env-file .env unishare-backend:latest"
    Write-Host ""
    Write-Host "Or use docker-compose:" -ForegroundColor Yellow
    Write-Host "  docker-compose up"
} else {
    Write-Host ""
    Write-Host "❌ Docker build failed. Check the errors above." -ForegroundColor Red
    exit 1
}
