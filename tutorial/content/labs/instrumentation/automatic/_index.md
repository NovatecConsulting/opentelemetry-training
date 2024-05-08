+++
title = "auto-instrumentation"
menuTitle = "automatic"
draft = false
weight = 2
+++

As illustrated by the previous labs, manual instrumentation of an application using the API and SDK can be a labor-intensive process.
The effort and time required to re-instrument a code base are often deterrents to getting started.
While one factor is the financial cost, the prospect of extensive code modifications and having to learn another telemetry framework is often overwhelming to developers.
Moreover, choosing what and knowing how to instrument is often anything but trivial and can take considerable experience.
Adopting new technologies tends to be (relatively) easy in green field environments.
In reality, things often get more complicated in the face of large amounts of legacy code.
One factor may simply be the volume of code that needs to be instrumented.
Another obstacle is that good instrumentation often requires understanding of the application.
However, it may be the case that the original author of a piece of code is no longer around.
In summary, even though OpenTelemetry promises to "instrument once and never again", for some, the cost will still be too high.
Recognizing these burdens, OpenTelemetry tries to simplify adoption as much as possible and allows incremental migration strategies.
This section explores how OpenTelemetry uses instrumentation libraries and auto-instrumentation to generate predefined metrics, traces and logs with close to zero code changes.