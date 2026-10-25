FROM node:22-alpine AS build
WORKDIR /workspace
RUN corepack enable && corepack prepare pnpm@9.15.5 --activate
COPY frontend/nexusedu-web/package.json frontend/nexusedu-web/pnpm-lock.yaml frontend/nexusedu-web/pnpm-workspace.yaml frontend/nexusedu-web/
RUN cd frontend/nexusedu-web && pnpm install --frozen-lockfile
COPY frontend/nexusedu-web frontend/nexusedu-web
ARG VITE_API_URL=/
ARG VITE_BASE_URL=/
ARG VITE_VERSION=local
ENV VITE_API_URL=$VITE_API_URL VITE_BASE_URL=$VITE_BASE_URL VITE_VERSION=$VITE_VERSION
RUN cd frontend/nexusedu-web && pnpm build

FROM nginx:1.27-alpine
COPY deploy/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/frontend/nexusedu-web/dist /usr/share/nginx/html
EXPOSE 80
