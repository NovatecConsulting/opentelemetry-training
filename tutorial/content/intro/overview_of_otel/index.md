---
title: "Overview of the OpenTelemetry framework"
linktitle: "Overview of the framework"
draft: false
weight: 40
---

<!-- TODO's and improvements

-->

<!--
- provide a uniform set of APIs and libraries that facilitate the instrumentation, generation, collection, and export of telemetry data
- a vendor-agnostic, independent, and heterogeneous layer, it serves as a foundational element for expressing telemetry data, capable of interfacing with a broad spectrum of downstream analysis, querying, alerting, and visualization tools
- design allows for the implementation of OpenTelemetry's capabilities within various libraries, frameworks, and programming languages, streamlining the adoption process
- principles ensure that it remains compatible with a myriad of monitoring and observability tools, guaranteeing long-term stability and consistency in telemetry data formats.
-->

Only time will tell if OpenTelemetry can live up to its ambitious goals.
Chances are, you're eager to explore the project and try it out yourself to see what the fuss is about.
However, newcomers often feel overwhelmed when getting into OpenTelemetry.
The reason is clear: OpenTelemetry is a vast endeavor that addresses a multitude of problems by creating a comprehensive observability framework.
Before you dive into the labs, we want to give you a high-level overview of the structure and scope of the project.

#### signal specification (language-agnostic)
On a high level, OpenTelemetry is organized into *signals*, which mainly include *tracing*, *metrics*, *logging* and *baggage*.
Every signal is developed as a standalone component (but there are ways to connect data streams to another).
Signals are defined inside OpenTelemetry's *language-agnostic* [specification](https://opentelemetry.io/docs/specs/), which lies at the very heart of the project.
The end-user probably won't come into direct contact with the specification, but it plays a vital role in ensuring consistency and interoperability within the OpenTelemetry ecosystem.

{{< figure src="images/otel_specification.drawio.png" width=600 caption="OpenTelemetry specification" >}}

