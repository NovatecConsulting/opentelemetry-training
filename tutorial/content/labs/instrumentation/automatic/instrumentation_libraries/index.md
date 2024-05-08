---
title: "instrumentation libraries"
draft: false
weight: 2
---


### background
<!-- Instrumentation Library -->
The long-term vision of OpenTelemetry is that third-party libraries or frameworks ship with native OpenTelemetry instrumentation, providing out-of-the-box observability.
However, as of now, some projects do not yet have native support for the (partially still evolving) OpenTelemetry APIs.
To bridge this gap, (temporary) solutions have been developed in the form of *instrumentation libraries*.
The core idea is to increase the adoption of OpenTelemetry without placing the entire burden on the back of the library maintainers.
An instrumentation library is a standalone (separately installed) library, whose sole purpose is to inject OpenTelemetry API calls into another popular library or framework which currently lacks native integration.
There is no unified way to achieve this, because it highly depends on the mechanisms of provided by the programming language and the library.
Examples range from wrapping interfaces, monkey-patching code, registering callback on library-specific hooks to translating existing telemetry into OpenTelemetry.
The OpenTelemetry has a [registry](https://opentelemetry.io/ecosystem/registry/) which can be used to find instrumentation libraries. A brief look into the libraries reveals that there are already a lot of libraries for many frameworks and programming languages. But many of these libraries are not stable yet and are in an alpha or beta state. To find a library that fits your software stack, just visit the registry and search for the framework or programming language that you are using.

While instrumentation libraries offer a valuable solution for enhancing observability in third-party libraries or frameworks that lack native OpenTelemetry support, they also present certain challenges. These include the necessity to manage additional dependencies, which adds complexity to the codebase and requires careful consideration of maintenance overhead. Additionally, as instrumentation libraries are still relatively new compared to native integrations, they may face limitations such as less community support, fewer resources, and a higher risk of encountering issues due to their nascent nature.

### example
We want to follow the previous software stack and use Python flask to show how instrumentation libraries are used. To find an appropriate library we search the registry and find the `opentelemetry-flask-instrumentation` library. We can install the library using `pip` with the command `pip install opentelemetry-flask-instrumentation`. This package provides the necessary hooks to automatically instrument your Flask application with OpenTelemetry. Next, you need to configure OpenTelemetry to use the appropriate exporters and processors. This usually involves setting up an exporter to send telemetry data to a backend service like Jaeger, Zipkin, or another OpenTelemetry-compatible service, or in this case the OpenTelemetry collector. With the library installed and OpenTelemetry configured, you can now instrument your Flask application. This involves initializing the OpenTelemetry Flask instrumentation at the start of your application and ensuring that it wraps your Flask app instance. Finally, run your Flask application as you normally would. The instrumentation will automatically capture telemetry data from incoming requests, outgoing responses, and any exceptions that occur.

When using the `opentelemetry-flask-instrumentation` library with a Python Flask application, a span is automatically created for each incoming HTTP request. The span represents the execution of a single operation within the context of a trace, such as handling an HTTP request. To instrument Flask we need to wrap the flask application inside the `FlaskInstrumentor` which is provided by the pip package.

```python
import time

import requests
from client import ChaosClient, FakerClient
from flask import Flask, make_response
from opentelemetry import trace
from opentelemetry.instrumentation.flask import FlaskInstrumentor
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor, ConsoleSpanExporter

# global variables
app = Flask(__name__)

FlaskInstrumentor().instrument_app(app)
```

With this setup the Flask app is instrumented. Now we also need to define exporters to export the telemetry data to a collector or the console. For simplicity reasons we will export just traces to the console. To achieve this we need to import the `ConsoleSpanExporter` and `BatchSpanProcessor`, the `TracerProvider`, and the general `trace` import.

```python
import time

import requests
from client import ChaosClient, FakerClient
from flask import Flask, make_response
from opentelemetry import trace
from opentelemetry.instrumentation.flask import FlaskInstrumentor
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor, ConsoleSpanExporter

# Initialize the OpenTelemetry tracer
trace.set_tracer_provider(TracerProvider())

# Configure the OTLP exporter
otlp_exporter = ConsoleSpanExporter()

# Set up the BatchSpanProcessor with the exporter
span_processor = BatchSpanProcessor(otlp_exporter)
trace.get_tracer_provider().add_span_processor(span_processor)
```

Then the tracer provider need to be set, the exporter needs to be initialized, and we can attach the exporter to the `BatchSpanProcessor` and add the processor to the trace provider.

The final result should look similar to this:

```python
import time

import requests
from client import ChaosClient, FakerClient
from flask import Flask, make_response
from opentelemetry import trace
from opentelemetry.instrumentation.flask import FlaskInstrumentor
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor, ConsoleSpanExporter

# Initialize the OpenTelemetry tracer
trace.set_tracer_provider(TracerProvider())

# Configure the OTLP exporter
otlp_exporter = ConsoleSpanExporter()

# Set up the BatchSpanProcessor with the exporter
span_processor = BatchSpanProcessor(otlp_exporter)
trace.get_tracer_provider().add_span_processor(span_processor)

# global variables
app = Flask(__name__)

FlaskInstrumentor().instrument_app(app)

@app.route("/users", methods=["GET"])
def get_user():
    user, status = db.get_user(123)
    data = {}
    if user is not None:
        data = {"id": user.id, "name": user.name, "address": user.address}
    response = make_response(data, status)
    return response


def do_stuff():
    time.sleep(0.1)
    url = "http://httpbin:80/anything"
    _response = requests.get(url)


@app.route("/")
def index():
    do_stuff()
    current_time = time.strftime("%a, %d %b %Y %H:%M:%S", time.gmtime())
    return f"Hello, World! It's currently {current_time}"


if __name__ == "__main__":
    db = ChaosClient(client=FakerClient())
    app.run(host="0.0.0.0", debug=True)

```

In this example, when a user sends a request to the root path (`/`), the `FlaskInstrumentor` automatically creates a span for the HTTP request. This span captures details such as the request method (GET, POST, etc.), the request URL, the status code of the response, and any exceptions that occur during the processing of the request. The span also includes timing information, such as the start and end times, which help in understanding the latency of the request.

The span is then processed by the `BatchSpanProcessor`, which batches multiple spans together and sends them to the configured `OTLPSpanExporter`. The exporter serializes the span data and sends it to the OpenTelemetry Collector, which then forwards the data to a backend service like Jaeger or Zipkin for storage, analysis, and visualization.

By using the `opentelemetry-flask-instrumentation` library, you can automatically instrument your Flask application to create and export spans without writing additional code to manually create and manage spans. This simplifies the process of adding observability to your application and allows you to focus on the application's functionality.

A JSON representation of a span created by the `opentelemetry-flask-instrumentation` library would typically include various attributes that describe the span's operation, its relationship to other spans, and any additional metadata or events associated with the span. Here's an example of what a JSON representation of a span might look like:

```json
{
  "traceId": "0123456789abcdef0123456789abcdef",
  "spanId": "0123456789abcdef",
  "traceFlags": "01",
  "parentSpanId": "abcdef0123456789",
  "name": "GET /",
  "startTime": [162,  861645600],
  "endTime": [162,  861645601],
  "attributes": {
    "http.method": "GET",
    "http.url": "http://localhost:5000/",
    "http.status_code":  200,
    "http.user_agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X  10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.114 Safari/537.36",
    "custom_attribute": "custom_value"
  },
  "events": [
    {
      "time": [162,  861645600],
      "name": "exception",
      "attributes": {
        "exception.type": "ValueError",
        "exception.message": "Invalid value"
      }
    }
  ],
  "links": [],
  "status": {
    "code": "OK"
  }
}
```

The JSON representation of a span created by the `opentelemetry-flask-instrumentation` library encapsulates various attributes that describe the span's operation and its relationship to other spans. It includes the `traceId`, which is a unique identifier for the trace that the span is part of, and the `spanId`, which is the unique identifier for the span itself. The `traceFlags` field can be used to control the behavior of the trace, and the `parentSpanId` field indicates the identifier of the parent span, if any. The `name` field typically describes the operation being performed, such as handling an HTTP request. Timestamps `startTime` and `endTime` represent when the span started and ended, in nanoseconds since the Unix epoch, providing insights into the span's duration. The `attributes` field is a set of key-value pairs that offer additional context about the span, including HTTP method, URL, status code, and user agent. The `events` array contains events that occurred during the span's lifetime, like exceptions or other significant occurrences. The `links` array is used to establish relationships between spans, and the `status` object indicates the span's status, with a `code` that can represent success, failure, or other states. This structured data allows for detailed analysis and visualization of the span's performance and behavior, which is crucial for observability and troubleshooting in distributed systems.

Please note that the actual JSON representation of a span may vary depending on the specific exporter and backend service you are using. The above example is a generic representation and may need to be adapted to fit the specifics of your OpenTelemetry setup.

### development
Developing an instrumentation library is a significant undertaking and requires a deep understanding of distributed tracing concepts, the OpenTelemetry specification, and the programming language ecosystem you're working with. Implementing an instrumentation library involves several key steps and conventions to ensure consistent behavior of libraries. The advantages on the other hand are several advantages of offering an instrumentation library for a framework. The biggest advantage is that a library enhances observability and improves the developer experience. Instead of libraries needing to create and document custom logging hooks, they can utilize the standardized and user-friendly APIs provided by OpenTelemetry, simplifying the interaction for users. This also ensures that traces, logs, and metrics from both the library and application code are interconnected and consistent. The use of common conventions across different libraries and languages guarantees uniform and coherent telemetry data. Furthermore, OpenTelemetry's extensibility allows telemetry signals to be finely adjusted, including filtering, processing, and aggregating, to cater to various usage scenarios. OpenTelemetry furthermore gives extensive information on how to instrument a library. The most critical resource are the semantic conventions that OpenTelemetry specifies. The metrics, traces, logs and events exported from the library should adhere to these conventions for a unified developer experience. There are a lot of different semantic conventions for different types of software. When defining spans for your library in the context of OpenTelemetry, it's crucial to consider the library from a user's perspective. Users are typically more interested in how their application functions rather than the intricate details of the library internals. Therefore, focus on providing information that aids in analyzing the library's usage. This could involve considering aspects such as span and span hierarchies, numerical attributes on spans as an alternative to aggregated metrics, and span events. For instance, if your library is interacting with a database, it would be more beneficial to create spans only for the logical requests made to the database. The physical network requests should be instrumented within the libraries implementing that specific functionality. Additionally, it's preferable to capture other activities, like object or data serialization, as span events instead of additional spans. Remember to adhere to the semantic conventions when setting span attributes. This ensures consistency and coherence in the telemetry data, providing users with meaningful insights into the behavior and activity of the library. There are instances when instrumentation may not be necessary, especially for libraries that are essentially thin clients encapsulating network calls. OpenTelemetry likely already has an instrumentation library for the underlying RPC client, which can be verified in the registry. In such cases, additional instrumentation of the wrapper library might be redundant. The rule of thumb is to instrument your library only at its own level. Avoid instrumentation if your library is a straightforward proxy on top of well-documented or self-explanatory APIs, if OpenTelemetry already has instrumentation for the underlying network calls, and if there are no conventions that your library needs to follow to enhance telemetry. If unsure, it's better to refrain from instrumenting, as it can always be added later if required. Even if you decide not to instrument, it may still be beneficial to offer a method to configure OpenTelemetry handlers for your internal RPC client instance. This is particularly important in languages that do not support fully automatic instrumentation but can still be useful in others. Libraries should strictly use the OpenTelemetry API. You might be wary about introducing new dependencies, but here are a few points to consider. The API is already stabilized, so there won't be any breaking changes in the future and new features will be added with backwards compatibility. All application configurations are concealed from your library via the Tracer API. Libraries may allow applications to pass `TracerProvider` instances to facilitate dependency injection and simplify testing, or they can obtain it from the global `TracerProvider`. Depending on what's considered idiomatic, different OpenTelemetry language implementations may prefer either passing instances or accessing the global.
When obtaining the tracer, provide your library's (or tracing plugin's) name and version. This information appears in the telemetry data, helping users process and filter telemetry, understand its origin, and debug/report any instrumentation issues.
When deciding what to instrument, public APIs are good candidates for tracing. Spans created for public API calls enable users to link telemetry to application code and understand the duration and outcome of library calls, as you saw in our tracing lab. Trace public methods that internally make network calls or perform local operations that may take a significant amount of time or fail (like IO), as well as handlers that process requests or messages.

