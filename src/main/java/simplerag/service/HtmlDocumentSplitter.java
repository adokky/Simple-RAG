package simplerag.service;

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.util.*;

@ApplicationScoped
class HtmlDocumentSplitter implements DocumentSplitter {
    private final FlexmarkHtmlConverter converter = FlexmarkHtmlConverter.builder().build();

    @Override
    public List<TextSegment> split(Document document) {
        String url = document.metadata().getString(DocumentTags.URL);
        Element body = Jsoup.parse(document.text()).body();

        List<TextSegment> result = new ArrayList<>();

        Deque<HeadingInfo> headingStack = new ArrayDeque<>();
        StringBuilder currentSectionContent = new StringBuilder();

        // Убираем структурные div. Итерируемся depth-first, чтобы получить физический порядок тегов
        List<Node> flatNodes = flattenDom(body);

        for (Node node : flatNodes) {
            if (node instanceof Element element && isHeading(element)) {
                // сбрасываем предыдущий контент (если он есть)
                flushCurrentSection(result, headingStack, currentSectionContent, url);

                int currentLevel = getHeadingLevel(element.tagName());

                String anchor = element.id();
                if (anchor.isEmpty()) anchor = element.attr("name");
                if (anchor.isEmpty()) anchor = null;

                while (!headingStack.isEmpty() && headingStack.peek().level() >= currentLevel) {
                    headingStack.pop();
                }
                headingStack.push(new HeadingInfo(currentLevel, element.text(), anchor));
            } else {
                if (node instanceof TextNode textNode) {
                    currentSectionContent.append(textNode.getWholeText());
                } else if (node instanceof Element element) {
                    converter.convert(element, currentSectionContent, 1);
                }
            }
        }

        // сбрасываем остаточный контент в самом конце документа
        flushCurrentSection(result, headingStack, currentSectionContent, url);

        return result;
    }

    private static List<Node> flattenDom(Element body) {
        List<Node> flatNodes = new ArrayList<>();
        for (Node child : body.childNodes()) {
            traverseAndFlatten(child, flatNodes);
        }
        return flatNodes;
    }

    private static void traverseAndFlatten(Node node, List<Node> flatNodes) {
        if (node instanceof Element element) {
            String tag = element.tagName();

            if (isHeading(element)) {
                flatNodes.add(element);
                return;
            }

            // пропускаем table of contents
            if (element.hasClass("tocwrapper")) return;

            // пропускаем врапперы
            if (tag.equals("div") ||
                tag.equals("section") ||
                tag.equals("article") ||
                tag.equals("main")
            ) {
                for (Node child : element.childNodes()) {
                    traverseAndFlatten(child, flatNodes);
                }
            } else {
                // p, span, ul, img, итд.
                flatNodes.add(element);
            }
        } else if (node instanceof TextNode textNode && !textNode.isBlank()) {
            // сырой текст внутри враппера
            flatNodes.add(textNode);
        }
    }

    private static boolean isHeading(Element element) {
        return element.tagName().matches("h[1-3]");
    }

    private static int getHeadingLevel(String tagName) {
        return Integer.parseInt(tagName.substring(1));
    }

    private void flushCurrentSection(
            List<TextSegment> sections,
            Deque<HeadingInfo> headingStack,
            StringBuilder contentBuilder,
            String url
    ) {
        Objects.requireNonNull(url);

        String content = contentBuilder.toString().trim();
        contentBuilder.setLength(0);

        if (content.isEmpty() || headingStack.isEmpty()) return;

        List<String> pathParts = headingStack.stream()
                .map(HeadingInfo::text)
                .toList()
                .reversed();

        if (pathParts.size() >= 2 && pathParts.get(1).equals("Related content")) return;

        // peekFirst() — текущий заголовок (самый глубокий)
        String anchor = headingStack.peekFirst().anchor();
        String path = String.join(". ", pathParts);

        var meta = Metadata.from(Map.of(
                DocumentTags.SECTION_PATH, path,
                DocumentTags.URL, url,
                DocumentTags.TITLE, pathParts.getLast()
        ));

        if (anchor != null) {
            meta.put(DocumentTags.ANCHOR, anchor);
        }

        var text = "# %s\n\n%s".formatted(path, content);
        sections.add(TextSegment.from(text, meta));
    }

    private record HeadingInfo(int level, String text, @Nullable String anchor) {
    }
}