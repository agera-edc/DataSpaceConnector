package org.eclipse.dataspaceconnector.tests;

import com.github.javafaker.Faker;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

public class FileTransferAsClientSimulation extends FileTransferSimulation {

    private Faker faker = new Faker();

    public FileTransferAsClientSimulation() {
        super(
                get("CONSUMER_URL"),
                get("PROVIDER_URL"),
                get("DESTINATION_PATH"),
                get("API_KEY"),
                1,
                1);
    }

    private static String get(String env) {
        return Objects.requireNonNull(StringUtils.trimToNull(System.getenv(env)), env);
    }
}
