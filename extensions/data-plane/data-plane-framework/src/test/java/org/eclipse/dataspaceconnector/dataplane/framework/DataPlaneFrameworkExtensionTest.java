package org.eclipse.dataspaceconnector.dataplane.framework;

import org.eclipse.dataspaceconnector.boot.system.injection.ReflectiveObjectFactory;
import org.eclipse.dataspaceconnector.dataplane.framework.manager.DataPlaneManagerImpl;
import org.eclipse.dataspaceconnector.dataplane.framework.pipeline.PipelineServiceImpl;
import org.eclipse.dataspaceconnector.dataplane.spi.manager.DataPlaneManager;
import org.eclipse.dataspaceconnector.dataplane.spi.pipeline.PipelineService;
import org.eclipse.dataspaceconnector.junit.launcher.DependencyInjectionExtension;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(DependencyInjectionExtension.class)
class DataPlaneFrameworkExtensionTest {


    @Test
    void initialize_registers_PipelineService(ServiceExtensionContext context, ReflectiveObjectFactory factory) {
        var extension = factory.constructInstance(DataPlaneFrameworkExtension.class);
        extension.initialize(context);

        assertThat(context.getService(PipelineService.class)).isInstanceOf(PipelineServiceImpl.class);
    }

    @Test
    void initialize_registers_DataPlaneManager(ServiceExtensionContext context, ReflectiveObjectFactory factory) {
        var extension = factory.constructInstance(DataPlaneFrameworkExtension.class);
        extension.initialize(context);
        assertThat(context.getService(DataPlaneManager.class)).isInstanceOf(DataPlaneManagerImpl.class);
    }
}