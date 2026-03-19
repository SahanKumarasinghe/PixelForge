package com.sahan.app.pixelforge.adapters;

import android.icu.text.NumberFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.sahan.app.pixelforge.R;
import com.sahan.app.pixelforge.databinding.FragmentCartBinding;
import com.sahan.app.pixelforge.databinding.FragmentHomeBinding;
import com.sahan.app.pixelforge.fragments.HomeFragment;
import com.sahan.app.pixelforge.models.CartItem;
import com.sahan.app.pixelforge.models.Product;

import java.util.List;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.ViewHolder> {

    private final List<CartItem> cartItems;
    private OnQuantityChangeListener qtyChangeListener;
    private OnCartItemRemovedListener removedListener;

    public CartAdapter(List<CartItem> cartItems) {
        this.cartItems = cartItems;
    }

    public void setOnQuantityChangeListener(OnQuantityChangeListener listener) {
        this.qtyChangeListener = listener;
    }

    public void setOnCartItemRemovedListener(OnCartItemRemovedListener listener) {
        this.removedListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CartItem cartItem = this.cartItems.get(position);
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("products")
                .whereEqualTo("productID", cartItem.getProductID())
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot qds) {

                        if (!qds.isEmpty()) {

                            int currentPosition = holder.getAbsoluteAdapterPosition();

                            if (currentPosition == RecyclerView.NO_POSITION) {
                                return;
                            }

                            Product product = qds.getDocuments().get(0).toObject(Product.class);

                            NumberFormat formatter = NumberFormat.getNumberInstance();
                            holder.cartProductTitle.setText(product.getTitle());
                            holder.cartProductPrice.setText("LKR " + formatter.format(product.getPrice()));
                            holder.cartProductQty.setText(String.valueOf(cartItem.getQuantity()));
//                            holder.totalProductCount.setText(String.valueOf(cartItems.size()));

                            double totalproductprice = product.getPrice() * cartItem.getQuantity();

                            holder.cartTotalProductPrice.setText("LKR " + formatter.format(totalproductprice));
                            Glide.with(holder.cartProductImage.getContext())
                                    .load(product.getProduct_images().get(0))
                                    .into(holder.cartProductImage);


                            holder.cartProductQtyPlus.setOnClickListener(v -> {
                                if (product.getStockCount() > cartItem.getQuantity()){
                                    cartItem.setQuantity(cartItem.getQuantity() + 1);
                                    notifyItemChanged(currentPosition);
                                    if (qtyChangeListener != null) {
                                        qtyChangeListener.onQtyChanged(cartItem);
                                    }
                                }
                            });
                            holder.cartProductQtyMinus.setOnClickListener(v -> {
                                if (cartItem.getQuantity() > 1) {
                                    cartItem.setQuantity(cartItem.getQuantity() - 1);
                                    notifyItemChanged(currentPosition);
                                    if (qtyChangeListener != null) {
                                        qtyChangeListener.onQtyChanged(cartItem);
                                    }
                                }

                            });

                            holder.cartProductRemove.setOnClickListener(v -> {
                                if (removedListener != null) {
                                    removedListener.onCartItemRemoved(currentPosition);
                                }
                            });
                        }

                    }
                });

    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView cartProductImage;
        TextView cartProductTitle;
        TextView cartProductPrice;
        TextView cartTotalProductPrice;
        TextView cartProductQty;
        MaterialButton cartProductQtyPlus;
        MaterialButton cartProductQtyMinus;
        ImageView cartProductRemove;
        TextView totalProductCount;
        TextView totalProductQty;
        MaterialButton shopNow;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cartProductImage = itemView.findViewById(R.id.cart_product_image);
            cartProductTitle = itemView.findViewById(R.id.cart_product_title);
            cartProductPrice = itemView.findViewById(R.id.cart_product_price);
            cartTotalProductPrice = itemView.findViewById(R.id.cart_product_total_price);
            cartProductQty = itemView.findViewById(R.id.cart_product_quantity);
            cartProductQtyPlus = itemView.findViewById(R.id.cart_product_quantity_plus);
            cartProductQtyMinus = itemView.findViewById(R.id.cart_product_quantity_minus);
            cartProductRemove = itemView.findViewById(R.id.cart_product_remove);
            totalProductCount = itemView.findViewById(R.id.total_product_count);
            totalProductQty = itemView.findViewById(R.id.total_product_quantity);
            this.shopNow = itemView.findViewById(R.id.cart_btn_shop_now);
        }
    }

    public interface OnQuantityChangeListener {
        void onQtyChanged(CartItem cartItem);
    }

    public interface OnCartItemRemovedListener {
        void onCartItemRemoved(int position);
    }

}
