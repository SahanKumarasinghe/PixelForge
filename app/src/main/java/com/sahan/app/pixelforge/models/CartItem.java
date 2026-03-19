package com.sahan.app.pixelforge.models;

import com.google.firebase.firestore.Exclude;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CartItem {
    @Getter(onMethod_ = {@Exclude})
    @Setter(onMethod_ = {@Exclude})
    private String documentID;
    private String productID;
    private int quantity;
    private List<Attribute> attributes;

    public CartItem(String productID,int qty,List<Attribute> attributes){
        this.productID = productID;
        this.quantity = qty;
        this.attributes = attributes;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class Attribute {
        private String name;
        private String value;
    }

}
