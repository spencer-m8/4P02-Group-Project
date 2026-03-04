package ca.group13.codecomparator.config;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BlobConfig {

    @Bean
    public BlobServiceClient blobServiceClient(@Value("${app.storage.connection-string:}") String connString) {
        if (connString == null || connString.isBlank()) {
            throw new IllegalArgumentException("Missing app.storage.connection-string");
        }

        return new BlobServiceClientBuilder().connectionString(connString.trim()).buildClient();
    }
}