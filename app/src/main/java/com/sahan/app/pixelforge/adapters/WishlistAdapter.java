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
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.sahan.app.pixelforge.R;
import com.sahan.app.pixelforge.models.Product;
import com.sahan.app.pixelforge.models.WishlistItem;

import java.util.List;

public class WishlistAdapter extends RecyclerView.Adapter<WishlistAdapter.ViewHolder> {

    private final List<WishlistItem> wishlistItems;
    private OnWishlistItemRemovedListener removedListener;

    public WishlistAdapter(List<WishlistItem> wishlistItems) {
        this.wishlistItems = wishlistItems;
    }

    public void setOnWishlistItemRemovedListener(OnWishlistItemRemovedListener listener) {
        this.removedListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wishlist, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WishlistItem wishlistItem = this.wishlistItems.get(position);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("products")
                .whereEqualTo("productID", wishlistItem.getProductID())
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot qds) {
                        if (!qds.isEmpty()) {
                            int currentPosition = holder.getAbsoluteAdapterPosition();
                            if (currentPosition == RecyclerView.NO_POSITION) return;

                            Product product = qds.getDocuments().get(0).toObject(Product.class);

                            NumberFormat formatter = NumberFormat.getNumberInstance();
                            holder.productTitle.setText(product.getTitle());
                            holder.productPrice.setText("LKR " + formatter.format(product.getPrice()));

                            Glide.with(holder.productImage.getContext())
                                    .load(product.getProduct_images().get(0))
                                    .into(holder.productImage);

                            holder.btnRemove.setOnClickListener(v -> {
                                if (removedListener != null) {
                                    removedListener.onWishlistItemRemoved(currentPosition);
                                }
                            });
                        }
                    }
                });
    }

    @Override
    public int getItemCount() {
        return wishlistItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView productTitle;
        TextView productPrice;
        ImageView btnRemove;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.wishlist_product_image);
            productTitle = itemView.findViewById(R.id.wishlist_product_title);
            productPrice = itemView.findViewById(R.id.wishlist_product_price);
            btnRemove = itemView.findViewById(R.id.wishlist_product_remove);
        }
    }

    public interface OnWishlistItemRemovedListener {
        void onWishlistItemRemoved(int position);
    }
}