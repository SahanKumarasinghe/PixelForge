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

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.sahan.app.pixelforge.R;
import com.sahan.app.pixelforge.adapters.HomeProductAdapter;
import com.sahan.app.pixelforge.adapters.ListingAdapter;
import com.sahan.app.pixelforge.databinding.FragmentListingBinding;
import com.sahan.app.pixelforge.databinding.FragmentProductHomeBinding;
import com.sahan.app.pixelforge.models.Product;

import java.util.List;

public class HomeProductFragment extends Fragment {

    private FragmentProductHomeBinding binding;
    private HomeProductAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.binding = FragmentProductHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.homeProductRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2)); //  2 means how many items it will show per line
        FirebaseFirestore firestoreDb = FirebaseFirestore.getInstance();

        firestoreDb.collection("products")
                .orderBy("title", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(ds -> {
                    if (!ds.isEmpty()) {
                        List<Product> products = ds.toObjects(Product.class);
                        adapter = new HomeProductAdapter(products, product -> {

                            Bundle bundle = new Bundle();
                            bundle.putString("productID", product.getProductID());

                            SingleProductFragment productDetailsFragment = new SingleProductFragment();
                            productDetailsFragment.setArguments(bundle);

                            requireActivity().getSupportFragmentManager()
                                    .beginTransaction()
                                    .replace(R.id.containerView, productDetailsFragment)
                                    .addToBackStack(null)
                                    .commit();
                        });

                        binding.homeProductRecyclerView.setAdapter(adapter);
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