# BankPro frontend image for `docker compose --profile app` (Vercel builds the site itself).
# Stage 1 builds the static site; stage 2 serves it with nginx, which also forwards /api to the backend.
FROM node:22-alpine AS build
WORKDIR /build
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /build/dist /usr/share/nginx/html
EXPOSE 80
