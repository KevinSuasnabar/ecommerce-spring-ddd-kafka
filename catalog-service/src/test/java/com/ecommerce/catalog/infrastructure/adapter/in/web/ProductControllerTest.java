package com.ecommerce.catalog.infrastructure.adapter.in.web;

import com.ecommerce.catalog.application.dto.*;
import com.ecommerce.catalog.application.port.in.ActivateProductUseCase;
import com.ecommerce.catalog.application.port.in.AssignCategoryToProductUseCase;
import com.ecommerce.catalog.application.port.in.ChangeProductPriceUseCase;
import com.ecommerce.catalog.application.port.in.CreateProductUseCase;
import com.ecommerce.catalog.application.port.in.GetProductUseCase;
import com.ecommerce.catalog.application.port.in.RemoveCategoryFromProductUseCase;
import com.ecommerce.catalog.application.port.in.RetireProductUseCase;
import com.ecommerce.catalog.application.port.in.SearchProductsUseCase;
import com.ecommerce.catalog.application.port.in.UpdateProductUseCase;
import com.ecommerce.catalog.domain.exception.ProductNotFoundException;
import com.ecommerce.catalog.domain.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final ProductId PRODUCT_ID = ProductId.newId();
    private static final CompanyId COMPANY_ID = new CompanyId(UUID.fromString("90000000-0000-0000-0000-000000000001"));
    private static final CategoryId CATEGORY_ID = CategoryId.newId();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CreateProductUseCase createProductUseCase;

    @MockitoBean
    private UpdateProductUseCase updateProductUseCase;

    @MockitoBean
    private ChangeProductPriceUseCase changeProductPriceUseCase;

    @MockitoBean
    private ActivateProductUseCase activateProductUseCase;

    @MockitoBean
    private RetireProductUseCase retireProductUseCase;

    @MockitoBean
    private GetProductUseCase getProductUseCase;

    @MockitoBean
    private SearchProductsUseCase searchProductsUseCase;

    @MockitoBean
    private AssignCategoryToProductUseCase assignCategoryToProductUseCase;

    @MockitoBean
    private RemoveCategoryFromProductUseCase removeCategoryFromProductUseCase;

    @MockitoBean
    private CompanyContext companyContext;

    @BeforeEach
    void setUp() {
        when(companyContext.currentCompanyId()).thenReturn(COMPANY_ID);
    }

    @Test
    void createProductReturns201WithLocation() throws Exception {
        when(createProductUseCase.createProduct(any())).thenReturn(PRODUCT_ID);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Notebook",
                                "description", "16GB RAM",
                                "price", 1500.00,
                                "currency", "USD"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/products/" + PRODUCT_ID.value()));
    }

    @Test
    void createProductCarriesCompanyIdFromContext() throws Exception {
        when(createProductUseCase.createProduct(any())).thenReturn(PRODUCT_ID);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Notebook",
                                "description", "16GB RAM",
                                "price", 1500.00,
                                "currency", "USD"))))
                .andExpect(status().isCreated());

        ArgumentCaptor<CreateProductCommand> captor = ArgumentCaptor.forClass(CreateProductCommand.class);
        verify(createProductUseCase).createProduct(captor.capture());
        assertThat(captor.getValue().companyId()).isEqualTo(COMPANY_ID);
    }

    @Test
    void createProductWithBlankNameReturns400() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", " ",
                                "price", 1500.00,
                                "currency", "USD"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void getExistingProductReturns200() throws Exception {
        when(getProductUseCase.getProduct(any(CompanyId.class), any(ProductId.class))).thenReturn(queryResult());

        mockMvc.perform(get("/api/products/" + PRODUCT_ID.value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PRODUCT_ID.value().toString()))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.price").value(1500.00));
    }

    @Test
    void getProductCarriesCompanyIdFromContext() throws Exception {
        when(getProductUseCase.getProduct(any(CompanyId.class), any(ProductId.class))).thenReturn(queryResult());

        mockMvc.perform(get("/api/products/" + PRODUCT_ID.value()))
                .andExpect(status().isOk());

        ArgumentCaptor<CompanyId> captor = ArgumentCaptor.forClass(CompanyId.class);
        verify(getProductUseCase).getProduct(captor.capture(), any(ProductId.class));
        assertThat(captor.getValue()).isEqualTo(COMPANY_ID);
    }

    @Test
    void getMissingProductReturns404() throws Exception {
        when(getProductUseCase.getProduct(any(CompanyId.class), any(ProductId.class)))
                .thenThrow(new ProductNotFoundException(PRODUCT_ID));

        mockMvc.perform(get("/api/products/" + PRODUCT_ID.value()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void searchReturnsPagedResponse() throws Exception {
        ProductSummary summary = new ProductSummary(PRODUCT_ID, "Notebook", new Money(new BigDecimal("1500.00"), USD), ProductStatus.ACTIVE);
        when(searchProductsUseCase.search(any(CompanyId.class), any(SearchProductsQuery.class)))
                .thenReturn(ProductPageResult.of(List.of(summary), 0, 20, 1));

        mockMvc.perform(get("/api/products").param("q", "notebook").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].name").value("Notebook"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void changePriceReturns204() throws Exception {
        mockMvc.perform(post("/api/products/" + PRODUCT_ID.value() + "/price")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("price", 1400.00, "currency", "USD"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void activateReturns204() throws Exception {
        mockMvc.perform(post("/api/products/" + PRODUCT_ID.value() + "/activate"))
                .andExpect(status().isNoContent());
    }

    @Test
    void updateProductReturns204() throws Exception {
        mockMvc.perform(patch("/api/products/" + PRODUCT_ID.value())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "name test", "description", "test description"))))
                .andExpect(status().isNoContent());

        ArgumentCaptor<CompanyId> captor = ArgumentCaptor.forClass(CompanyId.class);
        verify(updateProductUseCase).updateProduct(captor.capture(), any(ProductId.class), any(UpdateProductCommand.class));
        assertThat(captor.getValue()).isEqualTo(COMPANY_ID);
    }

    @Test
    void retireProductReturn204() throws Exception {
        mockMvc.perform(post("/api/products/" + PRODUCT_ID.value() + "/retire"))
                .andExpect(status().isNoContent());


        ArgumentCaptor<CompanyId> captor = ArgumentCaptor.forClass(CompanyId.class);
        verify(retireProductUseCase).retireProduct(captor.capture(), any(ProductId.class));
        assertThat(captor.getValue()).isEqualTo(COMPANY_ID);
    }

    @Test
    void removeCategoryReturns204() throws Exception {
        mockMvc.perform(delete("/api/products/" + PRODUCT_ID.value() + "/categories/" + CATEGORY_ID.value()))
                .andExpect(status().isNoContent());


        ArgumentCaptor<CompanyId> companyCaptor = ArgumentCaptor.forClass(CompanyId.class);
        ArgumentCaptor<CategoryId> categoryCaptor = ArgumentCaptor.forClass(CategoryId.class);

        verify(removeCategoryFromProductUseCase).removeCategory(companyCaptor.capture(), any(ProductId.class), categoryCaptor.capture());
        assertThat(companyCaptor.getValue()).isEqualTo(COMPANY_ID);
        assertThat(categoryCaptor.getValue()).isEqualTo(CATEGORY_ID);

    }

    @Test
    void searchByCategoryFilters() throws Exception {
        ProductSummary summary = new ProductSummary(PRODUCT_ID, "Notebook", new Money(new BigDecimal("1500.00"), USD), ProductStatus.ACTIVE);
        when(searchProductsUseCase.search(any(CompanyId.class), any(SearchProductsQuery.class)))
                .thenReturn(ProductPageResult.of(List.of(summary), 0, 20, 1));

        mockMvc.perform(get("/api/products")
                        .param("q", "teclado")
                        .param("categoryId", CATEGORY_ID.value().toString())
                        .param("status", "DRAFT")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items[0].name").value("Notebook"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        ArgumentCaptor<CompanyId> companyCaptor = ArgumentCaptor.forClass(CompanyId.class);
        ArgumentCaptor<SearchProductsQuery> searchProductsQueryCaptor = ArgumentCaptor.forClass(SearchProductsQuery.class);


        verify(searchProductsUseCase).search(companyCaptor.capture(), searchProductsQueryCaptor.capture());
        assertThat(companyCaptor.getValue()).isEqualTo(COMPANY_ID);
        assertThat(searchProductsQueryCaptor.getValue().keyword()).isEqualTo("teclado");
        assertThat(searchProductsQueryCaptor.getValue().categoryId()).isEqualTo(CATEGORY_ID);

    }

    private ProductQueryResult queryResult() {
        Product product = Product.create(PRODUCT_ID, "Notebook", "16GB RAM", new Money(new BigDecimal("1500.00"), USD), COMPANY_ID);
        return ProductQueryResult.from(product);
    }
}
