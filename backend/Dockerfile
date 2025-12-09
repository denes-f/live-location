# Stage 1: Build frontend
FROM node:18-alpine AS frontend-build
WORKDIR /web
COPY web/package*.json ./
RUN npm install
COPY web .
RUN npm run build

# Stage 2: Backend
FROM python:3.12-slim

# Set the working directory
WORKDIR /backend

# Copy backend project files into the container
COPY backend/ .

RUN rm -rf /backend/app/static/*

# Copy built frontend files to the static directory
COPY --from=frontend-build /web/dist/ /backend/app/static/

# Install Python dependencies
RUN pip install -r requirements.txt

# Expose the port FastAPI will run on
EXPOSE 8000

# Start the app using Uvicorn with xvfb-run
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]