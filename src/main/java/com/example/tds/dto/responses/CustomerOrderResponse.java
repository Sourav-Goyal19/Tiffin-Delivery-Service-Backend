package com.example.tds.dto.responses;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class CustomerOrderResponse extends OrderResponse {
    private Integer dropOtp;
}
