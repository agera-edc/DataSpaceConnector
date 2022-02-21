package org.eclipse.dataspaceconnector.micrometer;

import io.micrometer.core.instrument.binder.okhttp3.OkHttpMetricsEventListener;
import okhttp3.EventListener;
import org.eclipse.dataspaceconnector.core.executor.ExecutorInstrumentationImplementation;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.micrometer.MicrometerExtension.ENABLE_EXECUTOR_METRICS;
import static org.eclipse.dataspaceconnector.micrometer.MicrometerExtension.ENABLE_METRICS;
import static org.eclipse.dataspaceconnector.micrometer.MicrometerExtension.ENABLE_OKHTTP_METRICS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MicrometerExtensionTest {

    MicrometerExtension extension;
    ServiceExtensionContext extensionContextMock;

    @BeforeEach
    void setUp() {
        extension = new MicrometerExtension();
        extensionContextMock = mock(ServiceExtensionContext.class);
        // default mocking
        when(extensionContextMock.getSetting(anyString(), eq("true"))).thenReturn("true");
    }

    @Test
    public void initialize_metricsDisabled() {
        when(extensionContextMock.getSetting(eq(ENABLE_METRICS), anyString())).thenReturn("false");

        extension.initialize(extensionContextMock);

        verify(extensionContextMock, never()).registerService(any(), any());
    }

    @Test
    public void initialize_default() {
        var extensionContext = mock(ServiceExtensionContext.class);

        extension.initialize(extensionContext);

        verifyOkHttpMetricsConfigured(extensionContext);
        verifyExecutorInstrumentationConfigured(extensionContext);
    }

    @Test
    public void initialize_OkHttpMetrics() {
        var extensionContext = mock(ServiceExtensionContext.class);
        when(extensionContext.getSetting(anyString(), anyString())).thenReturn("false");
        when(extensionContext.getSetting(eq(ENABLE_METRICS), anyString())).thenReturn("true");
        when(extensionContext.getSetting(eq(ENABLE_OKHTTP_METRICS), anyString())).thenReturn("true");

        extension.initialize(extensionContext);

        verifyOkHttpMetricsConfigured(extensionContext);
    }

    @Test
    public void initialize_ExecutorInstrumentation() {
        var extensionContext = mock(ServiceExtensionContext.class);
        when(extensionContext.getSetting(anyString(), anyString())).thenReturn("false");
        when(extensionContext.getSetting(eq(ENABLE_METRICS), anyString())).thenReturn("true");
        when(extensionContext.getSetting(eq(ENABLE_EXECUTOR_METRICS), anyString())).thenReturn("true");

        extension.initialize(extensionContext);

        verifyExecutorInstrumentationConfigured(extensionContext);
    }

    private void verifyOkHttpMetricsConfigured(ServiceExtensionContext extensionContext) {
        var captor = ArgumentCaptor.forClass(EventListener.class);
        verify(extensionContext, times(1)).registerService(eq(EventListener.class), captor.capture());
        assertThat(captor.getValue()).isInstanceOf(OkHttpMetricsEventListener.class);
    }

    private void verifyExecutorInstrumentationConfigured(ServiceExtensionContext extensionContext) {
        var captor = ArgumentCaptor.forClass(ExecutorInstrumentationImplementation.class);
        verify(extensionContext, times(1)).registerService(eq(ExecutorInstrumentationImplementation.class), captor.capture());
        assertThat(captor.getValue()).isInstanceOf(MicrometerExecutorInstrumentation.class);
    }
}
