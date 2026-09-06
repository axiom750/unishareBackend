# UniShare Backend - Render Deployment Guide

## Prerequisites
- GitHub account with your backend repository
- Render account (https://render.com)
- PostgreSQL database (Supabase/Render PostgreSQL/Any cloud PostgreSQL)

## Deployment Steps

### 1. Prepare Repository
Push your code to GitHub:
```bash
cd unishareBackend
git add .
git commit -m "Add Docker deployment configuration"
git push origin main
```

### 2. Create Render Web Service

1. Go to https://render.com and sign in
2. Click "New +" → "Web Service"
3. Connect your GitHub repository
4. Configure the service:
   - **Name**: `unishare-backend`
   - **Region**: Singapore (or closest to your users)
   - **Branch**: `main` (or your deployment branch)
   - **Root Directory**: Leave empty (or specify if backend is in subdirectory)
   - **Environment**: `Docker`
   - **Plan**: Free (or paid for production)

### 3. Configure Environment Variables

Add the following environment variables in Render dashboard:

#### Required Variables:

**Spring Boot**
- `SPRING_PROFILES_ACTIVE` = `prod`

**Database** (from your Supabase or Render PostgreSQL)
- `DATABASE_URL` = `jdbc:postgresql://your-db-host:5432/your-database`
- `DATABASE_USERNAME` = `your-username`
- `DATABASE_PASSWORD` = `your-password`

**JWT**
- `JWT_SECRET` = Generate a secure 256-bit base64 string:
  ```bash
  openssl rand -base64 32
  ```

**Frontend**
- `FRONTEND_URL` = `https://your-frontend-domain.vercel.app`
- `BACKEND_URL` = `https://unishare-backend.onrender.com` (your Render URL)

**Google OAuth**
- `GOOGLE_CLIENT_ID` = Your Google OAuth client ID
- `GOOGLE_CLIENT_SECRET` = Your Google OAuth client secret

**GitHub OAuth**
- `GITHUB_CLIENT_ID` = Your GitHub OAuth client ID
- `GITHUB_CLIENT_SECRET` = Your GitHub OAuth client secret

**Cloudinary**
- `CLOUDINARY_CLOUD_NAME` = Your Cloudinary cloud name
- `CLOUDINARY_API_KEY` = Your Cloudinary API key
- `CLOUDINARY_API_SECRET` = Your Cloudinary API secret

### 4. Update OAuth Redirect URIs

After deployment, update your OAuth application redirect URIs:

**Google Cloud Console:**
- Authorized redirect URIs: `https://unishare-backend.onrender.com/api/auth/google/callback`

**GitHub Developer Settings:**
- Authorization callback URL: `https://unishare-backend.onrender.com/api/auth/github/callback`

### 5. Deploy

1. Click "Create Web Service"
2. Render will automatically:
   - Build Docker image
   - Deploy container
   - Assign a URL: `https://unishare-backend.onrender.com`

### 6. Verify Deployment

Test the health endpoint:
```bash
curl https://unishare-backend.onrender.com/actuator/health
```

Expected response:
```json
{
  "status": "UP"
}
```

Test the auth endpoint:
```bash
curl https://unishare-backend.onrender.com/api/auth/me
```

## Docker Commands (Local Testing)

### Build Docker Image
```bash
docker build -t unishare-backend .
```

### Run Container Locally
```bash
docker run -p 7500:7500 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DATABASE_URL=jdbc:postgresql://your-db:5432/postgres \
  -e DATABASE_USERNAME=your-username \
  -e DATABASE_PASSWORD=your-password \
  -e JWT_SECRET=your-jwt-secret \
  -e FRONTEND_URL=http://localhost:3000 \
  -e BACKEND_URL=http://localhost:7500 \
  -e GOOGLE_CLIENT_ID=your-client-id \
  -e GOOGLE_CLIENT_SECRET=your-secret \
  -e GITHUB_CLIENT_ID=your-client-id \
  -e GITHUB_CLIENT_SECRET=your-secret \
  -e CLOUDINARY_CLOUD_NAME=your-cloud \
  -e CLOUDINARY_API_KEY=your-key \
  -e CLOUDINARY_API_SECRET=your-secret \
  unishare-backend
```

### Test Local Container
```bash
curl http://localhost:7500/actuator/health
```

## Render Free Tier Notes

⚠️ **Important limitations on Render free tier:**

1. **Cold Starts**: Service spins down after 15 minutes of inactivity
   - First request after sleep takes 30-60 seconds
   - Consider upgrading to paid plan for production

2. **Build Time**: Free tier has 10-minute build timeout
   - Current build should complete in ~5 minutes

3. **Resources**: 
   - 512 MB RAM
   - 0.1 CPU
   - Shared infrastructure

## Troubleshooting

### Build Fails
- Check Docker logs in Render dashboard
- Verify all files are committed to Git
- Ensure pom.xml dependencies are correct

### Application Won't Start
- Check environment variables are set correctly
- Verify database connection string
- Check logs for specific errors

### 502 Bad Gateway
- Application might still be starting (wait 1-2 minutes)
- Check logs for startup errors
- Verify PORT environment variable handling

### Database Connection Issues
- Verify DATABASE_URL format: `jdbc:postgresql://host:port/database`
- Check database is accessible from Render's IP ranges
- Verify credentials are correct

### OAuth Not Working
- Verify redirect URIs match exactly (including https/http)
- Check BACKEND_URL is set correctly
- Verify OAuth credentials in environment variables

## Monitoring

- **Logs**: Render Dashboard → Your Service → Logs
- **Metrics**: Render Dashboard → Your Service → Metrics
- **Health**: `https://your-app.onrender.com/actuator/health`

## Auto-Deploy

Render automatically deploys when you push to your connected branch:
```bash
git push origin main
```

## Need Help?

- Render Docs: https://render.com/docs
- Spring Boot Docs: https://spring.io/projects/spring-boot
- Docker Docs: https://docs.docker.com
