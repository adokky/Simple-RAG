package simplerag.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15.BgeSmallEnV15EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import simplerag.dao.DocumentEmbedding;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class DocumentParserTest {
    private DocumentParser documentParser;

    @BeforeEach
    void setUp() {
        EmbeddingModel embeddingModel = Mockito.mock(EmbeddingModel.class);
        when(embeddingModel.embed(Mockito.anyString()))
                .thenReturn(Response.from(Embedding.from(new float[]{0.1f, 0.2f, 0.3f})));

        documentParser = new DocumentParser(embeddingModel);
    }

    @Test
    void shouldParseSingleHeadingWithContent() {
        String html = """
                <html>
                <body>
                    <h1>Main Title</h1>
                    <p>This is the main content.</p>
                    <p>More details here.</p>
                </body>
                </html>
                """;

        Document doc = Jsoup.parse(html);
        List<DocumentEmbedding> embeddings = documentParser.parse(doc, "https://example.com");

        assertEquals(1, embeddings.size());
        DocumentEmbedding section = embeddings.getFirst();
        assertEquals("Main Title", section.id.sectionName);
        assertEquals("https://example.com", section.id.url);
        assertEquals("Main TitleThis is the main content.\nMore details here.", section.content);
        assertEquals("Main Title", section.metadata.get("title"));
        assertArrayEquals(new float[]{0.1f, 0.2f, 0.3f}, section.embedding, 0.001f);
    }

    @Test
    void shouldParseNestedHeadingsAndBuildPath() {
        String html = """
                <html>
                <body>
                    <h1>Chapter 1</h1>
                    <p>Intro text.</p>
                    <h2>Section 1.1</h2>
                    <p>Details in subsection.</p>
                    <h3>Subsection 1.1.1</h3>
                    <p>Deep content.</p>
                    <h2>Section 1.2</h2>
                    <p>Another section.</p>
                </body>
                </html>
                """;

        Document doc = Jsoup.parse(html);
        List<DocumentEmbedding> embeddings = documentParser.parse(doc, "https://example.com/chapter1");

        assertEquals(4, embeddings.size());

        assertEquals("Chapter 1", embeddings.get(0).id.sectionName);
        assertEquals("Chapter 1. Section 1.1", embeddings.get(1).id.sectionName);
        assertEquals("Chapter 1. Section 1.1. Subsection 1.1.1", embeddings.get(2).id.sectionName);
        assertEquals("Chapter 1. Section 1.2", embeddings.get(3).id.sectionName);
    }

    @Test
    void shouldSkipRelatedContentSection() {
        String html = """
                <html>
                <body>
                    <h1>Start</h1>
                    <p>Normal content.</p>
                    <h2>Related content</h2>
                    <p>This should be skipped.</p>
                    <h2>Another Section</h2>
                    <p>This should be included.</p>
                </body>
                </html>
                """;

        Document doc = Jsoup.parse(html);
        List<DocumentEmbedding> embeddings = documentParser.parse(doc, "https://example.com");

        assertEquals(2, embeddings.size());
        assertFalse(embeddings.stream().anyMatch(e -> e.id.sectionName.contains("Related content")));
    }

    @Test
    void shouldHandleAnchorsFromIdOrName() {
        String html = """
                <html>
                <body>
                    <h2 id="intro">Introduction</h2>
                    <p>Welcome text.</p>
                    <h2 name="usage">Usage Guide</h2>
                    <p>How to use.</p>
                </body>
                </html>
                """;

        Document doc = Jsoup.parse(html);
        List<DocumentEmbedding> embeddings = documentParser.parse(doc, "https://example.com");

        assertEquals(2, embeddings.size());
        assertEquals("intro", embeddings.get(0).metadata.get("anchor"));
        assertEquals("usage", embeddings.get(1).metadata.get("anchor"));
    }

    @Test
    void shouldIgnoreEmptySections() {
        String html = """
                <html>
                <body>
                    <h1>Title</h1>
                    <div class="tocwrapper">
                        <p>Table of contents</p>
                    </div>
                    <p>   </p>
                    <h2>Section</h2>
                </body>
                </html>
                """;

        Document doc = Jsoup.parse(html);
        List<DocumentEmbedding> embeddings = documentParser.parse(doc, "https://example.com");

        assertEquals(0, embeddings.size());
    }

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