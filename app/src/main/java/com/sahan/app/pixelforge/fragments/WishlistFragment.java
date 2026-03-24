package com.sahan.app.pixelforge.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.sahan.app.pixelforge.activities.MainActivity;
import com.sahan.app.pixelforge.adapters.WishlistAdapter;
import com.sahan.app.pixelforge.databinding.FragmentWishlistBinding;
import com.sahan.app.pixelforge.models.WishlistItem;
import com.sahan.app.pixelforge.models.WishlistManager;

import java.util.ArrayList;
import java.util.List;

public class WishlistFragment extends Fragment {

    private FragmentWishlistBinding binding;
    private List<WishlistItem> wishlistItemList;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentWishlistBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadWishlist();

        binding.wishlistBtnShopNow.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToHome();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadWishlist();
    }

    private void loadWishlist() {
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

        if (firebaseAuth.getCurrentUser() != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            String uid = firebaseAuth.getCurrentUser().getUid();

            db.collection("users").document(uid).collection("wishlist").get()
                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                        @Override
                        public void onSuccess(QuerySnapshot qds) {
                            if (!qds.isEmpty()) {
                                wishlistItemList = new ArrayList<>();
                                showContent();

                                for (DocumentSnapshot ds : qds.getDocuments()) {
                                    WishlistItem item = ds.toObject(WishlistItem.class);
                                    if (item != null) {
                                        item.setDocumentID(ds.getId());
                                        wishlistItemList.add(item);
                                    }
                                }

                                setupRecyclerView();
                            } else {
                                showEmpty();
                            }
                        }
                    });
        } else {
            android.content.SharedPreferences prefs = getContext().getSharedPreferences("PixelForgeGuestData", android.content.Context.MODE_PRIVATE);
            java.util.Set<String> guestWishlist = prefs.getStringSet("GuestWishlist", new java.util.HashSet<>());

            if (!guestWishlist.isEmpty()) {
                wishlistItemList = new ArrayList<>();
                showContent();

                for (String productId : guestWishlist) {
                    WishlistItem item = new WishlistItem();
                    item.setProductID(productId);
                    wishlistItemList.add(item);
                }

                setupRecyclerView();
            } else {
                showEmpty();
            }
        }
    }

    private void setupRecyclerView() {
        binding.wishlistRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        WishlistAdapter adapter = new WishlistAdapter(wishlistItemList);

        adapter.setOnWishlistItemRemovedListener(position -> {
            String productId = wishlistItemList.get(position).getProductID();
            WishlistManager.removeFromWishlist(getContext(), productId);

            wishlistItemList.remove(position);
            adapter.notifyItemRemoved(position);
            adapter.notifyItemRangeChanged(position, wishlistItemList.size());

            if (wishlistItemList.isEmpty()) {
                showEmpty();
            }
        });

        binding.wishlistRecyclerView.setAdapter(adapter);
    }

    private void showEmpty() {
        binding.emptyWishlistLayout.setVisibility(View.VISIBLE);
        binding.wishlistContentLayout.setVisibility(View.GONE);
    }

    private void showContent() {
        binding.emptyWishlistLayout.setVisibility(View.GONE);
        binding.wishlistContentLayout.setVisibility(View.VISIBLE);
    }
}