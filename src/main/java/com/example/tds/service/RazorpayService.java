package com.example.tds.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RazorpayService {

    private final RazorpayClient razorpayClient;

    public String createOrder(double amount, String currency, String customerName, UUID subscriptionId) throws RazorpayException {
        long amountInPaise = Math.round(amount * 100);

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", currency);

        JSONObject notes = new JSONObject();
        notes.put("subscriptionId", subscriptionId.toString());
        notes.put("customerName", customerName);

        orderRequest.put("notes", notes);

        Order order = razorpayClient.orders.create(orderRequest);

        return order.get("id");
    }
}