package org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition;

import com.github.javafaker.Faker;
import org.eclipse.dataspaceconnector.api.datamanagement.configuration.DataManagementApiConfiguration;
import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.model.TransferProcessDto;
import org.eclipse.dataspaceconnector.api.datamanagement.contractdefinition.transform.TransferProcessTransformerTestData;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformerRegistry;
import org.eclipse.dataspaceconnector.api.transformer.DtoTransformerRegistryImpl;
import org.eclipse.dataspaceconnector.junit.launcher.DependencyInjectionExtension;
import org.eclipse.dataspaceconnector.spi.WebService;
import org.eclipse.dataspaceconnector.spi.system.ServiceExtensionContext;
import org.eclipse.dataspaceconnector.spi.system.injection.ObjectFactory;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.TransferProcess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(DependencyInjectionExtension.class)
class TransferProcessApiExtensionTest {
    static Faker faker = new Faker();
    ServiceExtensionContext context;
    ObjectFactory factory;
    TransferProcessTransformerTestData data = new TransferProcessTransformerTestData();
    String contextAlias  = faker.lorem().word();

    @BeforeEach
    void setUp(ServiceExtensionContext context, ObjectFactory factory) {
        this.context = context;
        this.factory = factory;
    }

    @Test
    void initialize() {
        var registry = new DtoTransformerRegistryImpl();
        context.registerService(DtoTransformerRegistry.class, registry);
        var webServiceMock = mock(WebService.class);
        context.registerService(WebService.class, webServiceMock);
        var mockConfiguration = new DataManagementApiConfiguration(contextAlias);
        context.registerService(DataManagementApiConfiguration.class, mockConfiguration);

        var extension = factory.constructInstance(TransferProcessApiExtension.class);
        extension.initialize(context);

        verify(webServiceMock).registerResource(eq(contextAlias), any(TransferProcessApiController.class));

        assertThat(registry.transform(data.entity.build(), TransferProcessDto.class).succeeded()).isTrue();
        assertThat(registry.transform(data.dto.build(), TransferProcess.class).succeeded()).isTrue();
    }
}