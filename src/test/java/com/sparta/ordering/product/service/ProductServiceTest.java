package com.sparta.ordering.product.service;

import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.product.dto.ProductCreateRequestDto;
import com.sparta.ordering.product.dto.ProductResponseDto;
import com.sparta.ordering.product.entity.Product;
import com.sparta.ordering.product.repository.ProductRepository;
import com.sparta.ordering.restaurant.entity.Restaurant;
import com.sparta.ordering.restaurant.repository.RestaurantRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

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

            when(productRepository.findByIdAndDeletedAtIsNull(productId)).thenReturn(Optional.of(product));

            // when
            ProductResponseDto response = productService.getProduct(productId);

            // then
            assertThat(response.id()).isEqualTo(productId);
            assertThat(response.restaurantId()).isEqualTo(restaurantId);
            assertThat(response.name()).isEqualTo("상품1");
            assertThat(response.description()).isEqualTo("상품 설명");
            assertThat(response.price()).isEqualTo(8000L);
        }

        @Test
        @DisplayName("실패-존재하지 않는 상품")
        void test2() {
            UUID productId = UUID.randomUUID();
            when(productRepository.findByIdAndDeletedAtIsNull(productId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getProduct(productId))
                    .isInstanceOf(ApiException.class)
                    .extracting("responseCode")
                    .isEqualTo(GeneralResponseCode.INVALID_REQUEST);
        }
    }

    @Nested
    @DisplayName("상품 생성")
    class CreateProduct {

        @Test
        @DisplayName("성공")
        void test1() {
            // given
            UUID restaurantId = UUID.randomUUID();
            Restaurant restaurant = Restaurant.builder().build();
            ReflectionTestUtils.setField(restaurant, "id", restaurantId);

            ProductCreateRequestDto requestDto = new ProductCreateRequestDto(
                    restaurantId, "상품1", "상품 설명", 8000L
            );

            UUID productId = UUID.randomUUID();
            Product savedProduct = Product.builder()
                    .restaurant(restaurant)
                    .name(requestDto.name())
                    .description(requestDto.description())
                    .price(requestDto.price())
                    .build();
            ReflectionTestUtils.setField(savedProduct, "id", productId);

            when(restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)).thenReturn(Optional.of(restaurant));
            when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

            // when
            ProductResponseDto responseDto = productService.createProduct(requestDto);

            // then
            assertThat(responseDto.id()).isEqualTo(productId);
            assertThat(responseDto.restaurantId()).isEqualTo(restaurantId);
            assertThat(responseDto.description()).isEqualTo(requestDto.description());
            assertThat(responseDto.name()).isEqualTo(requestDto.name());
            assertThat(responseDto.price()).isEqualTo(requestDto.price());
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 식당")
        void test2() {
            // given
            UUID restaurantId = UUID.randomUUID();

            ProductCreateRequestDto requestDto = new ProductCreateRequestDto(
                    restaurantId, "상품1", "상품 설명", 8000L
            );

            when(restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> productService.createProduct(requestDto))
                    .isInstanceOf(ApiException.class)
                    .extracting("responseCode")
                    .isEqualTo(GeneralResponseCode.INVALID_REQUEST);

        }
    }
}
