package com.teukgeupjeonsa.backend.px;

import com.teukgeupjeonsa.backend.common.response.ApiResponse;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/px-products")
@RequiredArgsConstructor
public class PxController {

    private final PxProductRepository pxProductRepository;

    @GetMapping
    public ApiResponse<List<PxProductResponse>> getProducts() {
        List<PxProductResponse> list = pxProductRepository.findByIsActiveTrue().stream()
                .map(p -> PxProductResponse.builder()
                        .id(p.getId())
                        .productName(p.getProductName())
                        .brandName(p.getBrandName())
                        .category(p.getCategory())
                        .calories(p.getCalories())
                        .proteinG(p.getProteinG())
                        .carbG(p.getCarbG())
                        .fatG(p.getFatG())
                        .build())
                .toList();
        return ApiResponse.ok(list);
    }

    @Getter
    @Builder
    static class PxProductResponse {
        private Long id;
        private String productName;
        private String brandName;
        private String category;
        private Integer calories;
        private Double proteinG;
        private Double carbG;
        private Double fatG;
    }
}
