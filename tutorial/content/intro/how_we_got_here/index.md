---
title: "How we (traditionally) observe our systems"
linktitle: "How we got here"
draft: false
weight: 10
---

<!-- TODO's and improvements

-->

> Observability is a measure of how well the internal states of a system can be inferred from knowledge of its external outputs. [[Wiki]](https://en.wikipedia.org/wiki/Observability)

To make a distributed system observable, we must model its state in a way that lets us reason about its behavior.
This is a composition of three factors:
First, there is the *workload*.
These are the operations a system performs to fulfill its objectives.
For instance, when a user sends a request, a distributed system often breaks it down into smaller tasks handled by different services.
Second, there are *software abstractions* that make up the structure of the distributed system.
This includes elements such as load balancers, services, pods, containers and more.
Lastly, there are physical machines that provide computational *resources* (e.g. RAM, CPU, disk space, network) to carry out work.

{{< figure src="images/workload_resource_analysis.drawio.png" width=400 caption="workload and resource analysis based on [[Gregg16]](https://www.brendangregg.com/Slides/ACMApplicative2016_SystemMethodology/#18)" >}}
<!-- 
developers need highly detailed telemetry that they can use to pinpoint specific problems in code. Operators need broad, aggregated information from across hundreds or thousands of servers and nodes so that they can spot trends and respond quickly to outliers. Security teams need to analyze many millions of events across endpoints to discover potential intrusions; 
-->
Depending on our background, we often have a certain bias when investigating the performance of / troubleshooting problems in a distributed system.
Application developers typically concentrate on workload-related aspects, whereas operations teams tend to look at physical resources.
To truly understand a system, we must combine insights from multiple angles and figure out how they relate to one another.
However, before we can analyze something, we must first capture aspects of system behavior.
As you may know, we commonly do this through a combination of *logs*, *metrics* and *traces*.
Although it seems normal today, things weren't always this way.
But why should you be concerned about the past?
The reason is that OpenTelemetry tries to address problems that are the result of historical developments. <!-- TODO: ref Ted Young -->

#### logs
{{< figure src="images/logs.png" width=600 caption="Exemplary log files" >}}
<!-- NOTEST
simplest / earliest form of telemetry, 
inform operator / developer about individual events in a system by emitting as text-based messages
initially just for human consumption (printf debugging) -> collected, parsed, enriched with metadata and indexed by machines
improved how they stored and searched these logs by creating specialized databases that were good at full-text search.

very hard to standardize (see video) because messages are written by humans
hard to agree on semantics / language
-->

A *log* is an append-only data structure that records events occurring in a system. 
A log entry consists of a timestamp that denotes when something happened and a message to describe details about the event. 
However, coming up with a standardized log format is no easy task. 
One reason is that different types of software often convey different pieces of information. The logs of an HTTP web server are bound to look different from those of the kernel. 
But even for similar software, people often have different opinions on what good logs should look like. 
Apart from content, log formats also vary with their consumers. Initially, text-based formats catered to human readability. 
However, as software systems became more complex, the volume of logs soon became unmanageable.
To combat this, we started encoding events as key/value pairs to make them machine-readable. 
This is commonly known as structured logging. 
Moreover, the distribution and ephemeral nature of containerized applications meant that it was no longer feasible to log onto individual machines and sift through logs. 
As a result, people started to build logging agents and protocols to forward logs to dedicated services. 
These logging systems allowed for efficient storage as well as the ability to search and filter logs in a central location. 

#### metrics
<!-- NOTES
from individual events to higher level view
track how system state changes over time
-->

{{< figure src="images/metric_types.drawio.png" width=400 caption="The four common types of metrics: counters, gauges, histograms and summaries" >}}

Logs shine at providing detailed information about individual events.
However, sometimes we need a high-level view of the current state of a system.
This is where *metrics* come in.
A metric is a single numerical value that was derived by applying a statistical measure to a group of events.
In other words, metrics represent an aggregate.
This is useful because their compact representation allows us to graph how a system changes over time.
In response, the industry developed instruments to extract metrics, formats and protocols to represent and transmit data, specialized time-series databases to store them, and frontends to make this data accessible to end-users.

#### traces
<!-- 
Instead of just looking at individual events—logs—tracing systems looked at entire operations and how they combined to form transactions.
-->
{{< figure src="images/distributed_system.drawio.png" width=400 caption="Exemplary architecture of a distributed system" >}}

As distributed systems grew in scale, it became clear that traditional logging systems often fell short when trying to debug complex problems. 
The reason is that we often have to understand the chain of events in a system.
On a single machine, stack traces allow us to track an exception back to a line of code.
In a distributed environment, we don't have this luxury.
Instead, we perform extensive filtering to locate log events of interest.
To understand the larger context, we must identify other related events. 
This often results in lots of manual labour (e.g. comparing timestamps) or requires extensive domain knowledge about the applications.
Recognizing this problem, Google developed [Dapper](https://storage.googleapis.com/pub-tools-public-publication-data/pdf/36356.pdf), which popularized the concept of distributed tracing.
In essence, tracing is an specialized form of logging.
First, we add transactional context to logs.
Then, an engine extracts this contextual information, analyzes it to infer causality between events, and stores it in a indexed manner.
Thereby, we are able to reconstruct the journey of requests in the system.

#### three pillars of observability
On the surface, logs, metrics, and traces share many similarities in their lifecycle and components.
Everything starts with instrumentation that captures and emits data.
The data has to have a certain structure, which is defined by a format.
Then, we need a mechanism to collect and forward a piece of telemetry.
Often, there is some kind of agent to further enrich, process and batch data before ingesting it in a backend.
This typically involves a database to efficiently store, index and search large volumes of data.
Finally, there is analysis frontend to make the data accessible to the end-user.
However, in practice, we develop dedicated systems for each type of telemetry, and for good reason:
Each telemetry signal poses its own unique technical challenge.
This is mainly due to the different nature of the data.
The design of data models, interchange formats, and transmission protocols, highly depends on whether you are dealing with un- or semi-structured textual information, compact numerical values inside a time series, or graph-like structures depicting causality between events.
Even for a single signal, there is no consensus on these kinds of topics.
Furthermore, the way we work with and derive insights from telemetry varies dramatically.
A system might need to perform full-text searches, inspect single events, analyze historical trends, visualize request flow, diagnose performance bottlenecks, and more.
These requirements manifest themselves in the design and optimizations of storage,  access patterns, query capabilities and more.
When addressing these technical challenges, [vertical integration](https://en.wikipedia.org/wiki/Vertical_integration) emerges as a pragmatic solution.
In practice, observability vendors narrow the scope of the problem to a single signal and provide instrumentation to generate *and* tools to analyze telemetry, as a single, fully integrated, solution. 

{{< figure src="images/three_pillars_of_observability.drawio.png" width=400 caption="The three pillars of observability, including metrics, traces and logs" >}}

Having dedicated systems for logs, metrics, and traces is why we commonly refer to them as the *three pillars of observability*.
The notion of pillars provides a great mental framework because it emphasizes that:
- there are different categories of telemetry
- each pillar has its own unique strengths and stands on its own
- pillars are complementary / must be combined to form a stable foundation for achieving observability


