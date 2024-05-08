---
title: "signal correlation (optional)"
date: 2023-12-06T09:43:24+01:00
draft: true
weight: 40
---

So far, we have seen that OpenTelemetry provides a framework to generate logs, metrics, and traces with vendor-agnostic instrumentation.
Besides that, OpenTelemetry's specification and conventions try to standardize aspects of telemetry collection to simplify analyzing data.
Due to historical reasons and different data models, telemetry signals usually reside in isolated silos.
This results in a poor user-experience for those working with observability data, because you typically have to pivot between different signal types in an investigation.
Therefore, a major design goal of OpenTelemetry is to facilitate cross-signal correlation.
To describe the relationship between different types of telemetry, we must add additional context to the data we generate.
This metadata comes in many forms.
For example, we have already seen how we can attach resources to logs/metrics/traces consistently.
This is a form of soft context, because the telemetry from the same resource may but is not guaranteed to be related.
Another example is span metadata from which we reconstruct a trace.
This represents a type of hard context because it connects events that have a causal relationship.
Correlated telemetry signals are the “holy grail” of observability because they have many advantages:
- create a broader picture of what is happening within a system
- replaces the guesswork that involves cobbling metrics and traces with timestamps
- help us drill down more quickly (from triage to mitigation)

Correlating metrics with traces is a powerful technique that enhances the observability of a system by integrating telemetry across different signals. Metrics, which are often useful on their own, gain significant value when they are linked with tracing information. This correlation provides a deeper context and understanding of the events taking place within the system, moving beyond the need to manually piece together metrics and traces based on timestamps. The absence of guesswork or the hunt for logs becomes a thing of the past, as direct correlation allows for a seamless transition from dashboards to traces with just a single click. Exemplars, a form of direct correlation, eliminate the guesswork associated with traditional methods of combining metrics and traces. They offer a more efficient way to drill down into specific traces for investigation directly from the metric, thus accelerating the process of troubleshooting and mitigation. This approach is a key component of OpenTelemetry's design goals, aiming to improve the user experience for those working with observability data by reducing the need to pivot between different signal types during an investigation.

Let's look at some mechanisms to achieve them.
