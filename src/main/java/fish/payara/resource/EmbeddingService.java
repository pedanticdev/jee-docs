package fish.payara.resource;

import dev.langchain4j.rag.content.retriever.ContentRetriever;

public interface EmbeddingService {

    ContentRetriever getContentRetriever();

    void embedNewDocs();
}
