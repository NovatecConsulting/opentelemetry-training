FROM dcagatay/ubuntu-dind:22.04 as base

ARG DEBIAN_FRONTEND=noninteractive
ENV PIP_ROOT_USER_ACTION=ignore

RUN apt-get update \
    && apt-get install --yes --no-install-recommends \
    sudo \
    bash \
    curl \
    build-essential \
    gradle \
    maven \
    python3 \
    python3-dev \
    python3-pip \
    python3-setuptools \
    python3-wheel \
    python3-psutil

RUN pip install \
    flask \
    Faker \
    requests \
    opentelemetry-api \
    opentelemetry-sdk \
    opentelemetry-exporter-prometheus \
    opentelemetry-exporter-otlp \
    opentelemetry-instrumentation-flask

WORKDIR /workspace

EXPOSE 5000