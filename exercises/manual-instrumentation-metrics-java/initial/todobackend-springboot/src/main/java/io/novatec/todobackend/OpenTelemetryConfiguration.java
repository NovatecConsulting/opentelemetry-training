package io.novatec.todobackend;

import java.time.Duration;

import io.opentelemetry.exporter.logging.LoggingMetricExporter;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//Basic Otel API & SDK
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.ServiceAttributes;

@Configuration
public class OpenTelemetryConfiguration {

    @Bean
    public OpenTelemetry openTelemetry(){

        Resource resource = Resource.getDefault().toBuilder()
                .put(ServiceAttributes.SERVICE_NAME, "todobackend")
                .put(ServiceAttributes.SERVICE_VERSION, "0.1.0")
                .build();

        SdkMeterProvider sdkMeterProvider = SdkMeterProvider.builder()
                .registerMetricReader(
                        PeriodicMetricReader
                                .builder(LoggingMetricExporter.create())
                                .setInterval(Duration.ofSeconds(10))
                                .build())
                .setResource(resource)
                .build();

        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
                .setMeterProvider(sdkMeterProvider)
                .build();

        return openTelemetry;
    }

}