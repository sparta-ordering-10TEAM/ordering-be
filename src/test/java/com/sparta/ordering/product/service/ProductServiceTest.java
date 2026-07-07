package com.sparta.ordering.product.service;

import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.product.dto.ProductCreateRequest;
import com.sparta.ordering.product.dto.ProductResponse;
import com.sparta.ordering.product.dto.ProductUpdateRequest;
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
            ProductResponse response = productService.getProduct(productId);

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
                    .isEqualTo(GeneralResponseCode.PRODUCT_NOT_FOUND);
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

            ProductCreateRequest requestDto = new ProductCreateRequest(
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
            ProductResponse responseDto = productService.createProduct(requestDto);

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

            ProductCreateRequest requestDto = new ProductCreateRequest(
                    restaurantId, "상품1", "상품 설명", 8000L
            );

            when(restaurantRepository.findByIdAndDeletedAtIsNull(restaurantId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> productService.createProduct(requestDto))
                    .isInstanceOf(ApiException.class)
                    .extracting("responseCode")
                    .isEqualTo(GeneralResponseCode.RESTAURANT_NOT_FOUND);

        }
    }
    @Nested
    @DisplayName("상품 업데이트")
    class UpdateProduct{

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

            ProductUpdateRequest updateDto = new ProductUpdateRequest("상품2", "상품 설명2", 8000L);

            when(productRepository.findByIdAndDeletedAtIsNull(productId)).thenReturn(Optional.of(product));

            //when
            ProductResponse updateProduct = productService.updateProduct(productId, updateDto);

            //then
            assertThat(updateProduct.description()).isEqualTo(updateDto.description());
            assertThat(updateProduct.name()).isEqualTo(updateDto.name());
            assertThat(updateProduct.price()).isEqualTo(updateDto.price());
        }

        @Test
        @DisplayName("실패 - 존재 하지 않는 상품")
        void test2() {
            // given
            UUID productId = UUID.randomUUID();
            ProductUpdateRequest updateDto = new ProductUpdateRequest("상품2", "상품 설명2", 8000L);

            when(productRepository.findByIdAndDeletedAtIsNull(productId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> productService.updateProduct(productId, updateDto))
                    .isInstanceOf(ApiException.class)
                    .extracting("responseCode")
                    .isEqualTo(GeneralResponseCode.PRODUCT_NOT_FOUND);
        }
    }
}
