package com.ecommerce.warehouse.infrastructure.adapter.in.web;

import com.ecommerce.warehouse.application.dto.ReceiveStockCommand;
import com.ecommerce.warehouse.application.dto.StockQueryResult;
import com.ecommerce.warehouse.application.port.in.GetStockMovementsUseCase;
import com.ecommerce.warehouse.application.port.in.GetStockUseCase;
import com.ecommerce.warehouse.application.port.in.ReceiveStockUseCase;
import com.ecommerce.warehouse.application.port.in.ReleaseStockUseCase;
import com.ecommerce.warehouse.application.port.in.ReserveStockUseCase;
import com.ecommerce.warehouse.domain.exception.InsufficientStockException;
import com.ecommerce.warehouse.domain.model.CompanyId;
import com.ecommerce.warehouse.domain.model.ProductId;
import com.ecommerce.warehouse.domain.model.StockId;
import com.ecommerce.warehouse.domain.model.StockMovementType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StockController.class)
class StockControllerTest {

    private static final CompanyId COMPANY_ID = new CompanyId(UUID.fromString("90000000-0000-0000-0000-000000000001"));
    private static final UUID PRODUCT_UUID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final StockId STOCK_ID = new StockId(COMPANY_ID, new ProductId(PRODUCT_UUID));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReceiveStockUseCase receiveStockUseCase;

    @MockitoBean
    private ReserveStockUseCase reserveStockUseCase;

    @MockitoBean
    private ReleaseStockUseCase releaseStockUseCase;

    @MockitoBean
    private GetStockUseCase getStockUseCase;

    @MockitoBean
    private GetStockMovementsUseCase getStockMovementsUseCase;

    @MockitoBean
    private CompanyContext companyContext;

    @BeforeEach
    void setUp() {
        when(companyContext.currentCompanyId()).thenReturn(COMPANY_ID);
    }

    @Test
    void getStockReturns200WithBody() throws Exception {
        when(getStockUseCase.getStock(any())).thenReturn(stockQueryResult(10, 3));

        mockMvc.perform(get("/api/stock/" + PRODUCT_UUID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(PRODUCT_UUID.toString()))
                .andExpect(jsonPath("$.available").value(10))
                .andExpect(jsonPath("$.reserved").value(3))
                .andExpect(jsonPath("$.movements").isArray());
    }

    @Test
    void receiveReturns204() throws Exception {
        mockMvc.perform(post("/api/stock/" + PRODUCT_UUID + "/receive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 5}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void reserveReturns204() throws Exception {
        mockMvc.perform(post("/api/stock/" + PRODUCT_UUID + "/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 3}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void releaseReturns204() throws Exception {
        mockMvc.perform(post("/api/stock/" + PRODUCT_UUID + "/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 2}"))
                .andExpect(status().isNoContent());
    }

    @Test
    void receiveCarriesStockIdFromContext() throws Exception {
        mockMvc.perform(post("/api/stock/" + PRODUCT_UUID + "/receive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 5}"))
                .andExpect(status().isNoContent());

        ArgumentCaptor<ReceiveStockCommand> captor = ArgumentCaptor.forClass(ReceiveStockCommand.class);
        verify(receiveStockUseCase).receiveStock(captor.capture());
        assertThat(captor.getValue().stockId()).isEqualTo(STOCK_ID);
    }

    @Test
    void reserveInsufficientStockReturns422() throws Exception {
        doThrow(new InsufficientStockException(STOCK_ID, 10, 0))
                .when(reserveStockUseCase).reserveStock(any());

        mockMvc.perform(post("/api/stock/" + PRODUCT_UUID + "/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\": 10}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Insufficient stock"));
    }

    private StockQueryResult stockQueryResult(int available, int reserved) {
        StockQueryResult.StockMovementResult movement = new StockQueryResult.StockMovementResult(
                StockMovementType.RECEIVED, available, Instant.now());
        return new StockQueryResult(STOCK_ID, available, reserved, List.of(movement));
    }
}
