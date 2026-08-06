package com.ecommerce.catalog.infrastructure.adapter.in.web;

import com.ecommerce.catalog.application.dto.CategoryQueryResult;
import com.ecommerce.catalog.application.dto.CreateCategoryCommand;
import com.ecommerce.catalog.application.service.CatalogApplicationService;
import com.ecommerce.catalog.domain.exception.DuplicateCategoryException;
import com.ecommerce.catalog.domain.model.CategoryId;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    private static final CategoryId CATEGORY_ID = CategoryId.newId();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CatalogApplicationService catalogApplicationService;

    @Test
    void createCategoryReturns201() throws Exception {
        when(catalogApplicationService.createCategory(any(CreateCategoryCommand.class))).thenReturn(CATEGORY_ID);

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Computers"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryId").value(CATEGORY_ID.value().toString()));
    }

    @Test
    void createDuplicateCategoryReturns422() throws Exception {
        when(catalogApplicationService.createCategory(any(CreateCategoryCommand.class)))
                .thenThrow(new DuplicateCategoryException("Computers"));

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Computers"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void listCategoriesReturns200() throws Exception {
        when(catalogApplicationService.getCategories())
                .thenReturn(List.of(new CategoryQueryResult(CATEGORY_ID, "Computers", null)));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Computers"));
    }
}
