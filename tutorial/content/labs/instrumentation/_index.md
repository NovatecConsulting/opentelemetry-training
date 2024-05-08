+++
title = "instrumentation"
draft = false
weight = 20
+++

<!-- 
- can be accomplished in two ways
  - automatic
    - collected within a library or framework
    - will yield a standard set of telemetry data that can be used to getting started quickly with observability
    - is either already added to a library or framework by the authors or can be added using agents
  - manual
    - more specific telemetry data can be generated
    - source code has to modified most of the time
    - allows for for greater control to collect more specific telemetry data that is tailored to your needs. 

The benefit of instrumenting code with OpenTelemetry to collect telemetry data is that the correlation of the previously mentioned signals is simplified since all signals carry metadata. 
Correlating telemetry data enables you to connect and analyze data from various sources, providing a comprehensive view of your system's behavior. 
By setting a unique correlation ID for each telemetry item and propagating it across network boundaries, you can track the flow of data and identify dependencies between different components. 
OpenTelemetry's trace ID can also be leveraged for correlation, ensuring that telemetry data from the same request or transaction is associated with the same trace. 
Correlation engines can further enhance this process by matching data based on correlation IDs, trace IDs, or other attributes like timestamps, allowing for efficient aggregation and analysis. 
Correlated telemetry data provides valuable insights for troubleshooting, performance monitoring, optimization, and gaining a holistic understanding of your system's behavior. 
In the labs' chapter you will see how correlated data looks like. 
Traditionally this had to be done by hand or just by timestamps which was a tedious task.
-->