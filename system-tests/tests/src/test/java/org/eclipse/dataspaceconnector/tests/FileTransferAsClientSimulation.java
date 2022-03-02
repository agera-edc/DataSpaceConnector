package org.eclipse.dataspaceconnector.tests;

import com.github.javafaker.Faker;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;
import static io.gatling.javaapi.core.CoreDsl.global;
import static io.gatling.javaapi.http.HttpDsl.http;

public class FileTransferAsClientSimulation extends FileTransferSimulation {

    private Faker faker = new Faker();

    public FileTransferAsClientSimulation() {
        super(
                getFromEnv("PROVIDER_URL"),
                getFromEnv("DESTINATION_PATH"),
                getFromEnv("API_KEY"),
                1);

        setUp(scenarioBuilder
                .injectOpen(atOnceUsers(1)))
                .protocols(http
                        .baseUrl(getFromEnv("CONSUMER_URL")))
                .assertions(
                        global().responseTime().max().lt(2000),
                        global().successfulRequests().percent().is(100.0)
                );
    }

    private static String getFromEnv(String env) {
        return Objects.requireNonNull(StringUtils.trimToNull(System.getenv(env)), env);
    }
}
