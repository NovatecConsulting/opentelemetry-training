---
title: "exemplar"
draft: false
weight: 1
---


While aggregated metrics offer a general understanding of the state of a system, traces give fine-grained insight into the operations of a single request.
During root cause analysis, a common task is to identify what had happened when there is a sudden change in a metric.
To do that, we must identify traces that are representative of the metric.
Historically, the most common approach was to compare timestamps and filter based on labels in order narrow down the search.
To avoid the laborious work of creating these relationships by hand, `OpenMetrics` introduces the concept of Exemplars.
An Exemplar creates a direct link between a metric data point and (one of the) traces that generated it.
When we record a new measurement for a metric, we retrieve the active span context of the and embed the (sampled) trace identifiers (trace_id, span_id) inside the metric.
By instrumenting metrics and traces together, OpenTelemetry ensures that the trace actively participated to generating a particular metric value, establishing *causality* rather than just *temporal* correlation.

Data points are enriched with an exemplar field that captures the context of the metric event. Each metric data point includes the current value and the latest exemplar, which contains a set of labels, typically just the trace ID in our case, along with the recorded value and timestamp. If a new request is made, a different exemplar should be recorded, reflecting the changing context of the metric.

To enable a metric to contain information about an active span, the data points include an exemplar field as part of their definition. This exemplar field contains several pieces of information:

- The trace ID, which uniquely identifies the trace.
- The span ID, which identifies the specific span within the trace.
- The timestamp of when the exemplar was recorded.
- A set of attributes associated with the exemplar, which can include additional contextual information.
- The value being recorded, which is the actual metric data point.

An exemplar is a recorded value that associates OpenTelemetry context to a metric event within a Metric. It is a recorded Measurement that consists of:

- An optional trace, identified by the trace ID and span ID, associated with the recording.
- The time of the observation, which is when the metric was recorded.
- The recorded value, which is the actual data point.
- A set of attributes associated with the Measurement, which are not already included in the metric data point and provide additional insight into the context when the observation was made.

This concept of exemplars is a powerful tool in observability, as it allows for a direct link between a metric data point and the traces that generated it, establishing causality rather than just temporal correlation. This is particularly useful during root cause analysis, where it is essential to identify what happened when there is a sudden change in a metric. By instrumenting metrics and traces together, OpenTelemetry ensures that the trace actively participated in generating a particular metric value.



<!--
- data points defined in OpenTelemetry include an exemplar field that contains ...

- So for each metric, we have
  - the current value
  - the latest exemplar
    - the set of labels — only the trace ID for us — along with the recorded value and timestamp
    - If you do another request, you should see different exemplar


- enable a metric to contain information about an active span
	- Data points include an **exemplar** field as part of their definition, which contains
		- trace id
		- span id
		- timestamp
		- a set of attributes associated with the exemplar
		- value being recorded

- is a recorded value that associates OpenTelemetry context to a metric event within a Metric
- is a recorded Measurement that consists of
  - trace (trace_id, span_id) associated with a recording  (optional)
  - time of the observation
  - recorded value
  - set of attributes associated with the Measurement which
    - not already included in a metric data point
    - provide additional insight into the Context when the observation was made
-->

{{< figure src="images/metrics_dashboard_with_exemplars.png" width=400 caption="exemplar on metric chart" >}}
{{< figure src="images/exemplar_popup.png" width=400 caption="jump to tracing backend" >}}

From the perspective of an end-user perspective, Exemplars function as a pointer between (otherwise separate) observability systems.
When Exemplars are implemented correctly, the user will notice a few highlighted values in the panel that visualizes the time series of a metric.
For instance, in Grafana, Exemplars are displayed as diamonds to distinguish them from regular measurements.
If we hover over it, a popup with further details about the exemplar shows up.
Here, we usually can click on a link that re-directs us to our trace visualization frontend and opens the trace with the corresponding ID.


The concept of exemplars was pioneered by the `OpenMetrics` project is still fairly novel.
This is important because a successful implementation requires support at various levels within the monitoring ecosystem.
Traditional metrics formats may lack native support for Exemplars, which prevents us from exporting them.
For instance, since Prometheus's own [text-based exposition protocol](https://github.com/prometheus/docs/blob/main/content/docs/instrumenting/exposition_formats.md) doesn't support Exemplars, we must expose metrics via the `OpenMetrics` format.
Similarly, the backend must be capable of accommodating Exemplar data.
The visualization frontend plays a crucial role in making exemplar data interpretable for users.
It must be capable of interpreting Exemplar data to represent them within the metric graphs.
To easily navigate from metric graphs to associated traces the frontend must also facilitate direct links to tracing visualization tools.


<!--
https://opentelemetry.io/docs/specs/otel/metrics/sdk/#exemplar
- provide specific context to otherwise general aggregations

https://medium.com/go-city/enriching-prometheus-metrics-with-exemplars-for-easier-observation-of-a-distributed-system-e6b2fd0c6b74
- Exemplars are references to data outside of the MetricSet
- common use case are IDs of program traces
- can only have one exemplar per metric

https://www.timescale.com/blog/a-deep-dive-into-open-telemetry-metrics/
- READ

https://grafana.com/blog/2021/03/31/intro-to-exemplars-which-enable-grafana-tempos-distributed-tracing-at-massive-scale/


https://cloud.google.com/blog/products/devops-sre/trace-exemplars-now-available-in-managed-service-for-prometheus?hl=en
-  Storing trace information with metric data lets you quickly identify the traces associated with a sudden change in metric values
-  you don't have to manually cross-reference trace information and metric data by using timestamps to identify what had happened in the application when the metric data was recorded.


https://cloud.google.com/stackdriver/docs/managed-prometheus/exemplars
-  typically used to associate trace identifiers with metric data collected in a time interval, but they can be used to associate any non-metric data with the collected metrics.


https://autometrics.dev/blog/autometrics-rs-0-5-automatically-connecting-prometheus-metrics-to-traces
- “once you’ve debugged an issue with exemplars, it’s hard to go back.”

https://www.youtube.com/watch?v=TzNZIEvhAdA
- try work out which trace is representative of a metric your are looking at
- today, user narrows down the seach space using labels and time window
- wouldn't it be nice if we could go straight from the metric datapoint to one of the traces for a request that comprosed that exact data point
- embed metadata in the metric that is used as a foreign key into the trace that was captured when incrementing a metric counter, latency measurement

https://blog.lunatech.com/posts/2022-01-21-linking-metrics-and-traces-with-exemplars
- concept from OpenMetrics, implemented by Prometheus.
- Exemplars are references to data outside of the MetricSet. A common use case are IDs of program traces.

https://vbehar.medium.com/using-prometheus-exemplars-to-jump-from-metrics-to-traces-in-grafana-249e721d4192
-  A few highlighted values in a time series,
- main use-case is to include the request’s trace ID in the labels so that we can jump from a metric time series to the interesting traces directly
- requires

-->