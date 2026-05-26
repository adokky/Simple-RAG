package simplerag.service;

public record DocumentDto(
    String url,
    String section_name,
    String content
) {}
