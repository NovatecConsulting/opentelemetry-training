+++
title = "Manual instrumentation - Java"
linkTitle = "Manual - Java"
draft = false
weight = 3
+++

Next, let’s look at how to instrument an application by directly using API and SDK packages provided by OpenTelemetry.
In other words, manual instrumentation requires you to make modifications to the source code.
This comes with its fair share of benefits and disadvantages.
The biggest disadvantage is that if you are just starting out, writing manual instrumentation can be daunting because you:
- need to familiarize yourself with OpenTelemetry packages
- must be willing to explore how telemetry signals work under the hood

It's totally reasonable to think that this is asking too much of the developers in your organization.
Another downside could be that you don't want to add observability instrumentation to your code base.
If that's the case, don't worry; OpenTelemetry's instrumentation libraries and auto-instrumentation offer a less invasive approach to generate and emit telemetry.

However, there are also reasons for using manual instrumentation:
- some languages do not support auto-instrumentation
- provides fine-grained control over what and how telemetry gets generated
- you want to make observability part of the development process
- you are a library author or maintainer who wants to offer native instrumentation to your users

So let's get started.


