package org.eclipse.dataspaceconnector.tests;

import static org.eclipse.dataspaceconnector.tests.FileTransferIntegrationTest.API_KEY_CONTROL_AUTH;
import static org.eclipse.dataspaceconnector.tests.FileTransferIntegrationTest.CONSUMER_ASSET_PATH;
import static org.eclipse.dataspaceconnector.tests.FileTransferIntegrationTest.CONSUMER_CONNECTOR_HOST;
import static org.eclipse.dataspaceconnector.tests.FileTransferIntegrationTest.PROVIDER_CONNECTOR_HOST;

public class FileTransferLocalSimulation extends FileTransferSimulation {

    public FileTransferLocalSimulation() {
        super(
                CONSUMER_CONNECTOR_HOST,
                PROVIDER_CONNECTOR_HOST,
                CONSUMER_ASSET_PATH,
                API_KEY_CONTROL_AUTH,
                1,
                1);
    }
}
