---
title: "traces"
date: 2023-12-06T09:43:24+01:00
draft: false
weight: 2
---

### How to perform the exercise
* You need to either start the [repository](https://github.com/JenSeReal/otel-getting-started/) with Codespaces, Gitpod or clone the repository with git and run it locally with dev containers or docker compose
* Initial directory: `labs/manual-instrumentation-traces/initial`
* Solution directory: `labs/manual-instrumentation-traces/solution`
* Source code: `labs/manual-instrumentation-traces/initial/src`
* How to run the application either:
  * Run the task for the application: `Run manual-instrumentation-traces initial application` (runs the Python application)
  * Run the application with Terminal commands `python3 src/app.py` (runs the Python application)

---

Regardless of your setup, open two separate terminals with a shell.
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

### Traces in OpenTelemetry
To generate traces in OpenTelemetry a few steps are required. With the SDK we can create a `TracingProvider` that implements the tracing API. For developers this is the central entrypoint to tracing with OpenTelemetry. The provider takes processors and ressources and saves a configuration for the tracer that is used in the source code. When generation traces the tracing pipeline that was defines sends the spans to the configured tracing backend. The simplest backend is the export to the console. But you can also define backends like Jaeger. The tracing pipeline consists of a processor and an exporter. We will use the `BatchSpanProcessor` to process spans in batches and `ConsoleSpanExporter` to export spans to the console.


{{< figure src="images/create_tracer_configure_pipeline.drawio.png" width=650 caption="Overview of the tracing signal" >}}

### configure tracing pipeline and obtain a tracer


```py { title="trace_utils.py" }
# OTel SDK
from opentelemetry.sdk.trace.export import ConsoleSpanExporter, BatchSpanProcessor

def create_tracing_pipeline() -> BatchSpanProcessor:
    console_exporter = ConsoleSpanExporter()
    span_processor = BatchSpanProcessor(console_exporter)
    return span_processor
```

Inside the `src` directory, create a new file `trace_utils.py` with the code displayed above.
We'll use it to separate tracing-related configuration from the main application.
At the top of the file, specify the imports as shown above.
Create a new function `create_tracing_pipeline`.
We typically want to send spans to a tracing backend for analysis.
For debugging purposes, instantiate a `ConsoleSpanExporter` to write spans straight to the console.
Next, let's create a SpanProcessor that sits at end of our pipeline.
Its main responsibility is to push spans to one (or more) SpanExporter(s).
There are different ways to achieve this:
- synchronous
    - blocks the program execution to forward spans as soon as they are generated.
    - means that tracing instrumentation adds additional latency to requests
- asynchronous
    - application starts and closes spans, but immediately resumes execution.
    - via a [`BatchSpanProcessor`](https://opentelemetry-python.readthedocs.io/en/latest/sdk/trace.export.html#opentelemetry.sdk.trace.export.BatchSpanProcessor)
        - maintains a buffer for completed spans
        - a separate thread is launched that flushes batches of spans to exporters at regular intervals (or when a threshold is met)
        - has performance advantages, but spans might be dropped if the application crashes before spans are exported / when the buffer capacity is met

```py { title="trace_utils.py" }
# OTel API
from opentelemetry import trace as trace_api

# OTel SDK
from opentelemetry.sdk.trace import TracerProvider

def create_tracer(name: str, version: str) -> trace_api.Tracer:
    provider = TracerProvider()
    provider.add_span_processor(create_tracing_pipeline())
    trace_api.set_tracer_provider(provider)
    tracer = trace_api.get_tracer(name, version)
    return tracer
```

Let's begin by importing OpenTelemetry's tracing API and the `TracerProvider` from the SDK.
Then, create a function `create_tracer`.
Instantiate a `TracerProvider`.
To connect the provider to the tracing pipeline, we call `add_span_processor` and pass the `BatchSpanProcessor`.
By default, the OpenTelemetry API calls a no-op implementation.
The API provides a `set_tracer_provider` function to register a `TracerProvider` and call the tracing SDK instead.
Finally, call `get_tracer` to obtain a [`Tracer`](https://opentelemetry-python.readthedocs.io/en/latest/api/trace.html#opentelemetry.trace.Tracer) from the tracing provider.
The tracer is what we'll use to generate spans.
We'll also pass the service `name` and `version` as parameters, to uniquely identify the instrumentation.

```py { title="app.py" }
from trace_utils import create_tracer

# global variables
app = Flask(__name__)
tracer = create_tracer("app.py", "0.1")
```

In `app.py` import the `create_tracer` function and assign the return value to a global variable called `tracer`.
Run `python app.py` to verify that there are no errors.

### generate trace data

{{< figure src="images/tracer_generates_spans.drawio.png" width=650 caption="tracing signal" >}}

```py { title="app.py" }
@app.route("/")
@tracer.start_as_current_span("index")
def index():
    # ...
```

With the help of a tracer, let's generate our first piece of telemetry.
On a high level, we must add instrumentation to our code that creates and finishes spans.
OpenTelemetry's Python implementation provides multiple ways to do this.
Some aim to be simple and non-intrusive, while others are explicit but offer greater control.
For brevity, we'll stick to a decorator-based approach.
Add the `start_as_current_span` decorator to the `index` function.
Notice that this decorator is a convenience function that abstracts away the details of trace context management from us.
It handles the creation of a new Span object, attaches it to the current context, and ends the span once the method returns.

```json
{
    "name": "index",
    "context": {
        "trace_id": "0x64d6aa00b229557023afb032160c9237",
        "span_id": "0xbff3f93fb12ff4ac",
        "trace_state": "[]"
    },
    "kind": "SpanKind.INTERNAL",
    "parent_id": null,
    "start_time": "2024-01-18T15:32:20.321307Z",
    "end_time": "2024-01-18T15:32:20.428228Z",
    "attributes": {},
    "resource": {
        "attributes": {
            "telemetry.sdk.language": "python",
            "telemetry.sdk.name": "opentelemetry",
            "telemetry.sdk.version": "1.24.0",
            "service.name": "unknown_service"
        },
    }
}
```
Save the file and run `python app.py` to start the web server on port 5000.
Open a second terminal in the container.
Curl the `/` endpoint via `curl -XGET localhost:5000; echo`.
This causes the tracer to generate a span object, for which the tracing pipeline writes a JSON representation to the terminal.
Take a look at the terminal where you application is running.
You should see an output similar to the one shown above.

A span in OpenTelemetry represents a single operation within a trace and carries a wealth of information that provides insight into the operation's execution. This includes the `name` of the span, which is a human-readable string that describes the operation. The trace context, consisting of the `trace_id`, `span_id`, and `trace_state`, uniquely identifies the span within the trace and carries system-specific configuration data. The `SpanKind` indicates the role of the span, such as whether it's an internal operation, a server-side operation, or a client-side operation. If the `parent_id` is `null`, it signifies that the span is the root of a new trace. The `start_time` and `end_time` timestamps mark the beginning and end of the span's duration. Additionally, spans can contain `attributes` that provide further context, such as HTTP methods or response status codes, and a `resource` field that describes the service and environment. Other fields like `events`, `links`, and `status` offer additional details about the span's lifecycle, outcome and context.

### enrich spans with context
{{< figure src="images/enrich_spans_with_context.drawio.png" width=650 caption="enriching spans with resources and attributes" >}}

So far, the contents of the span were automatically generated by the SDK.
This information is enough to reason about the chain of events in a transaction and allows us to measure latency.
However, it's important to understand that tracing is a much more potent tool.
To extract meaningful insights into what is happening in a system, our traces need additional context.

#### resource
A [Resource](https://opentelemetry.io/docs/specs/otel/resource/sdk/) is a set of static attributes that help us identify the source (and location) that captured a piece of telemetry.
Right now, the span's `resource` field only contains basic information about the SDK itself, as well as an unknown `service.name`.
Let's fix that.

```py { title="resource_utils.py" }
from opentelemetry.sdk.resources import Resource

def create_resource(name: str, version: str) -> Resource:
    svc_resource = Resource.create(
        {
            "service.name": name,
            "service.version": version,
        }
    )
    return svc_resource
```
```py { title="trace_utils.py" }
from resource_utils import create_resource

def create_tracer(name: str, version: str) -> trace_api.Tracer:
    # create provider
    provider = TracerProvider(
        resource=create_resource(name, version)
    )
    provider.add_span_processor(create_tracing_pipeline())
```

Create a new file `resource_utils.py`.
Specify the imports and create a function `create_resource`, which returns a [`Resource`](https://opentelemetry-python.readthedocs.io/en/latest/sdk/resources.html#opentelemetry.sdk.resources.Resource) object.
By separating the resource and tracing configuration, we can easily reuse it with other telemetry signals.
Inside `create_tracer`, we pass the value returned by `create_resource` to the `TraceProvider`.

```json
"resource": {
    "attributes": {
        "telemetry.sdk.language": "python",
        "telemetry.sdk.name": "opentelemetry",
        "telemetry.sdk.version": "1.24.0",
        "service.name": "app.py",
        "service.version": "0.1"
    },
    "schema_url": ""
}
```

Let's verify that everything works as expected.
Restart the web server and send a request using the previous curl command.
If you look at the span exported to the terminal, you should now see that the resource attached to telemetry contains context about the service.
This is just an example to illustrate how resources can be used to describe the environment an application is running in.
Other resource attributes may include information to identify the physical host, virtual machine, container instance, operating system, deployment platform, cloud provider, and more.

#### semantic conventions

> There are only two hard things in Computer Science: cache invalidation and naming things.
> -- Phil Karlton

Consistency is a hallmark of high-quality telemetry.
Looking at the previous example, nothing prevents a developer from using arbitrary keys for resource attributes (e.g. `version` instead of `service.version`).
While human intuition allows us to conclude that both refer to the same thing, machines are (luckily) not that smart.
Inconsistencies in the definition of telemetry make it harder to analyze the data down the line.
This is why OpenTelemetry's specification includes [semantic conventions](https://opentelemetry.io/docs/specs/semconv/).
This standardization effort helps to improve consistency, prevents us from making typos, and avoids ambiguity due to differences in spelling.
OpenTelemetry provides a Python package `opentelemetry-semantic-conventions` that was already installed as a dependency.

```py { title="resource_utils.py" }
from opentelemetry.semconv.resource import ResourceAttributes

def create_resource(name: str, version: str) -> Resource:
    svc_rc = Resource.create(
        {
            ResourceAttributes.SERVICE_NAME: name,
            ResourceAttributes.SERVICE_VERSION: version,
        }
    )
    return svc_rc
```

Let's refactor our code.
Start by specifying imports at the top of the file.
Explore the [documentation](https://opentelemetry.io/docs/specs/semconv) to look for a convention that matches the metadata we want to record.
The specification defines conventions for various aspects of a telemetry system (e.g. different telemetry signals, runtime environments, etc.).
Due to the challenges of standardization and OpenTelemetry's strong commitment to long-term API stability, many conventions are still marked as experimental.
For now, we'll use [this](https://opentelemetry.io/docs/specs/semconv/resource/#service) as an example.
Instead of specifying the attributes keys (and sometimes values) by typing their string by hand, we reference objects provided by OpenTelemetry's `semconv` package.

#### SpanAttributes

Aside from static resources, spans can also carry dynamic context through `SpanAttributes`.
Thereby, we can attach information specific to the current operation to spans.

```py { title="app.py" }
from flask import Flask, make_response, request

from opentelemetry import trace as trace_api
from opentelemetry.semconv.trace import SpanAttributes

@app.route("/")
@tracer.start_as_current_span("index")
def index():
    span = trace_api.get_current_span()
    span.set_attributes(
        {
            SpanAttributes.HTTP_REQUEST_METHOD: request.method,
            SpanAttributes.URL_PATH: request.path,
            SpanAttributes.HTTP_RESPONSE_STATUS_CODE: 200,
        }
    )
```

```json
"attributes": {
    "http.request.method": "GET",
    "url.path": "/",
    "http.response.status_code": 200
}
```

To illustrate this, let's add the request method, path and response code to a span.
Call `get_current_span` to retrieve the active span.
Then, we can use `set_attributes` to pass a dictionary of key / value pairs.
The semantic conventions related to [tracing](https://opentelemetry.io/docs/specs/semconv/general/trace/) standardize many attributes related to the [HTTP protocol](https://opentelemetry.io/docs/specs/semconv/http/http-spans/).
To access information like request method, headers, and more, we import `request` from the flask package.
Flask creates a [request context](https://flask.palletsprojects.com/en/2.3.x/reqcontext/) for each incoming HTTP request and tears it down after the request is processed.

### context propagation

{{< figure src="images/trace_propagation.drawio.png" width=650 caption="trace context propagation" >}}

Until now, we only looked at the contents of a singular span.
However, at its core, distributed tracing is about understanding the chain of operations that make up a transaction.
To construct a trace, we need to tie individual spans together.
To do that, we must pass contextual metadata between adjacent operations, which are separated by a logical boundary.
OpenTelemetry calls this information `SpanContext`.
The boundary could be in-process or a network.
As previously seen, the tracing SDK automatically generates a `SpanContext` for us.

```py { title="app.py" }
@tracer.start_as_current_span("do_stuff")
def do_stuff():
    # ...
```

```json
{
    "name": "do_stuff",
    "context": {
        "trace_id": "0xbd57d76b8a29e9ff6ee1b016cf3a6d8b",
        "span_id": "0x7c6f99cad316bafe",
        "trace_state": "[]"
    },
    "parent_id": "0xed291d97f9c00a5a",
}
```
```json
{
    "name": "index",
    "context": {
        "trace_id": "0xbd57d76b8a29e9ff6ee1b016cf3a6d8b",
        "span_id": "0xed291d97f9c00a5a",
        "trace_state": "[]"
    },
    "parent_id": null,
}
```
Let's investigate the current behavior of the application.
In `app.py` add a decorator to the `do_stuff` function to create a new span.
Curl the `/` endpoint and look at the output.
You should now see two span objects being generated.
If we compare the `index` and `do_stuff` span we see that:
- both share the same `trace_id`.
- the `parent_id` of the `do_stuff` span matches the `span_id` of the `index` span

In other words, `do_stuff` is a child span of `index`.
The tracing SDK automagically handles context propagation within the same process.
Let's see what happens when there is a network boundary in between.

#### inject trace context into outgoing requests

```py { title="app.py" }
@tracer.start_as_current_span("do_stuff")
def do_stuff():
    time.sleep(0.1)
    url = "http://httpbin:80/anything"
    response = requests.get(url)

    # debug
    print("response from httpbin:")
    print(response.text)
```

For testing purposes, the lab environment includes a `httpbin` server that echos the HTTP requests back to the client.
This is useful because it lets us examine what requests leaving our application look like.
`do_stuff` uses the `requests` package to send a get request to `httpbin`.
To examine what happens, let's add some `print` statements.

```json
"headers": {
    "Accept": "*/*",
    "Accept-Encoding": "gzip, deflate",
    "Connection": "keep-alive",
    "Host": "httpbin",
    "User-Agent": "python-requests/2.31.0"
},
```

After restarting the webserver and using curl to send a request to the `/` endpoint, you should see the output from our print statements.
The request didn't include any tracing headers!
If we do not inject SpanContext into outgoing requests, remote services have no information that the incoming request is part of an ongoing trace.
Therefore, the tracing instrumentation decides to start a new trace by generating a new, but disconnected, `SpanContext`.
The context propagation across services is currently broken.

```py { title="app.py" }
from opentelemetry.propagate import inject

@tracer.start_as_current_span("do_stuff")
def do_stuff():
    headers = {}
    inject(headers)

    time.sleep(.1)
    url = "http://httpbin:80/anything"
    response = requests.get(url, headers=headers) # <- inject trace context into headers
```

OpenTelemetry's data transmission system includes the concept of [propagators](https://opentelemetry-python.readthedocs.io/en/latest/api/propagate.html).
Propagators serialize context, so it can traverse the network along with the request.
Let's import the [`inject`](https://opentelemetry-python.readthedocs.io/en/latest/api/propagate.html#opentelemetry.propagate.inject) function from OpenTelemetry's `propagate` API.
Create an empty dictionary and `inject` the headers from the current context into it.
All we have to do now is to include this context in outgoing requests.
The requests `get` method has a second argument that allows us to pass in information that will be send as request headers.

```json
"headers": {
    "Accept": "*/*",
    "Accept-Encoding": "gzip, deflate",
    "Connection": "keep-alive",
    "Host": "httpbin",
    "Traceparent": "00-b9041f02352d1558fcdd9ea3804da1f0-aa8b34aa3f883298-01",
    "User-Agent": "python-requests/2.31.0"
},
```

Let's re-run our previous experiment with `httpbin` and locate the output of our print statement.
You should now see that the outgoing request contains a header called `traceparent`.
If you have prior experience with distributed tracing, you might already know that different tracing systems use different formats (such as [b3 propagation](https://github.com/openzipkin/b3-propagation) used by Zipkin).

{{< figure src="images/doordash_traceparent_header.png" width=650 caption="format used by the traceparent header [Gud21](https://doordash.engineering/2021/06/17/leveraging-opentelemetry-for-custom-context-propagation/)" >}}

By default, OpenTelemetry uses a [specification](https://www.w3.org/TR/trace-context/) established by the World Wide Web Consortium (W3C).
Looking at the header's value, we can infer that it encodes the trace context as `<spec version>-<trace_id>-<parent_id>-<trace flag>`.

#### continue an existing trace

```py { title="app.py" }
@app.route("/users", methods=["GET"])
@tracer.start_as_current_span("users")
def get_user():
    # ...
```
```sh
curl -XGET "localhost:5000/users" --header "traceparent: 00-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa-00ffffffffffffff-01"
```

Let's switch hats.
Image that our app is a service that we send a request to.
In this scenario, the remote service must recognize that the incoming request is part of an ongoing trace.
To simulate this, let's add the `start_as_current_span` decorator to the `/users` endpoint.
We'll use curl shown above to construct a request with a fictional tracing header, which is then send to application.
Let's examine the generated spans.
The output reveals that the tracer created a span with a random `trace_id` and no `parent_id`.

```py { title="app.py" }
from opentelemetry import context
from opentelemetry.propagate import inject, extract

@app.before_request
def before_request_func():
    ctx = extract(request.headers)
    previous_ctx = context.attach(ctx)
    request.environ["previous_ctx_token"] = previous_ctx
```

This is not what we want!
Recall that `start_as_current_span` instantiates a span based on the current context of the tracer.
We saw that outgoing request included tracing headers.
Apparently, the local context lacks this information.
Flask provides a special `before_request` decorator to execute a function before the normal request handler runs.
Create a function `before_request_func`.
We'll use it to create a custom context, which subsequently gets picked up by `start_as_current_span`.
Import [`extract`](https://opentelemetry-python.readthedocs.io/en/latest/api/propagate.html#opentelemetry.propagate.extract) function from `propagate`.
Construct a new Context object by passing the request headers as an argument.
Call [`attach`](https://opentelemetry-python.readthedocs.io/en/latest/api/context.html#opentelemetry.context.attach) to set a new active context.
The function returns a string token that references the context before we call attach.
Store it on Flask's request context, so we can retrieve it later.

```python
@app.teardown_request
def teardown_request_func(err):
    previous_ctx = request.environ.get("previous_ctx_token", None)
    if previous_ctx:
        context.detach(previous_ctx)
```

Flask's `teardown_request` request decorator allows us to register a callback function that gets executed at the end of each request.
We'll use it to restore the previous context.
Retrieve the token from Flask's request context and pass it as an argument to [`detach`](https://opentelemetry-python.readthedocs.io/en/latest/api/context.html#opentelemetry.context.detach).

```json
{
    "name": "users",
    "context": {
        "trace_id": "0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        "span_id": "0x48a97a9fd5588724",
        "trace_state": "[]"
    },
    "parent_id": "0x00ffffffffffffff"
}
```

The service should recognize the tracing header of the incoming request and create spans related to the transaction.
Let's verify our work
Restart the webserver and use the previous curl command to sends a request with a fictional trace context to `/users`.
Take a look at the span generated by the application.
Finally!
The context propagation is working as expected!
If we were to export spans to a tracing backend, it could analyze the SpanContext of the individual objects and piece together a distributed trace.

<!--
## quiz

{{< quizdown >}}

---
primary_color: orange
secondary_color: lightgray
text_color: black
shuffle_questions: false
---

### The OpenTelemetry specification includes semantic conventions to standardize the naming of telemetry attributes.

- [ ] True
> True. OpenTelemetry's semantic conventions provide a standardized way to name and describe telemetry attributes, which helps improve consistency and interoperability across different telemetry systems.
- [ ] False


### Which of the following is NOT a standard HTTP attribute in OpenTelemetry's semantic conventions?

- [ ] http.request.method
- [ ] http.response.status_code
- [ ] http.request.url
- [ ] http.request.headers
> http.request.headers. While headers are an important part of HTTP requests, they are not standardized as a single attribute in OpenTelemetry's semantic conventions.

### The OpenTelemetry SDK provides a `BatchSpanProcessor` to improve performance by batching span exports.

- [ ] True
> True. The `BatchSpanProcessor` in the OpenTelemetry SDK is designed to improve performance by buffering spans and exporting them in batches, reducing the overhead of individual span exports.
- [ ] False

### Which of the following is a common use case for distributed tracing?

- [ ] Monitoring the performance of a single service
- [ ] Tracking the flow of data through a system
> Tracking the flow of data through a system. Distributed tracing is particularly useful for understanding the behavior of a system across multiple services and components, which helps in diagnosing issues and optimizing performance.
- [ ] Ensuring that all services are running the same version of a library
- [ ] Managing the configuration of a distributed system

{{< /quizdown >}}

## optional tasks

#### ResourceDetectors

```py { title="resource_utils.py" }
import socket
from opentelemetry.sdk.resources import Resource, ResourceDetector
from opentelemetry.semconv.resource import ResourceAttributes

class HostDetector(ResourceDetector):
    def detect(self) -> Resource:
        return Resource.create(
            {
                ResourceAttributes.HOST_NAME: socket.gethostname(),
            }
        )

def create_resource(name: str, version: str) -> Resource:
    # ...
    host_rc = HostDetector().detect()
    rc = host_rc.merge(svc_rc)
    return rc
```


A `ResourceDetector` is a way to bundle and automate the discovery and extraction of such information in a plugin.
This makes it trivial to share it across different telemetry signals and services.
To implement it simply inherit from the `ResourceDetector` class and override the `detect` method.
Finally, we simply call the `merge` method on the `Resource` object, which combines both and returns a new object.

-->