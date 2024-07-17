package io.novatec.todobackend;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//Basic Otel
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
//Tracing and Spans
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.semconv.ResourceAttributes;


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
			.buildAndRegisterGlobal();

		return openTelemetry;
	}

}
