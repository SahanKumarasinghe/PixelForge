package com.sahan.app.pixelforge.adapters;

import android.icu.text.NumberFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.sahan.app.pixelforge.R;
import com.sahan.app.pixelforge.models.Product;

import java.util.List;

public class SectionAdapter extends RecyclerView.Adapter<SectionAdapter.ViewHolder> {

    private final List<Product> products;

    private OnProductClickListener clickListener;

    public SectionAdapter(List<Product> products, OnProductClickListener clickListener) {
        this.products = products;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product_recycler, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = products.get(position);
        holder.productTitle.setText(product.getTitle());
        NumberFormat formatter = NumberFormat.getNumberInstance();
        holder.productPrice.setText("LKR "+formatter.format(product.getPrice()));
        Glide.with(holder.productImage.getContext())
                .load(product.getProduct_images().get(0))
                .centerCrop()
                .into(holder.productImage);

        holder.itemView.setOnClickListener((View view) -> {
            Animation animation = AnimationUtils.loadAnimation(view.getContext(), R.anim.card_pressed_animation);
            view.startAnimation(animation);

            if (clickListener != null) {
                clickListener.onProductClick(product);
            }
        });

    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView productImage;
        TextView productTitle;
        TextView productPrice;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.item_product_r_image);
            productTitle = itemView.findViewById(R.id.item_product_r_title);
            productPrice = itemView.findViewById(R.id.item_product_r_price);
        }
    }

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

}
