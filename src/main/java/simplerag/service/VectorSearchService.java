package simplerag.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

@Blocking
@ApplicationScoped
public class VectorSearchService {
    private final EmbeddingModel embeddingModel;
    private final DocumentLoader documentLoader;
    private final EmbeddingStore<TextSegment> embeddingStore;

    VectorSearchService(EmbeddingModel embeddingModel, DocumentLoader documentLoader, EmbeddingStore<TextSegment> embeddingStore) {
        this.embeddingModel = embeddingModel;
        this.documentLoader = documentLoader;
        this.embeddingStore = embeddingStore;
    }

    public List<DocumentDto> vectorSearch(String query, int max) {
        Embedding embedding = embeddingModel.embed(query).content();

        var res = embeddingStore.search(
                EmbeddingSearchRequest.builder()
                        .query(query)
                        .queryEmbedding(embedding)
                        .maxResults(max)
                        .build()
        );

        return res.matches().stream().map((doc) -> {
                var meta = doc.embedded().metadata();
                var url =  meta.getString("url");
                var anchor = meta.getString("anchor");
                if (anchor != null) {
                    url = url + "#" + anchor;
                }
                return new DocumentDto(
                        url,
                        meta.getString("sectionName"),
                        doc.embedded().text()
                );
            }
        ).toList();
    }

    public void ingest(URL targetUrl) {
        String urlString = formatUrl(targetUrl);

        String html;
        try {
            var conn = Jsoup.connect(urlString)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
            conn.request().method(Connection.Method.GET);
            conn.execute();
            Validate.notNull(conn.request());
            html = conn.response().body();
        } catch (IOException e) {
            throw new RuntimeException("Не удалось загрузить страницу по указанному URL: " + urlString, e);
        }

        try {
            documentLoader.loadDocument(html, urlString);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String formatUrl(URL targetUrl) {
        try {
            var uri = targetUrl.toURI();
            var cleanUri = new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), null, null);
            return cleanUri.toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}