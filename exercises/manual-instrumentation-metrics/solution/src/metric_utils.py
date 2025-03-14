# pyright: reportMissingTypeStubs=false

from typing import Any

import psutil
from opentelemetry import metrics as metric_api

# prometheus export
from opentelemetry.exporter.prometheus import PrometheusMetricReader
from opentelemetry.metrics import Counter, Histogram, ObservableGauge
from opentelemetry.sdk.metrics import MeterProvider

# console export
from opentelemetry.sdk.metrics.export import (
    ConsoleMetricExporter,
    MetricReader,
    PeriodicExportingMetricReader,
)

# views
from opentelemetry.sdk.metrics.view import (
    DropAggregation,
    ExplicitBucketHistogramAggregation,
    View,
)
from prometheus_client import start_http_server
from resource_utils import create_resource


def create_views() -> list[View]:
    views = []

    # change name of an instrument
    traffic_volume_change_name = View(
        instrument_type=Counter,
        instrument_name="traffic_volume",
        name="test",
    )

    views.append(traffic_volume_change_name) # type: ignore

    # drop entire intrument
    drop_instrument = View(
        instrument_type=ObservableGauge,
        instrument_name="process.cpu.utilization",
        aggregation=DropAggregation(),
    )
    views.append(drop_instrument) # type: ignore

    # change the aggregation (buckets) for all histogram instruments
    histrogram_explicit_buckets = View(
        instrument_type=Histogram,
        instrument_name="*",  #  supports wildcard pattern matching
        aggregation=ExplicitBucketHistogramAggregation((1, 21, 50, 100, 1000)),
    )
    views.append(histrogram_explicit_buckets) # type: ignore

    return views # type: ignore


def create_metrics_pipeline(export_interval: int) -> MetricReader:
    console_exporter = ConsoleMetricExporter()
    reader = PeriodicExportingMetricReader(
        exporter=console_exporter, export_interval_millis=export_interval
    )
    return reader


def create_prometheus_reader(http_server_port: int = 8000) -> MetricReader:
    start_http_server(port=http_server_port, addr="localhost")
    reader = PrometheusMetricReader()
    return reader


def create_meter(name: str, version: str) -> metric_api.Meter:
    console_reader = create_metrics_pipeline(5000)
    prom_reader = create_prometheus_reader(8000)
    rc = create_resource(name, version)
    views = create_views()
    
    provider = MeterProvider(
        metric_readers=[console_reader, prom_reader], resource=rc, views=views
    )
    metric_api.set_meter_provider(provider)
    meter = metric_api.get_meter(name, version)
    return meter


def create_request_instruments(meter: metric_api.Meter) -> dict[str, Any]:
    traffic_volume = meter.create_counter(
        name="traffic_volume",
        unit="request",
        description="total volume of requests to an endpoint",
    )

    error_rate = meter.create_counter(
        name="error_rate", 
        unit="request", 
        description="rate of failed requests"
    )

    # https://github.com/open-telemetry/semantic-conventions/blob/main/docs/http/http-metrics.md#metric-httpserverrequestduration
    request_latency = meter.create_histogram(
        name="http.server.request.duration",
        unit="s",
        description="latency for a request to be served",
    )

    instruments = {
        "traffic_volume": traffic_volume,
        "error_rate": error_rate,
        "request_latency": request_latency,
    }
    return instruments


# units https://opentelemetry.io/docs/specs/semconv/general/metrics/#instrument-units
def create_resource_instruments(meter: metric_api.Meter) -> dict[str, Any]:
    cpu_util_gauge = meter.create_observable_gauge(
        name="process.cpu.utilization",
        callbacks=[
            lambda x: [metric_api.Observation(psutil.cpu_percent(interval=1) / 100)]
        ],
        unit="1",
        description="CPU utilization",
    )

    memory_usage_gauge = meter.create_observable_up_down_counter(
        name="process.memory.usage",
        callbacks=[lambda x: [metric_api.Observation(psutil.virtual_memory().used)]],
        unit="By",
        description="total amount of memory used",
    )

    instruments = {"cpu_utilization": cpu_util_gauge, "memory_used": memory_usage_gauge}
    return instruments
