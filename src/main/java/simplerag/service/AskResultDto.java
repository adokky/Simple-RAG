package simplerag.service;

import java.util.List;

public record AskResultDto(String response, List<Link> links) {
    public record Link(String url, String name) {}
}