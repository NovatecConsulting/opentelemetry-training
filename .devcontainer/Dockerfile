FROM debian:bookworm-slim as dind

ARG DEBIAN_FRONTEND=noninteractive

RUN set -eux; \
    apt-get update; \
    apt-get install -y --no-install-recommends \
    sudo \
    python3 \
    ca-certificates \
    iptables \
    net-tools \
    openssl \
    pigz \
    xz-utils \
    ; \
    rm -rf /var/lib/apt/lists/*

COPY --from=docker:26-dind /usr/local/bin /usr/local/bin
COPY --from=docker:26-dind /usr/local/libexec/docker/cli-plugins /usr/local/libexec/docker/cli-plugins

COPY .devcontainer/entrypoint.sh .devcontainer/start-docker.sh /usr/local/bin/

RUN chmod +x /usr/local/bin/start-docker.sh \
	/usr/local/bin/entrypoint.sh

RUN update-alternatives --set iptables /usr/sbin/iptables-legacy
RUN update-alternatives --set ip6tables /usr/sbin/ip6tables-legacy

VOLUME /var/lib/docker

ENTRYPOINT ["entrypoint.sh"]
CMD ["bash"]

FROM dind as base

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
    python-is-python3 \
    python3-dev \
    python3-pip \
    python3-setuptools \
    python3-wheel \
    python3-psutil

RUN rm -rf /usr/lib/python3*/EXTERNALLY-MANAGED

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