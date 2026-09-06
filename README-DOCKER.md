# UniShare Backend - Docker Setup

This guide covers local Docker development and Render deployment for the UniShare Spring Boot backend.

## 📋 Prerequisites

- Docker Desktop installed
- Docker Compose installed
- Java 21 (for local non-Docker development)
- Maven 3.9+ (for local non-Docker development)

## 🚀 Quick Start

### Option 1: Using Docker Compose (Recommended)

This will start both PostgreSQL and the backend:

```bash
# Copy environment variables
cp .env.example .env
# Edit .env and add your OAuth credentials

# Start services
docker-compose up
```

Backend will be available at: http://localhost:7500

### Option 2: Build and Run Docker Image Only

```bash
# Build the image
docker build -t unishare-backend .

# Run the container (requires external database)
docker run -p 7500:7500 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DATABASE_URL=jdbc:postgresql://your-db:5432/postgres \
  -e DATABASE_USERNAME=your-username \
  -e DATABASE_PASSWORD=your-password \
  -e JWT_SECRET=your-jwt-secret \
  -e FRONTEND_URL=http://localhost:3000 \
  -e BACKEND_URL=http://localhost:7500 \
  unishare-backend
```

## 🔧 Configuration

### Environment Variables

Create a `.env` file based on `.env.example`:

```env
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
GITHUB_CLIENT_ID=your-github-client-id
GITHUB_CLIENT_SECRET=your-github-client-secret
CLOUDINARY_CLOUD_NAME=your-cloudinary-cloud-name
CLOUDINARY_API_KEY=your-cloudinary-api-key
CLOUDINARY_API_SECRET=your-cloudinary-api-secret
```

## 🐳 Docker Commands

### Build Image
```bash
# Windows
.\docker-build.ps1

# Linux/Mac
./docker-build.sh

# Or manually
docker build -t unishare-backend .
```

### Run Container
```bash
docker run -p 7500:7500 --env-file .env unishare-backend
```

### Check Logs
```bash
docker logs -f <container-id>
```

### Stop Container
```bash
docker stop <container-id>
```

### Remove Container
```bash
docker rm <container-id>
```

### Clean Up
```bash
# Remove all stopped containers
docker container prune

# Remove unused images
docker image prune

# Remove all (careful!)
docker system prune -a
```

## 🔍 Testing

### Health Check
```bash
curl http://localhost:7500/actuator/health
```

Expected response:
```json
{
  "status": "UP"
}
```

### Auth Endpoint
```bash
curl http://localhost:7500/api/auth/me
```

### Docker Compose Health
```bash
docker-compose ps
```

## 📦 Multi-Stage Build

The Dockerfile uses a multi-stage build:

1. **Build Stage** (maven:3.9.9-eclipse-temurin-21-alpine)
   - Copies pom.xml
   - Downloads dependencies
   - Copies source code
   - Builds JAR file

2. **Runtime Stage** (eclipse-temurin:21-jre-alpine)
   - Uses minimal JRE image
   - Copies only the JAR file
   - Runs as non-root user
   - Includes health check

### Benefits:
- ✅ Smaller final image (~200MB vs 600MB+)
- ✅ Faster deployment
- ✅ More secure (no build tools in production)
- ✅ Cached layers for faster rebuilds

## 🌐 Render Deployment

See [RENDER_DEPLOYMENT.md](./RENDER_DEPLOYMENT.md) for detailed deployment instructions.

### Quick Deploy to Render:

1. Push to GitHub
2. Connect repository in Render
3. Select "Docker" environment
4. Add environment variables
5. Deploy!

## 🐛 Troubleshooting

### Container Exits Immediately
```bash
# Check logs
docker logs <container-id>

# Common issues:
# - Missing environment variables
# - Database connection failed
# - Port already in use
```

### Port Already in Use
```bash
# Windows - Find process using port 7500
netstat -ano | findstr :7500

# Kill the process
taskkill /PID <process-id> /F

# Or change port
docker run -p 8080:7500 unishare-backend
```

### Database Connection Issues
```bash
# Check if PostgreSQL is running
docker-compose ps

# Check database logs
docker-compose logs postgres

# Restart database
docker-compose restart postgres
```

### Build Fails
```bash
# Clean Maven cache
mvn clean

# Rebuild without cache
docker build --no-cache -t unishare-backend .
```

### Container Health Check Failing
```bash
# Check if actuator endpoint is accessible
docker exec -it <container-id> curl http://localhost:7500/actuator/health

# Check application logs
docker logs <container-id>
```

## 📊 Monitoring

### Container Stats
```bash
docker stats <container-id>
```

### View Logs
```bash
# Follow logs
docker logs -f <container-id>

# Last 100 lines
docker logs --tail 100 <container-id>

# With timestamps
docker logs -t <container-id>
```

### Inspect Container
```bash
docker inspect <container-id>
```

## 🔐 Security

The Docker image includes several security best practices:

- ✅ Multi-stage build (no build tools in production)
- ✅ Non-root user (appuser:appgroup)
- ✅ Minimal base image (alpine)
- ✅ Health checks
- ✅ No sensitive data in image
- ✅ Environment variable configuration

## 📝 Files Overview

- `Dockerfile` - Multi-stage Docker build configuration
- `docker-compose.yml` - Local development setup with PostgreSQL
- `.dockerignore` - Files excluded from Docker build
- `render.yaml` - Render deployment configuration
- `application-prod.yaml` - Production Spring Boot configuration
- `RENDER_DEPLOYMENT.md` - Detailed deployment guide
- `.env.example` - Environment variables template

## 🚀 Production Checklist

Before deploying to production:

- [ ] Update `application-prod.yaml` with production settings
- [ ] Generate secure JWT secret (256-bit)
- [ ] Configure production database
- [ ] Set up OAuth redirect URIs
- [ ] Enable HTTPS
- [ ] Configure CORS for production frontend URL
- [ ] Set up monitoring and logging
- [ ] Test all endpoints
- [ ] Verify health checks work
- [ ] Review security settings

## 📚 Additional Resources

- [Docker Documentation](https://docs.docker.com)
- [Spring Boot Docker Guide](https://spring.io/guides/topicals/spring-boot-docker)
- [Render Deployment Docs](https://render.com/docs)
- [PostgreSQL Docker Hub](https://hub.docker.com/_/postgres)

## 💡 Tips

1. **Use Docker Compose for development** - Easier to manage multiple services
2. **Layer caching** - Place changing files last in Dockerfile
3. **Environment variables** - Never commit secrets to Git
4. **Health checks** - Always configure for production
5. **Logs** - Use structured logging for easier debugging
6. **Resources** - Monitor memory and CPU usage in production

## 🤝 Support

For issues or questions:
- Check troubleshooting section above
- Review logs: `docker logs <container-id>`
- Check Render dashboard for deployment issues
- Verify all environment variables are set correctly
