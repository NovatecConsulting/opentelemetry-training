package io.novatec.todobackend;

import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;
import java.util.ArrayList;
import java.util.List;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.ObservableDoubleGauge;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.common.Attributes;
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

import static io.opentelemetry.api.common.AttributeKey.stringKey;

@SpringBootApplication
@RestController
@CrossOrigin(origins = "*")

public class TodobackendApplication {

	private Logger logger = LoggerFactory.getLogger(TodobackendApplication.class);

	private OpenTelemetry openTelemetry;
	private Meter meter;
	private LongCounter counter;
	private LongCounter errorCounter;
	private LongHistogram requestDuration;
	private ObservableDoubleGauge cpuLoad;

	@Value("${HOSTNAME:not_set}")
	String hostname;

	@Value("${spring.profiles.active: none}")
	String profile;

	@Autowired
	TodoRepository todoRepository;

	public TodobackendApplication(OpenTelemetry openTelemetry) {

		this.openTelemetry = openTelemetry;
		meter = openTelemetry.getMeter(TodobackendApplication.class.getName());

		counter = meter.counterBuilder("todobackend.requests.counter")
				.setDescription("How many times the GET call has been invoked")
				.setUnit("requests")
				.build();

		errorCounter = meter.counterBuilder("todobackend.requests.errors")
				.setDescription("How many times an error occurred")
				.setUnit("requests")
				.build();

		requestDuration = meter.histogramBuilder("http.server.request.duration")
				.setDescription("How long was a request processed on server side")
				.setUnit("ms")
				.ofLongs()
				.build();

		cpuLoad = meter.gaugeBuilder("system.cpu.utilization")
				.setDescription("The current system cpu utilization")
				.setUnit("percent")
				.buildWithCallback((measurement) -> measurement.record(this.getCpuLoad()));
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

		List<String> todos = new ArrayList<>();

		todoRepository.findAll().forEach(todo -> todos.add(todo.getTodo()));
		logger.info("GET /todos/ " + todos.toString());

		return todos;
	}

	@PostMapping("/todos/{todo}")
	String addTodo(HttpServletRequest request, HttpServletResponse response, @PathVariable String todo){

		long start = System.currentTimeMillis();

		counter.add(1, Attributes.of(stringKey("todo"), todo));
		this.someInternalMethod(todo);

		logger.info("POST /todos/ "+todo.toString());

		long duration = System.currentTimeMillis() - start;
		requestDuration.record(duration);
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

			errorCounter.add(1);
			System.out.println("Failing ...");
			throw new RuntimeException();
		} 

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

	double getCpuLoad() {
		OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
		return osBean.getCpuLoad() * 100;
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