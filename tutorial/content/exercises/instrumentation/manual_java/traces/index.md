---
title: "Traces"
date: 2023-12-06T09:43:24+01:00
draft: false
weight: 2
---

### Introduction


#### Overview
{{< figure src="images/create_tracer_configure_pipeline.drawio.png" width=800 caption="Overview of OpenTelemetry's tracing signal" >}}
Let's start with a quick recap of the components.
First, there is the tracing API.
It provides the interfaces that we use to add tracing instrumentation to our application.
Second, there is OpenTelemetry's SDK.
It includes a `TracingProvider`, which contains the logic that implements the tracing API.
We'll use it to create a `Tracer` and configure a tracing pipeline (within the SDK).
The application uses the tracer to generate spans.
The tracing pipeline consists of one (or more) [`SpanProcessor`](https://opentelemetry.io/docs/specs/otel/trace/sdk/#span-processor) and  [`SpanExporters`](https://opentelemetry.io/docs/specs/otel/trace/sdk/#span-exporter), which define how spans are processed and forwarded.

This lab exercise demonstrates how to add tracing instrumentation to a Java/Spring Boot application.
The purpose of the exercises is to learn about the anatomy of spans and OpenTelemetry's tracing signal.
It does not provide a realistic deployment scenario.
In this lab, we output spans to the local console to keep things simple and export it to [Jaeger](https://github.com/jaegertracing/jaeger) to show multiple ways of exporting.

## Learning Objectives
By the end of this lab, you will be able to:
- Apply manual instrumentation for tracing to a Java application 
- Use the OpenTelemetry API and configure the SDK to generate spans
- Understand the basic structure of a span
- Enrich spans with additional metadata
- Use the appropriate SDK objects to configure exporting to both console and Jaeger/OTLP

### How to perform the exercises

* This exercise is based on the following repository [repository](https://github.com/NovatecConsulting/opentelemetry-training/) 
* All exercises are in the subdirectory `exercises`. There is also an environment variable `$EXERCISES` pointing to this directory. All directories given are relative to this one.
* Initial directory: `manual-instrumentation-java/initial`
* Solution directory: `manual-instrumentation-java/solution`
* Java/Spring Boot backend component: `manual-instrumentation-java/initial/todobackend-springboot`

The environment consists of one component:
1. a Spring Boot REST API service
    - uses [Spring Boot ](https://www.spring.io) framework
    - listens on port 8080 and serves serveral CRUD style HTTP endpoints
    - simulates an application we want to instrument


To start with this lab, open **two terminals**.
1. Terminal to run the echo server

Navigate to 

```sh
cd $EXERCISES
cd manual-instrumentation-java/initial/todobackend-springboot
```

Run:

```sh
mvn spring-boot:run    
```


2. Terminal to send request to the HTTP endpoints of the service

The directory doesn't matter here

Test the Java app:
```sh
curl -XGET localhost:8080/todos/; echo
```

You should see a response of the following type:
```
[]
```

To keep things concise, code snippets only contain what's relevant to that step.
If you get stuck, you can find the solution in the `exercises/manual-instrumentation-java/solution`

---

### Configure the tracing pipeline and obtain a tracer
The application has not been modified for OpenTelemetry, so we start entirely from scratch.
Before we can make changes to the Java code we need to add some necessary dependencies.

In the first window stop the app using `Ctrl+C` and edit the `pom.xml` file. 
Add the following dependencies. Do not add the dots (...). Just embed the dependencies.


```xml { title="pom.xml" }

	<dependencies>
    ...
        <dependency>
            <groupId>io.opentelemetry</groupId>
            <artifactId>opentelemetry-api</artifactId>
        </dependency>
        <dependency>
            <groupId>io.opentelemetry</groupId>
            <artifactId>opentelemetry-sdk</artifactId>
        </dependency>
        <dependency>
            <groupId>io.opentelemetry</groupId>
            <artifactId>opentelemetry-exporter-logging</artifactId>
        </dependency>
        <dependency>
            <groupId>io.opentelemetry</groupId>
            <artifactId>opentelemetry-exporter-otlp</artifactId>
        </dependency>
        <dependency>
            <groupId>io.opentelemetry.semconv</groupId>
            <artifactId>opentelemetry-semconv</artifactId>
            <version>1.26.0-alpha</version>
        </dependency>
        ...
	</dependencies>
```

Within the same file add the following code snippet

```xml { title="pom.xml" }
<project>
...
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>io.opentelemetry</groupId>
                <artifactId>opentelemetry-bom</artifactId>
                <version>1.40.0</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
...
</project>
```

Within the folder of the main application file `TodobackendApplication.java` add a new file called `OpenTelemetryConfiguration.java`.
We'll use it to separate tracing-related configuration from the main application. The folder is `manual-instrumentation-java/initial/todobackend-springboot/src/main/java/io/novatec/todobackend`. It is recommended to edit the file not via command line, but to use your built-in editor.

Add the following content to this file:

```java { title="OpenTelemetryConfiguration.java" }
package io.novatec.todobackend;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//Basic Otel API & SDK
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.ResourceAttributes;

//Tracing and Spans
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;

import io.opentelemetry.exporter.logging.LoggingSpanExporter;

@SuppressWarnings("deprecation")
@Configuration
public class OpenTelemetryConfiguration {

    @Bean
	public OpenTelemetry openTelemetry(){

		Resource resource = Resource.getDefault().toBuilder().put(ResourceAttributes.SERVICE_NAME, "todobackend").put(ResourceAttributes.SERVICE_VERSION, "0.1.0").build();

		SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
			.addSpanProcessor(SimpleSpanProcessor.create(LoggingSpanExporter.create()))
			.setResource(resource)
			.build();	

		OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
			.setTracerProvider(sdkTracerProvider)
			.build();

		return openTelemetry;
	}

}
```
Explanation about the contents of this file:

This Configuration class will create a Bean to access OpenTelemetry API functionality. 
It is an initial configuration for tracing properties only.

For easy debugging purposes, we'll instantiate a `LoggingSpanExporter` to output spans to the local console.
Next, we have to create a `SpanProcessor` to push generated spans to the SpanExporter.
Here, we can choose between two categories:
- synchronous (i.e. [SimpleSpanProcessor](https://opentelemetry-python.readthedocs.io/en/latest/sdk/trace.export.html#opentelemetry.sdk.trace.export.SimpleSpanProcessor))
    - blocks the program execution to forward spans as soon as they are generated
    - real-time, but means that instrumentation increases latency of requests
- asynchronous (i.e. [BatchSpanProcessor](https://opentelemetry-python.readthedocs.io/en/latest/sdk/trace.export.html#opentelemetry.sdk.trace.export.BatchSpanProcessor))
    - application starts and closes spans, but immediately resumes execution
    - completed spans are maintained in a buffer, a separate thread flushes batches of spans at regular intervals
    - has performance advantages, but spans might be dropped (because the application crashes before spans are exported, spans exceed the buffer capacity)

We created a `SimpleSpanProcessor` and pass the exporter to the `create` method to connect both.

It is recommended to keep this file open in the editor as there will be addition to it over the course of this exercise.


Let’s begin by importing OpenTelemetry’s tracing API and the TracerProvider from the SDK in our main Java application as shown below. 
Open `TodobackendApplication.java` in your editor and tart by addind the following import statements. Place them below the already existing ones:

```java { title="TodobackendApplication.java" }

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;

```

As a next step we reference the bean in the main application.

Add two global variables at the top of the class:

```java { title="TodobackendApplication.java" }
public class TodobackendApplication {

	private Logger logger = LoggerFactory.getLogger(TodobackendApplication.class);

	private OpenTelemetry openTelemetry;
	private Tracer tracer;
```

We'll use constuctor injection, so add the following constructor to the class, too.
In this constuctor we instantiate the OpenTelemetry and Tracer object and make them usable.

```java { title="TodobackendApplication.java" }
	public TodobackendApplication(OpenTelemetry openTelemetry) {
		this.openTelemetry = openTelemetry;
		tracer = openTelemetry.getTracer(TodobackendApplication.class.getName(), "0.1.0");
		
	}
```

At this point it is recommended to rebuild and run the application to verify if all the changes have been applied correctly.

In your main terminal window run:

```sh
mvn spring-boot:run    
```

If there are any errors review the changes and repeat.

### Generate spans

{{< figure src="images/tracer_generates_spans.drawio.png" width=650 caption="Tracing signal" >}}

Now that the application is ready to generate traces let's start focussing on the method to be instrumented.

Locate the `addTodo` method which initially looks like this:

```java { title="TodobackendApplication.java" }
	@PostMapping("/todos/{todo}")
	String addTodo(@PathVariable String todo){

		this.someInternalMethod(todo);
		logger.info("POST /todos/ "+todo.toString());

		return todo;

	}
```

With the help of a tracer, let's generate our first piece of telemetry.
On a high level, we must add instrumentation to our code that creates and finishes spans.
OpenTelemetry's Java implementation provides  ways to do this.

Add two instructions at the beginning and end of the method to start and stop the span.
The resulting code is supposed to look like this:

```java { title="TodobackendApplication.java" }
	@PostMapping("/todos/{todo}")
	String addTodo(@PathVariable String todo){

		Span span = tracer.spanBuilder("addTodo").startSpan();
		
		this.someInternalMethod(todo);
		logger.info("POST /todos/ "+todo.toString());

		span.end();        

		return todo;

	}
```

As you can see we referenced the `tracer` object which was initialized in the constructor and passed a String to the `spanBuilder` method, which will later be the name of the span.

Stop, rebuild and restart the application:

```sh
mvn spring-boot:run    
```

Switch to your other terminal and use the following command to send a request to the `/` endpoint:

```bash
curl -XPOST localhost:8080/todos/NEW; echo
```

This causes the tracer to generate a span object, for which the tracing pipeline writes a logging statement into the application log.
Take a look at the terminal where you application is running.
You should see a log statement similar to the one shown below.

```log
2024-07-21T12:58:04.842Z  INFO 23816 --- [springboot-backend ] [nio-8080-exec-1] i.o.e.logging.LoggingSpanExporter        : 'addTodo' : ba6c894e6774d02d78fe2d48acbdfcc6 72cba2d03eab76a8 INTERNAL [tracer: io.novatec.todobackend.TodobackendApplication:0.1.0] {}
```

This shows that it actually works, however the output is still a bit cryptic.

Let's inspect this from a Java perspective, by writing the state of the object to standard out.

Add the following log statement between the `span.end()` call and the return statement:

```java { title="TodobackendApplication.java" }
		span.end();
		logger.info("Span.toString():"+span.toString());

		return todo;
	}
```

Stop, rebuild and restart the application:

```sh
mvn spring-boot:run    
```

Switch to your other terminal and use the following command to send a request to the `/` endpoint:

```bash
curl -XPOST localhost:8080/todos/NEW; echo
```

Below the logging statement from the `LoggingExporter` you should now see more descriptive details:

```log
2024-07-21T13:05:27.650Z  INFO 23816 --- [springboot-backend ] [nio-8080-exec-1] i.o.e.logging.LoggingSpanExporter        : 'addTodo' : 49fa6e942dd137fdc11ef1178f938078 eeda77d2bfd15a0c INTERNAL [tracer: io.novatec.todobackend.TodobackendApplication:0.1.0] {}
2024-07-21T13:05:27.651Z  INFO 23816 --- [springboot-backend ] [nio-8080-exec-1] i.n.todobackend.TodobackendApplication   : Span.toString():SdkSpan{traceId=49fa6e942dd137fdc11ef1178f938078, spanId=eeda77d2bfd15a0c, parentSpanContext=ImmutableSpanContext{traceId=00000000000000000000000000000000, spanId=0000000000000000, traceFlags=00, traceState=ArrayBasedTraceState{entries=[]}, remote=false, valid=false}, name=addTodo, kind=INTERNAL, attributes=null, status=ImmutableStatusData{statusCode=UNSET, description=}, totalRecordedEvents=0, totalRecordedLinks=0, startEpochNanos=1721567127643127423, endEpochNanos=1721567127650834923}
```

A span in OpenTelemetry represents a single operation within a trace and carries a wealth of information that provides insight into the operation's execution. This includes the `name` of the span, which is a human-readable string that describes the operation. The trace context, consisting of the `traceId`, `spadId`, and `traceState`, uniquely identifies the span within the trace and carries system-specific configuration data. The `SpanKind` indicates the role of the span, such as whether it's an internal operation, a server-side operation, or a client-side operation. If the `parentId` is `null`, it signifies that the span is the root of a new trace. The `startEpochNanos` and `endEpochNanos` timestamps mark the beginning and end of the span's duration. Additionally, spans can contain `attributes` that provide further context, such as HTTP methods or response status codes.
 Other fields like `events`, `links`, and `status` offer additional details about the span's lifecycle, outcome and context.

 This is also tells us a bit more about the output of the LoggingSpanExporter:

 ```log
2024-07-21T13:05:27.650Z  INFO 23816 --- [springboot-backend ] [nio-8080-exec-1] i.o.e.logging.LoggingSpanExporter        : 'addTodo' : 49fa6e942dd137fdc11ef1178f938078 eeda77d2bfd15a0c INTERNAL [tracer: io.novatec.todobackend.TodobackendApplication:0.1.0] {}
 ```

The first identifier is the `traceId`, the second one is `spandId` followed by `SpanKind`.

### Enrich spans with context
{{< figure src="images/enrich_spans_with_context.drawio.png" width=650 caption="enriching spans with resources and attributes" >}}

So far, the contents of the span were automatically generated by the SDK.
This information is enough to reason about the chain of events in a transaction and allows us to measure latency.
However, it's important to understand that tracing is a much more potent tool.
By enriching spans with additional context, traces can proivde meaningful insights about what is happening in a system.

Let's specify the Span kind and set some resource attributes:

Add the setSpanKind invocation to the call, which initializes the new span.
And specify two attributes:

```java { title="TodobackendApplication.java" }
		Span span = tracer.spanBuilder("addTodo").setSpanKind(SpanKind.SERVER).startSpan();
		
		span.setAttribute("http.method", "POST");
		span.setAttribute("http.url", "/todos/{todo}");
```

Stop, rebuild and restart the application:

```sh
mvn spring-boot:run    
```

Switch to your other terminal and use the following command to send a request to the `/` endpoint:

```bash
curl -XPOST localhost:8080/todos/NEW; echo
```

The resulting output will look like:

 ```log
2024-07-24T09:22:07.460Z  INFO 7977 --- [springboot-backend ] [nio-8080-exec-1] i.o.e.logging.LoggingSpanExporter        : 'addTodo' : f150c6404cf8c58398d94bbecb094fdb cd93535232b8d8ca SERVER [tracer: io.novatec.todobackend.TodobackendApplication:0.1.0] AttributesMap{data={http.url=/todos/{todo}, http.method=POST}, capacity=128, totalAddedValues=2}
2024-07-24T09:22:07.461Z  INFO 7977 --- [springboot-backend ] [nio-8080-exec-1] i.n.todobackend.TodobackendApplication   : Span.toString():SdkSpan{traceId=f150c6404cf8c58398d94bbecb094fdb, spanId=cd93535232b8d8ca, parentSpanContext=ImmutableSpanContext{traceId=00000000000000000000000000000000, spanId=0000000000000000, traceFlags=00, traceState=ArrayBasedTraceState{entries=[]}, remote=false, valid=false}, name=addTodo, kind=SERVER, attributes=AttributesMap{data={http.url=/todos/{todo}, http.method=POST}, capacity=128, totalAddedValues=2}, status=ImmutableStatusData{statusCode=UNSET, description=}, totalRecordedEvents=0, totalRecordedLinks=0, startEpochNanos=1721812927453408887, endEpochNanos=1721812927460726137}
```

In this case all span attribute values have been hardcoded. Of course you can also assign values that you retrieve through a Java API directly.
Let's get some details from the `HttpServletRequest` object.

Modify the entire method to look like this:

```java { title="TodobackendApplication.java" }
	@PostMapping("/todos/{todo}")
	String addTodo(HttpServletRequest request, HttpServletResponse response, @PathVariable String todo){

		Span span = tracer.spanBuilder("addTodo").setSpanKind(SpanKind.SERVER).startSpan();

		span.setAttribute("http.method", request.getMethod());
		span.setAttribute("http.url", request.getRequestURL().toString());
		span.setAttribute("client.address", request.getRemoteAddr());
		span.setAttribute("user.agent",request.getHeader("User-Agent"));
		
		this.someInternalMethod(todo);
		logger.info("POST /todos/ "+todo.toString());

		response.setStatus(HttpServletResponse.SC_CREATED);

		span.setAttribute("response.status", HttpServletResponse.SC_CREATED);

		span.end();
		logger.info("Span.toString():"+span.toString());

		return todo;
	}
``` 

If your editor does not automatically add the import statements, you need to do this manually.
Make sure the following import statements are in place:

```java { title="TodobackendApplication.java" }
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
``` 

Restart the app and repeat the curl call.

The resulting output will look like:

 ```log
2024-07-21T13:46:08.336Z  INFO 43453 --- [springboot-backend ] [nio-8080-exec-1] i.o.e.logging.LoggingSpanExporter        : 'addTodo' : 21d97f2813576a1a2942457e9f0c671b 7474ed21e4081af8 SERVER [tracer: io.novatec.todobackend.TodobackendApplication:0.1.0] AttributesMap{data={client.address=127.0.0.1, user.agent=curl/7.81.0, http.url=http://localhost:8080/todos/NEW, response.status=201, http.method=POST}, capacity=128, totalAddedValues=5}
2024-07-21T13:46:08.336Z  INFO 43453 --- [springboot-backend ] [nio-8080-exec-1] i.n.todobackend.TodobackendApplication   : Span.toString():SdkSpan{traceId=21d97f2813576a1a2942457e9f0c671b, spanId=7474ed21e4081af8, parentSpanContext=ImmutableSpanContext{traceId=00000000000000000000000000000000, spanId=0000000000000000, traceFlags=00, traceState=ArrayBasedTraceState{entries=[]}, remote=false, valid=false}, name=addTodo, kind=SERVER, attributes=AttributesMap{data={client.address=127.0.0.1, user.agent=curl/7.81.0, http.url=http://localhost:8080/todos/NEW, response.status=201, http.method=POST}, capacity=128, totalAddedValues=5}, status=ImmutableStatusData{statusCode=UNSET, description=}, totalRecordedEvents=0, totalRecordedLinks=0, startEpochNanos=1721569568329729512, endEpochNanos=1721569568336094221}
```

The trace will now contain attributes from the Servlet request and also details from the response, that have been set throughout the invocation of this method.

### Nested spans

So far the the manual instrumentation has all been taking place within the method `addTodo`. Even though this method invokes another method `someInternalMethod` nothing of that behaviour is being captured by the current isntrumentation.

Let's change that an put 3 statements into your code, 2 for the spans and one additional log.

```java { title="TodobackendApplication.java" }
	String someInternalMethod(String todo){

		Span childSpan = tracer.spanBuilder("someInternalMethod").setSpanKind(SpanKind.INTERNAL).startSpan();

        ...

		logger.info("childSpan.toString():"+childSpan.toString());
		childSpan.end();
		return todo;

	}
```

Again, restart the app and repeat the curl call.

You will now get information from two different spans in your log:

```log
2024-07-21T14:25:21.369Z  INFO 43453 --- [springboot-backend ] [nio-8080-exec-1] i.n.todobackend.TodobackendApplication   : childSpan.toString():SdkSpan{traceId=4824ee335e161b729416d1c3728da0d0, spanId=673a995310fa21b2, parentSpanContext=ImmutableSpanContext{traceId=00000000000000000000000000000000, spanId=0000000000000000, traceFlags=00, traceState=ArrayBasedTraceState{entries=[]}, remote=false, valid=false}, name=someInternalMethod, kind=INTERNAL, attributes=null, status=ImmutableStatusData{statusCode=UNSET, description=}, totalRecordedEvents=0, totalRecordedLinks=0, startEpochNanos=1721571921364073673, endEpochNanos=0}

...

2024-07-21T14:25:21.370Z  INFO 43453 --- [springboot-backend ] [nio-8080-exec-1] i.n.todobackend.TodobackendApplication   : Span.toString():SdkSpan{traceId=2551e1c45eeb37c9ab1bd7a016fa5833, spanId=c955fbcc8f45d28e, parentSpanContext=ImmutableSpanContext{traceId=00000000000000000000000000000000, spanId=0000000000000000, traceFlags=00, traceState=ArrayBasedTraceState{entries=[]}, remote=false, valid=false}, name=addTodo, kind=SERVER, attributes=AttributesMap{data={client.address=127.0.0.1, user.agent=curl/7.81.0, http.url=http://localhost:8080/todos/NEW, response.status=201, http.method=POST}, capacity=128, totalAddedValues=5}, status=ImmutableStatusData{statusCode=UNSET, description=}, totalRecordedEvents=0, totalRecordedLinks=0, startEpochNanos=1721571921363715964, endEpochNanos=1721571921370347506}
```

The interesting part in both spans is the followig part:

```log
parentSpanContext=ImmutableSpanContext{traceId=00000000000000000000000000000000, spanId=0000000000000000
```

The parent span will always be `null` with a new call. However here we have a relation between the two calls, so it is surprising that the child span (or let's say the one we know is the chaild span) has this setting as well. So from the perspective of OpenTelemetry these are two totally independent spans.

We need to use the OpenTelemetry context scope. Embed the call to the child method `someInternalMethod` with the following block:

```java { title="TodobackendApplication.java" }
		try (Scope scope = span.makeCurrent()) {
			this.someInternalMethod(todo);
		} finally {
			span.end();
		}
```

Also make sure the following import exists:

```java { title="TodobackendApplication.java" }
import io.opentelemetry.context.Scope;
```

Build, run and curl again.

```log
2024-07-21T15:11:10.327Z  INFO 43453 --- [springboot-backend ] [nio-8080-exec-1] i.n.todobackend.TodobackendApplication   : childSpan.toString():SdkSpan{traceId=4c561f212ee8a152663f960490dac269, spanId=aa5935fc54da79da, parentSpanContext=ImmutableSpanContext{traceId=4c561f212ee8a152663f960490dac269, spanId=c630f6e5a0cde90c, traceFlags=01, traceState=ArrayBasedTraceState{entries=[]}, remote=false, valid=true}, name=someInternalMethod, kind=INTERNAL, attributes=null, status=ImmutableStatusData{statusCode=UNSET, description=}, totalRecordedEvents=0, totalRecordedLinks=0, startEpochNanos=1721574670304760501, endEpochNanos=0}
``

2024-07-21T15:11:10.327Z  INFO 43453 --- [springboot-backend ] [nio-8080-exec-1] i.n.todobackend.TodobackendApplication   : Span.toString():SdkSpan{traceId=4c561f212ee8a152663f960490dac269, spanId=c630f6e5a0cde90c, parentSpanContext=ImmutableSpanContext{traceId=00000000000000000000000000000000, spanId=0000000000000000, traceFlags=00, traceState=ArrayBasedTraceState{entries=[]}, remote=false, valid=false}, name=addTodo, kind=SERVER, attributes=AttributesMap{data={client.address=127.0.0.1, user.agent=curl/7.81.0, http.url=http://localhost:8080/todos/NEW, http.method=POST}, capacity=128, totalAddedValues=4}, status=ImmutableStatusData{statusCode=UNSET, description=}, totalRecordedEvents=0, totalRecordedLinks=0, startEpochNanos=1721574670300677293, endEpochNanos=1721574670327453585}
```

If you look at the `someInternalMethod`  span first and focus on the parent span context, you will see:

```log
parentSpanContext=ImmutableSpanContext{traceId=4c561f212ee8a152663f960490dac269, spanId=c630f6e5a0cde90c
```

which is exactly the trace and span id of the `addTodo` method.

The OpenTelemetry API offers also an automated way to propagate the parent span to child spans. This works however only, if they run within the same thread.

### Handling an error

The `someInternalMethod` can simulate an error behaviour and throw an exception, if somebody uses the todo with name `fail`. 

```java { title="TodobackendApplication.java" }
		if(todo.equals("fail")){

			System.out.println("Failing ...");
			throw new RuntimeException();
			
		} 
```

We can catch this exception in the `addTodo` method.

Extend the `try{}` block we created in the previous step with the following code:

```java { title="TodobackendApplication.java" }
		try (Scope scope = span.makeCurrent()) {
			this.someInternalMethod(todo);
			response.setStatus(HttpServletResponse.SC_CREATED);
			span.setAttribute("response.status", HttpServletResponse.SC_CREATED);
		} catch (Throwable t) {
			span.setStatus(StatusCode.ERROR, "Error on server side!");
			span.recordException(t);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			span.setAttribute("response.status", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		} finally {
			span.end();
		}
```

Restart the app. 

This time execute the curl call with the todo triggering a failure.

```bash
curl -XPOST localhost:8080/todos/fail; echo
```

If you look at the output log now, you can see the error status in the parent span.

```log
2024-07-21T16:01:27.683Z  INFO 70461 --- [springboot-backend ] [nio-8080-exec-1] i.n.todobackend.TodobackendApplication   : Span.toString():SdkSpan{traceId=29b65aa14526263d1a74c117dbbf7ea8, spanId=6296d1d4ba880147, parentSpanContext=ImmutableSpanContext{traceId=00000000000000000000000000000000, spanId=0000000000000000, traceFlags=00, traceState=ArrayBasedTraceState{entries=[]}, remote=false, valid=false}, name=addTodo, kind=SERVER, attributes=AttributesMap{data={http.method=POST, http.url=http://localhost:8080/todos/fail, client.address=127.0.0.1, user.agent=curl/7.81.0, response.status=500}, capacity=128, totalAddedValues=5}, status=ImmutableStatusData{statusCode=ERROR, description=Error on server side!}, totalRecordedEvents=1, totalRecordedLinks=0, startEpochNanos=1721577687675216250, endEpochNanos=1721577687683172166}
```

### Adding events



### Exporting traces via OTLP

So far everything we collected as tracing information has been processed by the pipeline we defined in the `OpenTelemetryConfiguration` class.
This configures the pipeline to use `SimpleSpanProcessor` in combination with `LoggingSpanExporter`.

This time we want to export in OTLP format to a gRPC receiving endpoint. The `OtlpGrpcSpanExporter` can help us here:

Modify the beginning of the class to the code shown below:

```java { title="OpenTelemetryConfiguration.java" }
	public OpenTelemetry openTelemetry(){

		Resource resource = Resource.getDefault().toBuilder().put(ResourceAttributes.SERVICE_NAME, "todobackend").put(ResourceAttributes.SERVICE_VERSION, "0.1.0").build();

		OtlpGrpcSpanExporter jaegerOtlpExporter =
        OtlpGrpcSpanExporter.builder()
            .setEndpoint("http://localhost:4317")
            .setTimeout(30, TimeUnit.SECONDS)
            .build();

		SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
			.addSpanProcessor(SimpleSpanProcessor.create(LoggingSpanExporter.create()))
			.addSpanProcessor(SimpleSpanProcessor.create(jaegerOtlpExporter))
			.setResource(resource)
			.build();	
```

Also make sure the following import exists:

```java { title="OpenTelemetryConfiguration.java" }
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
```



As you can see we created an instance of `OtlpGrpcSpanExporter` called `jaegerOtlpExporter` and configured it to send the data to `http://localhost:4317`.

In the Tracer Provider we just added another `addSpanProcessor` call to the already existing one. OpenTelemetry is able to handle mulitple different and parallel processors.

Rebuild, restart and issue a curl call. 

```sh
mvn spring-boot:run    
```

```bash
curl -XPOST localhost:8080/todos/NEW; echo
```

Besides the familiar logging statements, you will see two errors in the logs now:

```log
2024-07-21T16:18:20.179Z  WARN 70461 --- [springboot-backend ] [alhost:4317/...] i.o.exporter.internal.grpc.GrpcExporter  : Failed to export spans. Server responded with gRPC status code 2. Error message: Failed to connect to localhost/[0:0:0:0:0:0:0:1]:4317
2024-07-21T16:18:20.179Z  WARN 70461 --- [springboot-backend ] [alhost:4317/...] i.o.exporter.internal.grpc.GrpcExporter  : Failed to export spans. Server responded with gRPC status code 2. Error message: Failed to connect to localhost/[0:0:0:0:0:0:0:1]:4317
```

This is because there is nothing listening on `http://localhost:4317`. 

Open another terminal window and start a docker container like this:

```sh
docker run -d --name jaeger \
  -e COLLECTOR_OTLP_ENABLED=true \
  -p 16686:16686 \
  -p 4317:4317 \
  jaegertracing/all-in-one
```

After this container has started, execute a couple of traces and investigate the details in the Jaeger web console `http://localhost:16686`

The exercise completes with this step.


<!--  
#### Semantic conventions

Looking at the previous example, nothing prevents a developer from using arbitrary keys for resource attributes (e.g. `version` instead of `service.version`).
While human intuition allows us to conclude that both refer to the same thing, machines are (luckily) not that smart.
Inconsistencies in the definition of telemetry make it harder to analyze the data down the line.
This is why OpenTelemetry's specification includes [semantic conventions](https://opentelemetry.io/docs/specs/semconv/).
This standardization effort helps to improve consistency, prevents us from making typos, and avoids ambiguity due to differences in spelling.
OpenTelemetry provides a Python package `opentelemetry-semantic-conventions` that was already installed as a dependency.

Let's refactor our code.
Start by specifying imports at the top of the file and use `ResourceAttributes` within the `create_resource` function.

```py { title="resource_utils.py" }
from opentelemetry.semconv.resource import ResourceAttributes

def create_resource(name: str, version: str) -> Resource:
    svc_rc = Resource.create(
        {
            ResourceAttributes.SERVICE_NAME: name,
            ResourceAttributes.SERVICE_VERSION: version,
        }
    )
    return svc_rc
```


Explore the [documentation](https://opentelemetry.io/docs/specs/semconv) to look for a convention that matches the metadata we want to record.
The specification defines conventions for various aspects of a telemetry system (e.g. different telemetry signals, runtime environments, etc.).
Due to the challenges of standardization and OpenTelemetry's strong commitment to long-term API stability, many conventions are still marked as experimental.
For now, we'll use [this](https://opentelemetry.io/docs/specs/semconv/resource/#service) as an example.
Instead of specifying the attributes keys (and sometimes values) by typing their string by hand, we reference objects provided by OpenTelemetry's `semconv` package.

If you repeat the `curl` invocation and observe the trace you won't see a difference there. Just the code is now cleaner as the attributes are being used from a standardized package instead of being hard-coded and hence prone to break when there are future changes.

#### SpanAttributes

Aside from static resources, spans can also carry dynamic context through `SpanAttributes`.
Thereby, we can attach information specific to the current operation to spans.

Modify `app.py` to match the following code snippet.

```py { title="app.py" }
from flask import Flask, make_response, request

from opentelemetry import trace as trace_api
from opentelemetry.semconv.trace import SpanAttributes

@app.route("/")
@tracer.start_as_current_span("index")
def index():
    span = trace_api.get_current_span()
    span.set_attributes(
        {
            SpanAttributes.HTTP_REQUEST_METHOD: request.method,
            SpanAttributes.URL_PATH: request.path,
            SpanAttributes.HTTP_RESPONSE_STATUS_CODE: 200,
        }
    )
    do_stuff()
    ...
```

Make sure the application is still running after the changes. Otherwise restart it and redo the curl call.
```bash
curl -XGET localhost:5000; echo
```

The response will onw contain span attributes.

```json
"attributes": {
    "http.request.method": "GET",
    "url.path": "/",
    "http.response.status_code": 200
}
```

To illustrate this, let's add the request method, path and response code to a span.
Call `get_current_span` to retrieve the active span.
Then, we can use `set_attributes` to pass a dictionary of key / value pairs.
The semantic conventions related to [tracing](https://opentelemetry.io/docs/specs/semconv/general/trace/) standardize many attributes related to the [HTTP protocol](https://opentelemetry.io/docs/specs/semconv/http/http-spans/).
To access information like request method, headers, and more, we import `request` from the flask package.
Flask creates a [request context](https://flask.palletsprojects.com/en/2.3.x/reqcontext/) for each incoming HTTP request and tears it down after the request is processed.
-->

<!--  

### Context propagation

{{< figure src="images/trace_propagation.drawio.png" width=650 caption="trace context propagation" >}}

Until now, we only looked at the contents of a singular span.
However, at its core, distributed tracing is about understanding the chain of operations that make up a transaction.
To construct a trace, we need to tie individual spans together.
To do that, we must pass contextual metadata between adjacent operations, which are separated by a logical boundary.
OpenTelemetry calls this information `SpanContext`.
The boundary could be in-process or a network.
As previously seen, the tracing SDK automatically generates a `SpanContext` for us.

Let's investigate the current behavior of the application.
In `app.py` add a decorator to the `do_stuff` function to create a new span.


```py { title="app.py" }
@tracer.start_as_current_span("do_stuff")
def do_stuff():
    # ...
```

Curl the `/` endpoint and look at the output.

```json
{
    "name": "do_stuff",
    "context": {
        "trace_id": "0xbd57d76b8a29e9ff6ee1b016cf3a6d8b",
        "span_id": "0x7c6f99cad316bafe",
        "trace_state": "[]"
    },
    "parent_id": "0xed291d97f9c00a5a",
}
```
```json
{
    "name": "index",
    "context": {
        "trace_id": "0xbd57d76b8a29e9ff6ee1b016cf3a6d8b",
        "span_id": "0xed291d97f9c00a5a",
        "trace_state": "[]"
    },
    "parent_id": null,
}
```

You should now see two span objects being generated.
If we compare the `index` and `do_stuff` span we see that:
- both share the same `trace_id`.
- the `parent_id` of the `do_stuff` span matches the `span_id` of the `index` span

In other words, `do_stuff` is a child span of `index`.
The tracing SDK automagically handles context propagation within the same process.
Let's see what happens when there is a network boundary in between.

#### Inject trace context into outgoing requests

For testing purposes, the lab environment includes a `echo` server that returns the HTTP requests back to the client.
This is useful because it lets us examine what requests leaving our application look like.
`do_stuff` uses the `requests` package to send a get request to `echo`.
To examine what happens, let's add some `print` statements to look at response.

Modify `app.py` according to the snippet below:

```py { title="app.py" }
import json # <-- to pretty print response

@tracer.start_as_current_span("do_stuff")
def do_stuff():
    # ...
    response = requests.get(url)

    # debug
    print("Headers included in outbound request:")
    print(json.dumps(response.json()["request"]["headers"], indent=2))
    # ...
```

After restarting the webserver and using curl to send a request to the `/` endpoint, you should see the output from our print statements.
This will be printed out before the two spans

```bash
Headers included in outbound request:
{
  "host": "echo:6000",
  "user-agent": "python-requests/2.31.0",
  "accept-encoding": "gzip, deflate",
  "accept": "*/*",
  "connection": "keep-alive"
}
```

The request didn't include any tracing headers!
If we do not inject SpanContext into outgoing requests, remote services have no information that the incoming request is part of an ongoing trace.
Therefore, the tracing instrumentation decides to start a new trace by generating a new, but disconnected, `SpanContext`.
The context propagation across services is currently broken.

OpenTelemetry's data transmission system includes the concept of [propagators](https://opentelemetry-python.readthedocs.io/en/latest/api/propagate.html).
Propagators serialize context, so it can traverse the network along with the request.
Let's import the [`inject`](https://opentelemetry-python.readthedocs.io/en/latest/api/propagate.html#opentelemetry.propagate.inject) function from OpenTelemetry's `propagate` API.
Create an empty dictionary and `inject` the headers from the current context into it.
All we have to do now is to include this context in outgoing requests.
The requests `get` method has a second argument that allows us to pass in information that will be sent as request headers.

Modify `app.py` again according to the snippet below:
```py { title="app.py" }
from opentelemetry.propagate import inject

@tracer.start_as_current_span("do_stuff")
def do_stuff():
    headers = {}
    inject(headers)
    # ...
    response = requests.get(url, headers=headers) # <- inject trace context into headers
    # ...
```

```bash
Headers included in outbound request:
{
  "traceparent": "00-69956358ff13fc2254725b20ad216aa9-4e3d02041cf8e504-01",
  # ...
}
```

Re-run our previous experiment with the `echo` server and look at the output of the print statement.
You should see that the outgoing request now contains a header called `traceparent`.
If you have prior experience with distributed tracing, you might already know that different tracing systems use different formats (such as [b3 propagation](https://github.com/openzipkin/b3-propagation) used by Zipkin).

{{< figure src="images/doordash_traceparent_header.png" width=650 caption="format used by the traceparent header [Gud21](https://doordash.engineering/2021/06/17/leveraging-opentelemetry-for-custom-context-propagation/)" >}}

By default, OpenTelemetry uses a [specification](https://www.w3.org/TR/trace-context/) established by the World Wide Web Consortium (W3C).
Looking at the header's value, we can infer that it encodes the trace context as `<spec version>-<trace_id>-<parent_id>-<trace flag>`.

#### Continue an existing trace

Let's switch perspective.
Imagine that our Python application is a remote service that we send a request to.
In this scenario, the service must recognize that the incoming request is part of an ongoing trace.
To simulate this, let's add the `start_as_current_span` decorator to the `/users` endpoint of the application.

```py { title="app.py" }
@app.route("/users", methods=["GET"])
@tracer.start_as_current_span("users")
def get_user():
    # ...
```

Use the `curl` command shown to send a request with a fictional tracing header to the application.

```sh
curl -XGET "localhost:5000/users" --header "traceparent: 00-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa-00ffffffffffffff-01"
```

If you look at output, you will notice that the tracer generated a span with a random `trace_id` and no `parent_id`.
But this is not the behaviour what we want!
Recall that `start_as_current_span` instantiates spans based on the current context of the tracer.
Apparently, the local context doesn't include information about the traceparent header in inbound requests.


Flask provides a special `before_request` decorator to execute a function before the normal request handler runs.
Create a function `before_request_func`.
We'll use it to create a custom context, which subsequently gets picked up by `start_as_current_span`.
Import [`extract`](https://opentelemetry-python.readthedocs.io/en/latest/api/propagate.html#opentelemetry.propagate.extract) function from `propagate`.
Construct a new Context object by passing the request headers as an argument.
Call the [`attach`](https://opentelemetry-python.readthedocs.io/en/latest/api/context.html#opentelemetry.context.attach) function to set a new active context.
The function returns a string token that references the context before we call attach.
Store it on Flask's request context, so we can retrieve it later.

Flask's `teardown_request` request decorator allows us to register a callback function that gets executed at the end of each request.
We'll use it to restore the previous context.

Retrieve the token from Flask's request context and pass it as an argument to [`detach`](https://opentelemetry-python.readthedocs.io/en/latest/api/context.html#opentelemetry.context.detach).

Add the import statements and the 2 functions to the code:

```py { title="app.py" }
from opentelemetry import context
from opentelemetry.propagate import inject, extract

@app.teardown_request
def teardown_request_func(err):
    previous_ctx = request.environ.get("previous_ctx_token", None)
    if previous_ctx:
        context.detach(previous_ctx)

@app.before_request
def before_request_func():
    ctx = extract(request.headers)
    previous_ctx = context.attach(ctx)
    request.environ["previous_ctx_token"] = previous_ctx
```

Make sure the app is up and running. After that repeat `curl` call:

```sh
curl -XGET "localhost:5000/users" --header "traceparent: 00-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa-00ffffffffffffff-01"
```

Take a look at the span generated by the application.
Finally!
The context propagation is working as expected!

```json
{
    "name": "users",
    "context": {
        "trace_id": "0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
        "span_id": "0x48a97a9fd5588724",
        "trace_state": "[]"
    },
    "parent_id": "0x00ffffffffffffff"
}
```

The service should recognize the tracing header of the incoming request and create spans related to the transaction.
Let's verify our work.

If we were to export spans to a tracing backend, it could analyze the `SpanContext` of the individual objects and piece together a distributed trace.

<!--
## quiz

{{< quizdown >}}

---
primary_color: orange
secondary_color: lightgray
text_color: black
shuffle_questions: false
---

### The OpenTelemetry specification includes semantic conventions to standardize the naming of telemetry attributes.

- [ ] True
> True. OpenTelemetry's semantic conventions provide a standardized way to name and describe telemetry attributes, which helps improve consistency and interoperability across different telemetry systems.
- [ ] False


### Which of the following is NOT a standard HTTP attribute in OpenTelemetry's semantic conventions?

- [ ] http.request.method
- [ ] http.response.status_code
- [ ] http.request.url
- [ ] http.request.headers
> http.request.headers. While headers are an important part of HTTP requests, they are not standardized as a single attribute in OpenTelemetry's semantic conventions.

### The OpenTelemetry SDK provides a `BatchSpanProcessor` to improve performance by batching span exports.

- [ ] True
> True. The `BatchSpanProcessor` in the OpenTelemetry SDK is designed to improve performance by buffering spans and exporting them in batches, reducing the overhead of individual span exports.
- [ ] False

### Which of the following is a common use case for distributed tracing?

- [ ] Monitoring the performance of a single service
- [ ] Tracking the flow of data through a system
> Tracking the flow of data through a system. Distributed tracing is particularly useful for understanding the behavior of a system across multiple services and components, which helps in diagnosing issues and optimizing performance.
- [ ] Ensuring that all services are running the same version of a library
- [ ] Managing the configuration of a distributed system

{{< /quizdown >}}

## optional tasks

#### ResourceDetectors

```py { title="resource_utils.py" }
import socket
from opentelemetry.sdk.resources import Resource, ResourceDetector
from opentelemetry.semconv.resource import ResourceAttributes

class HostDetector(ResourceDetector):
    def detect(self) -> Resource:
        return Resource.create(
            {
                ResourceAttributes.HOST_NAME: socket.gethostname(),
            }
        )

def create_resource(name: str, version: str) -> Resource:
    # ...
    host_rc = HostDetector().detect()
    rc = host_rc.merge(svc_rc)
    return rc
```


A `ResourceDetector` is a way to bundle and automate the discovery and extraction of such information in a plugin.
This makes it trivial to share it across different telemetry signals and services.
To implement it simply inherit from the `ResourceDetector` class and override the `detect` method.
Finally, we simply call the `merge` method on the `Resource` object, which combines both and returns a new object.

-->
