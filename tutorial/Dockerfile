FROM golang:1.21-alpine AS base

RUN apk add --update --no-cache --repository=https://dl-cdn.alpinelinux.org/alpine/edge/community \
    bash \
    hugo

FROM base AS rt-dev

WORKDIR /workspace

COPY --chmod=0755 entrypoint.sh /tmp

CMD [ "sh", "-c", "/tmp/entrypoint.sh" ]


FROM base AS builder

WORKDIR /opt/app

COPY . .

RUN wget -qO- https://github.com/McShelby/hugo-theme-relearn/archive/main.zip | unzip -d themes -

RUN hugo


FROM nginx:1.25-alpine AS rt

ARG USER=otel

WORKDIR /usr/share/nginx/html

COPY --from=builder /opt/app/public .

EXPOSE 80/tcp