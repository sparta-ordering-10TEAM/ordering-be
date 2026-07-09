package com.sparta.ordering.ai.client;

import com.sparta.ordering.global.code.ExternalResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class GeminiClient {
    private final RestClient geminiRestClient;

    public GeminiClient(
            RestClient.Builder restClientBuilder,
            GeminiProperties geminiProperties
    ) {
        this.geminiRestClient = restClientBuilder
                .baseUrl(geminiProperties.url())
                .defaultHeader("X-goog-api-key", geminiProperties.apiKey())
                .build();
    }

    public String generateDescription(String prompt) {
        try {
            GeminiResponse response = geminiRestClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(GeminiRequest.from(prompt))
                    .retrieve()
                    .body(GeminiResponse.class);

            return extractText(response); // 프롬프트 결과 반환
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage(), e);
            throw new ApiException(ExternalResponseCode.GEMINI_API_ERROR);
        }
    }

    private String extractText(GeminiResponse response) {
        return Optional.ofNullable(response)
                .map(GeminiResponse::candidates)
                .flatMap(c -> c.stream().findFirst())
                .map(GeminiResponse.Candidate::content)
                .map(GeminiResponse.Content::parts)
                .flatMap(p -> p.stream().findFirst())
                .map(GeminiResponse.Part::text)
                .orElseThrow(() -> new ApiException(ExternalResponseCode.GEMINI_API_INVALID_RESPONSE));
    }

    // API 요청 규격 구조화 DTO
    private record GeminiRequest(List<Content> contents) {
        public static GeminiRequest from(String prompt) {
            return new GeminiRequest(
                    List.of(new Content(
                            List.of(new Part(prompt))
                    ))
            );
        }

        private record Content(List<Part> parts) {
        }

        private record Part(String text) {
        }
    }

    // API 응답 규격 구조화 DTO
    private record GeminiResponse(List<Candidate> candidates) {
        private record Candidate(Content content) {
        }

        private record Content(List<Part> parts) {
        }

        private record Part(String text) {
        }
    }
}
