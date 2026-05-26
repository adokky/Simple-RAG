package simplerag.service;

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;
import dev.langchain4j.model.embedding.EmbeddingModel;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import simplerag.dao.DocumentEmbedding;
import simplerag.dao.DocumentEmbeddingId;

import java.util.*;

@ApplicationScoped
public class DocumentParser {
    private final FlexmarkHtmlConverter converter = FlexmarkHtmlConverter.builder().build();

    private final EmbeddingModel embeddingModel;

    DocumentParser(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public List<DocumentEmbedding> parse(Document doc, String url) {
        List<DocumentEmbedding> result = new ArrayList<>();

        Deque<HeadingInfo> headingStack = new ArrayDeque<>();
        StringBuilder currentSectionContent = new StringBuilder();

        // Убираем структурные div. Итерируемся depth-first, чтобы получить физический порядок тегов
        List<Node> flatNodes = flattenDom(doc.body());

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
            List<DocumentEmbedding> sections,
            Deque<HeadingInfo> headingStack,
            StringBuilder contentBuilder,
            String url
    ) {
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

        var doc = new DocumentEmbedding();

        doc.id = new DocumentEmbeddingId();
        doc.id.sectionName = String.join(". ", pathParts);
        doc.id.url = url;

        doc.metadata = new HashMap<>(4);
        doc.metadata.put("title", pathParts.getLast());
        if (anchor != null) {
            doc.metadata.put("anchor", anchor);
        }

        doc.content = pathParts.getLast() + content;
        doc.embedding = embeddingModel.embed("# " + doc.id.sectionName + content).content().vector();

        sections.add(doc);
    }

    private record HeadingInfo(int level, String text, @Nullable String anchor) {}
}
