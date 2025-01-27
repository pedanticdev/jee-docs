package fish.payara.resource;

import dev.langchain4j.data.document.Document;

import java.util.ArrayList;
import java.util.List;

public interface EmbeddingDocumentLoader {

    List<Document> loadDocuments();

    Document loadDocument(String documentKey);

    default void moveEmbeddedDocument(List<String> documentKeys) {}

    default List<String> listObjects() {
        return new ArrayList<>();
    }
}
