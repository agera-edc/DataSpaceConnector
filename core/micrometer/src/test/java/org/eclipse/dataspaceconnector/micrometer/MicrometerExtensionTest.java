package org.eclipse.dataspaceconnector.micrometer;

import io.micrometer.core.instrument.binder.okhttp3.OkHttpMetricsEventListener;
import okhttp3.EventListener;
import org.eclipse.dataspaceconnector.core.executor.ExecutorInstrumentationImplementation;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.micrometer.MicrometerExtension.ENABLE_EXECUTOR_METRICS;
import static org.eclipse.dataspaceconnector.micrometer.MicrometerExtension.ENABLE_METRICS;
import static org.eclipse.dataspaceconnector.micrometer.MicrometerExtension.ENABLE_OKHTTP_METRICS;
import static org.eclipse.dataspaceconnector.micrometer.MicrometerExtension.ENABLE_SYSTEM_METRICS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MicrometerExtensionTest {

    MicrometerExtension extension;

    @BeforeEach
    void setUp() {
        extension = new MicrometerExtension();
    }

    @Test
    public void initialize_metricsDisabled() {
        var extensionContext = mock(ServiceExtensionContext.class);
        when(extensionContext.getSetting(ENABLE_METRICS, "true")).thenReturn("false");

        extension.initialize(extensionContext);

        verify(extensionContext, never()).registerService(any(), any());
    }

    @Test
    public void initialize_OkHttpMetrics() {
        var extensionContext = mock(ServiceExtensionContext.class);
        when(extensionContext.getSetting(ENABLE_METRICS, "true")).thenReturn("true");
        when(extensionContext.getSetting(ENABLE_SYSTEM_METRICS, "true")).thenReturn("true");
        when(extensionContext.getSetting(ENABLE_OKHTTP_METRICS, "true")).thenReturn("true");
        when(extensionContext.getSetting(ENABLE_EXECUTOR_METRICS, "true")).thenReturn("false");

        extension.initialize(extensionContext);

        var captor = ArgumentCaptor.forClass(EventListener.class);
        verify(extensionContext, times(1)).registerService(eq(EventListener.class), captor.capture());
        assertThat(captor.getValue()).isInstanceOf(OkHttpMetricsEventListener.class);
    }

    @Test
    public void initialize_ExecutorInstrumentation() {
        var extensionContext = mock(ServiceExtensionContext.class);
        when(extensionContext.getSetting(ENABLE_METRICS, "true")).thenReturn("true");
        when(extensionContext.getSetting(ENABLE_SYSTEM_METRICS, "true")).thenReturn("true");
        when(extensionContext.getSetting(ENABLE_OKHTTP_METRICS, "true")).thenReturn("false");
        when(extensionContext.getSetting(ENABLE_EXECUTOR_METRICS, "true")).thenReturn("true");
        when(extensionContext.getSetting(any(), any())).thenReturn("true");

        extension.initialize(extensionContext);

        var captor = ArgumentCaptor.forClass(ExecutorInstrumentationImplementation.class);
        verify(extensionContext, times(1)).registerService(eq(ExecutorInstrumentationImplementation.class), captor.capture());
        assertThat(captor.getValue()).isInstanceOf(MicrometerExecutorInstrumentation.class);
    }
}
