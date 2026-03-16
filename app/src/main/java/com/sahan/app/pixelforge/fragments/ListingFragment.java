package com.sahan.app.pixelforge.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.sahan.app.pixelforge.R;
import com.sahan.app.pixelforge.adapters.ListingAdapter;
import com.sahan.app.pixelforge.databinding.FragmentListingBinding;
import com.sahan.app.pixelforge.models.Product;

import java.util.ArrayList;
import java.util.List;

public class ListingFragment extends Fragment {

    private FragmentListingBinding binding;
    private ListingAdapter adapter;
    private String categoryId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            this.categoryId = getArguments().getString("categoryId");

        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.binding = FragmentListingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.listingRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2)); //  2 means how many items it will show per line
        FirebaseFirestore firestoreDb = FirebaseFirestore.getInstance();

        firestoreDb.collection("products")
                .whereEqualTo("catID", categoryId)
                .orderBy("title", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(ds -> {
                    if (!ds.isEmpty()) {
                        List<Product> products = ds.toObjects(Product.class);
                        adapter = new ListingAdapter(products, product -> {

                            Bundle bundle = new Bundle();
                            bundle.putString("productID", product.getProductID());
                            bundle.putString("catID", categoryId);

                            SingleProductFragment productDetailsFragment = new SingleProductFragment();
                            productDetailsFragment.setArguments(bundle);

                            getParentFragmentManager()
                                    .beginTransaction()
                                    .replace(R.id.containerView, productDetailsFragment)
                                    .addToBackStack(null)
                                    .commit();
                        });

                        binding.listingRecyclerView.setAdapter(adapter);
                    }
                });

        getActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });

    }
}