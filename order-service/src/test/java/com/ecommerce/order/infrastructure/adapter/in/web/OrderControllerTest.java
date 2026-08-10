package com.ecommerce.order.infrastructure.adapter.in.web;

import com.ecommerce.order.application.dto.OrderQueryResult;
import com.ecommerce.order.application.service.OrderApplicationService;
import com.ecommerce.order.domain.exception.OrderNotFoundException;
import com.ecommerce.order.domain.model.Address;
import com.ecommerce.order.domain.model.CustomerId;
import com.ecommerce.order.domain.model.Money;
import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.model.OrderId;
import com.ecommerce.order.domain.model.OrderLine;
import com.ecommerce.order.domain.model.OrderStatus;
import com.ecommerce.order.domain.model.PaymentMethod;
import com.ecommerce.order.domain.model.ProductId;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final OrderId ORDER_ID = OrderId.newId();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderApplicationService orderApplicationService;

    @Test
    void createOrderReturns201WithLocationAndBody() throws Exception {
        when(orderApplicationService.createOrder(any())).thenReturn(ORDER_ID);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreatePayload()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/orders/" + ORDER_ID.value()))
                .andExpect(jsonPath("$.orderId").value(ORDER_ID.value().toString()));
    }

    @Test
    void createOrderWithInvalidPayloadReturns400() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "customerId", UUID.randomUUID().toString(),
                "lines", List.of(),
                "shippingAddress", Map.of("street", "", "city", "Springfield", "country", "AR", "zipCode", "1406"),
                "paymentMethod", "CREDIT_CARD"));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void getExistingOrderReturns200() throws Exception {
        when(orderApplicationService.getOrder(any())).thenReturn(queryResult(OrderStatus.CREATED));

        mockMvc.perform(get("/api/orders/" + ORDER_ID.value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ORDER_ID.value().toString()))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.total").value(3000.00));
    }

    @Test
    void getMissingOrderReturns404() throws Exception {
        when(orderApplicationService.getOrder(any())).thenThrow(new OrderNotFoundException(ORDER_ID));

        mockMvc.perform(get("/api/orders/" + ORDER_ID.value()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void confirmOrderReturns204() throws Exception {
        mockMvc.perform(post("/api/orders/" + ORDER_ID.value() + "/confirm"))
                .andExpect(status().isNoContent());
    }

    @Test
    void cancelOrderWithBlankReasonReturns400() throws Exception {
        mockMvc.perform(post("/api/orders/" + ORDER_ID.value() + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.reason", containsString("must not be blank")));
    }

    private String validCreatePayload() throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "customerId", "90000000-0000-0000-0000-000000000001",
                "lines", List.of(Map.of(
                        "productId", "10000000-0000-0000-0000-000000000001",
                        "quantity", 2)),
                "shippingAddress", Map.of(
                        "street", "Av. Siempre Viva 123",
                        "city", "Springfield",
                        "state", "",
                        "country", "AR",
                        "zipCode", "1406"),
                "paymentMethod", "CREDIT_CARD"));
    }

    private OrderQueryResult queryResult(OrderStatus status) {
        OrderLine line = new OrderLine(
                new ProductId(UUID.fromString("10000000-0000-0000-0000-000000000001")),
                "Notebook",
                2,
                new Money(new BigDecimal("1500.00"), USD));
        Order order = Order.create(
                ORDER_ID,
                new CustomerId(UUID.fromString("90000000-0000-0000-0000-000000000001")),
                List.of(line),
                new Address("Av. Siempre Viva 123", "Springfield", null, "AR", "1406"),
                PaymentMethod.CREDIT_CARD);
        if (status == OrderStatus.CONFIRMED) {
            order.confirm();
        }
        return OrderQueryResult.from(order);
    }
}
