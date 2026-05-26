package simplerag.service;

import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Blocking
@ApplicationScoped
public class SemanticSearchService {
    private final VectorSearchService vectorSearchService;
    private final Assistant assistant;

    SemanticSearchService(VectorSearchService vectorSearchService,
                          Assistant assistant) {
        this.vectorSearchService = vectorSearchService;
        this.assistant = assistant;
    }

    @RegisterAiService
    public interface Assistant {
        String chat(@UserMessage String message);
    }

    public AskResultDto askWithContext(String userQuestion) {
        List<DocumentDto> documents = vectorSearchService.vectorSearch(userQuestion, 3);

        if (documents.isEmpty()) {
            return new AskResultDto("not enough context for the response", Collections.emptyList());
        }

        String context = documents.stream()
                .map(doc -> "- " + doc.content())
                .collect(Collectors.joining("\n"));

        var enrichedPrompt = String.format("""
            Answer the user question based strictly on the context provided below.
            
            Context:
            %s
            
            User Question: %s
            """, context, userQuestion);

        var aiResponse = assistant.chat(enrichedPrompt);

        List<String> urls = documents.stream()
                .map(DocumentDto::url)
                .distinct()
                .toList();

        return new AskResultDto(aiResponse, urls);
    }
}
