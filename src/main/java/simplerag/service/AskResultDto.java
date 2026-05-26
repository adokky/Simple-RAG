package simplerag.service;

import java.util.List;

public record AskResultDto(String response, List<String> urls) {
}
