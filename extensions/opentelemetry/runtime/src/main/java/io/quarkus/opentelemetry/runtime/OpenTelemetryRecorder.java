package io.quarkus.opentelemetry.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.util.TypeLiteral;

import org.eclipse.microprofile.config.ConfigProvider;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.events.GlobalEventEmitterProvider;
import io.opentelemetry.api.logs.GlobalLoggerProvider;
import io.opentelemetry.context.ContextStorage;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.opentelemetry.runtime.config.runtime.OTelRuntimeConfig;
import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.NameIterator;
import io.smallrye.config.SmallRyeConfig;
import io.vertx.core.Vertx;

@Recorder
public class OpenTelemetryRecorder {

    public static final String OPEN_TELEMETRY_DRIVER = "io.opentelemetry.instrumentation.jdbc.OpenTelemetryDriver";

    /* STATIC INIT */
    public void resetGlobalOpenTelemetryForDevMode() {
        GlobalOpenTelemetry.resetForTest();
        GlobalLoggerProvider.resetForTest();
        GlobalEventEmitterProvider.resetForTest();
    }

    /* RUNTIME INIT */
    public void eagerlyCreateContextStorage() {
        ContextStorage.get();
    }

    /* RUNTIME INIT */
    public void storeVertxOnContextStorage(Supplier<Vertx> vertx) {
        QuarkusContextStorage.vertx = vertx.get();
    }

    /* RUNTIME INIT */
    public Function<SyntheticCreationalContext<OpenTelemetry>, OpenTelemetry> opentelemetryBean(
            OTelRuntimeConfig oTelRuntimeConfig) {
        return new Function<>() {
            @Override
            public OpenTelemetry apply(SyntheticCreationalContext<OpenTelemetry> context) {
                Instance<AutoConfiguredOpenTelemetrySdkBuilderCustomizer> builderCustomizers = context
                        .getInjectedReference(new TypeLiteral<>() {
                        });

                final Map<String, String> oTelConfigs = getOtelConfigs();

                if (oTelRuntimeConfig.sdkDisabled()) {
                    return AutoConfiguredOpenTelemetrySdk.builder()
                            .setResultAsGlobal(true)
                            .registerShutdownHook(false)
                            .addPropertiesSupplier(() -> oTelConfigs)
                            .build()
                            .getOpenTelemetrySdk();
                }

                var builder = AutoConfiguredOpenTelemetrySdk.builder()
                        .setResultAsGlobal(true)
                        .registerShutdownHook(false)
                        .addPropertiesSupplier(() -> oTelConfigs)
                        .setServiceClassLoader(Thread.currentThread().getContextClassLoader());
                for (var customizer : builderCustomizers) {
                    customizer.customize(builder);
                }

                return builder.build().getOpenTelemetrySdk();
            }

            private Map<String, String> getOtelConfigs() {
                Map<String, String> oTelConfigs = new HashMap<>();
                SmallRyeConfig config = ConfigProvider.getConfig().unwrap(SmallRyeConfig.class);

                // instruct OTel that we are using the AutoConfiguredOpenTelemetrySdk
                oTelConfigs.put("otel.java.global-autoconfigure.enabled", "true");

                // load new properties
                for (String propertyName : config.getPropertyNames()) {
                    if (propertyName.startsWith("quarkus.otel.")) {
                        ConfigValue configValue = config.getConfigValue(propertyName);
                        if (configValue.getValue() != null) {
                            NameIterator name = new NameIterator(propertyName);
                            name.next();
                            oTelConfigs.put(name.getName().substring(name.getPosition() + 1), configValue.getValue());
                        }
                    }
                }
                return oTelConfigs;
            }
        };
    }

}
