package com.sparta.ordering.ai.service;

import com.sparta.ordering.ai.client.GeminiClient;
import com.sparta.ordering.ai.dto.AiProductDescriptionResponse;
import com.sparta.ordering.ai.entity.AiProductDescription;
import com.sparta.ordering.ai.repository.AiProductDescriptionRepository;
import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.product.entity.Product;
import com.sparta.ordering.product.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiProductDescriptionServiceTest {

    @Mock
    private AiProductDescriptionRepository aiProductDescriptionRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private GeminiClient geminiClient;

    @InjectMocks
    private AiProductDescriptionService aiProductDescriptionService;

    @Nested
    @DisplayName("AI 상품 설명 조회 (search)")
    class Search {

        @Test
        @DisplayName("성공")
        void success() {
            // given
            UUID productId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 10);

            Product product = mock(Product.class);
            when(product.getId()).thenReturn(productId);

            AiProductDescription description = mock(AiProductDescription.class);
            when(description.getId()).thenReturn(UUID.randomUUID());
            when(description.getProduct()).thenReturn(product);
            when(description.getPrompt()).thenReturn("prompt");
            when(description.getDescription()).thenReturn("description");
            when(description.getCreatedAt()).thenReturn(Instant.now());
            when(description.getCreatedBy()).thenReturn(UUID.randomUUID());

            Page<AiProductDescription> page = new PageImpl<>(List.of(description), pageable, 1);

            when(productRepository.existsByIdAndRestaurant_User_IdAndDeletedAtIsNull(productId, userId)).thenReturn(true);
            when(aiProductDescriptionRepository.findByProductIdAndDeletedAtIsNull(productId, pageable)).thenReturn(page);

            // when
            Page<AiProductDescriptionResponse> response = aiProductDescriptionService.search(productId, userId, pageable);

            // then
            assertThat(response).hasSize(1);
            assertThat(response.getContent().get(0).productId()).isEqualTo(productId);
            assertThat(response.getContent().get(0).prompt()).isEqualTo("prompt");
            assertThat(response.getContent().get(0).description()).isEqualTo("description");
            verify(productRepository, times(1)).existsByIdAndRestaurant_User_IdAndDeletedAtIsNull(productId, userId);
            verify(aiProductDescriptionRepository, times(1)).findByProductIdAndDeletedAtIsNull(productId, pageable);
        }

        @Test
        @DisplayName("실패 - 상품 소유권 없음 또는 존재하지 않음")
        void failProductNotFound() {
            // given
            UUID productId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            Pageable pageable = PageRequest.of(0, 10);

            when(productRepository.existsByIdAndRestaurant_User_IdAndDeletedAtIsNull(productId, userId)).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> aiProductDescriptionService.search(productId, userId, pageable))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.PRODUCT_NOT_FOUND);

            verifyNoInteractions(aiProductDescriptionRepository);
        }
    }

    @Nested
    @DisplayName("AI 상품 설명 생성 (generateDescription)")
    class GenerateDescription {

        @Test
        @DisplayName("성공")
        void success() {
            // given
            String prompt = "맛있는 떡볶이 설명해줘";
            String expectedDescription = "매콤달콤한 떡볶이입니다.";

            when(geminiClient.generateDescription(prompt)).thenReturn(expectedDescription);

            // when
            String result = aiProductDescriptionService.generateDescription(prompt);

            // then
            assertThat(result).isEqualTo(expectedDescription);
            verify(geminiClient, times(1)).generateDescription(prompt);
        }
    }

    @Nested
    @DisplayName("상품 검증 (validateProduct)")
    class ValidateProduct {

        @Test
        @DisplayName("성공")
        void success() {
            // given
            UUID productId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            Product product = mock(Product.class);

            when(productRepository.findByIdAndRestaurant_User_IdAndDeletedAtIsNull(productId, userId))
                    .thenReturn(Optional.of(product));

            // when
            aiProductDescriptionService.validateProduct(productId, userId);

            // then
            verify(productRepository, times(1)).findByIdAndRestaurant_User_IdAndDeletedAtIsNull(productId, userId);
        }

        @Test
        @DisplayName("실패 - 상품이 존재하지 않거나 소유자가 아님")
        void failProductNotFound() {
            // given
            UUID productId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            when(productRepository.findByIdAndRestaurant_User_IdAndDeletedAtIsNull(productId, userId))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> aiProductDescriptionService.validateProduct(productId, userId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.PRODUCT_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("AI 상품 설명 저장 (saveDescription)")
    class SaveDescription {

        @Test
        @DisplayName("성공")
        void success() {
            // given
            UUID productId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            String prompt = "prompt";
            String description = "description";
            Product product = mock(Product.class);

            when(productRepository.findByIdAndRestaurant_User_IdAndDeletedAtIsNull(productId, userId))
                    .thenReturn(Optional.of(product));

            // when
            aiProductDescriptionService.saveDescription(productId, prompt, description, userId);

            // then
            verify(aiProductDescriptionRepository, times(1)).save(any(AiProductDescription.class));
        }

        @Test
        @DisplayName("실패 - 상품 소유권 없음 또는 존재하지 않음")
        void failProductNotFound() {
            // given
            UUID productId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            String prompt = "prompt";
            String description = "description";

            when(productRepository.findByIdAndRestaurant_User_IdAndDeletedAtIsNull(productId, userId))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> aiProductDescriptionService.saveDescription(productId, prompt, description, userId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.PRODUCT_NOT_FOUND);

            verify(aiProductDescriptionRepository, never()).save(any(AiProductDescription.class));
        }
    }

    @Nested
    @DisplayName("AI 상품 설명 수정 (update)")
    class Update {

        @Test
        @DisplayName("성공")
        void success() {
            // given
            UUID aiDescriptionId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            String newDescription = "수정된 설명";

            Product product = mock(Product.class);
            AiProductDescription aiProductDescription = spy(AiProductDescription.builder()
                    .product(product)
                    .prompt("prompt")
                    .description("description")
                    .build());

            when(aiProductDescriptionRepository.findByIdAndProduct_Restaurant_User_IdAndDeletedAtIsNull(aiDescriptionId, userId))
                    .thenReturn(Optional.of(aiProductDescription));

            // when
            aiProductDescriptionService.update(aiDescriptionId, userId, newDescription);

            // then
            verify(aiProductDescription, times(1)).update(newDescription);
            assertThat(aiProductDescription.getDescription()).isEqualTo(newDescription);
        }

        @Test
        @DisplayName("실패 - AI 설명이 존재하지 않거나 소유자가 아님")
        void failDescriptionNotFound() {
            // given
            UUID aiDescriptionId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            String newDescription = "수정된 설명";

            when(aiProductDescriptionRepository.findByIdAndProduct_Restaurant_User_IdAndDeletedAtIsNull(aiDescriptionId, userId))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> aiProductDescriptionService.update(aiDescriptionId, userId, newDescription))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.AI_PRODUCT_DESCRIPTION_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("AI 상품 설명 삭제 (delete)")
    class Delete {

        @Test
        @DisplayName("성공")
        void success() {
            // given
            UUID aiDescriptionId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            Product product = mock(Product.class);
            AiProductDescription aiProductDescription = spy(AiProductDescription.builder()
                    .product(product)
                    .prompt("prompt")
                    .description("description")
                    .build());

            when(aiProductDescriptionRepository.findByIdAndProduct_Restaurant_User_IdAndDeletedAtIsNull(aiDescriptionId, userId))
                    .thenReturn(Optional.of(aiProductDescription));

            // when
            aiProductDescriptionService.delete(aiDescriptionId, userId);

            // then
            verify(aiProductDescription, times(1)).softDelete(userId);
            assertThat(aiProductDescription.getDeletedAt()).isNotNull();
            assertThat(aiProductDescription.getDeletedBy()).isEqualTo(userId);
        }

        @Test
        @DisplayName("실패 - AI 설명이 존재하지 않거나 소유자가 아님")
        void failDescriptionNotFound() {
            // given
            UUID aiDescriptionId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            when(aiProductDescriptionRepository.findByIdAndProduct_Restaurant_User_IdAndDeletedAtIsNull(aiDescriptionId, userId))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> aiProductDescriptionService.delete(aiDescriptionId, userId))
                    .isInstanceOf(ApiException.class)
                    .hasFieldOrPropertyWithValue("responseCode", GeneralResponseCode.AI_PRODUCT_DESCRIPTION_NOT_FOUND);
        }
    }
}
