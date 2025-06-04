package status;
import java.util.Map;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ReadJson {
    private final Map<Integer, Status> statusMap;

    // this one is os dependent
    public ReadJson(String jsonFilePath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        statusMap = mapper.readValue(
            new File(jsonFilePath),
            new TypeReference<Map<Integer, Status>>() {}
        );
    }

    // i did this constructor for os dependency
    public ReadJson(InputStream in) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        statusMap = mapper.readValue(
            in,
            new TypeReference<Map<Integer, Status>>() {}
        );
    }

    public Status getStatus(int code) {
        return statusMap.getOrDefault(code, new Status("UNKNOWN", "No message for the code: " + code));
    }

    public void printAllStatuses() {
        statusMap.forEach((code, status) ->
            System.out.println(code + " → " + status)
        );
    }
}