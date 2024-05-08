---
title: "metrics"
date: 2023-12-06T09:43:24+01:00
draft: false
weight: 3
---

### Metrics in OpenTelemetry

A `MetricReader` in OpenTelemetry is an interface that defines how to read metrics from the SDK. It is responsible for collecting metrics data from the SDK and exporting it to a backend system for storage and analysis. There are different types of `MetricReader` implementations, such as `PeriodicExportingMetricReader`, which collects metrics at regular intervals and exports them to a backend.

The metric data model in OpenTelemetry defines the structure of the data that is collected and exported by the SDK. It includes information about the resource, instrumentation library, and the actual metrics data. Here's an example of what the metric data model might look like in JSON format:

```json
{
    "resource_metrics": [
        {
            "resource": {
                "attributes": {
                    "telemetry.sdk.language": "python",
                    "telemetry.sdk.name": "opentelemetry",
                    "telemetry.sdk.version": "1.24.0",
                    "host.name": "ebdcf73e98c0",
                    "service.name": "app.py",
                    "service.version": "0.1"
                },
                "schema_url": ""
            },
            "scope_metrics": [
                {
                    "scope": {
                        "name": "app.py",
                        "version": "0.1",
                        "schema_url": ""
                    },
                    "metrics": [
                        {
                            "name": "process.memory.usage",
                            "description": "total amount of memory used",
                            "unit": "By",
                            "data": {
                                "data_points": [
                                    {
                                        "attributes": {},
                                        "start_time_unix_nano": 1713351214657218027,
                                        "time_unix_nano": 1713351214657419373,
                                        "value": 673140736
                                    }
                                ],
                                "aggregation_temporality": 2,
                                "is_monotonic": false
                            }
                        }
                    ],
                    "schema_url": ""
                }
            ],
            "schema_url": ""
        }
    ]
}

```

