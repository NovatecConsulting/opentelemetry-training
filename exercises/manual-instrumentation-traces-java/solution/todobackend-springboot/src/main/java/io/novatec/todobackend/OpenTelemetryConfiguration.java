package io.novatec.todobackend;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

//Basic OTel API & SDK
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;

//Tracing and Spans
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.semconv.ServiceAttributes;

//Exporter
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;

//Propagation
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;

@Configuration
public class OpenTelemetryConfiguration {

	@Bean
	@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
	public OpenTelemetry openTelemetry() {

		Resource resource = Resource.getDefault().toBuilder()
				.put(ServiceAttributes.SERVICE_NAME, "todobackend")
				.put(ServiceAttributes.SERVICE_VERSION, "0.1.0")
				.build();

		OtlpGrpcSpanExporter jaegerOtlpExporter = OtlpGrpcSpanExporter.builder()
				.setEndpoint("http://localhost:4317")
				.setTimeout(30, TimeUnit.SECONDS)
				.build();

		SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
				.addSpanProcessor(SimpleSpanProcessor.create(LoggingSpanExporter.create()))
				.addSpanProcessor(SimpleSpanProcessor.create(jaegerOtlpExporter))
				.setResource(resource)
				.build();

		ContextPropagators contextPropagators = ContextPropagators.create(W3CTraceContextPropagator.getInstance());

		OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
				.setTracerProvider(sdkTracerProvider)
				.setPropagators(contextPropagators)
				.build();

		return openTelemetry;
	}

}
