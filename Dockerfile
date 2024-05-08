FROM docker:26-dind as base

ENV PIP_ROOT_USER_ACTION=ignore

RUN apk update --no-cache
RUN apk upgrade --no-cache

RUN apk add --update --no-cache \
    sudo \
    bash \
    curl \
    gcc \
    g++ \
    python3-dev \
    musl-dev \
    linux-headers \
    openjdk21-jdk \
    gradle \
    maven \
    python3 \
    py3-pip \
    py3-setuptools \
    py3-wheel \
    py3-psutil \
    py3-requests

RUN rm /usr/lib/python3.11/EXTERNALLY-MANAGED

RUN pip install \
    flask \
    Faker \
    opentelemetry-api \
    opentelemetry-sdk \
    opentelemetry-exporter-prometheus \
    opentelemetry-exporter-otlp \
    opentelemetry-instrumentation-flask

WORKDIR /workspace

EXPOSE 5000