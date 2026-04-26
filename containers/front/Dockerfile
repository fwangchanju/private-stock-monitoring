FROM node:20-alpine AS frontend-builder
WORKDIR /app
COPY front/package*.json ./
RUN npm ci
COPY front/ ./
RUN npm run build

FROM nginx:alpine
COPY --from=frontend-builder /app/dist /usr/share/nginx/html
COPY deploy/nginx.conf /etc/nginx/conf.d/default.conf
