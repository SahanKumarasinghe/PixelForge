package com.sahan.app.pixelforge.models;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Product {

    private String productID;
    private String title;
    private String description;
    private double price;
    private String catID;
    private List<String> product_images;
    private int stockCount;
    private boolean status;
    private int rating;
    private List<Attribute> attributes;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Attribute{
        private String name;
        private String type;
        private List<String> values;
    }

}
