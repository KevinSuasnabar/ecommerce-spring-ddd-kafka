package com.ecommerce.catalog.infrastructure.adapter.in.web;

import com.ecommerce.catalog.application.dto.CategoryQueryResult;
import com.ecommerce.catalog.application.dto.CreateCategoryCommand;
import com.ecommerce.catalog.application.port.in.CreateCategoryUseCase;
import com.ecommerce.catalog.application.port.in.GetCategoriesUseCase;
import com.ecommerce.catalog.domain.exception.DuplicateCategoryException;
import com.ecommerce.catalog.domain.model.CategoryId;
import com.ecommerce.catalog.domain.model.CompanyId;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    private static final CategoryId CATEGORY_ID = CategoryId.newId();
    private static final CompanyId COMPANY_ID = new CompanyId(UUID.fromString("90000000-0000-0000-0000-000000000001"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CreateCategoryUseCase createCategoryUseCase;

    @MockitoBean
    private GetCategoriesUseCase getCategoriesUseCase;

    @MockitoBean
    private CompanyContext companyContext;

    @BeforeEach
    void setUp() {
        when(companyContext.currentCompanyId()).thenReturn(COMPANY_ID);
    }

    @Test
    void createCategoryReturns201() throws Exception {
        when(createCategoryUseCase.createCategory(any(CreateCategoryCommand.class))).thenReturn(CATEGORY_ID);

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Computers"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryId").value(CATEGORY_ID.value().toString()));
    }

    @Test
    void createDuplicateCategoryReturns422() throws Exception {
        when(createCategoryUseCase.createCategory(any(CreateCategoryCommand.class)))
                .thenThrow(new DuplicateCategoryException("Computers"));

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Computers"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void listCategoriesReturns200() throws Exception {
        when(getCategoriesUseCase.getCategories(any(CompanyId.class)))
                .thenReturn(List.of(new CategoryQueryResult(CATEGORY_ID, "Computers", null)));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Computers"));
    }
}
