package simplerag.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import io.quarkus.runtime.Startup;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;

import static dev.langchain4j.data.document.splitter.DocumentSplitters.recursive;

@ApplicationScoped
class DocumentLoader {
    private final EmbeddingStore<TextSegment> store;
    private final EmbeddingStoreIngestor ingestor;

    DocumentLoader(EmbeddingStore<TextSegment> store,
                   EmbeddingModel embeddingModel,
                   DocumentSplitter documentSplitter) {
        this.store = store;

        // TODO "# %s\n\n%s".formatted(doc.id.sectionName, content)
        this.ingestor = EmbeddingStoreIngestor
                .builder()
                .embeddingStore(store)
                .embeddingModel(embeddingModel)
                .documentSplitter(documentSplitter)
                .build();
    }

    void loadDocument(String html, String url) {
        var doc = Document.document(html, Metadata.metadata("url", url));

        store.removeAll(new IsEqualTo("url", url));
        ingestor.ingest(doc);
    }
}