package org.eclipse.dataspaceconnector.dataplane.framework;

import org.eclipse.dataspaceconnector.boot.system.injection.ReflectiveObjectFactory;
import org.eclipse.dataspaceconnector.dataplane.framework.manager.DataPlaneManagerImpl;
import org.eclipse.dataspaceconnector.dataplane.framework.manager.TransferServiceSelectionStrategy;
import org.eclipse.dataspaceconnector.dataplane.framework.pipeline.PipelineServiceImpl;
import org.eclipse.dataspaceconnector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.dataspaceconnector.junit.launcher.DependencyInjectionExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.dataplane.framework.e2e.EndToEndTest.createRequest;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
class DataPlaneFrameworkExtensionTest {


    @Test
    void initialize_registers_PipelineService(ServiceExtensionContext context, ReflectiveObjectFactory factory) {
        var extension = factory.constructInstance(DataPlaneFrameworkExtension.class);
        extension.initialize(context);

        assertThat(context.getService(PipelineService.class)).isInstanceOf(PipelineServiceImpl.class);
    }

    @Test
    void initialize_registers_DataPlaneManager_withDefaultStrategy(ServiceExtensionContext context, ReflectiveObjectFactory factory) {
        var extension = factory.constructInstance(DataPlaneFrameworkExtension.class);
        extension.initialize(context);
        assertThat(context.getService(DataPlaneManager.class)).isInstanceOf(DataPlaneManagerImpl.class);
    }

    @Test
    void initialize_registers_DataPlaneManager_withProvidedStrategy(ServiceExtensionContext context, ReflectiveObjectFactory factory) {
        // Arrange
        TransferServiceSelectionStrategy strategy = mock(TransferServiceSelectionStrategy.class);
        context.registerService(TransferServiceSelectionStrategy.class, strategy);

        // Act
        var extension = factory.constructInstance(DataPlaneFrameworkExtension.class);
        extension.initialize(context);
        DataFlowRequest request = createRequest("1").build();
        context.getService(DataPlaneManager.class).validate(request);

        // Assert
        verify(strategy).chooseTransferService(eq(request), any(Stream.class));
    }
}