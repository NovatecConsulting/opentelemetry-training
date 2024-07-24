package io.novatec.todobackend;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.repository.CrudRepository;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;

@SpringBootApplication
@RestController
@CrossOrigin(origins = "*")

public class TodobackendApplication {

	private Logger logger = LoggerFactory.getLogger(TodobackendApplication.class);

	@Value("${HOSTNAME:not_set}")
	String hostname;

	@Value("${spring.profiles.active: none}")
	String profile;

	@Autowired
	TodoRepository todoRepository;

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
	List<String> getTodos(){

		List<String> todos = new ArrayList<String>();

		todoRepository.findAll().forEach(todo -> todos.add(todo.getTodo()));
		logger.info("GET /todos/ "+todos.toString());


		return todos;
	}

	@PostMapping("/todos/old/{todo}")
	String addTodoOld(@PathVariable String todo) {

		//Context extractedContext = openTelemetry.getPropagators().getTextMapPropagator().extract(Context.current(), httpExchange, getter);

		Span span = tracer.spanBuilder("addTodo").setSpanKind(SpanKind.SERVER).startSpan();

		System.out.println("Span initial:"+span.toString());
		
		span.setAttribute("http.method", "POST");
		span.setAttribute("http.url", "/todos/{todo}");

		try (Scope scope = span.makeCurrent()) {

			this.someInternalMethod(todo);
			logger.info("POST /todos/ " + todo.toString());
			return todo;

		} catch (Throwable t) {
			span.setStatus(StatusCode.ERROR, "Something bad happened!");
			span.recordException(t);
		} finally {
			System.out.println("Span final:"+span.toString());
			span.end();
		}

		return "";

	}

	@PostMapping("/todos/{todo}")
	String addTodo(@PathVariable String todo){

		Span span = tracer.spanBuilder("addTodo").startSpan();
		
		this.someInternalMethod(todo);
		logger.info("POST /todos/ "+todo.toString());

		span.end();

		return todo;

	}

	String someInternalMethod(String todo) {

		Span childSpan = tracer.spanBuilder("someInternalMethod")
			//.setParent(Context.current().with(parentSpan))
			.startSpan();

		childSpan.setAttribute("Todo item", todo);

		todoRepository.save(new Todo(todo));
		
		childSpan.addEvent("Persisted to database");
		
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

		childSpan.addEvent("Finished checking todo item");
		childSpan.end();
		return todo;

	}

	@DeleteMapping("/todos/{todo}")
	String removeTodo(@PathVariable String todo) {

		todoRepository.deleteById(todo);
		logger.info("DELETE /todos/ "+todo.toString());
		return "removed "+todo;

	}

	public static void main(String[] args) {
		SpringApplication.run(TodobackendApplication.class, args);
	}
}

@Entity
class Todo{

	@Id
	String todo;

	public Todo(){}

	public Todo(String todo){
		this.todo = todo;
	}

	public String getTodo(){
		return todo;
	}

	public void setTodo(String todo) {
		this.todo = todo;
	}

}

interface TodoRepository extends CrudRepository<Todo, String> {

}