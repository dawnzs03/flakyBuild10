/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.telemetry.tracing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.IOException;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;

/**
 * OTel based Telemetry provider
 */
public class OTelTracingTelemetry implements TracingTelemetry {

    private static final Logger logger = LogManager.getLogger(OTelTracingTelemetry.class);
    private final OpenTelemetry openTelemetry;
    private final io.opentelemetry.api.trace.Tracer otelTracer;

    /**
     * Creates OTel based Telemetry
     * @param openTelemetry OpenTelemetry instance
     */
    public OTelTracingTelemetry(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
        this.otelTracer = openTelemetry.getTracer("os-tracer");

    }

    @Override
    public void close() {
        try {
            ((Closeable) openTelemetry).close();
        } catch (IOException e) {
            logger.warn("Error while closing Opentelemetry", e);
        }
    }

    @Override
    public Span createSpan(SpanCreationContext spanCreationContext, Span parentSpan) {
        return createOtelSpan(spanCreationContext, parentSpan);
    }

    @Override
    public TracingContextPropagator getContextPropagator() {
        return new OTelTracingContextPropagator(openTelemetry);
    }

    private Span createOtelSpan(SpanCreationContext spanCreationContext, Span parentSpan) {
        io.opentelemetry.api.trace.Span otelSpan = otelSpan(
            spanCreationContext.getSpanName(),
            parentSpan,
            OTelAttributesConverter.convert(spanCreationContext.getAttributes()),
            OTelSpanKindConverter.convert(spanCreationContext.getSpanKind())
        );
        Span newSpan = new OTelSpan(spanCreationContext.getSpanName(), otelSpan, parentSpan);
        return newSpan;
    }

    io.opentelemetry.api.trace.Span otelSpan(
        String spanName,
        Span parentOTelSpan,
        io.opentelemetry.api.common.Attributes attributes,
        io.opentelemetry.api.trace.SpanKind spanKind
    ) {
        return parentOTelSpan == null || !(parentOTelSpan instanceof OTelSpan)
            ? otelTracer.spanBuilder(spanName).setAllAttributes(attributes).startSpan()
            : otelTracer.spanBuilder(spanName)
                .setParent(Context.current().with(((OTelSpan) parentOTelSpan).getDelegateSpan()))
                .setAllAttributes(attributes)
                .setSpanKind(spanKind)
                .startSpan();
    }
}
