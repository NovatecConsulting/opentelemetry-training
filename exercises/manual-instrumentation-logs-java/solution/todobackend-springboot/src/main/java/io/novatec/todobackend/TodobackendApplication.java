package io.novatec.todobackend;

import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
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

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

@SpringBootApplication
@RestController
@CrossOrigin(origins = "*")

public class TodobackendApplication {

	private Logger logger = LoggerFactory.getLogger(TodobackendApplication.class);

	private Tracer tracer;
	private io.opentelemetry.api.logs.Logger otelLogger;

	@Value("${HOSTNAME:not_set}")
	String hostname;

	@Value("${spring.profiles.active: none}")
	String profile;

	@Autowired
	TodoRepository todoRepository;

	@Autowired
	public TodobackendApplication(OpenTelemetry openTelemetry) {

		this.tracer = openTelemetry.getTracer(TodobackendApplication.class.getName(), "0.1.0");
		this.otelLogger = openTelemetry.getLogsBridge().loggerBuilder(TodobackendApplication.class.getName()).build();
		OpenTelemetryAppender.install(openTelemetry);
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

		Span span = tracer.spanBuilder("getTodos").startSpan();
		List<String> todos = new ArrayList<>();

		try (Scope scope = span.makeCurrent()) {

			todoRepository.findAll().forEach(todo -> todos.add(todo.getTodo()));
			logger.info("GET /todos/ " + todos.toString());

			otelLogger.logRecordBuilder()
					.setAttribute(stringKey("http.request.method"), "GET")
					.setTimestamp(Instant.now())
					.setSeverity(Severity.INFO)
					.setBody("GET /todos/")
					.emit();
		} finally {
			span.end();
		}

		return todos;
	}

	@PostMapping("/todos/{todo}")
	String addTodo(HttpServletRequest request, HttpServletResponse response, @PathVariable String todo){

		this.someInternalMethod(todo);

		logger.info("POST /todos/ "+todo.toString());

		return todo;
	} 

	String someInternalMethod(String todo){

		todoRepository.save(new Todo(todo));
		
		if(todo.equals("slow")){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} 		
		if(todo.equals("fail")){

			System.out.println("Failing ...");
			throw new RuntimeException();
		} 

		return todo;
	}

	boolean isValid(String todo) {

		return todo != null && !todo.isBlank();
	}

	double getCpuLoad() {

		OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
		return osBean.getCpuLoad() * 100;
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