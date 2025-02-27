package io.novatec.todobackend;

import static io.opentelemetry.api.common.AttributeKey.booleanKey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapGetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.repository.CrudRepository;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.semconv.ClientAttributes;
import io.opentelemetry.semconv.HttpAttributes;
import io.opentelemetry.semconv.UserAgentAttributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@SpringBootApplication
@RestController
@CrossOrigin(origins = "*")

public class TodobackendApplication {

	private Logger logger = LoggerFactory.getLogger(TodobackendApplication.class);

	private Tracer tracer;
	private ContextPropagators contextPropagators;

	@Value("${HOSTNAME:not_set}")
	String hostname;

	@Value("${spring.profiles.active: none}")
	String profile;

	@Autowired
	TodoRepository todoRepository;

	public TodobackendApplication(OpenTelemetry openTelemetry) {

		this.tracer = openTelemetry.getTracer(TodobackendApplication.class.getName(), "0.1.0");
		this.contextPropagators = openTelemetry.getPropagators();
	}

	private String getInstanceId() {

		if (!hostname.equals("not_set"))
			return hostname;
		return "probably localhost";

	}

	@GetMapping("/hello")
	String hello() {

		return getInstanceId() + " Hallo, Welt ! ";
	}

	@GetMapping("/fail")
	String fail() {

		System.exit(1);
		return "fixed!";
	}

	@GetMapping("/todos/")
	List<String> getTodos() {

		List<String> todos = new ArrayList<String>();

		todoRepository.findAll().forEach(todo -> todos.add(todo.getTodo()));
		logger.info("GET /todos/ " + todos.toString());

		return todos;
	}

	@PostMapping("/todos/{todo}")
	String addTodo(HttpServletRequest request, HttpServletResponse response, @PathVariable String todo) {

		Context parentContext = contextPropagators.getTextMapPropagator()
				.extract(Context.current(), request, new HttpRequestGetter());

		Span span = tracer.spanBuilder("addTodo")
				.setParent(parentContext)
				.setSpanKind(SpanKind.SERVER)
				.startSpan();

		boolean valid = this.isValid(todo);
		span.addEvent("todo validated", Attributes.of(booleanKey("valid"), valid));

		span.setAttribute(HttpAttributes.HTTP_REQUEST_METHOD, request.getMethod());
		span.setAttribute(HttpAttributes.HTTP_ROUTE, request.getRequestURL().toString());
		span.setAttribute(ClientAttributes.CLIENT_ADDRESS, request.getRemoteAddr());
		span.setAttribute(UserAgentAttributes.USER_AGENT_ORIGINAL, request.getHeader("User-Agent"));

		try (Scope scope = span.makeCurrent()) {
			this.someInternalMethod(todo);
			response.setStatus(HttpServletResponse.SC_CREATED);
			span.setAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, HttpServletResponse.SC_CREATED);
		} catch (Throwable t) {
			span.setStatus(StatusCode.ERROR, "Error on server side!");
			span.recordException(t);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			span.setAttribute(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		} finally {
			span.end();
		}

		logger.info("POST /todos/ "+todo.toString());
		logger.info("Span.toString():"+span.toString());

		return todo;
	}

	String someInternalMethod(String todo) {

		Span childSpan = tracer.spanBuilder("someInternalMethod").setSpanKind(SpanKind.INTERNAL).startSpan();

		todoRepository.save(new Todo(todo));

		if (todo.equals("slow")) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if (todo.equals("fail")) {

			System.out.println("Failing ...");
			throw new RuntimeException();

		}

		logger.info("childSpan.toString():" + childSpan.toString());
		childSpan.end();
		return todo;

	}

	boolean isValid(String todo) {
		return todo != null && !todo.isBlank();
	}

	@DeleteMapping("/todos/{todo}")
	String removeTodo(@PathVariable String todo) {

		todoRepository.deleteById(todo);
		logger.info("DELETE /todos/ " + todo.toString());
		return "removed " + todo;

	}

	public static void main(String[] args) {
		SpringApplication.run(TodobackendApplication.class, args);
	}

}

class HttpRequestGetter implements TextMapGetter<HttpServletRequest> {

	@Override
	public Iterable<String> keys(HttpServletRequest carrier) {
		return Collections.list(carrier.getHeaderNames());
	}

	@Override
	public String get(HttpServletRequest carrier, String key) {
		return carrier.getHeader(key);
	}
}

@Entity
class Todo {

	@Id
	String todo;

	public Todo() {
	}

	public Todo(String todo) {
		this.todo = todo;
	}

	public String getTodo() {
		return todo;
	}

	public void setTodo(String todo) {
		this.todo = todo;
	}

}

interface TodoRepository extends CrudRepository<Todo, String> {

}