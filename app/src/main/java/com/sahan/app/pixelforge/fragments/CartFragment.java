package com.sahan.app.pixelforge.fragments;

import android.icu.text.NumberFormat;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.sahan.app.pixelforge.R;
import com.sahan.app.pixelforge.activities.MainActivity;
import com.sahan.app.pixelforge.adapters.CartAdapter;
import com.sahan.app.pixelforge.databinding.FragmentCartBinding;
import com.sahan.app.pixelforge.models.CartItem;
import com.sahan.app.pixelforge.models.Product;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CartFragment extends Fragment {

    private FragmentCartBinding binding;
    private List<CartItem> cartItemList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCartBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadCart();

        binding.btnCheckout.setOnClickListener(v -> {
            CheckoutFragment checkoutFragment = new CheckoutFragment();
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.containerView, checkoutFragment)
                    .addToBackStack("cart").commit();

        });

    }

    @Override
    public void onResume() {
        super.onResume();
        loadCart();
    }

    private void loadCart() {

        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (firebaseAuth.getCurrentUser() != null) {

            String uid = firebaseAuth.getCurrentUser().getUid();

            db.collection("users")
                    .document(uid)
                    .collection("cart")
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                        @Override
                        public void onSuccess(QuerySnapshot qds) {
                            if (!qds.isEmpty()) {

                                cartItemList = new ArrayList<>();
                                showCartContent();

                                for (DocumentSnapshot ds : qds.getDocuments()) {
                                    CartItem cartItem = ds.toObject(CartItem.class);
                                    if (cartItem != null) {
                                        String documentID = ds.getId();
                                        cartItem.setDocumentID(documentID);

                                        cartItemList.add(cartItem);
                                    }
                                }

                                LinearLayoutManager manager = new LinearLayoutManager(getContext());
                                binding.cartRecyclerView.setLayoutManager(manager);

                                CartAdapter adapter = new CartAdapter(cartItemList);

                                updateTotalAmount();

                                adapter.setOnQuantityChangeListener(cartItem -> {
                                    String documentID = cartItem.getDocumentID();
                                    db.collection("users")
                                            .document(uid)
                                            .collection("cart")
                                            .document(documentID)
                                            .update("quantity", cartItem.getQuantity()).addOnSuccessListener(aVoid -> {
                                                updateTotalAmount();
                                            });
                                });

                                adapter.setOnCartItemRemovedListener(position -> {

                                    String documentID = cartItemList.get(position).getDocumentID();
                                    db.collection("users")
                                            .document(uid)
                                            .collection("cart")
                                            .document(documentID)
                                            .delete().addOnSuccessListener(aVoid -> {
                                                cartItemList.remove(position);
                                                adapter.notifyItemRemoved(position);
                                                adapter.notifyItemRangeChanged(position, cartItemList.size());
                                                updateTotalAmount();
                                                if (cartItemList.isEmpty()) {
                                                    showEmptyCart();
                                                }
                                            });
                                });

                                binding.cartBtnShopNow.setOnClickListener(v -> {
                                    if (getActivity() instanceof MainActivity) {
                                        ((MainActivity) getActivity()).navigateToHome();
                                    }
                                });

                                binding.cartRecyclerView.setAdapter(adapter);

                            } else {
                                showEmptyCart();
                            }
                        }
                    });

        }
    }

    private void showEmptyCart() {
        binding.emptyCartLayout.setVisibility(View.VISIBLE);
        binding.cartContentLayout.setVisibility(View.GONE);
    }

    private void showCartContent() {
        binding.emptyCartLayout.setVisibility(View.GONE);
        binding.cartContentLayout.setVisibility(View.VISIBLE);
    }

    private void updateTotalAmount() {
        NumberFormat formatter = NumberFormat.getNumberInstance();

        if (cartItemList == null || cartItemList.isEmpty()) {
            binding.totalAmount.setText("LKR 0.00");
            binding.totalProductCount.setText("0");
            binding.totalProductQuantity.setText("0");
            return;
        }

        List<String> productIDs = new ArrayList<>();
        cartItemList.forEach(cartItem -> productIDs.add(cartItem.getProductID()));

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("products")
                .whereIn("productID", productIDs)   // ← fix: was whereEqualTo (doesn't accept a List)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot qds) {

                        Map<String, Product> productMap = new HashMap<>();
                        qds.getDocuments().forEach(ds -> {
                            Product product = ds.toObject(Product.class);
                            if (product != null) {
                                productMap.put(product.getProductID(), product);
                            }
                        });

                        double total = 0;
                        int totalQuantity = 0;

                        for (CartItem cartItem : cartItemList) {
                            Product product = productMap.get(cartItem.getProductID());
                            if (product != null) {
                                total += product.getPrice() * cartItem.getQuantity();
                            }
                            totalQuantity += cartItem.getQuantity();
                        }

                        binding.totalAmount.setText("LKR " + formatter.format(total));
                        binding.totalProductCount.setText(String.valueOf(cartItemList.size()));
                        binding.totalProductQuantity.setText(String.valueOf(totalQuantity));
                    }
                });
    }

}