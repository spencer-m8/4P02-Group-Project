package ca.group13.codecomparator.controller;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class HealthController {

    private final JdbcTemplate jdbc;
    private final BlobServiceClient blobServiceClient;
    private final String testContainer;

    public HealthController(JdbcTemplate jdbc, BlobServiceClient blobServiceClient, @Value("${app.storage.containers.test}") String testContainer) {
        this.jdbc = jdbc;
        this.blobServiceClient = blobServiceClient;
        this.testContainer = testContainer;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        return out;
    }

    @GetMapping("/health/db")
    public Map<String, Object> db() {
        Map<String, Object> out = new LinkedHashMap<>();

        try {
            Integer one = jdbc.queryForObject("SELECT 1", Integer.class);
            out.put("success", one != null && one == 1);
            return out;

        } catch (Exception e) {
            out.put("success", false);
            out.put("error", e.getMessage());
            return out;
        }
    }

    @GetMapping("/health/blob")
    public Map<String, Object> blob() {
        Map<String, Object> out = new LinkedHashMap<>();

        String blobName = "healthcheck.txt";
        String payload = "success";
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);

        try {
            BlobClient client = blobServiceClient.getBlobContainerClient(testContainer).getBlobClient(blobName);

            client.upload(new ByteArrayInputStream(payloadBytes), payloadBytes.length, true);

            String roundTrip;
            try (InputStream in = client.openInputStream()) {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                byte[] buf = new byte[1024];
                int r;
                while ((r = in.read(buf)) != -1) {
                    bout.write(buf, 0, r);
                }
                roundTrip = bout.toString(StandardCharsets.UTF_8);
            }

            boolean matches = payload.equals(roundTrip);

            out.put("success", matches);
            out.put("container", testContainer);
            out.put("blob", blobName);

            return out;

        } catch (Exception e) {
            out.put("success", false);
            out.put("container", testContainer);
            out.put("error", e.getMessage());
            return out;
        }
    }
}