If OpenTelemetry doesn't support tracing your network client, you can use logs with verbosity or span events, which can be correlated to parent API calls. Context propagation is crucial in both inbound and outbound calls. When receiving upstream calls, context should be extracted from the incoming request/message using the Propagator API. Conversely, when making an outbound call, a new span should be created to trace the outgoing call, and the context should be injected into the message using the Propagator API. Events (or logs) and traces complement each other in providing observability. Events are better suited for verbose data and should always be attached to the span instance created by your instrumentation. Lastly, it's important to add your instrumentation library to the OpenTelemetry registry for easy discovery by users. It's also recommended testing your instrumentation with other telemetry to see how they interact.

<!-- {{< quizdown >}}

### The primary purpose of developing an instrumentation library for OpenTelemetry is to provide a way to automatically instrument applications.
  - [ ] True
  > This is true. The primary purpose of an instrumentation library is to automatically inject OpenTelemetry API calls into another library or framework that lacks native integration, thus enhancing observability without the need for manual instrumentation.
  - [ ] False

### Adhering to the OpenTelemetry API is NOT a key step in implementing an instrumentation library according to the OpenTelemetry specification.
  - [ ] True
  - [ ] False
  > This is false. Adhering to the OpenTelemetry API is a key step in implementing an instrumentation library. It ensures that the library is compatible with the OpenTelemetry ecosystem and can be used by applications that are already using or planning to use OpenTelemetry.

