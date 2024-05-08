---
title: "Collector"
draft: false
weight: 1
---

### How to perform the exercise
* You need to either start the [repository](https://github.com/JenSeReal/otel-getting-started/) with Codespaces, Gitpod or clone the repository with git and run it locally with dev containers or docker compose
* Initial directory: `labs/collector/initial`
* Solution directory: `labs/collector/solution`
* Source code: `labs/collector/initial/src`
* How to run the application either:
  * Run the task for the application: `Run collector initial application` (runs the Python application) and `Run collector initial` (runs the OpenTelemetry Collector in a Docker Container)
  * Run the application with Terminal commands `python3 src/app.py` (runs the Python application) and `docker compose up` (runs the OpenTelemetry Collector in a Docker Container)

### Why do we need Collectors?

Over the previous labs, we have seen how OpenTelemetry's SDK implements the instrumentation which produces the telemetry data.
We also configured a basic pipeline to export the generated telemetry directly from the SDK.
The *Collector* is a key component of OpenTelemetry to manage how telemetry is processed and forwarded.
At this point you might ask yourself: How are these capabilities different from the SDK?
With the SDK, the telemetry pipeline was defined *in* the application code.
Depending on your use-case, this approach can be perfectly fine.
A Collector, on the other hand, is a binary written in Go, that runs as a separate, standalone process.
Its provides flexible, configurable and vendor-agnostic system to process telemetry *outside* the application.
It essentially serves as a broker between a telemetry source and the backend storing the data.

Deploying a Collector has many advantages.
Most importantly, it allows for a cleaner separation of concerns.
Developers shouldn't have to care about what happens to telemetry after it has been generated.
With a collector, operators are able to control the telemetry configuration without having to modify the application code.
Additionally, consolidating these concerns in a central location streamlines maintenance.
In an SDK-based approach, the configuration of where telemetry is going, what format it needs to be in, and it should be processed is spread across various codebases managed by separate teams.
However, telemetry pipelines are rarely specific to individual applications.
Without a collector, making adjustments to the configuration and keeping it consistent across applications can get fairly difficult.
Moving things out of the SDK has other benefits.
For instance, the overall configuration of the SDK becomes much leaner.
Moreover, we no longer have to re-deploy the application every time we make a change to the telemetry pipeline.
Troubleshooting becomes significantly easier, since there is only a single location to monitor when debugging problems related to telemetry processing.
Offloading processing and forwarding to another process means applications can spend their resources on performing actual work, rather than dealing with telemetry.
Before going into more detail, let's look at the components that make up a collector.

### Architecture of a collector pipeline
{{< figure src="images/collector_arch.drawio.svg" width=600 caption="collector to process and forward telemetry" >}}

The pipeline for a telemetry signal consists of a combination of receivers, processors, and exporters.


A **receiver** is how data gets from a source (i.e. the application) to the OpenTelemetry collector.
This mechanism can either be pull- or push-based.
Out-of-the-box, the Collector supports an [OTLPReceiver](https://github.com/open-telemetry/opentelemetry-collector/tree/main/receiver/otlpreceiver) for receiving traces, metrics and logs in OpenTelemetry's native format
The [collector-contrib](https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/receiver) repository includes a range of receivers to ingest telemetry data encoded in various protocols.
For example, there is a [ZipkinReceiver](https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/receiver/zipkinreceiver) for traces, [StatsdReceiver](https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/receiver/statsdreceiver) and [PrometheusReceiver](https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/receiver/prometheusreceiver) and much more.
Once data has been imported, receivers convert telemetry into an internal representation.
Then, receivers pass the collected telemetry to a chain of processors.

A **processor** provides a mechanism to pre-process telemetry before sending it to a backend.
There are two categories of processors, some apply to all signals, while others are specific to a particular type of telemetry.
Broadly speaking processing telemetry is generally motivated by:
- to improve the data quality
  - add, delete, rename, transform attributes
  - create new telemetry based on existing data
  - convert older version of a data source into one that matches the current dashboards and queries used by the backend
- for governance and compliance reasons
  - use attributes to route data to specific backends
- to reduce cost
  - drop unwanted telemetry via allow and deny lists
  - tail-based sampling
- security
  - scrubbing data to prevent sensitive information from being stored (and potentially leaked) in a backend
- influence how data flows through the pipeline
  - batch
  - retry
  - memory limit

By connecting processors into a sequential hierarchy, we can process telemetry in complex ways.
Since data is passed from one processor to the next, the order in which processors are specified matters.

Finally, the last processor in a chain hands its output to an **exporter**.
The exporter takes the data, converts the internal representation into a protocol of choice, and forwards it to one (or more) destination.
Similar to receivers, the collector ships with built-in exporters for [OTLP](https://github.com/open-telemetry/opentelemetry-collector/tree/main/exporter).
As previously mentioned, many open-source or commercial observability backends are built around custom data formats.
Even though OpenTelemetry is becoming more popular, your current backend might not yet (or is in the early stages) support OTLP.
To solve this, the [collector-contrib](https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/exporter) repository includes exporters for many telemetry protocols.

<!-- configuration -->
{{< figure src="images/collector_config.drawio.svg" width=600 caption="YAML configuration" >}}

Now, let's look at how to configure the collector.
As with most cloud-native deployments, we express configuration via a YAML format.
It has three main sections to *define* the `receivers`, `processors`, `exporters`  components that we'll use to build our telemetry pipelines.
At the top level, each component has an ID that consists of the type (and optionally name).
The ID serves as a unique reference to the component.
Beneath that, a set of (type dependent) fields configure the component.

The `pipelines` section at the bottom of the file defines the pipelines within the collector.
Each pipeline is identified by a unique ID that specifies the type of telemetry data and (optionally) name.
Below that, the `receiver`, `processor`, and `exporter` fields outline the structure of the data flow with the pipeline.
Each field contains a list of references to component IDs.
If a pipeline has more than one receiver, the data streams will get merged before being fed to the sequence of processors.
If there are multiple exporters, the data stream will be copied to both.
It is also possible for receivers and exporters to be shared by pipelines.
If the same receiver is used in different pipelines, each pipeline receives a replica of the data stream.
If different pipelines target the same exporter, the data stream will be merged into one.

### Define a basic collector pipeline

Let's put the knowledge into practice.
Open the `docker-compose.yml` file to review the lab environment.
It for now only includes one service - an instance of the OpenTelemetry Collector.


```yaml { title="docker-compose.yml" }
services:
  otelcol:
    image: otel/opentelemetry-collector-contrib:0.97.0
    restart: unless-stopped
    command: ["--config=/etc/otel-collector-config.yml", "${OTELCOL_ARGS}"]
    volumes:
      - ./otel-collector-config.yml:/etc/otel-collector-config.yml
    ports:
      - 4317:4317
      - 4318:4318
```

A successful deployment consists of three things.
First, we deploy a Collector for which there are several strategies.
We'll examine them later.
For now, let's focus on the `otelcol` section in `docker-compose.yml`.
The collector exposes a set of network ports to which services can push their telemetry.
Besides that, we also mount a YAML file within the Collector's container.
It contains the configuration of Collector's components and pipelines.

Before we can receive telemetry, we must first ensure that the SDK of instrumented services send their telemetry to the collector.
For this lab we will reuse the code written in the previous labs. In the previous labs the data was sent to either the console or directly to a Prometheus instance.
This needs to be changed now. Open the `src` directory in `collector/initial`. You'll see that we took the liberty to copy the solution code of the previous labs into here.
When opening `logging_utils.py`, `metric_utils.py` or `tracing_utils.py` you will see that the exporters are set to `Console[TelemetryName]Exporter`. As we learned earlier this will send to telemetry data directly to `stdout`. Since we want to export the data to the Collector, we need to send the data via the OTLP protocol. To use the OTLP exporter we need a new Python package called `opentelemetry-exporter-otlp`. To install the package simply run

```bash { title="Install opentelemetry-exporter-otlp using pip" }
pip install opentelemetry-exporter-otlp
```

After the installation of the package we can use this in our code. With this package installed we get access to the `opentelemetry.exporter` imports. To use the `OTLPLogExporter` simply import it from `opentelemetry.exporter.otlp.proto.grpc._log_exporter`. Now you can replace the `ConsoleLogExporter` with the `OTLPLogExporter`. You might want to add `insecure=True` to the parameters since this is a development environment.

```python { title="Replace the ConsoleLogExporter with OTLPLogExporter" }
logger_provider.add_log_record_processor(SimpleLogRecordProcessor(exporter=OTLPLogExporter(insecure=True)))
```

Save your changes.
If you want to change the tracing and metric labs you can do so. Just as with the logs you just need to exchange the `ConsoleMetricExporter` with `OTLPMetricExporter` and `ConsoleTraceExporter` with `OTLPTraceExporter`.

To receive the sent telemetry data we need to tell the OpenTelemetry Collector that data is comping via the OTLP protocol. To do so open the `otel-collector-config.yml` in `collector/initial` and start editing it.
We'll sketch out a basic Collector pipeline that connects telemetry sources with their destinations.
It declares pipelines components (receiver, processor, exporter), specifies telemetry pipelines and their data flow.

```yaml { title="otel-collector-config.yml" }
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317
      http:
        endpoint: 0.0.0.0:4318
```

Start by creating an OTLP Receiver that listens for logs, metrics, and traces on a host port. With this configured the OpenTelemetry Collector will listen on port `4317` via `gRPC` and on port `4318` via `HTTP`. Since we are in a Docker environment with containers we need to specify the address with `0.0.0.0` or else the collector wouldn't be visible from outside the container.

Although OpenTelemetry is becoming widely adopted, applications may prefer to export telemetry in a non-native format (such as Prometheus, Zipkin, Jaeger, and more).
In the receiver section you could also define receivers for the other formats and applications. An example of this could be this config

```yaml { title="Exemplary prometheus receiver config" }
receivers:
    prometheus:
      config:
        scrape_configs:
          - job_name: 'otel-collector'
            scrape_interval: 5s
            static_configs:
              - targets: ['0.0.0.0:8888']
          - job_name: k8s
            kubernetes_sd_configs:
            - role: pod
            relabel_configs:
            - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
              regex: "true"
              action: keep
            metric_relabel_configs:
            - source_labels: [__name__]
              regex: "(request_duration_seconds.*|response_duration_seconds.*)"
              action: keep
```
You can find all receivers in the [GitHub repository of the OpenTelemetry Collector](https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/receiver).

To be able to use the received data in the Collector we need to process them. That's what the processors are there for. In this lab we will first look at one vital processor: the [batch processor](https://github.com/open-telemetry/opentelemetry-collector/tree/main/processor/batchprocessor).


```yaml { title="collector/initial/otel-collector-config.yml" }
processors:
  batch:
```

[Processors](https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/processor) are a powerful tool that let us shape telemetry in arbitrary ways.
Since this exercise focuses on ingest rather than on processing, we'll keep this part as simple as possible.
While it is possible to define pipelines without any processors, it is recommended to at least configure a [`batch processor`](https://github.com/open-telemetry/opentelemetry-collector/tree/main/processor/batchprocessor).
Buffering and flushing captured telemetry in batches means that the collector can significantly reduce the number of outgoing connections.
Another example of what you can achieve with processors are: filtering, updating, creating, deleting of unwanted data. You can also transform metrics or spans to better suit your needs. As a small example you can add an attribute processor that adds a field called `example_field` with a value of `Hello from the OpenTelemetry Collector`.

```yaml { title="collector/initial/otel-collector-config.yml" }
processors:
  batch:
  attributes/example:
    actions:
      - key: example_field
        action: insert
        value: Hello from the OpenTelemetry Collector
```

There are a variety of different [processors](https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/processor) that you can check out that are well documented.

With processors in place, move on to define exporters to forward telemetry to our backends.
Telemetry backends typically offer multiple ways to ingest telemetry for wide compatibility.
For Prometheus, one option is to use a [Prometheus Exporter](https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/exporter/prometheusexporter).
The collector exports and exposes metrics via an HTTP endpoint that can be scraped by the Prometheus server.
While this aligns with Prometheus a pull-based approach, another option is to use a feature flag to enable native OpenTelemetry support.
In this case, Prometheus serves an HTTP route `/otlp/v1/metrics` to which services push metrics via OTLP.
This approach is similar to the jaeger-collector (included in the jaeger-all-in-one binary), which exposes a set of ports to receive spans in different formats.
Add a [OTLP HTTP Exporter](https://github.com/open-telemetry/opentelemetry-collector/tree/main/exporter/otlphttpexporter) to write metrics to Prometheus and a [OTLP gRPC Exporter](https://github.com/open-telemetry/opentelemetry-collector/tree/main/exporter/otlpexporter) to ingest traces into Jaeger.
For simplicity, we'll set the `tls` property to insecure.
A production deployment should supply certificates to encrypt the communication between the collector and the backend.

```yaml { title="Exemplary exporters to prometheus and Jaeger" }
exporters:
  otlphttp/prometheus:
    endpoint: "http://prometheus:9090/api/v1/otlp"
    tls:
      insecure: true

  otlp/jaeger:
    endpoint: "jaeger:4317"
    tls:
      insecure: true
```

In this lab we will for simplicity reasons just add an output to `stdout`, which is in fact very useful for debugging purposes.
To add an export to `stdout` we will add the `debug` processor.


```yaml { title="collector/initial/otel-collector-config.yml" }
exporters:
  debug:
    verbosity: detailed
```

In this case we need to set the field`verbosity` to `detailed`, so we can see everything that the Collector receives in the terminal output.

As the last step we need to


```yaml { title="Exemplary pipelines to prometheus and Jaeger" }
# configure the data flow for pipelines
service:
  pipelines:
    metrics/example:
      receivers: [otlp, prometheus]
      processors: [batch]
      exporters: [otlphttp/prometheus]
    traces/example:
      receivers: [otlp]
      processors: [batch]
      exporters: [otlp/jaeger, debug]
```

After declaring the components, we can finally combine them to form telemetry pipelines.
A valid pipeline requires at least one receiver and exporter.
In addition, all components must support the data type specified by the pipeline.
Note that a component may support one *or* more data types.
For instance, we've defined a single OTLP Receiver for logs, metrics, and traces.
Placing these components in a pipeline, provides the necessary context for how they are used.
If we add a OTLP Receiver to a traces pipeline, its role is to receive spans.
Conversely, a Prometheus receiver is inherently limited to metrics data
Therefore, it can only be placed in a corresponding pipeline.

Now, let's define the data flow for the traces, metrics and log pipelines.
Add the references to the relevant components in the receivers, processors, and exporters lists.

```yaml { title="collector/initial/otel-collector-config.yml" }
service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch]
      exporters: [debug]
    metrics:
      receivers: [otlp]
      processors: [batch]
      exporters: [debug]
    logs:
      receivers: [otlp]
      processors: [batch, attributes/example]
      exporters: [debug]
```

In this example we define three pipelines - one for traces, metrics and logs each. The pipelines each receive the data via `otlp` (which corresponds to the name of the receiver field), process the data with `batch` and send the data to the `debug` exporter. To see the effect of the example attribute we also need to add this processor to the processors list. In this case we decided to add the processor only to the logs pipeline.

To confirm everything we need to start the Collector. To do so we prepared a `docker-compose.yml` in `collector/initial` file that you can start. Just start the collector with

```bash { title="collector/initial/docker-compose.yml" }
docker compose up
```

or hit <kbd>Ctrl/Command</kbd> + <kbd>Shift</kbd> + <kbd>P</kbd> `Task: Run Task` and select `Run collector initial`.

This will start the OpenTelemetry Collector. To send metrics, traces and logs from our application you can either run the application with

```bash { title="collector/initial" }
python3 src/app.py
```

<kbd>Ctrl/Command</kbd> + <kbd>Shift</kbd> + <kbd>P</kbd> `Task: Run Task` and select `Run collector initial application`

You should be able to see the first logs, traces and metrics to appear in the console window of the Collector.

### different ways to deploy a collector

To test whether the pipeline works as expected, we must deploy a collector.
Before we do that, let's look at different Collector topologies.
A collector can run as a sidecar, node agent, or standalone service.

<!-- sidecar -->
{{< figure src="images/collector.drawio_sidecar.svg" width=400 caption="sidecar-based collector deployment" >}}

In a **sidecar-based** deployment, the collector runs as a container next to the application.
Having a collection point to offload telemetry as quickly as possible has several advantages.
By sharing a pod, the application and collector can communicate via localhost.
This provides a consistent destination for the application to send its telemetry to.
Since local communication is fast and reliable, the application won't be affected by latency that might occur during telemetry transmission.
This ensures that the application can spend its resources processing workloads instead of being burdened by the telemetry collection, processing, and transmission.

<!-- local agent -->
{{< figure src="images/collector.drawio_agent.svg" width=400 caption="agent-based collector deployment" >}}

Another option is to run a collector **agent** on every node in the cluster.
In this case, the collector serves as a collection point for all applications running on a particular node.
Similar to sidecars, applications can evacuate the produced telemetry quickly.
However, having a single agent per node means that we decrease the number of connections to send telemetry.
Furthermore, a node agent provides a great place to augment the telemetry generated by the SDK.
For example, an agent can collect system-level telemetry about the host running the workloads, because it isn't isolated by a container.
It also allows us to enrich telemetry with resource attributes to describe where telemetry originates from.


<!-- standalone service -->
{{< figure src="images/collector.drawio_service.svg" width=400 caption="deploying collector as a standalone service" >}}

Finally, there is the option to run the collector as a dedicated **service** in the cluster.
It is no surprise that processing telemetry consumes memory resources and CPU cycles.
Since pod or node collectors run on the same physical machine, these resources are no longer available to applications.
Moreover, the capacity of the telemetry pipeline is tightly coupled to the number of pods or agents/nodes.
Certain conditions will cause applications to produce large volumes of telemetry.
If a local collector lacks the resources to buffer, process, and ship telemetry faster than it is produced, data will be dropped.
A horizontally scalable collector service lets us deal with low and high-tide telemetry scenarios more efficiently and robustly.
Having a load balancer in front of a pool of collectors allows us to scale resources based on demand.
Even distribution also reduces the risk that a single collector gets overwhelmed by a spike in telemetry.
By running collectors on a dedicated set of machines, we no longer compete with applications for available resources.

When deploying the collector as a dedicated service within the cluster, it's important to consider the potential for network latency, which can impact the performance of the telemetry pipeline. This deployment strategy also allows for the configuration of multiple tiers of Collector deployments, each tailored to perform specific processing tasks. This approach not only enhances the flexibility and efficiency of the telemetry pipeline but also ensures that resources are optimally utilized, preventing competition with applications for available resources.

<!--- ### 1.2 deploy a collector --->


<!-- Notes
- vendor-agnostic implementation of how to receive, process and export telemetry data
- scalable
- supports popular observability data formats
- can send to one or more open source or commercial back-ends
- local Collector agent is the default location to which instrumentation libraries export their telemetry data.
- allows your service to offload data quickly and the collector can take care of additional handling like retries, batching, encryption or even sensitive data filtering.
-->
