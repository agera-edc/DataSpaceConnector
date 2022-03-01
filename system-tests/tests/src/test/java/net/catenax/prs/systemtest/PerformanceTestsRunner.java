package net.catenax.prs.systemtest;

import com.github.javafaker.Faker;

import static org.eclipse.dataspaceconnector.tests.FileTransferIntegrationTest.*;

public abstract class PerformanceTestsRunner extends PerformanceSimulation {

    private Faker faker = new Faker();

    public PerformanceTestsRunner() {
        super(
                CONSUMER_CONNECTOR_HOST,
                PROVIDER_CONNECTOR_HOST,
                CONSUMER_ASSET_PATH,
                API_KEY_CONTROL_AUTH,
                1,
                1);
    }

}