###  What is the recommended approach when you are unsure about whether to instrument your library with OpenTelemetry?
  - [ ] Instrument your library at all levels
  - [ ] Refrain from instrumenting and add it later if required
  > The recommended approach is to refrain from instrumenting and add it later if required. This is because instrumentation can add complexity and overhead, and it's better to start with the most critical parts of the library that users are most interested in monitoring.
  - [ ] Instrument only the public APIs
  - [ ] Instrument all methods, regardless of their complexity

### When should you consider using logs with verbosity or span events instead of tracing your network client with OpenTelemetry?
  - A) When OpenTelemetry does not support tracing your network client
  > You should consider using logs with verbosity or span events instead of tracing your network client with OpenTelemetry when OpenTelemetry does not support tracing your network client. This allows you to still capture important information about the network calls without the overhead of full tracing.
  - B) When you want to create a new instrumentation library
  - C) When you need to analyze the performance of your application
  - D) When you want to implement distributed tracing

{{< /quizdown >}}

### Review
- **Instrumentation Libraries**: They are tools to add OpenTelemetry support to libraries without native support.
- **Example with Flask**: A Python Flask app can be instrumented using `opentelemetry-flask-instrumentation`.
- **Developing Libraries**: It's important to follow OpenTelemetry conventions and focus on user-facing operations.
- **Instrumentation Decisions**: Decide based on the complexity and necessity, considering logs or span events for unsupported clients. -->