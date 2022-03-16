import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.dataspaceconnector.common.annotations.IntegrationTest;
import org.eclipse.dataspaceconnector.junit.launcher.EdcExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.lang.management.ManagementFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.common.testfixtures.TestUtils.getFreePort;

@IntegrationTest
@Tag("MicrometerIntegrationTest")
@ExtendWith(EdcExtension.class)
public class MicrometerExtensionIntegrationTest {
    private static final int CONNECTOR_PORT = getFreePort();
    private static final String CONNECTOR_URL = String.format("http://localhost:%s", CONNECTOR_PORT);
    private static final String CALL_HEALTH_ENDPOINT = String.format("%s/api/callHealth?connectorUrl=%s", CONNECTOR_URL, CONNECTOR_URL);
    private static final String HEALTH_ENDPOINT = String.format("%s/api/check/health", CONNECTOR_URL);
    private static final String METRICS_ENDPOINT = "http://localhost:9464/metrics";
    private final OkHttpClient httpClient = new OkHttpClient();

    @BeforeAll
    static void checkForAgent() {
        var runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        assertThat(runtimeMXBean.getInputArguments())
                .withFailMessage("OpenTelemetry Agent JAR should be present. See README.md file for details.")
                .anyMatch(arg -> arg.startsWith("-javaagent"));
    }

    @BeforeEach
    void before(EdcExtension extension) {
        System.setProperty("web.http.port", Integer.toString(CONNECTOR_PORT));
        extension.registerSystemExtension(ServiceExtension.class, new HealthCallerExtension());
    }

    @Test
    void testMicrometerMetrics() throws IOException {
        // Call the callHealthEndpoint. After receiving this call, the connector will call the health endpoint.
        httpClient.newCall(new Request.Builder().url(CALL_HEALTH_ENDPOINT).build()).execute();

        // Collect the metrics.
        Request request =  new Request.Builder().url(METRICS_ENDPOINT).get().build();
        Response response = httpClient.newCall(request).execute();
        String[] metrics = response.body().string().split("\n");

        assertThat(metrics)
                // Executor metrics
                .anyMatch(s -> s.startsWith("executor_")) // ExecutorMetrics added by MicrometerExtension
                // System metrics
                .anyMatch(s -> s.startsWith("jvm_memory_")) // JvmMemoryMetrics added by MicrometerExtension
                .anyMatch(s -> s.startsWith("jvm_gc")) // JvmGcMetrics added by MicrometerExtension
                .anyMatch(s -> s.startsWith("system_cpu_")) // ProcessorMetrics added by MicrometerExtension
                .anyMatch(s -> s.startsWith("jvm_threads_")) // JvmThreadMetrics added by MicrometerExtension
                // Jetty and Jersey metrics
                .anyMatch(s -> s.startsWith("jetty_")) // See JettyMicrometerExtension
                .anyMatch(s -> s.startsWith("jersey_")) // See JerseyMicrometerExtension
                // Make sure that the connector HTTP client metrics are present and that the health endpoint call is tracked.
                .anyMatch(s -> s.startsWith("http_client_") && s.contains(HEALTH_ENDPOINT));
    }
}
