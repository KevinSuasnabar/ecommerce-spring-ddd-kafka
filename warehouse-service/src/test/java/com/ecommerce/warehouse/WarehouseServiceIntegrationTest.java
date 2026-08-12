package com.ecommerce.warehouse;

import com.ecommerce.warehouse.domain.model.Quantity;
import com.ecommerce.warehouse.domain.repository.StockRepository;
import com.ecommerce.warehouse.domain.model.CompanyId;
import com.ecommerce.warehouse.domain.model.ProductId;
import com.ecommerce.warehouse.domain.model.Stock;
import com.ecommerce.warehouse.domain.model.StockId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class WarehouseServiceIntegrationTest {

    private static final UUID PRODUCT_ID = UUID.fromString("50000000-0000-0000-0000-000000000001");
    private static final CompanyId COMPANY = new CompanyId(UUID.fromString("90000000-0000-0000-0000-000000000001"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StockRepository stockRepository;

    @BeforeEach
    void seedStock() {
        StockId stockId = new StockId(COMPANY, new ProductId(PRODUCT_ID));
        Stock stock = Stock.create(stockId);
        stock.receive(new Quantity(100));
        stockRepository.save(stock);
    }

    @Test
    void fullStockLifecycleThroughTheApi() throws Exception {
        mockMvc.perform(get("/api/stock/" + PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(100))
                .andExpect(jsonPath("$.reserved").value(0));

        mockMvc.perform(post("/api/stock/" + PRODUCT_ID + "/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 30}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/stock/" + PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(70))
                .andExpect(jsonPath("$.reserved").value(30));

        mockMvc.perform(post("/api/stock/" + PRODUCT_ID + "/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 10}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/stock/" + PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(80))
                .andExpect(jsonPath("$.reserved").value(20));

        mockMvc.perform(get("/api/stock/" + PRODUCT_ID + "/movements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(80))
                .andExpect(jsonPath("$.reserved").value(20))
                .andExpect(jsonPath("$.movements.length()").value(3));
    }

    @Test
    void reserveMoreThanAvailableReturns422() throws Exception {
        mockMvc.perform(post("/api/stock/" + PRODUCT_ID + "/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 999}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Insufficient stock"));
    }
}
