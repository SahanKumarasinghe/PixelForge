package com.sahan.app.pixelforge.fragments;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.icu.text.NumberFormat;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.renderscript.ScriptGroup;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.sahan.app.pixelforge.R;
import com.sahan.app.pixelforge.activities.LoginActivity;
import com.sahan.app.pixelforge.activities.MainActivity;
import com.sahan.app.pixelforge.adapters.ProductSliderAdapter;
import com.sahan.app.pixelforge.adapters.SectionAdapter;
import com.sahan.app.pixelforge.databinding.FragmentSingleProductBinding;
import com.sahan.app.pixelforge.models.CartItem;
import com.sahan.app.pixelforge.models.Product;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SingleProductFragment extends Fragment {
    private String productID;
    private String catID;
    private FragmentSingleProductBinding binding;
    private int buyingQty = 1;
    private double price;
    private int availableQty;
    private Map<String, ChipGroup> attributeGroups = new HashMap<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            this.productID = getArguments().getString("productID");
            this.catID = getArguments().getString("catID");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.binding = FragmentSingleProductBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getActivity().findViewById(R.id.bottomNavView).setVisibility(View.GONE);
        getActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });

        FirebaseFirestore firestoreDB = FirebaseFirestore.getInstance();

        firestoreDB.collection("products")
                .whereEqualTo("productID", productID)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot qs) {
                        if (!qs.isEmpty()) {
                            Product product = qs.getDocuments().get(0).toObject(Product.class);
                            assert product != null;
                            ProductSliderAdapter adapter = new ProductSliderAdapter(product.getProduct_images());
                            binding.productImageSlider.setAdapter(adapter);

                            binding.dotsIndicator.attachTo(binding.productImageSlider);

                            binding.productDetailsTitle.setText(product.getTitle());
                            binding.productDetailsDescription.setText(product.getDescription());
                            NumberFormat formatter = NumberFormat.getNumberInstance();
                            binding.productDetailsPrice.setText("LKR " + formatter.format(product.getPrice()));
                            binding.productDetailsRatingNumber.setText(String.format("%.1f", (float) product.getRating()));
                            binding.productDetailsRating.setRating(product.getRating());
                            binding.productDetailsStockCount.setText(String.valueOf(product.getStockCount()));

                            availableQty = product.getStockCount();

                            if (product.getAttributes() != null) {

                                product.getAttributes().forEach(attribute -> {
                                    renderAttributes(attribute, binding.productDetailsAttributesContainer);
                                });

                            }

                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("ProductDetailsFragment", e.toString());
                    }
                });

        binding.productDetailsQtyMinusBtn.setOnClickListener(v -> {
            if (buyingQty > 1) {
                buyingQty--;
                binding.productDetailsBuyingQty.setText(String.valueOf(buyingQty));
            }
        });

        binding.productDetailsQtyPlusBtn.setOnClickListener(v -> {
            if (buyingQty < availableQty) {
                buyingQty++;
                binding.productDetailsBuyingQty.setText(String.valueOf(buyingQty));
            }
        });


        loadRecommendedProducts();

        binding.productDetailsAddToCartBtn.setOnClickListener(v -> {

            FirebaseAuth auth = FirebaseAuth.getInstance();

            if (auth.getCurrentUser() == null) {
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                startActivity(intent);
            } else {

                List<CartItem.Attribute> attributeList = getFinalSelection();

                CartItem cartItem = new CartItem(productID, buyingQty, attributeList);

                String uid = auth.getCurrentUser().getUid();

                if (attributeList.size() < attributeGroups.size()){
                    Toast.makeText(getContext(), "Please Choose Attributes for the product", Toast.LENGTH_SHORT).show();
                }else{
                    firestoreDB.collection("users")
                            .document(uid)
                            .collection("cart")
                            .document()
                            .set(cartItem)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    Toast.makeText(getContext(), "Added to cart", Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            }
        });

    }

    private void loadRecommendedProducts() {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (catID == null) {
            db.collection("products")
                    .whereEqualTo("productID", productID)
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                        @Override
                        public void onSuccess(QuerySnapshot qs) {
                            catID = qs.getDocuments().get(0).getString("catID");
                            db.collection("products")
                                    .whereEqualTo("catID", catID)
                                    .whereNotEqualTo("productID", productID)
                                    .orderBy("productID")
                                    .get()
                                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                                        @Override
                                        public void onSuccess(QuerySnapshot qds) {
                                            if (!qds.isEmpty()) {
                                                List<Product> products = qds.toObjects(Product.class);

                                                LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
                                                binding.productDetailsTopSellingSection.itemSelectionContainer.setLayoutManager(layoutManager);

                                                SectionAdapter adapter = new SectionAdapter(products, product -> {
                                                    Bundle bundle = new Bundle();
                                                    bundle.putString("productID", product.getProductID());

                                                    SingleProductFragment productDetailsFragment = new SingleProductFragment();
                                                    productDetailsFragment.setArguments(bundle);

                                                    getParentFragmentManager()
                                                            .beginTransaction()
                                                            .replace(R.id.containerView, productDetailsFragment)
                                                            .addToBackStack(null)
                                                            .commit();
                                                });

                                                binding.productDetailsTopSellingSection.itemSelectionContainer.setAdapter(adapter);

                                            }
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {

                                        }
                                    });

                        }
                    });
        } else {
            db.collection("products")
                    .whereEqualTo("catID", catID)
                    .whereNotEqualTo("productID", productID)
                    .orderBy("productID")
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                        @Override
                        public void onSuccess(QuerySnapshot qds) {
                            if (!qds.isEmpty()) {
                                List<Product> products = qds.toObjects(Product.class);

                                LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
                                binding.productDetailsTopSellingSection.itemSelectionContainer.setLayoutManager(layoutManager);

                                SectionAdapter adapter = new SectionAdapter(products, product -> {
                                    Bundle bundle = new Bundle();
                                    bundle.putString("productID", product.getProductID());

                                    SingleProductFragment productDetailsFragment = new SingleProductFragment();
                                    productDetailsFragment.setArguments(bundle);

                                    getParentFragmentManager()
                                            .beginTransaction()
                                            .replace(R.id.containerView, productDetailsFragment)
                                            .addToBackStack(null)
                                            .commit();
                                });

                                binding.productDetailsTopSellingSection.itemSelectionContainer.setAdapter(adapter);

                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {

                        }
                    });
        }
    }

    private void renderAttributes(Product.Attribute attribute, ViewGroup container) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);

        TextView label = new TextView(getContext());
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        layoutParams.gravity = Gravity.CENTER_VERTICAL;
        label.setLayoutParams(layoutParams);

        label.setText(attribute.getName() + " : ");

        row.addView(label);

        ChipGroup group = new ChipGroup(getContext());
        group.setSelectionRequired(true);
        group.setSingleSelection(true);

        attribute.getValues().forEach(value -> {
            Chip chip = new Chip(getContext());
            chip.setCheckable(true);
            chip.setChipStrokeWidth(3f);
            chip.setTag(value);

            if ("color".equals(attribute.getType())) {
                chip.setChipBackgroundColor(ColorStateList.valueOf(Color.parseColor(value)));
            } else {
                chip.setText(value);
            }

            group.addView(chip);

        });

        row.addView(group);
        container.addView(row);

        attributeGroups.put(attribute.getName(), group);
    }

    private List<CartItem.Attribute> getFinalSelection() {

        List<CartItem.Attribute> attributes = new ArrayList<>();

        for (Map.Entry<String, ChipGroup> entry : attributeGroups.entrySet()) {
            String attributeName = entry.getKey();
            ChipGroup chipGroup = entry.getValue();

            chipGroup.setSelectionRequired(true);

            int checkedChipID = chipGroup.getCheckedChipId();

            if (checkedChipID != -1) {
                Chip chip = getView().findViewById(checkedChipID);
                String value = chip.getTag().toString();

                attributes.add(new CartItem.Attribute(attributeName, value));
            }
        }
        return attributes;
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().findViewById(R.id.bottomNavView).setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().findViewById(R.id.bottomNavView).setVisibility(View.GONE);
    }
}