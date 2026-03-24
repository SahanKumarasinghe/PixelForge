package com.sahan.app.pixelforge.adapters;

import android.icu.text.NumberFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.sahan.app.pixelforge.R;
import com.sahan.app.pixelforge.models.Product;
import com.sahan.app.pixelforge.models.WishlistManager;

import java.util.List;

public class HomeProductAdapter extends RecyclerView.Adapter<HomeProductAdapter.ViewHolder> {

    private final List<Product> products;

    private OnProductClickListener clickListener;

    public HomeProductAdapter(List<Product> products, OnProductClickListener clickListener) {
        this.products = products;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.home_product_listing, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = products.get(position);
        holder.productTitle.setText(product.getTitle());
        NumberFormat formatter = NumberFormat.getNumberInstance();
        holder.productPrice.setText("LKR " + formatter.format(product.getPrice()));
        holder.homeProductRatingBar.setRating((float)product.getRating());
        holder.ratingNumber.setText(String.valueOf((float)product.getRating()));
        Glide.with(holder.productImage.getContext())
                .load(product.getProduct_images().get(0))
                .into(holder.productImage);

        holder.itemView.setOnClickListener((View view) -> {
            Animation animation = AnimationUtils.loadAnimation(view.getContext(), R.anim.card_pressed_animation);
            view.startAnimation(animation);

            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {}

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (clickListener != null) {
                        clickListener.onProductClick(product);
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {}
            });
        });

        WishlistManager.checkIfInWishlist(holder.itemView.getContext(), product.getProductID(), isInWishlist -> {
            holder.wishlistButton.setChecked(isInWishlist);
            if (isInWishlist) {
                holder.wishlistButton.setIconResource(R.drawable.filled_heart);
            } else {
                holder.wishlistButton.setIconResource(R.drawable.outline_heart);
            }
        });

        holder.wishlistButton.setOnClickListener(v -> {
            boolean newState = holder.wishlistButton.isChecked();

            if (newState) {
                WishlistManager.addToWishlist(v.getContext(), product.getProductID());
                holder.wishlistButton.setIconResource(R.drawable.filled_heart);
            } else {
                WishlistManager.removeFromWishlist(v.getContext(), product.getProductID());
                holder.wishlistButton.setIconResource(R.drawable.outline_heart);
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
        MaterialButton wishlistButton;
        RatingBar homeProductRatingBar;
        TextView ratingNumber;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.listing_image);
            productTitle = itemView.findViewById(R.id.listing_title);
            productPrice = itemView.findViewById(R.id.listing_price);
            wishlistButton = itemView.findViewById(R.id.home_wishlist_button);
            homeProductRatingBar = itemView.findViewById(R.id.home_product_rating_bar);
            ratingNumber = itemView.findViewById(R.id.home_product_rating_number);
        }
    }

    public interface OnProductClickListener {
        void onProductClick(Product product);
    }

}
