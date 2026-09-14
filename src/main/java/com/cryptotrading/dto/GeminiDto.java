package com.cryptotrading.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public final class GeminiDto {

    private GeminiDto() {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Request(
            List<Content> contents,
            @JsonProperty("system_instruction") Content systemInstruction
    ) {
        public static Request of(String prompt, String systemPrompt) {
            Content system = (systemPrompt != null && !systemPrompt.isBlank())
                    ? new Content(List.of(new Part(systemPrompt)))
                    : null;
            return new Request(List.of(new Content(List.of(new Part(prompt)))), system);
        }
    }

    public record Content(List<Part> parts) {}

    public record Part(String text) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(List<Candidate> candidates) {
        public String extractFirstText() {
            if (candidates == null || candidates.isEmpty()) {
                return "";
            }
            Candidate candidate = candidates.get(0);
            if (candidate.content() == null || candidate.content().parts() == null || candidate.content().parts().isEmpty()) {
                return "";
            }
            return candidate.content().parts().get(0).text();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Candidate(Content content, @JsonProperty("finishReason") String finishReason) {}
}