package simplerag.controller;

import io.smallrye.common.annotation.Blocking;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import simplerag.service.AskResultDto;
import simplerag.service.DocumentDto;
import simplerag.service.SemanticSearchService;
import simplerag.service.VectorSearchService;

import java.net.URL;
import java.util.List;

@Path("/ai")
@Blocking
public class SemanticSearchResource {
    private final SemanticSearchService semanticSearchService;
    private final VectorSearchService vectorSearchService;

    public SemanticSearchResource(SemanticSearchService semanticSearchService,
                                  VectorSearchService vectorSearchService) {
        this.semanticSearchService = semanticSearchService;
        this.vectorSearchService = vectorSearchService;
    }

    @POST
    @Path("/ask")
    @Produces(MediaType.APPLICATION_JSON)
    public Response askAi(@NotBlank @QueryParam("q") String userQuestion) {
        try {
            AskResultDto result = semanticSearchService.askWithContext(userQuestion);
            return Response.ok(result).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError()
                    .entity("Ошибка при обработке RAG-задачи").build();
        }
    }

    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    public List<DocumentDto> vectorSearch(
            @QueryParam("q")
            @NotBlank
            String query,

            @QueryParam("max")
            @DefaultValue("3")
            @Min(1)
            @Max(5)
            int max
    ) {
        return vectorSearchService.vectorSearch(query, max);
    }

    /**
     * @param targetUrl URL string (например, "https://quarkus.io/guides/cdi")
     */
    @POST
    @Path("/ingest")
    @Produces(MediaType.TEXT_PLAIN)
    public Response ingestExternalGuide(@QueryParam("url") URL targetUrl) {
        try {
            int count = vectorSearchService.ingest(targetUrl);
            return Response.ok("Успешно собрано, загружено и разрезано на " + count + " структурных эмбеддингов").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity("Ошибка при обработке сбора данных").build();
        }
    }
}
