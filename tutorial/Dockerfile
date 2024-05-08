FROM golang:1.21-alpine as base

RUN apk add --update --no-cache --repository=https://dl-cdn.alpinelinux.org/alpine/edge/community \
    bash \
    hugo

FROM base as rt-dev

WORKDIR /workspace

COPY --chmod=0755 entrypoint.sh /tmp

CMD [ "sh", "-c", "/tmp/entrypoint.sh" ]


FROM base as builder

WORKDIR /opt/app

COPY . .

RUN wget -qO- https://github.com/McShelby/hugo-theme-relearn/archive/main.zip | unzip -d themes -

RUN hugo


FROM nginx:1.25-alpine as rt

ARG USER=otel

WORKDIR /usr/share/nginx/html

COPY --from=builder /opt/app/public .

EXPOSE 80/tcp