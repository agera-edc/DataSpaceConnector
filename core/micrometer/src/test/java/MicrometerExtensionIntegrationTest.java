import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.dataspaceconnector.azure.testfixtures.annotations.OpenTelemetryIntegrationTest;
import org.eclipse.dataspaceconnector.junit.launcher.EdcExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.lang.management.ManagementFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.common.testfixtures.TestUtils.getFreePort;

@OpenTelemetryIntegrationTest
@ExtendWith(EdcExtension.class)
public class MicrometerExtensionIntegrationTest {
    static final int CONNECTOR_PORT = getFreePort();
    static final String HEALTH_ENDPOINT = String.format("http://localhost:%s/api/check/health", CONNECTOR_PORT);
    static final String METRICS_ENDPOINT = "http://localhost:9464/metrics";
    static final String[] METRIC_PREFIXES = new String[] {
            "executor_", // ExecutorMetrics added by MicrometerExtension
            "jvm_memory_", // JvmMemoryMetrics added by MicrometerExtension
            "jvm_gc", // JvmGcMetrics added by MicrometerExtension
            "system_cpu_", // ProcessorMetrics added by MicrometerExtension
            "jvm_threads_", // JvmThreadMetrics added by MicrometerExtension
            "jetty_", // Added by JettyMicrometerExtension
            "jersey_", // Added by JerseyMicrometerExtension
            "http_client_"}; // OkHttp metrics

    @BeforeAll
    static void checkForAgent() {
        var runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        assertThat(runtimeMxBean.getInputArguments())
                .withFailMessage("OpenTelemetry Agent JAR should be present. See README.md file for details.")
                .anyMatch(arg -> arg.startsWith("-javaagent"));
    }

    @BeforeEach
    void before() {
        System.setProperty("web.http.port", Integer.toString(CONNECTOR_PORT));
    }

    @Test
    void testMicrometerMetrics(OkHttpClient httpClient) throws IOException {
        // Call the health endpoint with the client used by the connector. This is needed to check if OkHttp metrics are present.
        Response healthResponse = httpClient.newCall(new Request.Builder().url(HEALTH_ENDPOINT).build()).execute();
        // The call needs to be successful to have Jetty & Jersey metrics.
        assertThat(healthResponse.code()).isEqualTo(200);

        // Collect the metrics.
        Response response = httpClient.newCall(new Request.Builder().url(METRICS_ENDPOINT).build()).execute();
        String[] metrics = response.body().string().split("\n");

        for (String metricPrefix : METRIC_PREFIXES) {
            assertThat(metrics)
                    .withFailMessage(String.format("There is no metric starting by %s. ", metricPrefix))
                    .anyMatch(s -> s.startsWith(metricPrefix));
        }
    }
}
