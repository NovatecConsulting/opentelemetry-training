---
title: "Why is OpenTelemetry promising?"
linktitle: "Goals of OpenTelemetry"
draft: false
weight: 30
---

<!-- ### history

OpenTelemetry is the result of the merger from OpenTracing and OpenCensus. Both of these products had the same goal - to standardize the instrumentation of code and how telemetry data is sent to observability backends. Neither of the products could solve the problem independently, so the CNCF merged the two projects into OpenTelemetry. This came with two major advantages. One both projects joined forces to create a better overall product and second it was only one product and not several products. With that standardization can be reached in a wider context of telemetry collection which in turn should increase the adoption rate of telemetry collection in applications since the entry barrier is much lower. The CNCF describes OpenTelemetry as the next major version of OpenTracing and OpenCensus and as such there are even migration guides for both projects to OpenTelemetry. 

-->

<!-- ### promises -->
At the time of writing, OpenTelmetry is the [second fastest-growing project](https://www.cncf.io/reports/cncf-annual-report-2023/#projects) within the CNCF.
OpenTelemetry receives so much attention because it promises to be a fundamental shift in the way we produce telemetry.
It's important to remember that observability is a fairly young discipline.
In the past, the rate of innovation and conflicts of interest prevented us from defining widely adopted standards for telemetry. <!-- quote -->
However, the timing and momentum of OpenTelemetry appear to have a realistic chance of pushing for standardization of common aspects of telemetry.

#### Instrument once, use everywhere
A key promise of OpenTelemetry is that you *instrument code once and never again* and the ability *to use that instrumentation everywhere*.
OpenTelemetry recognizes that, should its efforts be successful, it will be a core dependency for many software projects.
Therefore, it follows strict processes to provide [*long-term stability guarantees*](https://opentelemetry.io/docs/specs/otel/versioning-and-stability/).
Once a signal is declared stable, the promise is that clients will never experience a breaking API change.

#### Separate telemetry generation from analysis
Another core idea of OpenTelemetry is *separate the mechanisms that produce telemetry from the systems that analyzes it*.
Open and vendor-agnostic instrumentation marks a fundamental *change in the observability business*.
Instead of pouring resources into building proprietary instrumentation and keeping it up to date, vendors must differentiate themselves through feature-rich analysis platforms with great usability.
OpenTelemetry *fosters competition*, because users no longer stuck with the observability solution they chose during development.
After switching to OpenTelemetry, you can move platforms without having to re-instrument your entire system.

#### Make software observable by default
With OpenTelemetry, open-source developers are able to add *native instrumentation to their projects without introducing vendor-specific code* that burdens their users.
The idea is to *make observability a first-class citizen during development*.
By having software ship with built-in instrumentation, we no longer need elaborate mechanisms to capture and integrate it after the fact.

#### Improve how we use telemetry
Last (and definitely not least), OpenTelemetry tries to change how we think about and use telemetry.
Instead of having three separate silos for logs, metrics, and traces, OpenTelemetry follows a paradigm of linking telemetry signals together.
With context creating touch points between signals, the overall value and usability of telemetry increase drastically.
For instance, imagine the ability to jump from conspicuous statistics in a dashboard straight to the related logs.
Correlated telemetry data helps to reduce the cognitive load on humans operating complex systems.
Being able to take advantage of linked data will mark a new generation of observability tools.
