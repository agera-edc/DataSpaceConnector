package org.eclipse.dataspaceconnector.dataplane.framework;

import org.eclipse.dataspaceconnector.dataplane.framework.manager.DataPlaneManagerImpl;
import org.eclipse.dataspaceconnector.dataplane.framework.pipeline.PipelineServiceImpl;
import org.eclipse.dataspaceconnector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DataPlaneFrameworkExtensionTest {

    DataPlaneFrameworkExtension extension = new DataPlaneFrameworkExtension(null);
    ServiceExtensionContext context = mock(ServiceExtensionContext.class);

    @Test
    void initialize_registers_PipelineService() {
        extension.initialize(context);
        verify(context).registerService(eq(PipelineService.class), any(PipelineServiceImpl.class));
    }

    @Test
    void initialize_registers_DataPlaneManager() {
        extension.initialize(context);
        verify(context).registerService(eq(DataPlaneManager.class), any(DataPlaneManagerImpl.class));
    }
}