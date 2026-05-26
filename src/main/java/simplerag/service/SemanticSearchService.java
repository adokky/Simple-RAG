package simplerag.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
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
        @SystemMessage("""
            Answer the user question based strictly on the context (Markdown) provided below.
            Produce Markdown if answer expected to be long or highly structured.
        
            Context:
            {context}
        """)
        String ask(@V("context") String context, @UserMessage String message);
    }

    public AskResultDto askWithContext(String userQuestion) {
        List<DocumentDto> documents = vectorSearchService.vectorSearch(userQuestion, 3);

        if (documents.isEmpty()) {
            return new AskResultDto("not enough context for the response", Collections.emptyList());
        }

        String context = documents.stream()
                .map(DocumentDto::content)
                .collect(Collectors.joining("\n"));

        var aiResponse = assistant.ask(context, userQuestion);

        List<AskResultDto.Link> urls = documents.stream()
                .map(doc -> new AskResultDto.Link(doc.url(), doc.path()))
                .distinct()
                .toList();

        return new AskResultDto(aiResponse, urls);
    }
}
