---
title: "auto-instrumentation"
draft: false
weight: 3
---

Instrumentation libraries simplify the experience of adopting OpenTelemetry by injecting instrumentation into popular third-party libraries and frameworks.
This is especially useful in situations where we don't want to write manual instrumentation, but no native instrumentation is available.
Since instrumentation libraries are developed and maintained by members of the OpenTelemetry community, we
- don't have to wait for native instrumentation
- don't put burden on the back of the library or framework maintainer

A common way to take advantage of instrumentation libraries is in combination with OpenTelemetry's auto-instrumentation.
In contrast to the API and SDK, auto-instrumentation allows us to dynamically inject observability into the application without having to make changes to the source code.

Generally speaking, auto-instrumentation is implemented by some kind of agent or runner.
In this lab, we'll use a Java application to understand what this could look like.

### byte code manipulation via Java agent
```java
public class MyAgent {
    public static void premain(String agentArgs, Instrumentation inst) {
        inst.addTransformer(new MyTransformer());
    }
}

public class MyTransformer implements ClassFileTransformer {
    @Override
    public byte[] transform(
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain,
            byte[] classfileBuffer
    ) {
        // Perform bytecode transformation
        // ...

        // Return the modified bytecode
        return modifiedBytecode;
```

You'll likely know that `main` method is the entry point of Java program.
In addition to that, the Java Virtual Machine (JVM) also supports two other types of entry points: `premain` and `agentmain`.
Both `premain` and `agentmain` have an optional parameter to pass an `Instrumentation` instance as an argument to the method.
Java's built-in Instrumentation interface provides access to low-level functionality of the JVM.
It operates on a byte code level and provides mechanisms to modify and inspect the behavior of Java applications at runtime.
Most notables, the `ClassFileTransformer` API allows you to take a class file (basically a compilation unit of a .java file) and manipulates the byte array before it is loaded.
Instead of trying to identify classes and methods and edit the byte code directly, we typically use libraries such as [Byte Buddy](https://bytebuddy.net/) that make these byte code transformations more convenient.
With the help of these tools, we are able to develop static and dynamic Java agents.
On a high level, auto-instrumentation agents of different APM vendors (e.g. Instana, ...) work similarly.
On startup, the agent discover what clients (e.g. JDBC driver, HTTP Client) are used by the application and decides whether to instrument them.
After identifying the methods of interest (e.g. that do the HTTP calls), a transformer rewrites the byte array to inject the custom instrumentation logic that captures telemetry.
The transformer returns the modified byte code.
To attach the agent to the target application, its program is packaged as a separate .jar file and passed to the Java runtime via the `-javaagent` argument.
This allows the agent to modify the byte code of classes as they are loaded into the Java Virtual Machine (JVM).

Fortunately, OpenTelemetry simplifies this process by providing an [`opentelemetry-javaagent.jar`](https://github.com/open-telemetry/opentelemetry-java-instrumentation).
This jar includes instrumentation libraries for various frameworks and third-party libraries. It also contains components like OpenTelemetryAgent and AgentInstaller, which initiate the process, analyze the application, detect, and load available third-party instrumentation libraries.
These components leverage the mentioned mechanisms to adapt the byte code of Java classes at runtime.
Additionally, the OpenTelemetryInstaller configures emitters based on configuration options provided at invocation time (e.g., via the -D flag or Java properties file) to produce and deliver telemetry without any additional work on part of the user.

This section should highlight that auto-instrumentation is built on mechanisms specific to the given programming language.
Other languages may lack similar native capabilities.
Therefore, not all languages come with support for auto-instrumentation.

#### exercise

### How to perform the exercise
* You need to either start the [repository](https://github.com/JenSeReal/otel-getting-started/) with Codespaces, Gitpod or clone the repository with git and run it locally with dev containers or docker compose
* Initial directory: `labs/automatic-instrumentation/auto-instrumentation/initial`
* Solution directory: `labs/automatic-instrumentation/auto-instrumentation/solution`
* Source code: `labs/automatic-instrumentation/auto-instrumentation/initial/todobackend-springboot`

Make sure the docker compose environment from Otel in Action chapter is stopped.
Otherwise you will run into port conflicts.

You can run `docker compose ls` to verify. If it shows a process running in the `otel-in-action` directory,
please switch to this directory and call `docker compose down` to stop it.

Change back to the `labs` directory within your git project root and then into the `auto-instrumentation/initial/todobackend-springboot` path, e.g.

```sh
cd labs/automatic-instrumentation/auto-instrumentation/initial/todobackend-springboot
```

This is the same Java project as used for the backend component in the `OpenTelemetry in Action`chapter.

Build the project using maven:

```sh
mvn clean package
```

This will take a few seconds to complete. It will create an executable `jar` file which you can run.
Run it with the following command:

```sh
java -jar target/todobackend-0.0.1-SNAPSHOT.jar
```

This will take the control over your terminal. The output should look like:

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.2)

2024-04-17T06:43:10.236+02:00  INFO 73702 --- [springboot-backend ] [           main] i.n.todobackend.TodobackendApplication   : Starting TodobackendApplication v0.0.1-SNAPSHOT using Java 21.0.1 with PID 
```

Stop it again using `Ctrl`+`C`

At this point there is no OpenTelemetry instrumentation present. It's just a stand-alone Spring Boot application.

To add this download the OpenTelemetry agent from GitHub with the following command. Make sure you are still in the same directory where you successfully executed the `maven` command.

```sh
wget https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar
```

This will download a `jar` file, which you need to attach to the Java process. You don't need to modify any code or dependency in your project, you can simply add it as `javaagent` parameter like this:

```sh
java -javaagent:./opentelemetry-javaagent.jar -jar target/todobackend-0.0.1-SNAPSHOT.jar
```

Right at the start of the app you should see the following output:

```
[otel.javaagent 2024-04-17 06:56:37:329 +0200] [main] INFO io.opentelemetry.javaagent.tooling.VersionLogger - opentelemetry-javaagent - version: 2.3.0
```

This means that the agent as successfully been picked up.
It will run the application as before, but in the output you will see a lot of errors, e.g.

```
[otel.javaagent 2024-04-16 22:55:15:812 +0200] [OkHttp http://localhost:4318/...] ERROR io.opentelemetry.exporter.internal.http.HttpExporter - Failed to export spans. The request could not be executed. Full error message: Failed to connect to localhost/[0:0:0:0:0:0:0:1]:4318
java.net.ConnectException: Failed to connect to localhost/[0:0:0:0:0:0:0:1]:4318
        at okhttp3.internal.connection.RealConnection.connectSocket(RealConnection.kt:297)
        at okhttp3.internal.connection.RealConnection.connect(RealConnection.kt:207)
        at okhttp3.internal.connection.ExchangeFinder.findConnection(ExchangeFinder.kt:226)
        at okhttp3.internal.connection.ExchangeFinder.findHealthyConnection(ExchangeFinder.kt:106)
        at okhttp3.internal.connection.ExchangeFinder.find(ExchangeFinder.kt:74)
        at okhttp3.internal.connection.RealCall.initExchange$okhttp(RealCall.kt:255)
```

Stop the application again using `Ctrl`+`C`
The reason for this behaviour is that the agent is not configured and hence falling back to defaults.
The default configuration will make the agent look for a collector, which is currently not present in our environment.

So we need to overwrite the default settings. The most important one is to tell the agent to not look for a collector,
but export all the collected information to the console.

To achieve this, set the following environment variables:

```sh
export OTEL_TRACES_EXPORTER=console
```

```sh
export OTEL_METRICS_EXPORTER=none
```

```sh
export OTEL_LOGS_EXPORTER=none
```

This basically means that you tell the agent to only export trace information and to not try to reach out to a collector.

Now let's try again to run the application with the latest settings using:

```sh
java -javaagent:./opentelemetry-javaagent.jar -jar target/todobackend-0.0.1-SNAPSHOT.jar
```

The errors from the previous run should now disappear and you can see trace information in your console output.
It might be difficult to spot on the first try, but there are outputs like:

```
[otel.javaagent 2024-04-17 06:56:40:731 +0200] [main] INFO io.opentelemetry.exporter.logging.LoggingSpanExporter - 'CREATE table testdb' : e9bdebdf58d227cdbb8f9f9f406ebd82 b657cfe82cc88e3b CLIENT [tracer: io.opentelemetry.jdbc:2.3.0-alpha] AttributesMap{data={db.operation=CREATE table, db.name=testdb, thread.name=main, thread.id=1, db.user=sa, db.connection_string=h2:mem:, db.system=h2, db.statement=create table todo (todo varchar(?) not null, primary key (todo))}, capacity=128, totalAddedValues=8}
```

This one is coming from the JDBC library for OpenTelemetry where you can see the SQL statements how Spring Boot initializes the database.

Congratulations. At this point you have successfully configured your Java app with OpenTelemetry!

Please let the application run within this terminal window and open another terminal. 
In the new terminal execute a request against the application using:

```sh
curl localhost:8080/todos/
```

You should simply see an output like:

```sh
[]%
```

This is just because there are no items stored in your application.

If you switch back to the terminal of the Java application process you should see plenty of information in your console output.

```
[otel.javaagent 2024-04-17 07:04:57:384 +0200] [http-nio-8080-exec-1] INFO io.opentelemetry.exporter.logging.LoggingSpanExporter - 'SELECT testdb.todo' : 22571b7a308941882c3d203a2c1b2179 fcabde9f56343650 CLIENT [tracer: io.opentelemetry.jdbc:2.3.0-alpha] AttributesMap{data={db.operation=SELECT, db.sql.table=todo, db.name=testdb, thread.name=http-nio-8080-exec-1, thread.id=44, db.user=sa, db.connection_string=h2:mem:, db.system=h2, db.statement=select t1_0.todo from todo t1_0}, capacity=128, totalAddedValues=9}
[otel.javaagent 2024-04-17 07:04:57:387 +0200] [http-nio-8080-exec-1] INFO io.opentelemetry.exporter.logging.LoggingSpanExporter - 'SELECT io.novatec.todobackend.Todo' : 22571b7a308941882c3d203a2c1b2179 8a64843c1fb5217d INTERNAL [tracer: io.opentelemetry.hibernate-6.0:2.3.0-alpha] AttributesMap{data={thread.name=http-nio-8080-exec-1, thread.id=44}, capacity=128, totalAddedValues=2}
[otel.javaagent 2024-04-17 07:04:57:397 +0200] [http-nio-8080-exec-1] INFO io.opentelemetry.exporter.logging.LoggingSpanExporter - 'Transaction.commit' : 22571b7a308941882c3d203a2c1b2179 a5b7712f0edab44e INTERNAL [tracer: io.opentelemetry.hibernate-6.0:2.3.0-alpha] AttributesMap{data={thread.name=http-nio-8080-exec-1, thread.id=44}, capacity=128, totalAddedValues=2}
[otel.javaagent 2024-04-17 07:04:57:397 +0200] [http-nio-8080-exec-1] INFO io.opentelemetry.exporter.logging.LoggingSpanExporter - 'TodoRepository.findAll' : 22571b7a308941882c3d203a2c1b2179 0463a11569155a8d INTERNAL [tracer: io.opentelemetry.spring-data-1.8:2.3.0-alpha] AttributesMap{data={thread.name=http-nio-8080-exec-1, code.namespace=io.novatec.todobackend.TodoRepository, thread.id=44, code.function=findAll}, capacity=128, totalAddedValues=4}
2024-04-17T07:04:57.397+02:00  INFO 79699 --- [springboot-backend ] [nio-8080-exec-1] i.n.todobackend.TodobackendApplication   : GET /todos/ []
[otel.javaagent 2024-04-17 07:04:57:422 +0200] [http-nio-8080-exec-1] INFO io.opentelemetry.exporter.logging.LoggingSpanExporter - 'GET /todos/' : 22571b7a308941882c3d203a2c1b2179 61a3089ef357ce36 SERVER [tracer: io.opentelemetry.tomcat-10.0:2.3.0-alpha] AttributesMap{data={url.path=/todos/, thread.id=44, network.peer.address=0:0:0:0:0:0:0:1, server.address=localhost, client.address=0:0:0:0:0:0:0:1, http.response.status_code=200, http.route=/todos/, server.port=8080, http.request.method=GET, url.scheme=http, thread.name=http-nio-8080-exec-1, user_agent.original=curl/8.4.0, network.protocol.version=1.1, network.peer.port=52219}, capacity=128, totalAddedValues=14}
```

You can see multiple statements of the `otel.javaagent` but if you take a closer look each of them is originating from a different `tracer` library. You may spot jdbc, hibernate, spring data and tomcat.

This is how the auto-instrumentation works here. It uses a collection of instrumentation library to trace default components, which the Java application uses here.

However it makes another problem obvious: There are many spans being collected and it is hard to read on the console with the human eye.

If you execute another `curl` call in your other shell to add a new item, e.g.

```sh
curl -X POST localhost:8080/todos/NEW
```

you will get a whole set of entries including the one with the `INSERT` statement.

```
[otel.javaagent 2024-04-17 07:12:33:886 +0200] [http-nio-8080-exec-2] INFO io.opentelemetry.exporter.logging.LoggingSpanExporter - 'INSERT testdb.todo' : 4877d4bdd961fbf220a98aa4a9cda57b b97f57f46d6b96e1 CLIENT [tracer: io.opentelemetry.jdbc:2.3.0-alpha] AttributesMap{data={db.operation=INSERT, db.sql.table=todo, db.name=testdb, thread.name=http-nio-8080-exec-2, thread.id=45, db.user=sa, db.connection_string=h2:mem:, db.system=h2, db.statement=insert into todo (todo) values (?)}, capacity=128, totalAddedValues=9}
```

We can make our lives easier here and export the information to a visualisation tool like Jaeger.
First stop the application again using `Ctrl`+`C`.

Now deploy an all-in-one option of the Jaeger application, which comes with a bundled OpenTelemetry collector:

```sh
docker run -d --name jaeger \
  -e COLLECTOR_OTLP_ENABLED=true \
  -p 16686:16686 \
  -p 14268:14268 \
  -p 4317:4317 \
  -p 4318:4318 \
  jaegertracing/all-in-one
```

This will put the process into the background. Make sure it is up and running using

```sh
docker ps
```

which should include a container called `jaeger`

```
CONTAINER ID   IMAGE                      COMMAND                  CREATED         STATUS         PORTS                                                                                                                                          NAMES
65efd5788ad6   jaegertracing/all-in-one   "/go/bin/all-in-one-…"   7 seconds ago   Up 6 seconds   5775/udp, 5778/tcp, 9411/tcp, 0.0.0.0:4317-4318->4317-4318/tcp, 0.0.0.0:14268->14268/tcp, 0.0.0.0:16686->16686/tcp, 6831-6832/udp, 14250/tcp   jaeger
```

To make sure the information will be sent to the right endpoint, reconfigure the environment variable from before:

```sh
export OTEL_TRACES_EXPORTER=otlp
```

we could additionally configure the location of the Collector by specifying:

```sh
export OTEL_COLLECTOR_HOST=localhost
```

This is however the default anyway, so we can skip that. Also we don't need to configure port as per default `4317` is being used for gRPC and `4318` for HTTP. The agent will try to look for it and of course complain if it can't reach it.

Run the Java application again:

```sh
java -javaagent:./opentelemetry-javaagent.jar -jar target/todobackend-0.0.1-SNAPSHOT.jar
```

You may already notice that there is less output in the console despite the agent being present.

In your second terminal execute this command again:

```sh
curl -X POST localhost:8080/todos/NEW
```

Now point your browser to `http://localhost:16686/`.

If you run your application with a local container daemon, `localhost` in your browser will work. If you are using a cloud-based setup like Codespaces or Gitpod , please see the section "How to use this lab".

Especially the section about ports and hostnames is relevant here.
[Link](/labs/introduction/#important-differences-between-local-and-remote-way-of-running-the-lab)

The Jaeger UI will come up. Click the button `Find traces`.

Please refer to the chapter "OpenTelemetry in Action" for steps how to navigate within

[Using Jaeger UI](/labs/use_case_scenarios/#using-jaeger-ui)

Click on a trace in the list that shows a certain number of spans,
e.g. the `POST /todos/` or GET /todos/` one.

You will get a breakdown of the spans which you saw before in the console output. Jaeger has them neatly arranged,
so you can expand and basically walk through the call stack.

If you want to simulate a slow or a breaking call, you can execute:

```sh
curl -X POST localhost:8080/todos/slow
```

and accordingly:

```sh
curl -X POST localhost:8080/todos/fail
```

You can observe the different behaviour in the Jaeger console.

If you are familiar with Java you can of course also look at the code in the folder: `src/main/java/io/novatec/todobackend`
Open the TodobackendApplication.java with your VS built-in explorer.


### limitations of auto-instrumentation

A major advantage of dynamically attaching instrumentation at runtime is that we don't have to make modifications to the application's source code.
Auto-instrumentation provides a great starting point when trying to instrument an existing application.
The observability insights it provides are sufficient for many, while avoiding the significant time investment and understanding of OpenTelemetry's internals that is required with manual instrumentation.
However, as you might have guessed, it is not a magic bullet as there are inherent limitations to what auto-instrumentation can do for us.
Building on top of instrumentation libraries, auto-instrumentation inherently capture telemetry data at *known* points in a library or framework.
These points are deemed interesting from an observability perspective because they relate to actions such an incoming HTTP request, making a database query, etc.

<!--
`/src` contains a Java service that was build using Spring Boot.
Image this is a legacy application and that observability wasn't considered during the development process.
Hence, the service currently lacks native instrumentation.
How would we start insights into the application?
With manual instrumentation, we first would have to get familiar with the codebase to know what to instrument.
Then, we would need to write the instrumentation, which (as you have seen) can take quite a bit of work effort.

Therefore, the fastest way to generate telemetry is to leverage OpenTelemetry's instrumentation libraries and auto-instrumentation.

```Dockerfile { title="app.Dockerfile" }
# build app
FROM maven:3-eclipse-temurin-21 AS build
# ...

# application don't need full JDK at runtime
FROM eclipse-temurin:17-jre-alpine
# ...

# run app
ENTRYPOINT ["java","-cp","app:app/lib/*", "org.springframework.samples.petclinic.PetClinicApplication"]
```

To make your life a bit easier, `./deployment/docker/app.Dockerfile` already contains the dockerized Java application for you.
Open it and have a brief look at it.
It is a multi-stage Dockerfile that basically does three things.
First, it uses maven to compile the source code into a `.jar` byte code.
Then, it copies the files from build stage into another container, since the application doesn't require the full JDK and build tools at runtime.
Finally, the `ENTRYPOINT` runs the application on startup.


It contains two stages, one for building the Java application through maven and one to run it.
Let's create the container image via `docker build -t myorg/myapp .`
You can start the container by executing `docker run -it -p 8080:8080 myorg/myapp`.
Looking at `pom.xml` and theh boot process reveals that the application is built using Spring Boot.
The application also uses other libraries besides the Spring framework (e.g. JDBC, a hsqldb ).
As mentioned earlier, OpenTelemetry's Java agent  a rich set of [instrumentation libraries](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation).


```Dockerfile { title="app.Dockerfile" }
# get java agent for auto instrumentation
ARG OTEL_AGENT_VERSION=1.31.0
ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v$OTEL_AGENT_VERSION/opentelemetry-javaagent.jar ./otel/opentelemetry-javaagent.jar
```
To add auto-instrumentation, we must first get a copy of OpenTelemetry's Java Agent.
Fortunately, the [releases](https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases) of the [repository](https://github.com/open-telemetry/opentelemetry-java-instrumentation) already provides a pre-compiled .jar archive for us.
Let's use Docker's `ADD` command to copy the jar from the URL to the container's file system.

```Dockerfile { title="app.Dockerfile" }
ENTRYPOINT ["java","-cp","app:app/lib/*", "-javaagent:otel/opentelemetry-javaagent.jar", "org.springframework.samples.petclinic.PetClinicApplication"]
```
To manipulate the byte code of the application, we'll simply pass the Java Agent to the runtime using the `--javaagent` argument.


```sh { title="terminal" }
docker build -f deployment/docker/app.Dockerfile -t myorg/myapp .
docker run -it -p 8080:8080 myorg/myapp
```

After injecting the agent, let's build the application with and start the container.

```sh
[otel.javaagent 2024-01-17 09:24:06:037 +0000] [main] INFO io.opentelemetry.javaagent.tooling.VersionLogger - opentelemetry-javaagent - version: 1.31.0

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.2.1)
```

```sh
[otel.javaagent 2024-01-17 09:25:06:631 +0000] [OkHttp http://localhost:4317/...] WARN io.opentelemetry.exporter.internal.grpc.GrpcExporter - Failed to export metrics. Server responded with gRPC status code 2. Error message: Failed to connect to localhost/127.0.0.1:4317
```

If everything works as expected, a `otel.javaagent` log message should appear before the Spring Boot application starts.
After boot process completes, log messages appear which indicate that the agent failed to export metrics.
We previously used the SDK to define instrumentation *and* a pipeline of how telemetry should be processed and where it should go.
By default, the agent tries to forward telemetry to a local collector listening on `localhost:4317`.
At moment, we neither have a Collector nor telemetry backends running.
Inside `/deployments/docker`, we prepared a `compose.yaml` file that simulates a complete observability stack.
- Prometheus for metrics
- Jaeger for traces

To ingest telemetry into these backends, we must supply configuration to the OpenTelemetry agent.
It receives user-defined configuration options and automatically sets up the SDK (i.e. the tracer, meter and logger provider) for us.
The agent is designed to be highly-configurable and provides a bunch of parameters to control various ...
Here, we'll focus on the essentials, which include:
- what exporter to use
- where the backends are
- resource attached to telemetry

The agent can be configured with the help of environment variables, configuration files and by passing command line arguments.

```
- configure the agent
- docker-compose.yaml with backend
```

```sh
docker compose up --detach
```

With everything setup, let's finally start the demo environment.
- Open browser and go to
  - Jaeger
    - generate traffic
    - see trace with breakdown of what is being invoked inside the application
      - methods
      - database calls
      - tomcat web server
  - Grafana
    - view metric dashboards

-->


<!--
```
show source code custom method without annotation
```
However, sometimes we want to observe aspects of our application that don't naturally align with these standard instrumentation points.
Developers often encapsulate related logic in dedicated functions to improve code maintainability and organization.
The OpenTelemetry agent may lack awareness of these custom functions.
It has no way of knowing whether they are important and what telemetry data to capture from them.

```
```
Let's test this.
Exec into our utility container `docker exec ...` and use `curl ....` to issue a request to the endpoint which calls our custom method.
Go to Jaeger and find the corresponding trace.
As you'll see, the trace currently doesn't contain a span associated with our custom method.
Such gaps in observability may mean that you may miss critical insights in the behavior of the application.
However, since they aren't mutually exclusive, we can always enhance auto- with manual instrumentation.

```
pom.xml
```
```
import package
```
To achieve this, we must install the API and SDK dependencies for manual instrumentation by adding them to `pom.xml`.
Next, open up `xyz.java` and specify the respective imports as shown above.
Now, we can add the `@WithSpan` annotation to our custom method.
Rebuild the application.
Again, use the `curl` command to issue a request and look at the corresponding trace in Jaeger.
You should now see that a dedicated Span was created for the custom method.

```
add  @SpanAttribute
```
We might want to include some additional context in the Span.
For example, the span should include the value of a parameter passed to the function.

## finish

Congratulations on successfully completing this chapter!
This lab illustrated how auto-instrumenation can perform work, which previously required us to write manual instrumentation.
It showed how OpenTelemetry allows us to extend automatic with custom instrumentation to gain additional visibility where it is needed.
It is important to emphasize that instrumentation libraries and agents are not a drop-in replacement for manual instrumentation.
Library instrumentation and agent-based approaches offer convenience and quick setup, manual instrumentation using OpenTelemetry's API and SDK provides more control and flexibility.
For example, auto-instrumentation might instrument aspects of the code that are not relevant to the specific monitoring goals.
While OpenTelemetry ships with powerful procession tools to shape telemetry, manual instrumentation allows us to precisely instrument only what is necessary.
In addition, there's a fundamental tension between the desire for zero-effort, minimally invasive observability and the shift-left approach that advocates for making observability an integral part of the development process.
Last (and certainly not least), library and framework instrumentation only cover generic aspects of an application.
To make a system truly observable, one often must record telemetry specific to the domain of the application.
This goes beyond what automatic instrumentation can provide, because it requires a higher-level understanding of the code and business.
It is important to emphasize that both approaches are not mutually exclusive.
Therefore, it is crucial to find the right balance between them to create an observable system without imposing undue burdens on the development team.


<!-- NOTES  -->

<!--
- Agent Extensions
  - demo von Matthias verwendet annotations das eine custom Methode als span im trace angezeigt wird
  - es gibt scheinbar auch die möglichkeit Extensions für custom instrumentaion zu schreiben
    - InstrumentationModule and TypeInstrumentation
  - related links:
    - https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/contributing/writing-instrumentation-module.md
    - https://www.elastic.co/blog/auto-instrumentation-of-java-applications-opentelemetry
    - https://www.youtube.com/watch?v=hXTlV_RnELc
    - https://opentelemetry.io/docs/instrumentation/java/automatic/extensions/
    - https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/examples/extension
-->