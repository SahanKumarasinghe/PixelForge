package com.sahan.app.pixelforge.adapters;

import android.icu.text.NumberFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sahan.app.pixelforge.R;
import com.sahan.app.pixelforge.models.Order;
import com.sahan.app.pixelforge.models.Product;

import java.util.List;

public class OrderItemsAdapter extends RecyclerView.Adapter<OrderItemsAdapter.ViewHolder> {

    private final List<Order.OrderItem> orderItems;

    public OrderItemsAdapter(List<Order.OrderItem> orderItems) {
        this.orderItems = orderItems;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order.OrderItem item = orderItems.get(position);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        holder.itemQty.setText("Qty: " + item.getQuantity());

        NumberFormat formatter = NumberFormat.getNumberInstance();
        double rowTotal = item.getUnitPrice() * item.getQuantity();
        holder.itemPrice.setText("LKR " + formatter.format(rowTotal));

        db.collection("products")
                .whereEqualTo("productID", item.getProductId())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Product product = queryDocumentSnapshots.getDocuments().get(0).toObject(Product.class);
                        if (product.getTitle() != null) {
                            holder.itemTitle.setText(product.getTitle());
                        }
                        if (product.getProduct_images() != null && !product.getProduct_images().isEmpty()) {
                            String imageUrl = product.getProduct_images().get(0);
                            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                                Glide.with(holder.itemView.getContext())
                                        .load(imageUrl)
                                        .placeholder(R.drawable.box_svgrepo_com)
                                        .into(holder.itemImage);
                            } else {
                                holder.itemImage.setImageResource(R.drawable.box_svgrepo_com);
                            }
                        } else {
                            holder.itemImage.setImageResource(R.drawable.box_svgrepo_com);
                        }
                    } else {
                        holder.itemTitle.setText("Product Unavailable");
                    }
                })
                .addOnFailureListener(e -> {
                    holder.itemTitle.setText("Error loading product");
                });
    }

    @Override
    public int getItemCount() {
        return orderItems != null ? orderItems.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView itemImage;
        TextView itemTitle;
        TextView itemQty;
        TextView itemPrice;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            itemImage = itemView.findViewById(R.id.order_item_image);
            itemTitle = itemView.findViewById(R.id.order_item_title);
            itemQty = itemView.findViewById(R.id.order_item_qty);
            itemPrice = itemView.findViewById(R.id.order_item_price);
        }
    }
}