package simplerag.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import simplerag.dao.DocumentEmbedding;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Blocking
@ApplicationScoped
public class VectorSearchService {
    private final EmbeddingModel embeddingModel;
    private final DocumentParser documentParser;

    VectorSearchService(EmbeddingModel embeddingModel, DocumentParser documentParser) {
        this.embeddingModel = embeddingModel;
        this.documentParser = documentParser;
    }

    public List<DocumentDto> vectorSearch(String query, int max) {
        Embedding embedding = embeddingModel.embed(query).content();
        var results = DocumentEmbedding.findClosest(embedding, max);
        return results.stream().map((doc) -> {
                var url =  doc.id.url;
                var anchor = doc.metadata.get("anchor");
                if (anchor != null) {
                    url = url + "#" + anchor;
                }
                return new DocumentDto(
                        url,
                        doc.id.sectionName,
                        doc.content,
                        doc.metadata
                                .entrySet()
                                .stream()
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        entry -> entry.getValue().toString()
                                ))
                );
            }
        ).toList();
    }

    public int ingest(URL targetUrl) {
        var urlString = targetUrl.toString();

        Document doc;
        try {
            doc = Jsoup.connect(urlString)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .get();
        } catch (IOException e) {
            throw new RuntimeException("Не удалось загрузить страницу по указанному URL: " + urlString, e);
        }

        var docs = documentParser.parse(doc, urlString);

        persistAll(docs);

        return docs.size();
    }

    @Transactional
    void persistAll(Collection<DocumentEmbedding> docs) {
        for (var doc : docs ) {
            doc.persist();
        }
    }
}