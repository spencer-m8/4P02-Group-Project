package ca.group13.codecomparator.service;

import com.azure.storage.blob.BlobServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class EngineCompareService {

    private final BlobServiceClient blobServiceClient;
    private final String containerName;

    public EngineCompareService(BlobServiceClient blobServiceClient, @Value("${app.storage.containers.zips:zips}") String containerName) {
        this.blobServiceClient = blobServiceClient;
        this.containerName = containerName;
    }

    /*
     * ENGINE:
     *
     * - zip1 and zip2 zip files retrieved from blob storage and can be used like any normal zip
     *
     * - return a similarity percent from 0..100 (ex: 72.5)
     */
    public Double comparePercent(UUID id1, String zipKey1, UUID id2, String zipKey2) {

        Path zip1 = downloadZip(zipKey1);
        Path zip2 = downloadZip(zipKey2);

        return null; // return null for now, change to similarity value when ready
    }

    private Path downloadZip(String zipKey) {
        var container = blobServiceClient.getBlobContainerClient(containerName);
        var blob = container.getBlobClient(zipKey);

        if (!blob.exists()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Blob not found: " + zipKey);
        }

        try {
            Path tmp = Files.createTempFile("sub-", ".zip");
            blob.downloadToFile(tmp.toString(), true);
            return tmp;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to download zip: " + zipKey, e);
        }
    }
}