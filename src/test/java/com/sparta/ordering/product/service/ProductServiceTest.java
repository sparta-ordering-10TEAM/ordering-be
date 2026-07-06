package com.sparta.ordering.product.service;

import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.product.dto.ProductResponseDto;
import com.sparta.ordering.product.entity.Product;
import com.sparta.ordering.product.repository.ProductRepository;
import com.sparta.ordering.restaurant.entity.Restaurant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;


    @InjectMocks
    private ProductService productService;

    @Nested
    @DisplayName("상품 단건 조회")
    class GetProduct {

        @Test
        @DisplayName("성공")
        void test1() {
            // given
            UUID restaurantId = UUID.randomUUID();
            Restaurant restaurant = Restaurant.builder().build();
            ReflectionTestUtils.setField(restaurant, "id", restaurantId);

            UUID productId = UUID.randomUUID();
            Product product = Product.builder()
                    .restaurant(restaurant)
                    .name("상품1")
                    .description("상품 설명")
                    .price(8000L)
                    .build();
            ReflectionTestUtils.setField(product, "id", productId);

            when(productRepository.findById(productId)).thenReturn(Optional.of(product));

            // when
            ProductResponseDto response = productService.getProduct(productId);

            // then
            assertThat(response.getId()).isEqualTo(productId);
            assertThat(response.getRestaurantId()).isEqualTo(restaurantId);
            assertThat(response.getName()).isEqualTo("상품1");
            assertThat(response.getDescription()).isEqualTo("상품 설명");
            assertThat(response.getPrice()).isEqualTo(8000L);
        }

        @Test
        @DisplayName("존재하지 않는 상품")
        void test2() {
            UUID productId = UUID.randomUUID();
            when(productRepository.findById(productId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getProduct(productId))
                    .isInstanceOf(ApiException.class)
                    .extracting("responseCode")
                    .isEqualTo(GeneralResponseCode.INVALID_REQUEST);
        }
    }
}
