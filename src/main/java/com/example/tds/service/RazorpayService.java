package com.example.tds.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;

import java.util.UUID;

@RequiredArgsConstructor
public class RazorpayService {
    private final RazorpayClient razorpayClient;

    public String createOrder(int amount, int currency, UUID billId) throws RazorpayException {
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amount);
        orderRequest.put("currency", currency);
        orderRequest.put("billId", billId);

        Order order = razorpayClient.orders.create(orderRequest);

        return order.toString();
    }
}
