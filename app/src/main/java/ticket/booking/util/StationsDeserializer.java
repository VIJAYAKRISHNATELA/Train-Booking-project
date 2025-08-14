package ticket.booking.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StationsDeserializer extends JsonDeserializer<List<String>> {
    @Override
    public List<String> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        // If we encounter an object instead of an array
        if (p.currentToken() == JsonToken.START_OBJECT) {
            // Read it as a Map and extract the keys
            Map<String, Object> map = p.readValueAs(new TypeReference<Map<String, Object>>() {});
            return p.readValueAs(new TypeReference<List<String>>() {});
        }
        // Normal array handling
        else if (p.currentToken() == JsonToken.START_ARRAY) {
            return p.readValueAs(new TypeReference<List<String>>() {});
        }

        return new ArrayList<>(); // Empty list as fallback
    }
}