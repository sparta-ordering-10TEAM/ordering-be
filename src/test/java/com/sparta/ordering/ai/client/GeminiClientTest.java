package com.sparta.ordering.ai.client;

import com.sparta.ordering.global.code.ExternalResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiClientTest {

    private GeminiClient geminiClient;
    private MockRestServiceServer mockServer;
    private final String baseUrl = "https://generativelanguage.googleapis.com";
    private final String apiKey = "test-api-key";

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();

        GeminiProperties properties = new GeminiProperties(baseUrl, apiKey);
        geminiClient = new GeminiClient(restClientBuilder, properties);
    }

    @Nested
    @DisplayName("AI 설명 생성 (generateDescription)")
    class GenerateDescription {

        @Test
        @DisplayName("성공")
        void success() {
            // given
            String prompt = "맛있는 떡볶이 레시피";
            String expectedText = "떡볶이 레시피는 다음과 같습니다...";
            String mockResponseJson = """
                    {
                      "candidates": [
                        {
                          "content": {
                            "parts": [
                              {
                                "text": "떡볶이 레시피는 다음과 같습니다..."
                              }
                            ]
                          }
                        }
                      ]
                    }
                    """;

            mockServer.expect(requestTo(baseUrl))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(header("X-goog-api-key", apiKey))
                    .andRespond(withSuccess(mockResponseJson, MediaType.APPLICATION_JSON));

            // when
            String result = geminiClient.generateDescription(prompt);

            // then
            assertThat(result).isEqualTo(expectedText);
            mockServer.verify();
        }

        @Test
        @DisplayName("실패 - 올바르지 않은 응답 규격 (candidates가 비어있음)")
        void failInvalidResponseFormat() {
            // given
            String prompt = "맛있는 떡볶이 레시피";
            String mockResponseJson = """
                    {
                      "candidates": []
                    }
                    """;

            mockServer.expect(requestTo(baseUrl))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess(mockResponseJson, MediaType.APPLICATION_JSON));

            // when & then
            assertThatThrownBy(() -> geminiClient.generateDescription(prompt))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", ExternalResponseCode.GEMINI_API_INVALID_RESPONSE);
            mockServer.verify();
        }

        @Test
        @DisplayName("실패 - API 서버 에러 (HTTP 500)")
        void failApiServerError() {
            // given
            String prompt = "맛있는 떡볶이 레시피";

            mockServer.expect(requestTo(baseUrl))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

            // when & then
            assertThatThrownBy(() -> geminiClient.generateDescription(prompt))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", ExternalResponseCode.GEMINI_API_ERROR);
            mockServer.verify();
        }
    }
}
