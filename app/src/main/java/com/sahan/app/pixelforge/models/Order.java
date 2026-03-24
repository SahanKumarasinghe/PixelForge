package com.sahan.app.pixelforge.models;

import com.google.firebase.Timestamp;

import java.util.Date;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Order {

    private String orderId;
    private String userId;
    private double totalAmount;
    private String status;
    private Timestamp orderDate;
    private List<OrderItem> orderItems;
    private Address shippingAddress;
    private Address billingAddress;


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class OrderItem {
        private String productId;
        private int quantity;
        private double unitPrice;
        private List<OrderItem.Attribute> attributes;

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        @Builder
        public static class Attribute {
            private String name;
            private String value;
        }

    }
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Address{
        private String name;
        private String email;
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String postCode;
        private String phoneNumber;
    }

}
