package com.ecommerce.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.task.scheduling.enabled=false")
@AutoConfigureMockMvc
@Transactional
class CatalogServiceIT extends AbstractPostgresIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void productLifecycleThroughTheApi() throws Exception {
        String createPayload = """
                {
                  "name": "Notebook",
                  "description": "16GB RAM",
                  "price": 1500.00,
                  "currency": "USD"
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").isNotEmpty())
                .andReturn();

        String location = createResult.getResponse().getHeader("Location");
        String productId = location.substring(location.lastIndexOf('/') + 1);

        mockMvc.perform(get("/api/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.price").value(1500.00));

        mockMvc.perform(post("/api/products/" + productId + "/activate"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/products").param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].id").value(productId));

        mockMvc.perform(get("/api/products").param("status", "DRAFT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void categoryHierarchyAndProductAssignment() throws Exception {
        String computers = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Computers\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        String computersId = computers.substring(computers.lastIndexOf('/') + 1);

        String laptops = mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Laptops\", \"parentId\": \"" + computersId + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");
        String laptopsId = laptops.substring(laptops.lastIndexOf('/') + 1);

        MvcResult product = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Notebook\", \"price\": 1500.00, \"currency\": \"USD\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String productLocation = product.getResponse().getHeader("Location");
        String productId = productLocation.substring(productLocation.lastIndexOf('/') + 1);

        mockMvc.perform(post("/api/products/" + productId + "/categories/" + laptopsId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/products").param("categoryId", laptopsId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void duplicateCategoryReturns422() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Unique\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"unique\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }
}
