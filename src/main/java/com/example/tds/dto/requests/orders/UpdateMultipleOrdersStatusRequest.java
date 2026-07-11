package com.example.tds.dto.requests.orders;

import com.example.tds.enums.OrderStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class UpdateMultipleOrdersStatusRequest {
    @NotEmpty(message = "Order IDs list cannot be empty")
    private List<UUID> orderIds;

    @NotNull(message = "Status cannot be null")
    private OrderStatus status;
}
