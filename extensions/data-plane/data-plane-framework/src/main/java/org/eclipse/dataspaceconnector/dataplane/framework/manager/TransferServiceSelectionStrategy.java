package org.eclipse.dataspaceconnector.dataplane.framework.manager;

import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.TransferService;

import java.util.stream.Stream;

public interface TransferServiceSelectionStrategy {
    TransferService chooseTransferService(Stream<TransferService> transferServices);

    static TransferServiceSelectionStrategy selectFirst() {
        return (s) -> s.findFirst().orElse(null);
    }
}
