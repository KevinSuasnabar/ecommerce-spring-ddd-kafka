package com.ecommerce.warehouse.infrastructure.adapter.in.web;

import com.ecommerce.warehouse.application.dto.ReceiveStockCommand;
import com.ecommerce.warehouse.application.dto.ReleaseStockCommand;
import com.ecommerce.warehouse.application.dto.ReserveStockCommand;
import com.ecommerce.warehouse.application.port.in.GetStockMovementsUseCase;
import com.ecommerce.warehouse.application.port.in.GetStockUseCase;
import com.ecommerce.warehouse.application.port.in.ReceiveStockUseCase;
import com.ecommerce.warehouse.application.port.in.ReleaseStockUseCase;
import com.ecommerce.warehouse.application.port.in.ReserveStockUseCase;
import com.ecommerce.warehouse.domain.model.CompanyId;
import com.ecommerce.warehouse.domain.model.ProductId;
import com.ecommerce.warehouse.domain.model.Quantity;
import com.ecommerce.warehouse.domain.model.StockId;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/stock")
public class StockController {

    private final ReceiveStockUseCase receiveStockUseCase;
    private final ReserveStockUseCase reserveStockUseCase;
    private final ReleaseStockUseCase releaseStockUseCase;
    private final GetStockUseCase getStockUseCase;
    private final GetStockMovementsUseCase getStockMovementsUseCase;
    private final CompanyContext companyContext;

    public StockController(ReceiveStockUseCase receiveStockUseCase,
                           ReserveStockUseCase reserveStockUseCase,
                           ReleaseStockUseCase releaseStockUseCase,
                           GetStockUseCase getStockUseCase,
                           GetStockMovementsUseCase getStockMovementsUseCase,
                           CompanyContext companyContext) {
        this.receiveStockUseCase = receiveStockUseCase;
        this.reserveStockUseCase = reserveStockUseCase;
        this.releaseStockUseCase = releaseStockUseCase;
        this.getStockUseCase = getStockUseCase;
        this.getStockMovementsUseCase = getStockMovementsUseCase;
        this.companyContext = companyContext;
    }

    @GetMapping("/{productId}")
    public ResponseEntity<StockResponse> get(@PathVariable UUID productId) {
        CompanyId companyId = companyContext.currentCompanyId();
        StockId stockId = new StockId(companyId, new ProductId(productId));
        return ResponseEntity.ok(StockResponse.from(getStockUseCase.getStock(stockId)));
    }

    @GetMapping("/{productId}/movements")
    public ResponseEntity<StockResponse> getMovements(@PathVariable UUID productId) {
        CompanyId companyId = companyContext.currentCompanyId();
        StockId stockId = new StockId(companyId, new ProductId(productId));
        return ResponseEntity.ok(StockResponse.from(getStockMovementsUseCase.getStockMovements(stockId)));
    }

    @PostMapping("/{productId}/receive")
    public ResponseEntity<Void> receive(@PathVariable UUID productId,
                                        @Valid @RequestBody StockQuantityRequest request) {
        StockId stockId = new StockId(companyContext.currentCompanyId(), new ProductId(productId));
        receiveStockUseCase.receiveStock(new ReceiveStockCommand(stockId, new Quantity(request.quantity())));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{productId}/reserve")
    public ResponseEntity<Void> reserve(@PathVariable UUID productId,
                                        @Valid @RequestBody StockQuantityRequest request) {
        StockId stockId = new StockId(companyContext.currentCompanyId(), new ProductId(productId));
        reserveStockUseCase.reserveStock(new ReserveStockCommand(stockId, new Quantity(request.quantity())));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{productId}/release")
    public ResponseEntity<Void> release(@PathVariable UUID productId,
                                        @Valid @RequestBody StockQuantityRequest request) {
        StockId stockId = new StockId(companyContext.currentCompanyId(), new ProductId(productId));
        releaseStockUseCase.releaseStock(new ReleaseStockCommand(stockId, new Quantity(request.quantity())));
        return ResponseEntity.noContent().build();
    }
}
