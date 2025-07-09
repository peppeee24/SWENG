# frontend/Dockerfile
# Stage 1: Build Angular app
FROM node:18-alpine AS builder

WORKDIR /app

# Copia package files
COPY package*.json ./

# Installa dipendenze
RUN npm ci

# Copia source code
COPY . .

# Build dell'applicazione
RUN npm run build --prod

# Stage 2: Serve with nginx
FROM nginx:alpine

# Copia i file build da stage precedente
COPY --from=builder /app/dist/* /usr/share/nginx/html/

# Copia configurazione nginx personalizzata (opzionale)
COPY nginx.conf /etc/nginx/nginx.conf

# Espone porta 80
EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
