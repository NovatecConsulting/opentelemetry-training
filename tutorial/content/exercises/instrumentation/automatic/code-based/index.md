---
title: "Code-based instrumentation"
draft: false
weight: 3
---

## Introduction

This part of the exercise extends the zero-code instrumentation approach, by using instrumentation libraries and manual instrumentation in conjunction with the full automatic approach.

To differentiate from the upcoming manual instrumentation this step can be seen as a hybrid approach.

## Learning Objectives

By the end of this chapter, you should be able to:
- Understand how to extend zero-code instrumentation with usage of libraries or mixing with manual instrumentation
- Apply this form of instrumentation to an existing Java and Python application
- Have an understanding of how much you need to interfere with source code in order to get a certain degree of information depth

### Background
<!-- Instrumentation Library -->
The long-term vision of OpenTelemetry is that third-party libraries or frameworks ship with native OpenTelemetry instrumentation, providing out-of-the-box observability.
However, as of now, some projects do not yet have native support for the (partially still evolving) OpenTelemetry APIs.
To bridge this gap, (temporary) solutions have been developed in the form of *instrumentation libraries*.
The core idea is to increase the adoption of OpenTelemetry without placing the entire burden on the back of the library maintainers.
An instrumentation library is a standalone (separately installed) library, whose sole purpose is to inject OpenTelemetry API calls into another popular library or framework which currently lacks native integration.
There is no unified way to achieve this, because it highly depends on the mechanisms of provided by the programming language and the library.
Examples range from wrapping interfaces, monkey-patching code, registering callback on library-specific hooks to translating existing telemetry into OpenTelemetry.
The OpenTelemetry has a [registry](https://opentelemetry.io/ecosystem/registry/) which can be used to find instrumentation libraries. A brief look into the libraries reveals that there are already a lot of libraries for many frameworks and programming languages. But many of these libraries are not stable yet and are in an alpha or beta state. To find a library that fits your software stack, just visit the registry and search for the framework or programming language that you are using.

While instrumentation libraries offer a valuable solution for enhancing observability in third-party libraries or frameworks that lack native OpenTelemetry support, they also present certain challenges. These include the necessity to manage additional dependencies, which adds complexity to the codebase and requires careful consideration of maintenance overhead. Additionally, as instrumentation libraries are still relatively new compared to native integrations, they may face limitations such as less community support, fewer resources, and a higher risk of encountering issues due to their nascent nature.

### How to perform the exercise

* This exercise is based on the following repository [repository](https://github.com/NovatecConsulting/opentelemetry-training/) 
* All exercises are in the subdirectory `exercises`. There is also an environment variable `$EXERCISES` pointing to this directory. All directories given are relative to this one.
* Initial directory: `automatic-instrumentation/initial`
* Solution directory: `automatic-instrumentation/solution`
* Java source code: `automatic-instrumentation/initial/todobackend-springboot`
* Python source code: `automatic-instrumentation/initial/todoui-flask`

### Exercise - Java instrumentation annotations

The previous exercise using Java `zero-code` instrumentation can illustrate the need for additional libraries.
Using OpenTelemetry's Java agent - as we did - covered a generic set of set of [instrumentation libraries](https://github.com/open-telemetry/opentelemetry-java-instrumentation/tree/main/instrumentation) and we did not have to worry about selecting any of them individually.

This makes it very helpful to get started with as no modification or rebuild of the application is necessary.

Sometimes however we want to observe aspects of our application that don't naturally align with these standard instrumentation points.
Developers often encapsulate related logic in dedicated functions to improve code maintainability and organization.
The OpenTelemetry agent may lack awareness of these custom functions.
It has no way of knowing whether they are important and what telemetry data to capture from them.

To apply a more granular configuration to the already existing agent you can use the `opentelemetry-instrumentation-annotations` library. 

If the Java part of the application is still running from this exercise, stop it using `Ctrl+C` in the corresponding terminal window.

This library needs to be added to the application source code. To be precise to the build dependencies of the application in the first place. As the sample application uses Maven as build tool, we need to locate the `pom.xml`in the root folder of the application.

Change to the directory within to `exercises/automatic-instrumentation/initial/todobackend-springboot` path, if you are in the project root directory it is:

```sh
cd $EXERCISES
cd automatic-instrumentation/initial/todobackend-springboot
```

Once you open it up in the editor it, you will see there is a section containing dependencies.
If you prefer you can do this via command-line, but we recommend to use the editor within VS Code.

Locate the `dependencies` section within the `pom.xml` file:

```xml
<dependencies>
...
</dependencies>
```

Add the following dependency to it and make sure to align/indent with the already existing ones. (the order of dependencies in the file does not matter)

```xml
		<dependency>
			<groupId>io.opentelemetry.instrumentation</groupId>
			<artifactId>opentelemetry-instrumentation-annotations</artifactId>
			<version>2.4.0</version>
		</dependency>
```

and save the file.

Now re-run the build command 

```sh
mvn clean package
```

This change will not have any effect yet on how the application will be monitored, but it will allow us to apply more granular configuration.

Let's repeat some steps from the `zero-code` exercise. In case the docker container is still running, let it run. Otherise start using the command:

```sh
docker run -d --name jaeger \
  -e COLLECTOR_OTLP_ENABLED=true \
  -p 16686:16686 \
  -p 14268:14268 \
  -p 4317:4317 \
  -p 4318:4318 \
  jaegertracing/all-in-one
```

Download the agent jar file, if it is not there yet.

```sh
wget https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar
```

And make sure the environment variables are set appropriately:

```sh
export OTEL_TRACES_EXPORTER=otlp
export OTEL_METRICS_EXPORTER=none
export OTEL_LOGS_EXPORTER=none
```

Now run the newly build jar including the agent:

```sh
java -javaagent:./opentelemetry-javaagent.jar -jar target/todobackend-0.0.1-SNAPSHOT.jar
```

Let this process run in it's terminal window and switch to a new one.
Within that new window execute:

```sh
curl -X POST localhost:8080/todos/NEW
```

Open the Jaeger Web UI and search for the last trace we just generated. It will look like this:

{{< figure src="images/jaeger_trace_auto_instrument.png" width=700 caption="Jaeger trace - Auto instrumentation" >}}

Now open the Java source file under `todobackend-springboot/src/main/java/io/novatec/todobackend/TodobackendApplication.java` directly here in the editor.

You will see the two following methods:

```java
	@PostMapping("/todos/{todo}")
	String addTodo(@PathVariable String todo){

		this.someInternalMethod(todo);
		//todoRepository.save(new Todo(todo));
		logger.info("POST /todos/ "+todo.toString());

		return todo;

	}

	String someInternalMethod(String todo){

		todoRepository.save(new Todo(todo));
    ...
		return todo;

	}
```

The `addTodo` method is the entry point when the REST call arrives at the application. We can see that in the Jaeger trace. This is visible in the entry span called `POST/todos/{todo}`:

{{< figure src="images/jaeger_trace_auto_instrument.png" width=700 caption="Jaeger trace - Auto instrumentation" >}}

With the invocation of `todoRepository.save(new Todo(todo));` the new item will be persisted in the database.
This is also visible in the Jaeger trace.

What we cannot see however is the method in between `someInternalMethod`. This one is invoked by `addTodo` and invokes `todoRepository.save` but it is not being displayed in the trace.

In order to change this we need to add an annotation to this method in the code.

Within the source code of the Java class `TodobackendApplication.java` add the following import statements to the top.

```java
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
```

This enables functionality in the code that we provided through adding the new maven dependency.

As a next step we need to annotate the method. Locate the `someInternalMethod` and place the `@WithSpan` annotation just above as shown in the following code snippet.

```java
	@WithSpan
	String someInternalMethod(String todo){
```

The file will be automatically saved by VSCode.

Select the terminal window where the Java process runs and press `Ctrl+C`. However do not stop the Jaeger docker container, we still need it.
(This should put you back into the `todobackend-springboot` directory, where the `pom.xml` file is located. If not change the directory as before.)

Rebuild the Java application with the new code.

```sh
mvn clean package
```

Run the newly build jar file:

```sh
java -javaagent:./opentelemetry-javaagent.jar -jar target/todobackend-0.0.1-SNAPSHOT.jar
```

After it has come up, switch to another terminal window and generate some more load as you did before:

```sh
curl -X POST localhost:8080/todos/TEST
curl localhost:8080/todos/
curl -X DELETE localhost:8080/todos/TEST
```

Access the Jaeger UI again and find the latest traces.

Observe the `POST` call once more.

You will now see that between the entry span `POST/todos/{todo}` and the third span `todoRepository.save` there is one called `todoRepository.someInternalMethod`. This is due to the annotation we did.

{{< figure src="images/jaeger_trace_annotated.png" width=700 caption="Jaeger trace - Auto instrumentation with annotations" >}}

If you expand the line of trace in the Jaeger UI you will get `Tags` and `Process` details. Expand all of it.

{{< figure src="images/jaeger_trace_span_detail.jpg" width=700 caption="Jaeger trace - Span Detail" >}}

Among other details you will be able to see the details of the method and name of the used library.

```
code.function - someInternalMethod
code.namespace - io.novatec.todobackend.TodobackendApplication

otel.library.name - io.opentelemetry.opentelemetry-instrumentation-annotations-1.16
otel.library.version - 2.5.0-alpha
```

Let's take this one step further. The library does not only allow to annotate methods and hence observe their specific span details, it also let's you observe the contents of variable parameters.

In order to achieve this it is required to modify the `TodobackendApplication.java` again. So stop the Java process again with `Ctrl+C` and open the source code file in the editor.

Navigate to the method signature of the `someInternalMethod` method. This time you need to add the `@SpanAttribute` annotation right in front of the `String todo` parameter like shown here:

```java
	@WithSpan
	String someInternalMethod(@SpanAttribute String todo){
```

That's it. Now save, build and run it again.

```sh
mvn clean package
```

```sh
java -javaagent:./opentelemetry-javaagent.jar -jar target/todobackend-0.0.1-SNAPSHOT.jar
```

After it has come up, generate some more load:

```sh
curl -X POST localhost:8080/todos/SMILE
```

Access the Jaeger UI again and find the latest traces.
Expand it like you did before and focus on the Tags part in the trace.

As the latest entry you should now see

```
todo - SMILE
```

{{< figure src="images/jaeger_trace_span_detail_todo.png" width=700 caption="Jaeger trace - Span Detail with value" >}}

This means you can now also see the specific parameter which has been passed and relate it to a slow performing call in case it happens.

Leave the Java application running, you will need it for the Python part as well.

### Alternative approach 

These are no exercise steps, this is just supporting information.

An alternative approach to use the annotations library is by configuring it through environment variables.
There are two environment variables you can use to configure the annotations library.

`OTEL_INSTRUMENTATION_METHODS_INCLUDE` and `OTEL_INSTRUMENTATION_OPENTELEMETRY_INSTRUMENTATION_ANNOTATIONS_EXCLUDE_METHODS`

As the name already implies they configure methods to be included as spans or excluded from instrumentation (e.g. if you want to suppress an existing `@WithSpan` implementation after code is already compiled)

In our example the corresponding environment setting to include the `someInternalMethod` to the spans without using the `@WithSpan` annotation in code would be:

```
export OTEL_INSTRUMENTATION_METHODS_INCLUDE=io.novatec.todobackend.TodobackendApplication[someInternalMethod]
```

In case the `@WithSpan` annotation was already present in the compiled jar and you want to exclude it without rebuild you have to set:

```
export OTEL_INSTRUMENTATION_OPENTELEMETRY_INSTRUMENTATION_ANNOTATIONS_EXCLUDE_METHODS=io.novatec.todobackend.TodobackendApplication[someInternalMethod]
```

Both environment variables only correspond with the `@WithSpan` annotation. There is no possibility (yet) to configure span attributes through environment setting. This only works on code level.

The part of the Java library exercise completes with this step.

---

### exercise - Python mixed automatic and manual instrumentation 

Change to the directory within to `exercises/automatic-instrumentation/initial/todoui-flask` path, if you are in the project root directory it is:

```sh
cd $EXERCISES
cd automatic-instrumentation/initial/todoui-flask
```

Similar to the exercise case in the Java example before, also in a Python there can be the requirement to get more observability information than the plain automatic instrumentation might reveal.
This example will show a mixed mode of automatic and manual instrumentation to achieve this behaviour and will already give a lookout to what will be covered in the dedicated `manual instrumentation` chapter.

In this case we will custom instrument the already existing auto instrumentation and add a way to access the processed todo item on the frontend side of the application.

Open the `app.py` Python source code in your editor.

Locate the 2 import statements on top:

```
import logging
import requests
import os
```

Add the following statement right underneath to import the trace API from OpenTelemetry:

```python
# Import the trace API
from opentelemetry import trace
```

Directly after that add a statement to acquire a tracer


```python
# Acquire a tracer
tracer = trace.get_tracer("todo.tracer")
```

The way we call it here does not matter. The name `todo.tracer` will later appear as such in the trace metadata.

So the resulting code after the edit should look like:

```python
import logging
import requests
import os

# Import the trace API
from opentelemetry import trace

# Acquire a tracer
tracer = trace.get_tracer("todo.tracer")

app = Flask(__name__)
logging.getLogger(__name__)
logging.basicConfig(format='%(levelname)s:%(name)s:%(module)s:%(message)s', level=logging.INFO)
```

As a next step we will add a custom span to the trace which contains the information we want to have.

Locate the `add` function in the code, which starts like this:

```python
@app.route('/add', methods=['POST'])
def add():
```

The central code block of this function receives the todo from the web UI and sends it to the backend.
The code is supposed to look like this:

```python
    if request.method == 'POST':
        new_todo = request.form['todo']
        logging.info("POST  %s/todos/%s",app.config['BACKEND_URL'],new_todo)
        response = requests.post(app.config['BACKEND_URL']+new_todo)
    return redirect(url_for('index'))
```

Replace it entirely with the following code block. (You can also edit manually, but replacing will avoid typos.)

```python
    if request.method == 'POST':
        with tracer.start_as_current_span("add") as span:
            new_todo = request.form['todo']
            span.set_attribute("todo.value",new_todo)
            logging.info("POST  %s/todos/%s",app.config['BACKEND_URL'],new_todo)
            response = requests.post(app.config['BACKEND_URL']+new_todo)
    return redirect(url_for('index'))
```

As you can observe 2 lines of code have been added.

The following line
```python
        with tracer.start_as_current_span("add") as span:
```
will create a new span and the line
```python
            span.set_attribute("todo.value",new_todo)
```
will add the currently processed `todo` item as attribute to it.

This is all we need to do for now. Before you run it make sure all the environment variables are still set.
If you have switched the terminal session they might now be active any more.

Execute:

```sh
export OTEL_LOGS_EXPORTER="none"
export OTEL_METRICS_EXPORTER="none"
export OTEL_TRACES_EXPORTER="otlp"
export OTEL_EXPORTER_OTLP_ENDPOINT="localhost:4317"
export OTEL_SERVICE_NAME=todoui-flask
export OTEL_EXPORTER_OTLP_INSECURE=true
```

After that you are good to go and run the autoinstrumented app again:

```sh
opentelemetry-instrument python app.py
```

Now access the both Web UIs again - the Python frontend and Jaeger. Add and remove some todos and observe the behaviour in Jaeger.

If you analyse an individual trace of `todoui-flask` with a `POST` operation, you will see an additional span in the trace.

This part will show up as 2nd in the list an is called `todoui-flask add`.

{{< figure src="images/jaeger_trace_custom_span.png" width=700 caption="Jaeger trace - Custom span" >}}

Once you expand the details of this trace you will also see the value of the passed variable. Under the section of tags you see the `todo.value`.

{{< figure src="images/jaeger_trace_custom_span_details.png" width=700 caption="Jaeger trace - Custom span" >}}

### Summary - What you have learned.

The first part of instrumenation of polyglot applications completes with this step.

You have seen and applied zero-code, automatic instrumentation for both Java and Python application components. The advantage here is obvious - there is no need to understand and modify source code. The limitation is also obvious - you are limited to what the default configuration will set. It is not possible to instrument custom methods or functions and trace internal variables.

To overcome these limitations you have seen two different approaches. Adding custom libraries to the source code and implementing annotations in the code or using a mixed mode of automatic and manual instrumentation.

It becomes clear that the more granular details you want to get out of your application the more you have to put instrumentation code into it.

The following chapter `manual instrumentatoin` will do this in full depth.

Please stop all Java, Python and docker processes.

<!-- 
We want to follow the previous software stack and use Python flask to show how instrumentation libraries are used. To find an appropriate library we search the registry and find the `opentelemetry-flask-instrumentation` library. We can install the library using `pip` with the command `pip install opentelemetry-flask-instrumentation`. This package provides the necessary hooks to automatically instrument your Flask application with OpenTelemetry. Next, you need to configure OpenTelemetry to use the appropriate exporters and processors. This usually involves setting up an exporter to send telemetry data to a backend service like Jaeger, Zipkin, or another OpenTelemetry-compatible service, or in this case the OpenTelemetry collector. With the library installed and OpenTelemetry configured, you can now instrument your Flask application. This involves initializing the OpenTelemetry Flask instrumentation at the start of your application and ensuring that it wraps your Flask app instance. Finally, run your Flask application as you normally would. The instrumentation will automatically capture telemetry data from incoming requests, outgoing responses, and any exceptions that occur.

When using the `opentelemetry-flask-instrumentation` library with a Python Flask application, a span is automatically created for each incoming HTTP request. The span represents the execution of a single operation within the context of a trace, such as handling an HTTP request. To instrument Flask we need to wrap the flask application inside the `FlaskInstrumentor` which is provided by the pip package.

```python
import time

import requests
from client import ChaosClient, FakerClient
from flask import Flask, make_response
from opentelemetry import trace
from opentelemetry.instrumentation.flask import FlaskInstrumentor
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor, ConsoleSpanExporter

# global variables
app = Flask(__name__)

FlaskInstrumentor().instrument_app(app)
```

With this setup the Flask app is instrumented. Now we also need to define exporters to export the telemetry data to a collector or the console. For simplicity reasons we will export just traces to the console. To achieve this we need to import the `ConsoleSpanExporter` and `BatchSpanProcessor`, the `TracerProvider`, and the general `trace` import.

```python
import time

import requests
from client import ChaosClient, FakerClient
from flask import Flask, make_response
from opentelemetry import trace
from opentelemetry.instrumentation.flask import FlaskInstrumentor
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor, ConsoleSpanExporter

# Initialize the OpenTelemetry tracer
trace.set_tracer_provider(TracerProvider())

# Configure the OTLP exporter
otlp_exporter = ConsoleSpanExporter()

# Set up the BatchSpanProcessor with the exporter
span_processor = BatchSpanProcessor(otlp_exporter)
trace.get_tracer_provider().add_span_processor(span_processor)
```

Then the tracer provider need to be set, the exporter needs to be initialized, and we can attach the exporter to the `BatchSpanProcessor` and add the processor to the trace provider.

The final result should look similar to this:

```python
import time

import requests
from client import ChaosClient, FakerClient
from flask import Flask, make_response
from opentelemetry import trace
from opentelemetry.instrumentation.flask import FlaskInstrumentor
from opentelemetry.sdk.trace import TracerProvider
from opentelemetry.sdk.trace.export import BatchSpanProcessor, ConsoleSpanExporter

# Initialize the OpenTelemetry tracer
trace.set_tracer_provider(TracerProvider())

# Configure the OTLP exporter
otlp_exporter = ConsoleSpanExporter()

# Set up the BatchSpanProcessor with the exporter
span_processor = BatchSpanProcessor(otlp_exporter)
trace.get_tracer_provider().add_span_processor(span_processor)

# global variables
app = Flask(__name__)

FlaskInstrumentor().instrument_app(app)

@app.route("/users", methods=["GET"])
def get_user():
    user, status = db.get_user(123)
    data = {}
    if user is not None:
        data = {"id": user.id, "name": user.name, "address": user.address}
    response = make_response(data, status)
    return response


def do_stuff():
    time.sleep(0.1)
    url = "http://localhost:6000/"
    response = requests.get(url)
    return response


@app.route("/")
def index():
    do_stuff()
    current_time = time.strftime("%a, %d %b %Y %H:%M:%S", time.gmtime())
    return f"Hello, World! It's currently {current_time}"


if __name__ == "__main__":
    db = ChaosClient(client=FakerClient())
    app.run(host="0.0.0.0", debug=True)

```

In this example, when a user sends a request to the root path (`/`), the `FlaskInstrumentor` automatically creates a span for the HTTP request. This span captures details such as the request method (GET, POST, etc.), the request URL, the status code of the response, and any exceptions that occur during the processing of the request. The span also includes timing information, such as the start and end times, which help in understanding the latency of the request.

The span is then processed by the `BatchSpanProcessor`, which batches multiple spans together and sends them to the configured `OTLPSpanExporter`. The exporter serializes the span data and sends it to the OpenTelemetry Collector, which then forwards the data to a backend service like Jaeger or Zipkin for storage, analysis, and visualization.

By using the `opentelemetry-flask-instrumentation` library, you can automatically instrument your Flask application to create and export spans without writing additional code to manually create and manage spans. This simplifies the process of adding observability to your application and allows you to focus on the application's functionality.

A JSON representation of a span created by the `opentelemetry-flask-instrumentation` library would typically include various attributes that describe the span's operation, its relationship to other spans, and any additional metadata or events associated with the span. Here's an example of what a JSON representation of a span might look like:

```json
{
  "traceId": "0123456789abcdef0123456789abcdef",
  "spanId": "0123456789abcdef",
  "traceFlags": "01",
  "parentSpanId": "abcdef0123456789",
  "name": "GET /",
  "startTime": [162,  861645600],
  "endTime": [162,  861645601],
  "attributes": {
    "http.method": "GET",
    "http.url": "http://localhost:5000/",
    "http.status_code":  200,
    "http.user_agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X  10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.114 Safari/537.36",
    "custom_attribute": "custom_value"
  },
  "events": [
    {
      "time": [162,  861645600],
      "name": "exception",
      "attributes": {
        "exception.type": "ValueError",
        "exception.message": "Invalid value"
      }
    }
  ],
  "links": [],
  "status": {
    "code": "OK"
  }
}
```

The JSON representation of a span created by the `opentelemetry-flask-instrumentation` library encapsulates various attributes that describe the span's operation and its relationship to other spans. It includes the `traceId`, which is a unique identifier for the trace that the span is part of, and the `spanId`, which is the unique identifier for the span itself. The `traceFlags` field can be used to control the behavior of the trace, and the `parentSpanId` field indicates the identifier of the parent span, if any. The `name` field typically describes the operation being performed, such as handling an HTTP request. Timestamps `startTime` and `endTime` represent when the span started and ended, in nanoseconds since the Unix epoch, providing insights into the span's duration. The `attributes` field is a set of key-value pairs that offer additional context about the span, including HTTP method, URL, status code, and user agent. The `events` array contains events that occurred during the span's lifetime, like exceptions or other significant occurrences. The `links` array is used to establish relationships between spans, and the `status` object indicates the span's status, with a `code` that can represent success, failure, or other states. This structured data allows for detailed analysis and visualization of the span's performance and behavior, which is crucial for observability and troubleshooting in distributed systems.

Please note that the actual JSON representation of a span may vary depending on the specific exporter and backend service you are using. The above example is a generic representation and may need to be adapted to fit the specifics of your OpenTelemetry setup.

### development
Developing an instrumentation library is a significant undertaking and requires a deep understanding of distributed tracing concepts, the OpenTelemetry specification, and the programming language ecosystem you're working with. Implementing an instrumentation library involves several key steps and conventions to ensure consistent behavior of libraries. The advantages on the other hand are several advantages of offering an instrumentation library for a framework. The biggest advantage is that a library enhances observability and improves the developer experience. Instead of libraries needing to create and document custom logging hooks, they can utilize the standardized and user-friendly APIs provided by OpenTelemetry, simplifying the interaction for users. This also ensures that traces, logs, and metrics from both the library and application code are interconnected and consistent. The use of common conventions across different libraries and languages guarantees uniform and coherent telemetry data. Furthermore, OpenTelemetry's extensibility allows telemetry signals to be finely adjusted, including filtering, processing, and aggregating, to cater to various usage scenarios. OpenTelemetry furthermore gives extensive information on how to instrument a library. The most critical resource are the semantic conventions that OpenTelemetry specifies. The metrics, traces, logs and events exported from the library should adhere to these conventions for a unified developer experience. There are a lot of different semantic conventions for different types of software. When defining spans for your library in the context of OpenTelemetry, it's crucial to consider the library from a user's perspective. Users are typically more interested in how their application functions rather than the intricate details of the library internals. Therefore, focus on providing information that aids in analyzing the library's usage. This could involve considering aspects such as span and span hierarchies, numerical attributes on spans as an alternative to aggregated metrics, and span events. For instance, if your library is interacting with a database, it would be more beneficial to create spans only for the logical requests made to the database. The physical network requests should be instrumented within the libraries implementing that specific functionality. Additionally, it's preferable to capture other activities, like object or data serialization, as span events instead of additional spans. Remember to adhere to the semantic conventions when setting span attributes. This ensures consistency and coherence in the telemetry data, providing users with meaningful insights into the behavior and activity of the library. There are instances when instrumentation may not be necessary, especially for libraries that are essentially thin clients encapsulating network calls. OpenTelemetry likely already has an instrumentation library for the underlying RPC client, which can be verified in the registry. In such cases, additional instrumentation of the wrapper library might be redundant. The rule of thumb is to instrument your library only at its own level. Avoid instrumentation if your library is a straightforward proxy on top of well-documented or self-explanatory APIs, if OpenTelemetry already has instrumentation for the underlying network calls, and if there are no conventions that your library needs to follow to enhance telemetry. If unsure, it's better to refrain from instrumenting, as it can always be added later if required. Even if you decide not to instrument, it may still be beneficial to offer a method to configure OpenTelemetry handlers for your internal RPC client instance. This is particularly important in languages that do not support fully automatic instrumentation but can still be useful in others. Libraries should strictly use the OpenTelemetry API. You might be wary about introducing new dependencies, but here are a few points to consider. The API is already stabilized, so there won't be any breaking changes in the future and new features will be added with backwards compatibility. All application configurations are concealed from your library via the Tracer API. Libraries may allow applications to pass `TracerProvider` instances to facilitate dependency injection and simplify testing, or they can obtain it from the global `TracerProvider`. Depending on what's considered idiomatic, different OpenTelemetry language implementations may prefer either passing instances or accessing the global.
When obtaining the tracer, provide your library's (or tracing plugin's) name and version. This information appears in the telemetry data, helping users process and filter telemetry, understand its origin, and debug/report any instrumentation issues.
When deciding what to instrument, public APIs are good candidates for tracing. Spans created for public API calls enable users to link telemetry to application code and understand the duration and outcome of library calls, as you saw in our tracing lab. Trace public methods that internally make network calls or perform local operations that may take a significant amount of time or fail (like IO), as well as handlers that process requests or messages.

If OpenTelemetry doesn't support tracing your network client, you can use logs with verbosity or span events, which can be correlated to parent API calls. Context propagation is crucial in both inbound and outbound calls. When receiving upstream calls, context should be extracted from the incoming request/message using the Propagator API. Conversely, when making an outbound call, a new span should be created to trace the outgoing call, and the context should be injected into the message using the Propagator API. Events (or logs) and traces complement each other in providing observability. Events are better suited for verbose data and should always be attached to the span instance created by your instrumentation. Lastly, it's important to add your instrumentation library to the OpenTelemetry registry for easy discovery by users. It's also recommended testing your instrumentation with other telemetry to see how they interact.

 {{< quizdown >}}

### The primary purpose of developing an instrumentation library for OpenTelemetry is to provide a way to automatically instrument applications.
  - [ ] True
  > This is true. The primary purpose of an instrumentation library is to automatically inject OpenTelemetry API calls into another library or framework that lacks native integration, thus enhancing observability without the need for manual instrumentation.
  - [ ] False

### Adhering to the OpenTelemetry API is NOT a key step in implementing an instrumentation library according to the OpenTelemetry specification.
  - [ ] True
  - [ ] False
  > This is false. Adhering to the OpenTelemetry API is a key step in implementing an instrumentation library. It ensures that the library is compatible with the OpenTelemetry ecosystem and can be used by applications that are already using or planning to use OpenTelemetry.

###  What is the recommended approach when you are unsure about whether to instrument your library with OpenTelemetry?
  - [ ] Instrument your library at all levels
  - [ ] Refrain from instrumenting and add it later if required
  > The recommended approach is to refrain from instrumenting and add it later if required. This is because instrumentation can add complexity and overhead, and it's better to start with the most critical parts of the library that users are most interested in monitoring.
  - [ ] Instrument only the public APIs
  - [ ] Instrument all methods, regardless of their complexity

### When should you consider using logs with verbosity or span events instead of tracing your network client with OpenTelemetry?
  - A) When OpenTelemetry does not support tracing your network client
  > You should consider using logs with verbosity or span events instead of tracing your network client with OpenTelemetry when OpenTelemetry does not support tracing your network client. This allows you to still capture important information about the network calls without the overhead of full tracing.
  - B) When you want to create a new instrumentation library
  - C) When you need to analyze the performance of your application
  - D) When you want to implement distributed tracing

{{< /quizdown >}}

### Review
- **Instrumentation Libraries**: They are tools to add OpenTelemetry support to libraries without native support.
- **Example with Flask**: A Python Flask app can be instrumented using `opentelemetry-flask-instrumentation`.
- **Developing Libraries**: It's important to follow OpenTelemetry conventions and focus on user-facing operations.
- **Instrumentation Decisions**: Decide based on the complexity and necessity, considering logs or span events for unsupported clients. 

-->
