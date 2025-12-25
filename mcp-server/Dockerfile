# Build stage
FROM node:18-alpine AS builder

WORKDIR /app

# Copy package files
COPY package*.json ./
COPY tsconfig.json ./

# Install dependencies
RUN npm ci

# Copy source code
COPY src ./src

# Build TypeScript
RUN npm run build

# Production stage
FROM node:18-alpine

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install production dependencies only
RUN npm ci --omit=dev && \
    npm cache clean --force

# Copy built files from builder
COPY --from=builder /app/dist ./dist

# Create non-root user
RUN addgroup -g 1001 -S mcp && \
    adduser -u 1001 -S mcp -G mcp

# Switch to non-root user
USER mcp

# Environment variables (can be overridden)
ENV NODE_ENV=production
ENV PORT=3000
ENV OPENL_BASE_URL=http://studio:8080/rest

# Expose HTTP port
EXPOSE 3000

# Health check - verify HTTP server is responding
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD node -e "require('http').get('http://localhost:3000/health', (r) => {process.exit(r.statusCode === 200 ? 0 : 1)})" || exit 1

# Run the Express HTTP server
CMD ["node", "dist/server.js"]
