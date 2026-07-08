package com.sparta.ordering.ai.service;

import com.sparta.ordering.ai.client.GeminiClient;
import com.sparta.ordering.ai.dto.AiProductDescriptionResponse;
import com.sparta.ordering.ai.entity.AiProductDescription;
import com.sparta.ordering.ai.repository.AiProductDescriptionRepository;
import com.sparta.ordering.global.code.GeneralResponseCode;
import com.sparta.ordering.global.exception.ApiException;
import com.sparta.ordering.product.entity.Product;
import com.sparta.ordering.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiProductDescriptionService {
    private final AiProductDescriptionRepository aiProductDescriptionRepository;
    private final ProductRepository productRepository;
    private final GeminiClient geminiClient;

    @Transactional(readOnly = true)
    public Page<AiProductDescriptionResponse> search(UUID productId, UUID userId, Pageable pageable) {
        if(productRepository.existsByIdAndRestaurant_User_IdAndDeletedAtIsNull(productId, userId)){
            throw new ApiException(GeneralResponseCode.PRODUCT_NOT_FOUND); // Product 소유권 검증
        }

        return aiProductDescriptionRepository.findByProductIdAndDeletedAtIsNull(productId, pageable)
                .map(AiProductDescriptionResponse::fromEntity);
    }

    public String generateDescription(String prompt){
        return geminiClient.generateDescription(prompt);
    }

    @Transactional(readOnly = true)
    public void validateProduct(UUID productId, UUID userId) {
        productRepository.findByIdAndRestaurant_User_IdAndDeletedAtIsNull(productId, userId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.PRODUCT_NOT_FOUND));
    }

    @Transactional
    public void saveDescription(UUID productId, String prompt, String description) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.PRODUCT_NOT_FOUND));

        AiProductDescription aiProductDescription = AiProductDescription.builder()
                .product(product)
                .prompt(prompt)
                .description(description)
                .build();

        aiProductDescriptionRepository.save(aiProductDescription);
    }

    @Transactional
    public void update(UUID aiDescriptionId, UUID userId, String description) {
        AiProductDescription aiProductDescription = aiProductDescriptionRepository
                .findByIdAndProduct_Restaurant_User_IdAndDeletedAtIsNull(aiDescriptionId, userId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.AI_PRODUCT_DESCRIPTION_NOT_FOUND));

        aiProductDescription.update(description);
    }

    @Transactional
    public void delete(UUID aiDescriptionId, UUID userId) {
        AiProductDescription aiProductDescription = aiProductDescriptionRepository
                .findByIdAndProduct_Restaurant_User_IdAndDeletedAtIsNull(aiDescriptionId, userId)
                .orElseThrow(() -> new ApiException(GeneralResponseCode.AI_PRODUCT_DESCRIPTION_NOT_FOUND));

        aiProductDescription.softDelete(userId);
    }
}
