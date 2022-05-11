package org.eclipse.dataspaceconnector.dataplane.kafka.serializers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

public class JacksonDeserializer implements Deserializer<JsonNode> {

    private ObjectMapper objectMapper;

    public JacksonDeserializer() {
    }

    @Override
    public void configure(Map<String, ?> props, boolean isKey) {
        objectMapper = new ObjectMapper();
    }


    @Override
    public JsonNode deserialize(String topic, byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }

        try {
            return objectMapper.readTree(data);
        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public void close() {
    }
}