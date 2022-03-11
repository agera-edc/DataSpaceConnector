package org.eclipse.dataspaceconnector.azure.dataplane.azuredatafactory;

import com.azure.core.credential.TokenCredential;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.datafactory.DataFactoryManager;
import com.azure.resourcemanager.resources.models.GenericResource;
import com.azure.security.keyvault.secrets.SecretClient;
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

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Base64.getEncoder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.schema.AzureBlobStoreSchema.ACCOUNT_NAME;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.schema.AzureBlobStoreSchema.CONTAINER_NAME;
import static org.eclipse.dataspaceconnector.azure.dataplane.azurestorage.schema.AzureBlobStoreSchema.SHARED_KEY;
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
        secretClient = mock(SecretClient.class);
        azureProfile = mock(AzureProfile.class);
        dataFactoryManager = mock(DataFactoryManager.class);
        azureDataFactoryTransferService = new AzureDataFactoryTransferServiceImpl(
                monitor,
                dataFactoryManager,
                factory,
                secretClient,
                FAKER.lorem().word(), Duration.ofMillis(FAKER.number().numberBetween(1, 10)),
                clock);
    }

    @Test
    void canHandle_success() {
        // Arrange
        var source = createDataAddress(AzureBlobStoreSchema.TYPE, Collections.emptyMap());
        var destination = createDataAddress(AzureBlobStoreSchema.TYPE, Collections.emptyMap());
        var request = createRequest(Collections.emptyMap(), source.build(), destination.build());
        // Act & Assert
        assertThat(azureDataFactoryTransferService.canHandle(request.build())).isEqualTo(true);

    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("provideInvalidDataAddressType")
    void canHandle_failure(String name, String sourceType, String destinationType) {
        // Arrange
        var source = createDataAddress(sourceType, Collections.emptyMap());
        var destination = createDataAddress(destinationType, Collections.emptyMap());
        var request = createRequest(Collections.emptyMap(), source.build(), destination.build());
        // Act & Assert
        assertThat(azureDataFactoryTransferService.canHandle(request.build())).isEqualTo(false);

    }

    @Test
    void validate_failure() {
        // Arrange
        var source = createDataAddress(AzureBlobStoreSchema.TYPE, Collections.emptyMap());
        var extraProp = "extra-property";
        var destination = createDataAddress(AzureBlobStoreSchema.TYPE, Map.of(
                ACCOUNT_NAME, "validaccount",
                CONTAINER_NAME, "validcontainer",
                SHARED_KEY, getEncoder().encodeToString("validkey".getBytes(StandardCharsets.UTF_8)),
                extraProp, FAKER.lorem().word()
        ));
        var request = createRequest(Collections.emptyMap(), source.build(), destination.build()).build();
        // Act
        var response = azureDataFactoryTransferService.validate(request);
        // Arrange
        assertThat(response.failed()).isTrue();
        assertThat(response.getFailureMessages()).containsOnly(format("Unexpected property %s", extraProp));
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
