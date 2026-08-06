package com.ecommerce.order;

import com.ecommerce.order.application.dto.CatalogProduct;
import com.ecommerce.order.application.port.out.CatalogProductStore;
import com.ecommerce.order.domain.model.CatalogProductStatus;
import com.ecommerce.order.domain.model.Money;
import com.ecommerce.order.domain.model.ProductId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
@AutoConfigureMockMvc
class OrderServiceIntegrationTest {

    private static final Currency USD = Currency.getInstance("USD");

    private static final ProductId NOTEBOOK = new ProductId(java.util.UUID.fromString("10000000-0000-0000-0000-000000000001"));
    private static final ProductId MOUSE = new ProductId(java.util.UUID.fromString("10000000-0000-0000-0000-000000000002"));
    private static final ProductId RETIRED = new ProductId(java.util.UUID.fromString("10000000-0000-0000-0000-000000000003"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CatalogProductStore catalogProductStore;

    @BeforeEach
    void seedCatalogSnapshot() {
        catalogProductStore.upsert(new CatalogProduct(NOTEBOOK, "Notebook",
                new Money(new BigDecimal("1500.00"), USD), CatalogProductStatus.ACTIVE, Instant.now()));
        catalogProductStore.upsert(new CatalogProduct(MOUSE, "Mouse",
                new Money(new BigDecimal("10.00"), USD), CatalogProductStatus.ACTIVE, Instant.now()));
        catalogProductStore.upsert(new CatalogProduct(RETIRED, "Old gadget",
                new Money(new BigDecimal("5.00"), USD), CatalogProductStatus.RETIRED, Instant.now()));
    }

    @Test
    void fullOrderLifecycleThroughTheApi() throws Exception {
        String createPayload = createPayload(NOTEBOOK, 1);

        MvcResult createResult = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andReturn();

        String location = createResult.getResponse().getHeader("Location");
        String orderId = location.substring(location.lastIndexOf('/') + 1);

        mockMvc.perform(get("/api/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.total").value(1500.00))
                .andExpect(jsonPath("$.lines[0].productName").value("Notebook"));

        mockMvc.perform(post("/api/orders/" + orderId + "/confirm"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(post("/api/orders/" + orderId + "/ship"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));

        mockMvc.perform(post("/api/orders/" + orderId + "/deliver"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));
    }

    @Test
    void confirmOrderWithOutOfStockProductReturns422() throws Exception {
        String createPayload = createPayload(MOUSE, 999);

        MvcResult createResult = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andReturn();

        String location = createResult.getResponse().getHeader("Location");
        String orderId = location.substring(location.lastIndexOf('/') + 1);

        mockMvc.perform(post("/api/orders/" + orderId + "/confirm"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));
    }

    @Test
    void createOrderWithUnknownProductReturns404() throws Exception {
        ProductId unknown = new ProductId(java.util.UUID.fromString("30000000-0000-0000-0000-000000000001"));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload(unknown, 1)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_AVAILABLE"));
    }

    @Test
    void createOrderWithRetiredProductReturns404() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload(RETIRED, 1)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_AVAILABLE"));
    }

    private String createPayload(ProductId productId, int quantity) {
        return """
                {
                  "customerId": "90000000-0000-0000-0000-000000000001",
                  "lines": [
                    {
                      "productId": "%s",
                      "quantity": %d
                    }
                  ],
                  "shippingAddress": {
                    "street": "Av. Siempre Viva 123",
                    "city": "Springfield",
                    "state": "",
                    "country": "AR",
                    "zipCode": "1406"
                  },
                  "paymentMethod": "CREDIT_CARD"
                }
                """.formatted(productId.value(), quantity);
    }
}
