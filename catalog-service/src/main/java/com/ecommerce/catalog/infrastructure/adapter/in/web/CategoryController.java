package com.ecommerce.catalog.infrastructure.adapter.in.web;

import com.ecommerce.catalog.application.port.in.CreateCategoryUseCase;
import com.ecommerce.catalog.application.port.in.GetCategoriesUseCase;
import com.ecommerce.catalog.domain.model.CategoryId;
import com.ecommerce.catalog.domain.model.CompanyId;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private static final String CATEGORIES_PATH = "/api/categories/";

    private final CreateCategoryUseCase createCategoryUseCase;
    private final GetCategoriesUseCase getCategoriesUseCase;
    private final CompanyContext companyContext;

    public CategoryController(CreateCategoryUseCase createCategoryUseCase,
                              GetCategoriesUseCase getCategoriesUseCase,
                              CompanyContext companyContext) {
        this.createCategoryUseCase = createCategoryUseCase;
        this.getCategoriesUseCase = getCategoriesUseCase;
        this.companyContext = companyContext;
    }

    @PostMapping
    public ResponseEntity<CreateCategoryResponse> create(@Valid @RequestBody CreateCategoryRequest request) {
        CompanyId companyId = companyContext.currentCompanyId();
        CategoryId categoryId = createCategoryUseCase.createCategory(request.toCommand(companyId));
        return ResponseEntity
                .created(URI.create(CATEGORIES_PATH + categoryId.value()))
                .body(CreateCategoryResponse.from(categoryId));
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> list() {
        CompanyId companyId = companyContext.currentCompanyId();
        List<CategoryResponse> categories = getCategoriesUseCase.getCategories(companyId).stream()
                .map(CategoryResponse::from)
                .toList();
        return ResponseEntity.ok(categories);
    }
}
