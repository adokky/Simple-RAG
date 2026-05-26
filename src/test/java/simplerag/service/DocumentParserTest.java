package simplerag.service;

import dev.langchain4j.model.embedding.onnx.bgesmallenv15.BgeSmallEnV15EmbeddingModel;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class DocumentParserTest {
    @Test
    void test() {
        var ingestor = new DocumentParser(
                new BgeSmallEnV15EmbeddingModel()
        );

        Document html;
        try {
            html = Jsoup.connect("https://quarkus.io/guides/cdi")
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .get();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        var docs = ingestor.parse(html, "https://quarkus.io/guides/cdi");

        for (var doc : docs) {
            for (int i = 0; i < 10; i++) {
                System.out.println();
            }

            System.out.println(doc.id.sectionName);
            if (!doc.metadata.isEmpty()) {
                System.out.println("META: " +  doc.metadata);
            }
            System.out.println();
            System.out.println(doc.content);
        }
    }
}