package com.ecommerce.catalog.infrastructure.adapter.in.web;

import com.ecommerce.catalog.application.dto.ProductPageResult;
import com.ecommerce.catalog.application.dto.SearchProductsQuery;
import com.ecommerce.catalog.application.port.in.ActivateProductUseCase;
import com.ecommerce.catalog.application.port.in.AssignCategoryToProductUseCase;
import com.ecommerce.catalog.application.port.in.ChangeProductPriceUseCase;
import com.ecommerce.catalog.application.port.in.CreateProductUseCase;
import com.ecommerce.catalog.application.port.in.GetProductUseCase;
import com.ecommerce.catalog.application.port.in.RemoveCategoryFromProductUseCase;
import com.ecommerce.catalog.application.port.in.RetireProductUseCase;
import com.ecommerce.catalog.application.port.in.SearchProductsUseCase;
import com.ecommerce.catalog.application.port.in.UpdateProductUseCase;
import com.ecommerce.catalog.domain.model.CategoryId;
import com.ecommerce.catalog.domain.model.ProductId;
import com.ecommerce.catalog.domain.model.ProductStatus;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private static final String PRODUCTS_PATH = "/api/products/";

    private final CreateProductUseCase createProductUseCase;
    private final UpdateProductUseCase updateProductUseCase;
    private final ChangeProductPriceUseCase changeProductPriceUseCase;
    private final ActivateProductUseCase activateProductUseCase;
    private final RetireProductUseCase retireProductUseCase;
    private final GetProductUseCase getProductUseCase;
    private final SearchProductsUseCase searchProductsUseCase;
    private final AssignCategoryToProductUseCase assignCategoryToProductUseCase;
    private final RemoveCategoryFromProductUseCase removeCategoryFromProductUseCase;

    public ProductController(CreateProductUseCase createProductUseCase,
                             UpdateProductUseCase updateProductUseCase,
                             ChangeProductPriceUseCase changeProductPriceUseCase,
                             ActivateProductUseCase activateProductUseCase,
                             RetireProductUseCase retireProductUseCase,
                             GetProductUseCase getProductUseCase,
                             SearchProductsUseCase searchProductsUseCase,
                             AssignCategoryToProductUseCase assignCategoryToProductUseCase,
                             RemoveCategoryFromProductUseCase removeCategoryFromProductUseCase) {
        this.createProductUseCase = createProductUseCase;
        this.updateProductUseCase = updateProductUseCase;
        this.changeProductPriceUseCase = changeProductPriceUseCase;
        this.activateProductUseCase = activateProductUseCase;
        this.retireProductUseCase = retireProductUseCase;
        this.getProductUseCase = getProductUseCase;
        this.searchProductsUseCase = searchProductsUseCase;
        this.assignCategoryToProductUseCase = assignCategoryToProductUseCase;
        this.removeCategoryFromProductUseCase = removeCategoryFromProductUseCase;
    }

    @PostMapping
    public ResponseEntity<CreateProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        ProductId productId = createProductUseCase.createProduct(request.toCommand());
        return ResponseEntity
                .created(URI.create(PRODUCTS_PATH + productId.value()))
                .body(CreateProductResponse.from(productId));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> get(@PathVariable UUID productId) {
        ProductResponse response = ProductResponse.from(getProductUseCase.getProduct(new ProductId(productId)));
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<ProductSummaryResponse>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        SearchProductsQuery query = new SearchProductsQuery(
                q,
                categoryId == null ? null : new CategoryId(categoryId),
                status,
                page,
                size);
        ProductPageResult result = searchProductsUseCase.search(query);
        List<ProductSummaryResponse> items = result.items().stream()
                .map(ProductSummaryResponse::from)
                .toList();
        return ResponseEntity.ok(new PageResponse<>(items, result.page(), result.size(), result.totalElements(), result.totalPages()));
    }

    @PatchMapping("/{productId}")
    public ResponseEntity<Void> update(@PathVariable UUID productId, @Valid @RequestBody UpdateProductRequest request) {
        updateProductUseCase.updateProduct(new ProductId(productId), request.toCommand());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{productId}/price")
    public ResponseEntity<Void> changePrice(@PathVariable UUID productId, @Valid @RequestBody ChangeProductPriceRequest request) {
        changeProductPriceUseCase.changeProductPrice(new ProductId(productId), request.toCommand());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{productId}/activate")
    public ResponseEntity<Void> activate(@PathVariable UUID productId) {
        activateProductUseCase.activateProduct(new ProductId(productId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{productId}/retire")
    public ResponseEntity<Void> retire(@PathVariable UUID productId) {
        retireProductUseCase.retireProduct(new ProductId(productId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{productId}/categories/{categoryId}")
    public ResponseEntity<Void> assignCategory(@PathVariable UUID productId, @PathVariable UUID categoryId) {
        assignCategoryToProductUseCase.assignCategory(new ProductId(productId), new CategoryId(categoryId));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{productId}/categories/{categoryId}")
    public ResponseEntity<Void> removeCategory(@PathVariable UUID productId, @PathVariable UUID categoryId) {
        removeCategoryFromProductUseCase.removeCategory(new ProductId(productId), new CategoryId(categoryId));
        return ResponseEntity.noContent().build();
    }
}
