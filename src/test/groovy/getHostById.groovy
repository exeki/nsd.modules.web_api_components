import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper

String json = "{\"name\":\"John Doe\",\"age\":30,\"email\":\"john@example.com\"}";

ObjectMapper objectMapper = new ObjectMapper();
Map<String, Object> map = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});

// Выводим содержимое Map
for (Map.Entry<String, Object> entry : map.entrySet()) {
    System.out.println(entry.getKey() + ": " + entry.getValue());
}
