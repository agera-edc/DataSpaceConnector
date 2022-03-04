package org.eclipse.dataspaceconnector.dataplane.framework.manager;

import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.TransferService;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.dataplane.framework.e2e.EndToEndTest.createRequest;
import static org.mockito.Mockito.mock;

class TransferServiceSelectionStrategyTest {

    TransferServiceSelectionStrategy strategy = TransferServiceSelectionStrategy.selectFirst();
    DataFlowRequest request = createRequest("1").build();
    TransferService service1 = mock(TransferService.class);
    TransferService service2 = mock(TransferService.class);

    @Test
    void selectFirst_withNoItems() {
        assertThat(strategy.chooseTransferService(request, Stream.of())).isNull();
    }

    @Test
    void selectFirst_withOneItem() {
        assertThat(strategy.chooseTransferService(request, Stream.of(service1))).isEqualTo(service1);
    }

    @Test
    void selectFirst_withMultipleItems() {
        assertThat(strategy.chooseTransferService(request, Stream.of(service1, service2))).isEqualTo(service1);
    }
}