package simplerag.service;

public record DocumentDto(
    String url,
    String path,
    String content
) {}