### How to perform the exercise
* You need to either start the [repository](https://github.com/JenSeReal/otel-getting-started/) with Codespaces, Gitpod or clone the repository with git and run it locally with dev containers or docker compose
* Initial directory: `labs/manual-instrumentation-metrics/initial`
* Solution directory: `labs/manual-instrumentation-metrics/solution`
* Source code: `labs/manual-instrumentation-metrics/initial/src`
* How to run the application either:
  * Run the task for the application: `Run manual-instrumentation-metrics initial application` (runs the Python application)
  * Run the application with Terminal commands `python3 src/app.py` (runs the Python application)

---

### Let's start

Regardless of your setup, open two separate terminals with a shell in the container.
We'll use one to start the application's web server and the other to send requests to the service endpoints.
This lab demonstrates how to add traces to a Python application. The service is built using the [Flask](https://flask.palletsprojects.com) web framework.
We chose Python because its simple syntax keeps code snippets concise and readable.
```sh
opentelemetry-api==1.24.0
opentelemetry-sdk==1.24.0
opentelemetry-semantic-conventions==0.42b0
```

Run `pip freeze | grep opentelemetry`.
The output reveals that OpenTelemetry's API and SDK packages have already been installed in your Python environment.

### configure metrics pipeline and obtain a meter
{{< figure src="images/create_meter_configure_pipeline.drawio.png" width=700 caption="metric signal" >}}

```py { title="metric_utils.py" }
# OTel SDK
from opentelemetry.sdk.metrics.export import (
    ConsoleMetricExporter,
    PeriodicExportingMetricReader,
    MetricReader,
)

def create_metrics_pipeline(export_interval: int) -> MetricReader:
    console_exporter = ConsoleMetricExporter()
    reader = PeriodicExportingMetricReader(
        exporter=console_exporter, export_interval_millis=export_interval
    )
    return reader
```

Let's create a new file `metric_utils.py` inside the `src` directory.
We'll use it to bundle configuration related to the metrics signal.
At the top of the file, specify the following imports for OpenTelemetry's metrics SDK.
Then, create a new function `create_metrics_pipeline`.
In a production scenario, one would deploy a backend to store and a front-end to analyze time-series data.
Create a `ConsoleMetricExporter` to write a JSON representation of metrics generated by the SDK to stdout.
<!-- TODO -->
Next, we instantiate a `PeriodicExportingMetricReader` that collects metrics at regular intervals and passes them to the exporter.

```py { title="metric_utils.py" }
# OTel API
from opentelemetry import metrics as metric_api

# OTel SDK
from opentelemetry.sdk.metrics import MeterProvider

def create_meter(name: str, version: str) -> metric_api.Meter:
    # configure provider
    metric_reader = create_metrics_pipeline(5000)
    provider = MeterProvider(
        metric_readers=[metric_reader],
    )

    # obtain meter
    metric_api.set_meter_provider(provider)
    meter = metric_api.get_meter(name, version)
    return meter
```

Then, define a new function `create_meter`.
To obtain a `Meter` we must first create a `MeterProvider`.
To connect the MeterProvider to our metrics pipeline, pass the `PeriodicExportingMetricReader` to the constructor.
Use the metrics API to register the global MeterProvider and retrieve the meter.

<!-- TODO re-use resource_utils -->

```py { title="app.py" }
# custom
from metric_utils import create_meter

# global variables
app = Flask(__name__)
meter = create_meter("app.py", "0.1")
```

Finally, open `app.py` and import `create_meter`.
Invoke the function and assign the return value to a global variable `meter`.

### create instruments to record measurements

{{< figure src="images/meter.drawio_instruments.svg" width=700 caption="overview of metric signal" >}}

As you have noticed, thus far, everything was fairly similar to the tracing lab.
However, in contrast to tracers, we do not use meters directly to generate metrics.
Instead, meters produce (and are associated with) a set of [`instruments`](https://opentelemetry-python.readthedocs.io/en/latest/api/metrics.html#opentelemetry.metrics.Instrument).
An instrument reports `measurements`, which represent a data point reflecting the state of a metric at a particular point in time. So a meter can be seen as a factory for creating instruments and is associated with a specific library or module in your application. Instruments are the objects that you use to record metrics and represent specific metric types, such as counters, gauges, or histograms. Each instrument has a unique name and is associated with a specific meter. Measurements are the individual data points that instruments record, representing the current state of a metric at a specific moment in time. Data points are the aggregated values of measurements over a period of time, used to analyze the behavior of a metric over time, such as identifying trends or anomalies.

To illustrate this, consider a scenario where you want to measure the number of requests to a web server. You would use a meter to create an instrument, such as a counter, which is designed to track the number of occurrences of an event. Each time a request is made to the server, the counter instrument records a measurement, which is a single data point indicating that a request has occurred. Over time, these measurements are aggregated into data points, which provide a summary of the metric's behavior, such as the total number of requests received.

{{< figure src="images/instrument_types.drawio.png" width=600 caption="overview of different instruments" >}}

Similar to the real world, there are different types of instruments depending on what you try to measure.
OpenTelemetry provides different types of instruments to measure various aspects of your application. For example:
- `Counters` are used for monotonically increasing values, such as the total number of requests handled by a server.
- `UpAndDownCounters` are used to track values that can both increase and decrease, like the number of active connections to a database
- [`Gauge`](https://opentelemetry.io/docs/specs/otel/metrics/data-model/#gauge) instruments reflect the state of a value at a given time, such as the current memory usage of a process.
- `Histogram` instruments are used to analyze the distribution of how frequently a value occurs, which can help identify trends or anomalies in the data.

Each type of instrument, except for histograms, has a synchronous and asynchronous variant. Synchronous instruments are invoked in line with the application code, while asynchronous instruments register a callback function that is invoked on demand. This allows for more efficient and flexible metric collection, especially in scenarios where the metric value is expensive to compute or when the metric value changes infrequently.

For now, we will focus on the basic concepts and keep things simple, but as you become more familiar with OpenTelemetry, you will be able to leverage these components to create more sophisticated metric collection and analysis strategies.

```py { title="metric_utils.py" }
def create_request_instruments(meter: metric_api.Meter) -> dict[str, metric_api.Instrument]:
    index_counter = meter.create_counter(
        name="index_called",
        unit="request",
        description="Total amount of requests to /"
    )

    instruments = {
        "index_counter": index_counter,
    }
    return instruments
```

In `metric_utils.py` and add a new function `create_request_instruments`.
Here, we'll define workload-related instruments for the application.
As a first example, use the `meter` to create a `Counter` instrument to measure the number of requests to the `/` endpoint.
Every instrument must have a `name`, but we'll also supply the `unit` of measurement and a short `description`.
For analysis tools to interpret the metric correctly, the name should follow OpenTelemetry's [semantic conventions](https://opentelemetry.io/docs/specs/semconv/general/metrics/) and the unit should follow the [Unified Code for Units of Measure (UCUM)](https://opentelemetry.io/docs/specs/semconv/general/metrics/).

```py { title="app.py" }
from metric_utils import create_meter, create_request_instruments

@app.route("/", methods=["GET", "POST"])
def index():
    request_instruments['index_counter'].add(1)
    # ...

if __name__ == "__main__":
    request_instruments = create_request_instruments(meter)
    # ...
```

Now that we have defined our first instrument, import the helper function into `app.py`.
Let's generate some metrics.
Call `create_request_instruments` in the file's main section and assign it to a global variable.
In our `index` function, reference the counter instrument and call the `add` method to increment its value.
Start the web server with `python app.py` and use the second terminal to send a request to `/` via `curl -XGET localhost:5000; echo`.


```json
"resource": { // <- origin
    "attributes": {
        "telemetry.sdk.language": "python",
        "telemetry.sdk.name": "opentelemetry",
        "telemetry.sdk.version": "1.24.0",
        "service.name": "unknown_service"
    },
},
"scope_metrics": [
    {
        "scope": { // <-- defined by meter
            "name": "app.py",
            "version": "0.1",
            "schema_url": ""
        },
        "metrics": [
            {
                "name": "index_called", // <-- identify instrument
                "description": "Total amount of requests to /",
                "unit": "request",
                "data": {
                    "data_points": [ // <-- reported measurements
                        {
                            "attributes": {},
                            "start_time_unix_nano": 1705676073229533664,
                            "time_unix_nano": 1705676101944590149,
                            "value": 1
                        }
                    ],
                    "aggregation_temporality": 2, // <-- aggregation
                    "is_monotonic": true
                }
            }
        ],
    }
]
```
  The `scope_metrics` array contains metrics scopes, each representing a logical unit of the application code with which the telemetry is associated. Each scope includes a `scope` section that identifies the instrumentation scope, such as the name and version of the instrumented module or library, and an optional schema URL. The `metrics` array within each scope contains the actual metrics reported by instruments associated with the meter, with each metric having a `name`, `description`, `unit`, and `data points`. The `data points` include `attributes`, `timestamps`, and the `value` of the measurement, along with properties that describe the `aggregation temporality` and whether the metric is `monotonic`.

The `PeriodicExportingMetricReader` records the state of metrics from the SDK at a regular interval and `ConsoleMetricExporter` writes a JSON representation to stdout.
Similar to tracing, we can attach a resource to the telemetry generated by the `MeterProvider`.
The JSON snippet provided is an example of the OpenTelemetry metrics data model, which is a structured representation of telemetry data collected from an application.
The `resource` section describes the entity being monitored, such as a service or a host, and includes attributes like the language of the telemetry SDK, the SDK's name and version, and the service's name.
The `scope_metrics` section has two main parts.
First, the instrumentation `scope` identifies a logical unit in the application code with which the telemetry is associated.
It represents the `name` and `version` parameters we passed to `get_meter`.
The `metrics` field contains a list of metrics reported by instruments associated with the meter.
Each metric consists of two main parts.
First, there is information to identify the instrument (e.g. name, kind, unit, and description).
Second, the `data` second contains a list of `data_points`, which are measurements recorded by the instrument.
Each measurement typically consists of a `value`, `attributes`, and a `timestamp`.
The `aggregation_temporality` indicates whether the metric is cumulative, and `is_monotonic` specifies whether the metric only increases (or decreases, in the case of a gauge). This model is designed to be flexible and extensible, ensuring compatibility with existing monitoring systems and standards like Prometheus and StatsD, facilitating interoperability with various monitoring tools.

### metric dimensions

```py { title="app.py" }
from flask import Flask, make_response, request

@app.route("/", methods=["GET", "POST"])
def index():
    request_instruments["index_counter"].add(
        1,
        { "http.request.method": request.method }
    )
```

So far, we only used the `add` method to increment the counter.
However, `add` also has a second optional parameter to specify attributes.
This brings us to the topic of metric dimensions.
To illustrate their use, modify the `index` function as shown above.
Send a couple of POST and GET requests to `/` via `curl -XPOST localhost:5000; echo` and `curl -XGET localhost:5000; echo`.
Look at the output, what do you notice?


```json
"data_points": [
    {
        "attributes": {
            "http.request.method": "POST"
        },
        "start_time_unix_nano": 1705916954721405005,
        "time_unix_nano": 1705916975733088638,
        "value": 1
    },
    {
        "attributes": {
            "http.request.method": "GET"
        },
        "start_time_unix_nano": 1705916954721405005,
        "time_unix_nano": 1705916975733088638,
        "value": 1
    }
],
```

We can conclude that the instrument reports separate counters for each unique combination of attributes.
This can be incredibly useful.
For example, if we pass the status code of a request as an attribute, we can track distinct counters for successes and errors.
While it is tempting to be more specific, we have to be careful with introducing new metric dimensions.
The number of attributes and the range of values can quickly lead to many unique combinations.
High cardinality means we have to keep track of numerous distinct time series, which leads to increased storage requirements, network traffic, and processing overhead.
Moreover, specific metrics may have less aggregative quality, which can make it harder to derive meaningful insights.

In conclusion, the selection of metric dimensions is a delicate balancing act. Metrics with high cardinality, which result from introducing many attributes or a wide range of values, can lead to numerous unique combinations. This can increase storage requirements, network traffic, and processing overhead, as each unique combination of attributes represents a distinct time series that must be tracked. Moreover, metrics with low aggregative quality may be less useful when aggregated, making it more challenging to derive meaningful insights from the data. Therefore, it is essential to carefully consider the dimensions of the metrics to ensure that they are both informative and manageable within the constraints of the monitoring system.

### instruments to measure golden signals

{{< figure src="images/resource_workload_analysis.PNG" width=600 caption="workload and resource analysis" >}}

Now, let's put our understanding of the metrics signal to use.
Before we do that, we must address an important question: *What* do we measure?
Unfortunately, the answer is anything but simple.
Due to the vast amount of events within a system and many statistical measures to calculate, there are nearly infinite things to measure.
A catch-all approach is cost-prohibitive from a computation and storage point, increases the noise which makes it harder to find important signals, leads to alert fatigue, and so on.
The term metric refers to a statistic that we consciously chose to collect because we deem it to be *important*.
Important is a deliberately vague term, because it means different things to different people.
A system administrator typically approaches an investigation by looking at the utilization or saturation of physical system resources.
A developer is usually more interested in how the application responds, looking at the applied workload, response times, error rates, etc.
In contrast, a customer-centric role might look at more high-level indicators related to contractual obligations (e.g. as defined by an SLA), business outcomes, and so on.
The details of different monitoring perspectives and methodologies (such as [USE](https://www.brendangregg.com/usemethod.html) and RED) are beyond the scope of this lab.
However, the [four golden signals](https://sre.google/sre-book/monitoring-distributed-systems/#xref_monitoring_golden-signals) of observability often provide a good starting point:
- **Traffic**: volume of requests handled by the system
- **Errors**: rate of failed requests
- **Latency**: the amount of time it takes to serve a request
- **Saturation**: how much of a resource is being consumed at a given time

Let's instrument our application accordingly.

#### traffic

```py { title="metric_utils.py" }
def create_request_instruments(meter: metrics.Meter) -> dict:
    traffic_volume = meter.create_counter(
        name="traffic_volume",
        unit="request",
        description="total volume of requests to an endpoint",
    )

    instruments = {
        "traffic_volume": traffic_volume,
    }

    return instruments
```
```py { title="app.py" }
@app.before_request
def before_request_func():
    request_instruments["traffic_volume"].add(
        1, attributes={"http.route": request.path}
    )
```

Let's measure the total amount of traffic for a service.
First, go to `create_request_instruments` and `index` to delete everything related to the `index_counter` instrument.
Incrementing a counter on every route we serve would lead to a lot of code duplication.
Instead, let's create a custom function `before_request_func` and annotate it with Flask's `@app.before_request` decorator.
Thereby, the function is executed on incoming requests before they are handled by the view serving a route.


#### error rate
```py { title="metric_utils.py" }
def create_request_instruments(meter: metrics.Meter) -> dict:
    error_rate = meter.create_counter(
        name="error_rate",
        unit="request",
        description="rate of failed requests"
    )

    instruments = {
        "traffic_volume": traffic_volume,
        "error_rate": error_rate,
    }
```
```py { title="app.py" }
from flask import Flask, make_response, request, Response

@app.after_request
def after_request_func(response: Response) -> Response:
    # ...
    request_instruments["error_rate"].add(1, {
            "http.route": request.path
            "state": "success" if response.status_code < 400 else "fail",
        }
    )
    return response
```

As a next step, let's track the error rate of the service.
Create a separate Counter instrument.
Ultimately, the decision of what constitutes a failed request is up to us.
In this example, we'll simply refer to the status code of the response.
To access it, create a function `after_request_func` and use Flask's `@app.after_request` decorator to execute it after a view function returns.

#### latency

The time it takes a service to process a request is a crucial indicator of potential problems.
The tracing lab showed that spans contain timestamps that measure the duration of an operation.
Traces allow us to analyze latency in a specific transaction.
However, in practice, we often want to monitor the overall latency for a given service.
While it is possible to compute this from span metadata, converting between telemetry signals is not very practical.
For example, since capturing traces for every request is resource-intensive, we might want to use sampling to reduce overhead.
Depending on the strategy, sampling may increase the probability that outlier events are missed.
Therefore, we typically analyze the latency via a Histogram.
Histograms are ideal for this because they represent a frequency distribution across many requests.
They allow us to divide a set of data points into percentage-based segments, commonly known as percentiles.
For example, the 95th percentile latency (P95) represents the value below which 95% of response times fall.
A significant gap between P50 and higher percentiles suggests that a small percentage of requests experience longer delays.
A major challenge is that there is no unified definition of how to measure latency.
We could measure the time a service spends processing application code, the time it takes to get a response from a remote service, and so on.
To interpret measurements correctly, it is vital to have information on what was measured.


```py { title="metric_utils.py" }
def create_request_instruments(meter: metrics.Meter) -> dict:
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
```

```py { title="app.py" }
@app.before_request
def before_request_func():
    request.environ["request_start"] = time.time_ns()

@app.after_request
def after_request_func(response: Response) -> Response:
    request_end = time.time_ns()
    duration = (request_end - request.environ["request_start"]) / 1_000_000_000 # convert ns to s
    request_instruments["http.server.request.duration"].record(
        duration,
        attributes = {
            "http.request.method": request.method,
            "http.route": request.path,
            "http.response.status_code": response.status_code
        }
    )
    return response
```

Let's use the meter to create a Histogram instrument.
Refer to the semantic conventions for [HTTP Metrics](https://opentelemetry.io/docs/specs/semconv/http/http-metrics/) for an instrument name and preferred unit of measurement.
To measure the time it took to serve the request, we'll use our `before_request_func` and `after_request_func` functions.
In `before_request_func`, create a timestamp for the start of the request and add it to the request context.
In `after_request_func`, take a timestamp for the end of the request and subtract them to calculate the duration.
We often need additional context to draw the right conclusions.
For example, a service's latency number might indicate fast replies.
However, in reality, the service might be fast because it serves errors instead of replies.
Therefore, let's add some additional attributes.

```sh
python app.py | tail -n +3 | jq '.resource_metrics[].scope_metrics[].metrics[] | select (.name=="http.server.request.duration")'
```
```json
{
"name": "http.server.request.duration",
"description": "latency for a request to be served",
"unit": "s",
"data": {
    "data_points": [
        {
            "attributes": {
                "http.request.method": "POST",
                "http.route": "/",
                "http.response.status_code": 200
            },
            "start_time_unix_nano": 1705931460176920755,
            "time_unix_nano": 1705931462026489226,
            "count": 1,
            "sum": 0.107024987,
            "bucket_counts": [0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0], // <- frequency
            "explicit_bounds": [ 0.0, 5.0, 10.0, 25.0, 50.0, 75.0, 100.0, 250.0, 500.0, 750.0, 1000.0, 2500.0, 5000.0, 7500.0, 10000.0 ], // <- buckets
            "min": 0.107024987,
            "max": 0.107024987
        }
    ],
    "aggregation_temporality": 2
}
```

To test if everything works, run the app, use curl to send a request, and locate the Histogram instrument in the output.
You should see that the request was associated with a bucket.
Note that conventions recommend seconds as the unit of measurement for the request duration.
We expect a majority of requests to be served in milliseconds.
Therefore, the default bucket bounds defined by the Histogram aren't a good fit.
We'll address this later, so ignore this for now.

#### saturation
All the previous metrics have been request-oriented.
For completeness, we'll also capture some resource-oriented metrics.
According to Google's SRE book, the fourth golden signal is called "[saturation](https://sre.google/sre-book/monitoring-distributed-systems/#saturation)".
Unfortunately, the terminology is not well-defined.
Brendan Gregg, a renowned expert in the field, defines saturation as the amount of work that a resource is unable to service.
In other words, saturation is a backlog of unprocessed work.
An example of a saturation metric would be the length of a queue.
In contrast, utilization refers to the average time that a resource was busy servicing work.
We usually measure utilization as a percentage over time.
For example, 100% utilization means no more work can be accepted.
If we go strictly by definition, both terms refer to separate concepts.
One can lead to the other, but it doesn't have to.
It would be perfectly possible for a resource to experience high utilization without any saturation.
However, Google's definition of saturation, confusingly, resembles utilization.
Let's put the matter of terminology aside.

```py { title="metric_utils.py" }
import psutil

# callbacks for asynchronous instruments
def get_cpu_utilization(opt: metric_api.CallbackOptions) -> metric_api.Observation:
    cpu_util = psutil.cpu_percent(interval=None) / 100
    yield metric_api.Observation(cpu_util)

def create_resource_instruments(meter: metric_api.Meter) -> dict:
    cpu_util_gauge = meter.create_observable_gauge(
        name="process.cpu.utilization",
        callbacks=[get_cpu_utilization],
        unit="1",
        description="CPU utilization since last call",
    )

    instruments = {
        "cpu_utilization": cpu_util_gauge
    }
    return instruments
```

Let's measure some resource utilization metrics.
To keep things simple, we already installed the `psutil` library for you.
Create a function `create_resource_instruments` to obtain instruments related to resources.
Use the meter to create an `ObservableGauge` to track the current CPU utilization and an `ObservableUpDownCounter` to record the memory usage.
Since both are asynchronous instruments, we also define two callback functions that are called on demand and return an Observation.

```py { title="app.py" }
import logging
from metric_utils import create_meter, create_request_instruments, create_resource_instruments

if __name__ == "__main__":
    # disable logs of builtin webserver for load test
    logging.getLogger("werkzeug").disabled = True

    # instrumentation
    request_instruments = create_request_instruments(meter)
    create_resource_instruments(meter)

    # launch app
    db = ChaosClient(client=FakerClient())
    app.run(host="0.0.0.0", debug=True)
```
```sh { title="shell" }
# start app and filter output
python app.py | tail -n +3 | jq '.resource_metrics[].scope_metrics[].metrics[] | select (.name=="process.cpu.utilization")'

# apache bench
ab -n 50000 -c 100 http://localhost:5000/users
```

Open `app.py`, import `create_resource_instruments`, and call it inside the main section.
With our golden signals in place, let's use load-testing tools to simulate a workload.
We'll use [ApacheBench](https://httpd.apache.org/docs/2.4/programs/ab.html), which is a single-threaded CLI tool for benchmarking HTTP web servers.
Flask's `app.run()` starts a built-in web server that is meant for development purposes.
Running a stress test and logging requests would render the console useless.
To observe the output of the ConsoleMetricExporter, add a statement to disable the logger.
Start the application, run the `ab` command to apply the load, and examine the metrics rendered to the terminal.

### Views

```py { title="metric_utils.py" }
from opentelemetry.sdk.metrics.view import (
    View,
    DropAggregation,
    ExplicitBucketHistogramAggregation,
)

def create_views() -> list[View]:
    views = []
    # ...
    return views

def create_meter(name: str, version: str) -> metrics.Meter:
    views = create_views()
    provider = MeterProvider(
        metric_readers=[metric_reader],
        resource=rc,
        views=views # <-- register views
    )
```

So far, we have seen how to generate some metrics.
Views let us customize how metrics are collected and output by the SDK.
<!-- why don't we right in the first place? -->
Before we begin, create a new function `create_views`.
To register views, we pass them to the `MeterProvider`.
The definition of a View consists of two parts.
First, we must *match* the instrument(s) that we want to modify.
The [`View`](https://opentelemetry-python.readthedocs.io/en/latest/sdk/metrics.view.html#opentelemetry.sdk.metrics.view.View) constructor provides many criteria to specify a selection.
Nothing prevents us from defining Views that apply to multiple instruments.
Second, we must instruct the View on *how to modify* the metrics stream.
Let's look at some examples to illustrate why Views can be useful.

```py { title="metric_utils.py" }
# adjust aggregation of an instrument
histrogram_explicit_buckets = View(
    instrument_type=Histogram,
    instrument_name="*",  #  wildcard pattern matching
    aggregation=ExplicitBucketHistogramAggregation((0.005, 0.01, 0.025, 0.05, 0.075, 0.1, 0.25, 0.5, 0.75, 1, 2.5, 5, 7.5, 10)) # <-- define buckets
)
views.append(histrogram_explicit_buckets)
```
```bash
python app.py | tail -n +3 | jq '.resource_metrics[].scope_metrics[].metrics[] | select (.name=="http.server.request.duration")'
```
Every instrument type has a default method for aggregating a metric based on incoming measurements.
For instance, a Counter performs a `SumAggregation`, while a Gauge defaults to `LastValueAggregation`.
Previously, semantic conventions recommend to measure request latency in seconds.
However, this meant that measurements didn't align with the default bucket boundaries defined by the Histogram.
The View's `aggregation` argument lets us customize how instruments aggregate metrics.
The example above illustrates how to change the bucket sizes for all histogram instruments.


```py { title="metric_utils.py" }
# change what attributes to report
traffic_volume_drop_attributes = View(
    instrument_type=Counter,
    instrument_name="traffic_volume",
    attribute_keys={}, # <-- drop all attributes
)
views.append(traffic_volume_drop_attributes)

# change name of an instrument
traffic_volume_change_name = View(
    instrument_type=Counter,
    instrument_name="traffic_volume",
    name="test", # <-- change name
)
views.append(traffic_volume_change_name)

# drop entire intrument
drop_instrument = View(
    instrument_type=ObservableGauge,
    instrument_name="process.cpu.utilization",
    aggregation=DropAggregation(), # <-- drop measurements
)
views.append(drop_instrument)
```

However, Views are much more powerful than changing an instrument's aggregation.
For example, we can use `attribute_keys` to specify a white list of attributes to report.
An operator might want to drop metric dimensions because they are deemed unimportant, to reduce memory usage and storage, prevent leaking sensitive information, and so on.
If we pass an empty set, the SDK should no longer report separate counters for the URL paths.
Moreover, a View's `name` parameter to rename a matched instrument.
This could be used to ensure that generated metrics align with OpenTelemetry's semantic conventions.
Moreover, Views allow us to filter what instruments should be processed.
If we pass `DropAggregation`, the SDK will ignore all measurements from the matched instruments.
You have now seen some basic examples of how Views let us match instruments and customize the metrics stream.
Feel free to add these code snippets to `create_views` and observe the changes in the output.

<!--
## quiz

{{< quizdown >}}

### OpenTelemetry's `MetricReader` is responsible for collecting metrics data from the SDK and exporting it to a backend system for storage and analysis.
- [ ] true
> This is true. The `MetricReader` in OpenTelemetry is an interface that defines how to read metrics from the SDK. It is responsible for collecting metrics data from the SDK and exporting it to a backend system for storage and analysis.
- [ ] false

### The `ConsoleMetricExporter` in OpenTelemetry writes a JSON representation of metrics generated by the SDK to stdout.
- [ ] true
> This is true. The `ConsoleMetricExporter` is a simple exporter that writes a JSON representation of the metrics generated by the SDK to the standard output (stdout).
- [ ] false

### What is the primary design goal of the metric signal in OpenTelemetry?
- [ ] A. To work with existing standards
> The correct answer is A. One of the primary design goals of the metric signal in OpenTelemetry is to work with existing standards, such as Prometheus and StatsD, to ensure compatibility and interoperability with existing monitoring systems.
- [ ] B. To provide a unified metrics collection system
- [ ] C. To replace existing monitoring tools
- [ ] D. To introduce new metrics collection methods

### Which of the following is a correct way to measure the number of requests to a web server using OpenTelemetry?
- [ ] A. Use a `Gauge` instrument to track the number of requests
- [ ] B. Use a `Counter` instrument to track the number of requests
> The correct answer is B. A `Counter` instrument is used to track the number of occurrences of an event, such as the total number of requests handled by a server.
- [ ] C. Use a `Histogram` instrument to track the number of requests
- [ ] D. Use an `UpAndDownCounter` instrument to track the number of requests

{{< /quizdown >}}
-->

<!--
### push and pull-based exporter

So far, we have seen how the ConsoleMetricsExporter can be a useful tool when debugging output generated by the SDK.
In reality, we typically want to export metrics to a backend for storage and analysis.
As previously mentioned, OpenTelemetry defines and implements OTLP, a protocol to encode and exchange telemetry.
Due to OpenTelemetry's prominence, OTLP is supported by many backends.
However, a primary design goal of the metric signal is to work with existing standards.
Today, Prometheus and StatsD are the two most common players in this space.
Aside from their different instrumentation standards, they also follow a different philosophy on how metrics are exported.
For instance, StatsD uses a [push-based approach](https://opentelemetry.io/docs/specs/otel/metrics/sdk/#push-metric-exporter).
Similar to ConsoleMetricsExporter, we can pair an exporter with a periodic exporting MetricReader to flush a batch of metrics based on a user-defined interval.
In contrast, Prometheus uses a [pull-based](https://opentelemetry.io/docs/specs/otel/metrics/sdk/#pull-metric-exporter) pattern.
We passively expose metrics as an HTTP endpoint.
The Prometheus server can discover these targets and actively scrapes them.

In a production scenario, we will probably use a Collector to collect, process, and forward telemetry to a backend.
- more versatile
- separates concerns beyond instrumentation from the operational aspects
We will cover this in a dedicated chapter.
Here, we will quickly demonstrate that it is also possible to send telemetry directly to a backend using just the SDK.

```sh
pip install prometheus-client
pip install opentelemetry-exporter-prometheus
```

We begin by installing the Prometheus Client library for our language of choice.
OpenTelemetry's API and SDK packages do not include a Prometheus Exporter.
Therefore, we must install the official exporter library, which you find [here](https://github.com/open-telemetry/opentelemetry-python/tree/main/exporter).

```py { title="metric_utils.py" }
from opentelemetry.exporter.prometheus import PrometheusMetricReader
from prometheus_client import start_http_server

def create_prometheus_reader(http_server_port: int = 8000) -> MetricReader:
    start_http_server(port=http_server_port, addr="localhost")
    reader = PrometheusMetricReader(prefix="MetricExample")
    return reader

def create_meter(name: str, version: str) -> metrics.Meter:
    # ...
    prom_reader = create_prometheus_reader(8000)
    provider = MeterProvider(
        metric_readers=[console_reader, prom_reader], resource=rc, views=views
    )
```

Next, specify the necessary imports.
Prometheus Client library provides a function `start_http_server`, which starts a separate thread with an HTTP server.
It serves the metrics endpoint, responding to scrape requests.
The `PrometheusMetricReader` combines the role of MetricReader and Exporter in a single entity.
- pulls metrics from the SDK

Let's create a function `create_prometheus_reader` to start the server and configure the MetricReader.
Then, pass the return value to our MetricProvider.

```py { title="metric_utils.py" }
if __name__ == "__main__":
    # ...
    app.run(host="0.0.0.0", debug=False)
```
If you try to launch the application, you'll notice it crashes.
The reason is that in Flask's debug mode the code is reloaded after the flask server is up.
As a result, the Prometheus server tries to bind to a port a second time, which the OS prevents.
To fix this, set the debug parameter to False.
Start the app and open [localhost:8000](localhost:8000) in your browser.
You should now see the metrics being exported in Prometheus text-based exposition format.
-->