The specification consists of three parts.
First, there are [*definitions of terms*](https://opentelemetry.io/docs/specs/otel/glossary/) that establish a common vocabulary and shared understanding to avoid confusion.
Second, it specifies the technical details of how each signal is designed.
This includes:
- an *API specification* (see [traces](https://opentelemetry.io/docs/specs/otel/trace/api/), [metric](https://opentelemetry.io/docs/specs/otel/metrics/api/), and [logs](https://opentelemetry.io/docs/specs/otel/logs/))
  - defines (conceptual) interfaces that implementations must adhere to
  - ensures that implementations are compatible with each other
  - includes the methods that can be used to generate, process, and export telemetry data
- a *SDK specification* (see [trace](https://opentelemetry.io/docs/specs/otel/trace/sdk/),[metrics](https://opentelemetry.io/docs/specs/otel/metrics/sdk/), [logs](https://opentelemetry.io/docs/specs/otel/logs/sdk/))
  - serves as a guide for developers 
  - defines requirements that a language-specific implementation of the API must meet to be compliant
  - includes concepts around the configuration, processing, and exporting of telemetry data

Besides signal architecture, the specification also covers aspects related to telemetry data.
For example, OpenTelemetry defines [semantic conventions](https://opentelemetry.io/docs/specs/semconv/).
By pushing for consistency in the naming and interpretation of common telemetry metadata, OpenTelemetry aims to reduce the need to normalize data coming from different sources.
Finally, there is also the [OpenTelemetry Protocol (OTLP)](https://opentelemetry.io/docs/specs/otlp/), which we'll cover later.

#### vendor-agnostic instrumentation (language-specific) 
{{< figure src="images/otel_implementation.drawio.png" width=600 caption="generate and emit telemetry via the OTel API and SDK packages" >}}

To generate and emit telemetry from applications, we use **language-*specific* implementations**, which adhere to OpenTelemetry's specification.
OpenTelemetry supports a wide-range of popular [programming languages](https://opentelemetry.io/docs/instrumentation/#status-and-releases) at varying levels of matureity.
The implementation of a signal consists of two parts:
- *API*
  - defines the interfaces and constants outlined in the specification
  - used by application and library developers for vendor-agnostic instrumentation
  - refers to a no-op implementation by default
- *SDK*
  - provider implement the OpenTelemetry API
  - contains the actual logic to generate, process and emit telemetry
  - OpenTelemetry ships with official providers that serve as the reference implementation (commonly referred to as the SDK) 
  - possible to write your own


<!-- auto and manual instrumentation -->
Generally speaking, we use the OpenTelemetry API to add instrumentation to our source code.
In practice, this can be achieved in various ways, such as:
- manual instrumentation (for fine-grained control)
- auto-instrumentation and instrumentation libraries (if available and to avoid code changes)
- by using code that has already been instrumented with OpenTelemetry

<!-- API / SDK separation -->
For now, let's skip futher details an focus on why OpenTelemetry decided to separate the API from the SDK.
On startup, the application registers a provider for every type of signal.
After that, calls to the API are forwarded to the respective provider.
If we don't explicitly register one, OpenTelemetry will use a fallback provider that translates API calls into no-ops.

The primary reason is that it makes it easier to embed native instrumentation into open-source library code.
OpenTelemetry's API is designed to be lightweight and safe to depend on.
The signal's implementation provided by the SDK is significantly more complex and likely contains dependencies on other software.
Forcing these dependencies on users could lead to conflicts with their particular software stack.
Registering a provider during the initial setup allows users to resolve dependency conflicts by choosing a different implementation.
Furthermore, it allows us to ship software with built-in observability without forcing the runtime cost of instrumentation onto users that don't need it.

<!-- 
However, it comes with its own set of trade-offs.
Implementing OpenTelemetry can introduce complexity to an application, potentially impacting performance, when configured wrong, and may lead to vendor lock-in if heavily invested in a specific implementation.
As a relatively new project, it may face challenges with adoption and compatibility, and while it aims to be vendor-agnostic, there is still a risk of vendor lock-in.
Customization and flexibility may be limited compared to tailored solutions for specific use cases, and there can be a learning curve associated with understanding OpenTelemetry's concepts and APIs. Maintenance and support, particularly for organizations that rely on open-source projects, may require additional investment. 
Integration with existing systems can be challenging and may require extra effort. 
Costs may also be incurred depending on the scale of implementation and the need for additional services or support. 
Lastly, while OpenTelemetry has a growing community, it may not yet have the same level of community support or ecosystem of tools and integrations as more established projects.
Additionally, it is important to consider that alternative implementations might offer better performance, as the SDK is designed to be extensible and general-purpose.
This implies that while the SDK provides a robust framework for observability, it may not be the most optimized solution for every scenario. 
It is essential to weigh these trade-offs against the benefits of OpenTelemetry to determine if it is the right fit for a particular application or organization. 
But if OpenTelemetry is used in the right way and configured well - the benefits might

The benefit of instrumenting code with OpenTelemetry to collect telemetry data is that the correlation of the previously mentioned signals is simplified since all signals carry metadata. Correlating telemetry data enables you to connect and analyze data from various sources, providing a comprehensive view of your system's behavior. By setting a unique correlation ID for each telemetry item and propagating it across network boundaries, you can track the flow of data and identify dependencies between different components. OpenTelemetry's trace ID can also be leveraged for correlation, ensuring that telemetry data from the same request or transaction is associated with the same trace. Correlation engines can further enhance this process by matching data based on correlation IDs, trace IDs, or other attributes like timestamps, allowing for efficient aggregation and analysis. Correlated telemetry data provides valuable insights for troubleshooting, performance monitoring, optimization, and gaining a holistic understanding of your system's behavior. In the labs' chapter you will see how correlated data looks like. Traditionally this had to be done by hand or just by timestamps which was a tedious task. -->


#### telemetry processor (stand-alone component)
{{< figure src="images/otel_collector_overview.drawio.png" width=650 caption="processing and fowarding telemetry to backends" >}}

So far, we have seen that OpenTelemetry provides tooling for vendor-agnostic instrumentation to application and library developers.
This alone marks a significant milestone, but OpenTelemetry's framework goes much further.
After generating and emitting telemetry, operators are responsible for managing and ingesting it into the respective backends.
This includes tasks such as:
- gathering data from various sources
- parsing and converting it for downstream processing
- enrichment with additional metadata
- filtering out irrelevant data to reduce noise and storage requirements
- normalization and applying transformations 
- buffering for resilience and performance
- routing to steer subsets of telemetry to different destinations
- forwarding to backends

To build and configure such telemetry pipelines, operations teams often deploy additional infrastructure.
A popular example is the [fluentbit](https://fluentbit.io/) telemetry agents.
Similarly, OpenTelemetry provides a standalone component with these capabilities: the OpenTelemetry [Collector](https://opentelemetry.io/docs/collector/).

#### wire protocol

Completing the package of standardization, generation and management, OpenTelemetry also defines how to transport telemetry between producers, agents, and backends.
The [OpenTelemetry Protocol (OTLP)](https://opentelemetry.io/docs/specs/otel/protocol/) is an open-source and vendor-neutral wire format that defines:
- how data is encoded in memory
- a protocol to transport that data across the network

As a result, OTLP is used throughout the observability stack.
Emitting telemetry in OLTP means that instrumented applications and third-party services are compatible with countless observability solutions.
The collector supports receiving telemetry from and exporting to a wide array of formats (e.g. Prometheus Metrics, Zipkin traces, etc.).
However, using OTLP is generally preferred, because the Collector also uses it internally to represent and process telemetry.
Thereby, we avoid the cost of having to convert between formats and increase consistency.
This is because the native format closely aligns with the ideas proposed by the framework (having attributes follow semantic conventions, cross-signal correlation, etc.).
Similarly, most observability backends support OTLP right out-of-the-box.
Given the rapid adoption of OpenTelemetry, integrating with OTLP automatically gives you access to a wide audience of potential users.
Moreover, an open and vendor-neutral telemetry protocol means less work for developers of observability tools.
Before, you had to develop countless adapters to be able to ingest data arriving in various proprietary formats.
In other words, OTLP is a significant push for interoperability between tools and services in the observability ecosystem.

OTLP offers three transport mechanisms for transmitting telemetry data: HTTP/1.1, HTTP/2, and gRPC. 
When using OTLP, the choice of transport mechanism depends on application requirements, considering factors such as performance, reliability, and security. 
OTLP data is often encoded using the Protocol Buffers (Protobuf) binary format, which is compact and efficient for network transmission and supports schema evolution, allowing for future changes to the data model without breaking compatibility.
Data can also be encoded in the JSON file format, which allows for a human-readable format with the disadvantage of higher network traffic and larger file sizes. 