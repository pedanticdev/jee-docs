package fish.payara.resource;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class FirebaseBlobManager implements EmbeddingDocumentLoader {

    static final Logger LOGGER = Logger.getLogger(FirebaseBlobManager.class.getName());
    static final String BUCKET_PREFIX = "rag/uploaded/";
    static final String BUCKET_NAME = "jakarta-ee-adc2a.appspot.com";
    private final static ApachePdfBoxDocumentParser PDF_BOX_DOCUMENT_PARSER = new ApachePdfBoxDocumentParser();

    @Inject
    Storage storage;

    @Override
    public List<Document> loadDocuments() {
        List<Document> documents = new ArrayList<>();

        listObjects().forEach(document -> {
                    LOGGER.log(Level.INFO, "Loaded and converted blob document --> " + document);
                    documents.add(loadDocument(document));

                }
        );

        return documents;
    }

    @Override
    public Document loadDocument(final String documentKey) {

        BlobId blobId = BlobId.of(BUCKET_NAME, documentKey);
        Blob blob = storage.get(blobId);
        if (blob == null) {
            throw new IllegalArgumentException("File not found: " + documentKey);
        }
        return PDF_BOX_DOCUMENT_PARSER.parse(new ByteArrayInputStream(blob.getContent()));
    }

    @Override
    public void moveEmbeddedDocument(final List<String> documentKeys) {
        if (documentKeys == null || documentKeys.isEmpty()) {
            LOGGER.log(Level.WARNING, "No documents provided to move");
            return;
        }

        LOGGER.log(Level.INFO, "Moving embedded documents --> {0}", documentKeys);

        for (String fileName : documentKeys) {
            if (fileName == null || fileName.trim().isEmpty()) {
                LOGGER.log(Level.WARNING, "Skipping null or empty filename");
                continue;
            }

            LOGGER.log(Level.INFO, "Moving embedded document --> {0}", fileName);

            try {
                // Get source blob reference
                BlobId sourceBlobId = BlobId.of(BUCKET_NAME, fileName);
                Blob sourceBlob = storage.get(sourceBlobId);

                if (sourceBlob == null) {
                    LOGGER.log(Level.WARNING, "Source file not found --> {0}", fileName);
                    continue;
                }
                String newName = fileName.replace(BUCKET_PREFIX, "rag/embedded/");
                BlobId targetBlobId = BlobId.of(BUCKET_NAME, newName);
                if (storage.get(targetBlobId) != null) {
                    LOGGER.log(Level.WARNING, "Target file already exists --> {0}", newName);
                    continue;
                }
                Storage.CopyRequest copyRequest = Storage.CopyRequest.newBuilder()
                        .setSource(sourceBlobId)
                        .setTarget(targetBlobId)
                        .build();
                storage.copy(copyRequest).getResult();
                if (storage.get(targetBlobId) != null) {
                    boolean deleted = storage.delete(sourceBlobId);
                    if (deleted) {
                        LOGGER.log(Level.INFO, "Successfully moved {0} to {1}", new Object[]{fileName, newName});
                    } else {
                        LOGGER.log(Level.WARNING, "Copy succeeded but failed to delete source file --> {0}", fileName);
                    }
                } else {
                    LOGGER.log(Level.SEVERE, "Failed to copy file --> {0}", fileName);
                }

            } catch (StorageException e) {
                LOGGER.log(Level.SEVERE, "Storage operation failed for file --> " + fileName, e);
            } catch (IllegalArgumentException e) {
                LOGGER.log(Level.SEVERE, "Invalid file path --> " + fileName, e);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unexpected error moving file --> " + fileName, e);
            }
        }
    }

    @Override
    public List<String> listObjects() {
        List<String> fileNames = new ArrayList<>();
        Page<Blob> blobs = storage.list(
                BUCKET_NAME,
                Storage.BlobListOption.prefix(BUCKET_PREFIX),
                Storage.BlobListOption.currentDirectory()
        );
        for (Blob blob : blobs.iterateAll()) {
            if (!blob.getName().endsWith("/")) {
                fileNames.add(blob.getName());
            }
        }
        LOGGER.log(Level.INFO, "Found " + fileNames.size() + " files");
        LOGGER.log(Level.INFO, "Loaded documents --> " + fileNames);
        return fileNames;
    }
}
