package simplerag.service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.document.Document;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DocumentParserTest {
    private HtmlDocumentSplitter documentParser = new HtmlDocumentSplitter();

    private List<TextSegment> split(String html) {
        var result = Document.document(html);
        result.metadata().put("url", "http://example.com");
        return documentParser.split(result);
    }

    @Test
    void shouldParseSingleHeadingWithContent() {
        List<TextSegment> segments = split("""
                <html>
                <body>
                    <h1>Main Title</h1>
                    <p>This is the main content.</p>
                    <p>More details here.</p>
                </body>
                </html>
                """);

        assertEquals(1, segments.size());
        TextSegment section = segments.getFirst();
        assertEquals("Main Title", section.metadata().getString("sectionName"));
        assertEquals("http://example.com", section.metadata().getString("url"));
        assertEquals("Main TitleThis is the main content.\nMore details here.", section.text());
    }

    @Test
    void shouldParseNestedHeadingsAndBuildPath() {
        List<TextSegment> segments = split("""
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
                """);

        assertEquals(4, segments.size());

        assertEquals("Chapter 1", segments.get(0).metadata().getString("sectionName"));
        assertEquals("Chapter 1. Section 1.1", segments.get(1).metadata().getString("sectionName"));
        assertEquals("Chapter 1. Section 1.1. Subsection 1.1.1", segments.get(2).metadata().getString("sectionName"));
        assertEquals("Chapter 1. Section 1.2", segments.get(3).metadata().getString("sectionName"));
    }

    @Test
    void shouldSkipRelatedContentSection() {
        List<TextSegment> segments = split("""
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
                """);

        assertEquals(2, segments.size());
        assertFalse(segments.stream().anyMatch(e -> e.metadata().getString("sectionName").contains("Related content")));
    }

    @Test
    void shouldHandleAnchorsFromIdOrName() {
        List<TextSegment> segments = split("""
                <html>
                <body>
                    <h2 id="intro">Introduction</h2>
                    <p>Welcome text.</p>
                    <h2 name="usage">Usage Guide</h2>
                    <p>How to use.</p>
                </body>
                </html>
                """);

        assertEquals(2, segments.size());
        assertEquals("intro", segments.get(0).metadata().getString("anchor"));
        assertEquals("usage", segments.get(1).metadata().getString("anchor"));
    }

    @Test
    void shouldIgnoreEmptySections() {
        List<TextSegment> segments = split("""
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
                """);

        assertEquals(0, segments.size());
    }
}