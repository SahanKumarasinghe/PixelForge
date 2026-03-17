package com.sahan.app.pixelforge.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.sahan.app.pixelforge.R;
import com.sahan.app.pixelforge.adapters.ProductSliderAdapter;
import com.sahan.app.pixelforge.adapters.SectionAdapter;
import com.sahan.app.pixelforge.databinding.FragmentHomeBinding;
import com.sahan.app.pixelforge.models.Product;
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator;

import java.util.Arrays;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private Handler handler;
    private Runnable runnable;
    private boolean isAutoScrollEnabled = true;

    public HomeFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        loadHomepageCarousel();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        handler = new Handler(Looper.getMainLooper());
        loadHotDeals();
    }

    @Override
    public void onPause() {
        super.onPause();
        isAutoScrollEnabled = false;
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        isAutoScrollEnabled = true;
        if (handler != null && runnable != null) {
            handler.postDelayed(runnable, 4000);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
            runnable = null;
        }
        handler = null;
        binding = null;
    }

    private void loadHomepageCarousel() {

        List<String> images = Arrays.asList(
                "https://www.sakuraindex.jp/wp-content/uploads/2024/02/ROG-Strix-Laptops-14th-Gen-01-KV.jpg"
                , "https://cdn.wccftech.com/wp-content/uploads/2024/08/2024-08-20_19-52-54-728x410.png"
                , "https://www.asus.com/microsite/motherboard/upgrade-what-matters/v1/img/banner/main-Desktop.jpg"
                , "https://shorturl.at/tz2TP"
                , "https://cdn.custompc.com/wp-content/sites/custompc/2023/06/HallOfMirrors1.jpg"
                , "https://p2-ofp.static.pub//fes/cms/2026/01/04/9o7m7uqt8n1i21qw0441bnmn1v6rur314585.jpg"
        );

        binding.homeImageCarousel.setAdapter(new ProductSliderAdapter(images));
        binding.homeDotsIndicator.setViewPager2(binding.homeImageCarousel);
        if (runnable != null) {
            handler.removeCallbacks(runnable);
        }

        runnable = new Runnable() {
            @Override
            public void run() {
                if (!isAutoScrollEnabled || binding == null || images == null || images.isEmpty()) {
                    return;
                }
                int currentItem = binding.homeImageCarousel.getCurrentItem();
                int nextItem = (currentItem + 1) % images.size();

                if (images.size() > 1) {
                    binding.homeImageCarousel.setCurrentItem(nextItem, true);
                }
                if (isAutoScrollEnabled) {
                    handler.postDelayed(this, 4000);
                }
            }
        };
        if (isResumed()) {
            handler.postDelayed(runnable, 4000);
        }
    }

    private void loadHotDeals() {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("products")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot qds) {
                        if (!qds.isEmpty()) {
                            List<Product> products = qds.toObjects(Product.class);


                            LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
                            binding.homeTopSellingSection.itemSelectionContainer.setLayoutManager(layoutManager);

                            SectionAdapter adapter = new SectionAdapter(products, product -> {
                                Bundle bundle = new Bundle();
                                bundle.putString("productID", product.getProductID());

                                SingleProductFragment singleProductFragment = new SingleProductFragment();
                                singleProductFragment.setArguments(bundle);

                                getParentFragmentManager()
                                        .beginTransaction()
                                        .replace(R.id.containerView, singleProductFragment)
                                        .addToBackStack(null)
                                        .commit();
                            });

                            binding.homeTopSellingSection.itemSelectionContainer.setAdapter(adapter);

                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });
    }
}

