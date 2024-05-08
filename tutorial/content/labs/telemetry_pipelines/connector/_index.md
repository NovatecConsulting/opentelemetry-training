---
title: "Connector (optional)"
draft: true
weight: 2
---

So far, we have seen that collector is build around the notion of telemetry pipelines.
Each pipeline consists of three classes of components (receivers, processors, exporters) that are specific to a certain type of telemetry.
While this allowed us to alter telemetry data using predefined components, the functionality of the components cannot be changed. If there are specific wishes to what a pipeline component should do one need to implement a connector.

Connectors are part of the functionality provided by the collector.
A connector functions as an exporter to one pipeline and receiver to another.
This dual role allows us to connect two distinct collector pipelines together.
Therefore, every connector defines a data type pair for its exporter and receiver side.
In essence, it either emits the same type of telemetry it received or transforms one type of telemetry into another.
This generic concept provides a powerful tool to shape how telemetry is processed.
Given the wide range of scenarios, different [types of connectors](https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/connector) and their early stage of development, looking at connectors in detail is beyond the scope of this lab.
However, generally speaking, connectors fall into four categories.

{{< figure src="images/forward_connector_replication.drawio.svg" width=450 caption="use forward connector for replication" >}}
The simplest use-case is to connect pipelines into a sequence.
OpenTelemetry provides the [forward connector](https://github.com/open-telemetry/opentelemetry-collector/tree/main/connector/forwardconnector) to pass the data stream from the exporters to straight through to the receiver side.
For example, this can be used to replicate data to one or more receivers or to merge data from different exporter into one stream.

{{< figure src="images/routing_connector.drawio.svg" width=450 caption="routing connector for conditional telemetry flow" >}}
Another capability is to use connectors to implement conditional branches in the data flow.
This [routing connector](https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/connector/routingconnector) specifies a routing table and criteria to evaluate for incoming telemetry.
It determines whether the data is to be emitted to a specific receiver.
Another example for the conditional flow of telemetry is the failover connector.
Specifically, when an error occurs downstream, it has the capability to propagate this error back up the pipeline. This mechanism allows the system to identify and address issues in the data flow more effectively. Upon detecting such an error, the failover connector can react by re-routing the data to another pipeline. This re-routing process is essential for maintaining the integrity and reliability of the telemetry data, ensuring that it can be processed and analyzed without interruption. By dynamically adjusting the data flow based on the occurrence of errors, the failover connector enhances the resilience of the telemetry pipeline, making it more robust and adaptable to various operational conditions.

{{< figure src="images/count_connector.drawio.svg" width=450 caption="use count connector to convert between telemetry signals" >}}
Moreover, connectors can be used to generate a new data stream based on an existing one.
Sometimes we may want to convert between telemetry signals.
One advantage of connectors is that they can consume a certain type of telemetry and emit another.
For example, the [count connector](https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/connector/countconnector) might receive a log stream, analyze it to track of the number of records above a certain severity level, producing a metrics data stream.
Similarly, one might use a [span metrics connector](https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/connector/spanmetricsconnector) to derive a latency histogram metric from the span's start and end timestamps.

<!-- {{< figure src="images/forward_connector_replication.drawio.svg" width=450 caption="use forward connector for replication" >}}
Finally, a connector also provides correlated processing
- sampling connector
  - like forward connector (log to log, metric to metric, trace to trace exporter to receiver)
  - but all is flowing through *one* component
  - reason about multiple data types in one place -->


resources:
- https://www.youtube.com/watch?v=uPpZ23iu6kI
- https://opentelemetry.io/docs/collector/configuration/#connectors
- https://opentelemetry.io/docs/collector/building/connector/


## excercises
### 1.1 connect two telemetry pipelines using a forward connector
