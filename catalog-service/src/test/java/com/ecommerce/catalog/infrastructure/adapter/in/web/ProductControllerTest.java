package com.ecommerce.catalog.infrastructure.adapter.in.web;

import com.ecommerce.catalog.application.dto.ProductPageResult;
import com.ecommerce.catalog.application.dto.ProductQueryResult;
import com.ecommerce.catalog.application.dto.ProductSummary;
import com.ecommerce.catalog.application.dto.SearchProductsQuery;
import com.ecommerce.catalog.application.service.CatalogApplicationService;
import com.ecommerce.catalog.domain.exception.ProductNotFoundException;
import com.ecommerce.catalog.domain.model.Money;
import com.ecommerce.catalog.domain.model.Product;
import com.ecommerce.catalog.domain.model.ProductId;
import com.ecommerce.catalog.domain.model.ProductStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final ProductId PRODUCT_ID = ProductId.newId();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CatalogApplicationService catalogApplicationService;

    @Test
    void createProductReturns201WithLocation() throws Exception {
        when(catalogApplicationService.createProduct(any())).thenReturn(PRODUCT_ID);

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
        when(catalogApplicationService.getProduct(any())).thenReturn(queryResult());

        mockMvc.perform(get("/api/products/" + PRODUCT_ID.value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PRODUCT_ID.value().toString()))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.price").value(1500.00));
    }

    @Test
    void getMissingProductReturns404() throws Exception {
        when(catalogApplicationService.getProduct(any())).thenThrow(new ProductNotFoundException(PRODUCT_ID));

        mockMvc.perform(get("/api/products/" + PRODUCT_ID.value()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void searchReturnsPagedResponse() throws Exception {
        ProductSummary summary = new ProductSummary(PRODUCT_ID, "Notebook", new Money(new BigDecimal("1500.00"), USD), ProductStatus.ACTIVE);
        when(catalogApplicationService.search(any(SearchProductsQuery.class)))
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

    private ProductQueryResult queryResult() {
        Product product = Product.create(PRODUCT_ID, "Notebook", "16GB RAM", new Money(new BigDecimal("1500.00"), USD));
        return ProductQueryResult.from(product);
    }
}
