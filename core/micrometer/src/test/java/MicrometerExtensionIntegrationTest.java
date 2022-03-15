import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.eclipse.dataspaceconnector.junit.launcher.EdcExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(EdcExtension.class)
public class MicrometerExtensionIntegrationTest {

    private final OkHttpClient httpClient = new OkHttpClient();

    @Test
    public void testMicrometerMetrics() {

        var request =  new Request.Builder()
                .url("http://localhost:9464/metrics")
                .get()
                .build();

        var response = httpClient.newCall(request).execute();
        var metrics = response.body().string().split("\n");

        assertThat(metrics)
                .anyMatch(s -> s.startsWith("executor_"))
                .anyMatch(s -> s.startsWith("jvm_memory_"))
                .anyMatch(s -> s.startsWith("jvm_gc"))
                .anyMatch(s -> s.startsWith("system_cpu_"))
                .anyMatch(s -> s.startsWith("jvm_threads_"));

        for (String metric: metrics) {
            System.out.println(metric);
        }
    }
}
