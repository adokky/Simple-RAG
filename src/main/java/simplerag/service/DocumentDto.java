package simplerag.service;

import java.util.Map;

public record DocumentDto(
    String url,
    String section_name,
    String content,
    Map<String, String> metadata
) {}
