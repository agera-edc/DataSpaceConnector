package org.eclipse.dataspaceconnector.azure.dataplane.azuredatafactory;

import com.azure.core.credential.TokenCredential;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.datafactory.DataFactoryManager;
import com.azure.resourcemanager.resources.models.GenericResource;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.github.javafaker.Faker;
import org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.schema.AzureBlobStoreSchema;
import org.eclipse.dataspaceconnector.spi.monitor.Monitor;
import org.eclipse.dataspaceconnector.spi.types.domain.DataAddress;
import org.eclipse.dataspaceconnector.spi.types.domain.transfer.DataFlowRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Clock;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class AzureDataFactoryTransferServiceImplTest {

    private DataFactoryManager dataFactoryManager;
    private TokenCredential tokenCredential;
    private AzureProfile azureProfile;
    private Monitor monitor;
    private GenericResource factory;
    private SecretClient secretClient;
    private Clock clock;
    private AzureDataFactoryTransferServiceImpl azureDataFactoryTransferService;

    private static final Faker FAKER = new Faker();

    @BeforeEach
    public void setUp() {
        tokenCredential = mock(TokenCredential.class);
        monitor = mock(Monitor.class);
        factory = mock(GenericResource.class);
        clock = mock(Clock.class);

        secretClient = new SecretClientBuilder()
                .vaultUrl(format("https://%s", FAKER.internet().url()))
                .credential(tokenCredential)
                .buildClient();
        azureProfile = new AzureProfile(AzureEnvironment.AZURE);
        dataFactoryManager = DataFactoryManager.authenticate(tokenCredential, azureProfile);
        azureDataFactoryTransferService = new AzureDataFactoryTransferServiceImpl(
                monitor,
                dataFactoryManager,
                factory,
                secretClient,
                FAKER.lorem().word(), Duration.ofMillis(FAKER.number().numberBetween(1, 10)),
                clock);
    }

    @Test
    void canHandle_validRequest() {
        var source = createDataAddress(AzureBlobStoreSchema.TYPE, Collections.emptyMap());
        var destination = createDataAddress(AzureBlobStoreSchema.TYPE, Collections.emptyMap());
        var request = createRequest(Collections.emptyMap(), source.build(), destination.build());

        assertThat(azureDataFactoryTransferService.canHandle(request.build())).isEqualTo(true);

    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("provideInvalidDataAddressType")
    void canHandle_invalidRequest(String name, String sourceType, String destinationType) {
        var source = createDataAddress(sourceType, Collections.emptyMap());
        var destination = createDataAddress(destinationType, Collections.emptyMap());
        var request = createRequest(Collections.emptyMap(), source.build(), destination.build());

        assertThat(azureDataFactoryTransferService.canHandle(request.build())).isEqualTo(false);

    }

    private static Stream<Arguments> provideInvalidDataAddressType() {
        return Stream.of(
                Arguments.of("Invalid source and valid destination", FAKER.lorem().word(), AzureBlobStoreSchema.TYPE),
                Arguments.of("Valid source and invalid destination", AzureBlobStoreSchema.TYPE, FAKER.lorem().word()),
                Arguments.of("Invalid source and destination", FAKER.lorem().word(), FAKER.lorem().word())
        );
    }

    /**
     * Helper method to create {@link DataFlowRequest}
     *
     * @param properties  Map of request properties
     * @param source      Source data address {@link DataAddress}
     * @param destination Destination data address {@link DataAddress}
     * @return Builder of {@link DataFlowRequest}
     */
    private static DataFlowRequest.Builder createRequest(Map<String, String> properties, DataAddress source, DataAddress destination) {
        return DataFlowRequest.Builder.newInstance()
                .id(FAKER.internet().uuid())
                .processId(FAKER.internet().uuid())
                .properties(properties)
                .sourceDataAddress(source)
                .destinationDataAddress(destination)
                .trackable(true);
    }

    /**
     * Helper method to create {@link DataAddress}
     *
     * @param type       Type of data address e.g. HttpData, AzureStorageBlobData
     * @param properties Map of properties
     * @return Builder of {@link DataAddress}
     */
    private static DataAddress.Builder createDataAddress(String type, Map<String, String> properties) {
        return DataAddress.Builder.newInstance()
                .type(type)
                .properties(properties);
    }
}
