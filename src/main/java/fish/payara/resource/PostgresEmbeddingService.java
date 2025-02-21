package fish.payara.resource;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.annotation.sql.DataSourceDefinition;
import jakarta.ejb.Singleton;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

@Transactional
@DataSourceDefinition(
        name = "java:app/NeonDB",
        className = "com.zaxxer.hikari.HikariDataSource",
        properties = {
            "driverClassName=org.postgresql.ds.PGSimpleDataSource",
            "jdbcUrl=${MPCONFIG=DB_URL}",
            "username=${MPCONFIG=DB_USER}",
            "transactionIsolation=TRANSACTION_READ_COMMITTED",
            "password=${MPCONFIG=DB_PASSWORD}"
        })
@ApplicationScoped
public class PostgresEmbeddingService implements EmbeddingService {
    Logger log = Logger.getLogger(PostgresEmbeddingService.class.getName());


    @Resource
    ManagedExecutorService managedExecutorService;

    @Resource(lookup = "java:app/NeonDB")
    DataSource dataSource;

    @Inject
    @ConfigProperty(name = "OPEN_API_KEY")
    String apiKey;

    @Inject
    @ConfigProperty(name = "gpt.embedding.mode")
    String gptEmbeddingMode;

    @Inject
    @ConfigProperty(name = "openai.text-embedding")
    String textEmbedding;

    @Inject
    @ConfigProperty(name = "embedding.timer.initial.delay.minutes", defaultValue = "2")
    private long initialDelayMinutes;

    @Inject
    @ConfigProperty(name = "embedding.timer.interval.minutes", defaultValue = "60")
    private long intervalMinutes;

    @Inject
    EmbeddingDocumentLoader documentLoader;

    EmbeddingModel embeddingModel;
    EmbeddingStore<TextSegment> embeddingStore;
    DocumentSplitter splitter;
    EmbeddingStoreIngestor ingestor;
    final Timer timer = new Timer();

    @PostConstruct
    void init() {

        embeddingStore =
                PgVectorEmbeddingStore.datasourceBuilder()
                        .datasource(dataSource)
                        .createTable(true)
                        .dimension(3072)
                        .table("embeddings")
                        .build();

        embeddingModel =
                OpenAiEmbeddingModel.builder()
                        .apiKey(apiKey)
                        .modelName(textEmbedding)
                        .logRequests(true)
                        .logResponses(true)
                        .build();

        splitter = DocumentSplitters.recursive(100, 0, new OpenAiTokenizer(gptEmbeddingMode));
        ingestor =
                EmbeddingStoreIngestor.builder()
                        .documentSplitter(DocumentSplitters.recursive(300, 0))
                        .embeddingModel(embeddingModel)
                        .embeddingStore(embeddingStore)
                        .build();

        timer.scheduleAtFixedRate(
                new TimerTask() {
                    @Override
                    public void run() {
//                        managedExecutorService.submit(() -> embedNewDocs());
                        embedNewDocs();
                    }
                },
                initialDelayMinutes * 60 * 1000L,
                intervalMinutes * 1000L);
    }

    public void embedNewDocs() {
        log.log(Level.INFO, "Starting document embedding");
        List<String> strings = documentLoader.listObjects();
        log.log(Level.INFO, "About to embed found blob objects {0}", strings);

        List<Document> documents = new ArrayList<>();
        List<String> embeddedObjects = new ArrayList<>();
        try {
            for (String string : strings) {
                documents.add(documentLoader.loadDocument(string));
                embeddedObjects.add(string);
            }
            List<TextSegment> textSegments = new ArrayList<>();
            for (Document document : documents) {
                textSegments.addAll(splitter.split(document));
                log.log(Level.INFO, "About to embed text segments {0}", textSegments);
                List<Embedding> embeddings = embeddingModel.embedAll(textSegments).content();

                log.log(Level.INFO, "About to embed {0}", embeddings);
                embeddingStore.addAll(embeddings, textSegments);
                ingestor.ingest(documents);
                log.log(Level.INFO,"Embedding complete");
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "An error occurred while processing embeddings", e);
        }
        if (!embeddedObjects.isEmpty()) {
            log.log(Level.INFO, "Moving embedded objects {0}", embeddedObjects);
            documentLoader.moveEmbeddedDocument(embeddedObjects);
        }
    }

    @WithSpan("Get Content Retriever")
    public ContentRetriever getContentRetriever() {
        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(1)
                .build();
    }
}
