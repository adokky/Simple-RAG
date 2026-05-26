package simplerag.controller;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
@Path("/")
public class MetaResource {
    @ConfigProperty(name = "quarkus.application.version")
    private String appVersion;

    @GET
    @Path("/about")
    @Produces(MediaType.TEXT_PLAIN)
    public String meta() { return "Simple RAG " + appVersion; }
}
