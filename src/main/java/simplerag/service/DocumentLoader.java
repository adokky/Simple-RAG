package simplerag.service;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
class DocumentLoader {
    private final EmbeddingStore<TextSegment> store;
    private final EmbeddingStoreIngestor ingestor;

    DocumentLoader(EmbeddingStore<TextSegment> store,
                   EmbeddingModel embeddingModel,
                   DocumentSplitter documentSplitter) {
        this.store = store;

        this.ingestor = EmbeddingStoreIngestor
                .builder()
                .embeddingStore(store)
                .embeddingModel(embeddingModel)
                .documentSplitter(documentSplitter)
                .build();
    }

    @Transactional
    void loadDocument(String html, String url) {
        var doc = Document.document(html, Metadata.metadata(DocumentTags.URL, url));

        store.removeAll(new IsEqualTo(DocumentTags.URL, url));

        ingestor.ingest(doc);
    }
}