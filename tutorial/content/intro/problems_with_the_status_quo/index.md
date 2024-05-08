---
title: "Problems with our approach to observability"
linktitle: "Problems with status quo"
draft: false
weight: 20
---

With loads of open-source and commercial observability solutions on the market, you might (rightly) ask yourself: 
- Why is there so much hype around OpenTelemetry?
- If there are plenty of mature solutions for generating, collecting, storing, and analyzing logs, metrics and traces, why should I care?
- What's wrong with the current state of observability?
- Oh, great ... is this yet another attempt at standardization?

These are valid questions.
To answer them, we must identify (some) downsides that result from building and working with *pillar-based* observability systems.

#### siloed telemetry is difficult to work with
<!--
- need data that serves the needs of the people who build and run systemsP
- overwhelming amount of data
- Outages take longer to detect, diagnose, and remediate
-->
{{< figure src="images/need_for_correlated_telemetry.drawio.png" width=700 caption="The need for correlated telemetry [[Young21]](https://www.oreilly.com/library/view/the-future-of/9781098118433/)" >}}

First, there are deficits in the *quality* of telemetry data.
To illustrate this, let's imagine that we want to investigate the root cause of a problem.
The first indicator of a problem is usually an alert or an anomaly in a metrics dashboard.
To confirm the incident is worth investigating, we have to form an initial hypothesis.
The only information we currently have is that something happened at a particular point in time.
Therefore, the first step is to use the metrics system to look for other metrics showing temporally correlated, abnormal behavior.
After making an educated guess about the problem, we want to drill down and investigate the root cause of the problem.
To gain additional information, we typically switch to the logging system.
Here, we write queries and perform extensive filtering to find log events related to suspicious metrics.
After discovering log events of interest, we often want to know about the larger context the operation took place in.
Unfortunately, traditional logging systems lack the mechanisms to reconstruct the chain of events in that particular transaction.
Traditional logging systems often fail to capture the full context of an operation, making it difficult to correlate events across different services or components. 
They frequently lack the ability to preserve critical metadata, such as trace IDs or span IDs, which are essential for linking related events together. This limitation results in fragmented views of the system's behavior, where the story of a single operation is spread across multiple logs without a clear narrative. Furthermore, the absence of standardized query languages or interfaces adds to the difficulty of searching and analyzing logs effectively, as operators must rely on custom scripts or manual filtering to uncover patterns and anomalies.
Switching perspectives from someone building an observability solution to someone using it reveals an inherent disconnect.
The real world isn't made up of logging, metrics, or tracing problems.
Instead, we have to move back and forth between different types of telemetry to build up a mental model and reason about the behavior of a system.
Since observability tools are silos of disconnected data, figuring out how pieces of information relate to one another causes a significant cognitive load for the operator.

#### lack of instrumentation standard leads to low quality data
<!-- 
- dozens of tools to collect a variety of signals in different formats on varying cadences
- data is inconsistent -> difficult to analyse
- report the same thing in different ways
-->

Another factor that makes root-cause analysis hard is that telemetry data often suffers from a lack of consistency. This leads to difficulties in correlating events across different services or components, as there is no standardized way to identify related events, such as through trace IDs or span IDs. Additionally, there is no straightforward method to integrate multiple solution-specific logging libraries into a coherent system, resulting in fragmented and disjointed views of the system's behavior.

#### no built-in instrumentation in open-source software
Let's look at this from the perspective of open-source software developers.
Today, most applications are built on top of open-source libraries, frameworks, and standalone components.
With a majority of work being performed outside the business logic of the application developer, it is crucial to collect telemetry from open-source components.
The people with the most knowledge of what is important when operating a piece of software are the developers and maintainers themselves.
However, there is currently no good way to communicate through native instrumentation.
One option would be to pick the instrumentation of an observability solution.
However, this would add additional dependencies to the project and force users to integrate it into their systems.
While running multiple logging and metrics systems is impractical but technically possible, tracing is outright impossible as it requires everyone to agree on a standard for trace context propagation to work.
A common strategy for solving problems in computer science is to add a layer of indirection.
Instead of embedding vendor-specific instrumentation, open-source developers often provide observability hooks.
This allows users to write adapters that connect the open-source component to their observability system.
While this approach provides greater flexibility, it also has its fair share of problems.
For example, whenever there is a new version of software, users have to notice and update their adapters.
Moreover, the indirection also increases the overhead, as we have to convert between different telemetry formats.

#### combining telemetry generation with results in vendor lock-in
<!--

- vertical integration means that instrumentation, protocols, and interchange formats are tied to a specific solution.

- cost of managed platforms can be extremely high
- expensive migration process
- stuck with the features of that platform

-->

Let's put on the hat of an end user.
After committing to a solution, the application contains many solution-specific library calls throughout its codebase.
To switch to another observability tool down the line, we would have to rip out and replace all existing instrumentation and migrate our analysis tooling.
This up-front cost of re-instrumentation makes migration difficult, which is a form of vendor lock-in.

#### struggleing observability vendors / high barrier for entry
<!-- 
- status quo of telemetry is not a sustainable one. 
-->
The last part of the equation is the observability vendors themselves.
At first glance, vendors appear to be the only ones profiting from the current situation.
In the past, high-quality instrumentation was a great way to differentiate yourself from the competition.
Moreover, since developing integrations for loads of pre-existing software is expensive, the observability market has a relatively high barrier to entry.
With customers shying away from expensive re-instrumentation, established vendors faced less competition and pressure to innovate.
However, they are also experiencing major pain points.
The rate at which software is being developed has increased exponentially over the last decade.
Today's heterogeneous software landscape makes it impossible to maintain instrumentation for every library, framework, and component.
As soon as you start struggling with supplying instrumentation, customers will start refusing to adopt your product.
As a result, solutions compete over who can build the best n-to-n format converter instead of investing these resources into creating great analysis tools.
Another downside is that converting data that was generated by foreign sources often leads to a degradation in the quality of telemetry.
Once data is no longer well-defined, it becomes harder to analyze.