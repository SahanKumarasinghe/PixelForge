package com.sahan.app.pixelforge.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.sahan.app.pixelforge.R;
import com.sahan.app.pixelforge.activities.MainActivity;
import com.sahan.app.pixelforge.databinding.FragmentProfileBinding;
import com.sahan.app.pixelforge.models.User;

import java.util.UUID;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loadUserProfile();
        setupClickListeners();

        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            binding.profileImage.setOnClickListener(v -> {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                activityResultLauncher.launch(intent);
            });
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.findViewById(R.id.app_bar_home).setVisibility(View.GONE);

            BottomNavigationView bottomNav = mainActivity.findViewById(R.id.bottomNavView);
            if (bottomNav != null) bottomNav.getMenu().findItem(R.id.bottom_nav_profile).setChecked(true);

            NavigationView sideNav = mainActivity.findViewById(R.id.sideNavView);
            if (sideNav != null) sideNav.getMenu().findItem(R.id.app_bar_profile).setChecked(true);
        }
    }

    private ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    Glide.with(requireContext())
                            .load(uri)
                            .placeholder(R.drawable.empty_profile_img)
                            .into(binding.profileImage);

                    String imageID = UUID.randomUUID().toString().trim();

                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).updateNavHeaderImage(uri);
                    }

                    FirebaseStorage storage = FirebaseStorage.getInstance();
                    StorageReference imgReference = storage.getReference("profile-images").child(imageID);

                    imgReference.putFile(uri).addOnSuccessListener(taskSnapshot -> {
                        db.collection("users").document(auth.getUid()).update("profilePicUrl", imageID).addOnSuccessListener(Avoid -> {
                            Toast.makeText(getContext(), "Profile Image Changed Successfully", Toast.LENGTH_SHORT).show();
                        });
                    });
                }
            });

    private void loadUserProfile() {
        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {
            String uid = user.getUid();
            if (user.getMetadata() != null) {
                long creationTimestamp = user.getMetadata().getCreationTimestamp();
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.US);
                String formattedDate = sdf.format(new java.util.Date(creationTimestamp));
                binding.profileMemberSince.setText("Member since: " + formattedDate);
            }

            db.collection("users").document(uid).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {

                            String name = documentSnapshot.getString("name");
                            String email = documentSnapshot.getString("email");

                            binding.profileName.setText(name != null && !name.isEmpty() ? name : "PixelForge User");
                            binding.profileEmail.setText(email != null && !email.isEmpty() ? email : "No Email Provided");

                            User profileUser = documentSnapshot.toObject(User.class);
                            assert profileUser != null;

                            if (profileUser.getProfilePicUrl() != null && !profileUser.getProfilePicUrl().isEmpty()) {
                                FirebaseStorage storage = FirebaseStorage.getInstance();
                                storage.getReference("profile-images/" + profileUser.getProfilePicUrl()).getDownloadUrl()
                                        .addOnSuccessListener(uri -> {
                                            if (getContext() != null) {
                                                Glide.with(requireContext())
                                                        .load(uri)
                                                        .placeholder(R.drawable.empty_profile_img)
                                                        .into(binding.profileImage);
                                            }
                                            if (getActivity() instanceof MainActivity) {
                                                ((MainActivity) getActivity()).updateNavHeaderImage(uri);
                                            }
                                        });
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("ProfileFragment", "Failed to load user data", e);
                    });
            loadDatabaseCounts(uid);

        } else {
            Toast.makeText(getContext(), "Please log in to view profile", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadDatabaseCounts(String uid) {
        db.collection("users").document(uid).collection("cart").get()
                .addOnSuccessListener(qs -> {
                    int count = qs.size();
                    binding.profileCartItemCount.setText(String.valueOf(count));
                    binding.goToCartItemCount.setText(count + " items in cart");
                });
        db.collection("users").document(uid).collection("wishlist").get()
                .addOnSuccessListener(qs -> {
                    int count = qs.size();
                    binding.profileWishlistItemCount.setText(String.valueOf(count));
                    binding.gotoWishlistItemCount.setText(count + " saved items");
                });

        db.collection("orders").whereEqualTo("userId", uid).get()
                .addOnSuccessListener(qs -> {
                    int count = qs.size();
                    binding.profileOrderCount.setText(String.valueOf(count));
                });
    }

    private void setupClickListeners() {
        // 👇 Using our new helper method instead of triggering the MainActivity listener
        binding.rowGotoCart.setOnClickListener(v ->
                navigateWithBackStack(new CartFragment(), R.id.bottom_nav_cart, true)
        );

        binding.rowWishlist.setOnClickListener(v ->
                navigateWithBackStack(new WishlistFragment(), R.id.bottom_nav_wishlist, true)
        );

        binding.rowOrders.setOnClickListener(v ->
                navigateWithBackStack(new OrdersFragment(), R.id.app_bar_orders, false)
        );

        binding.rowSecurity.setOnClickListener(v ->
                navigateWithBackStack(new SettingsFragment(), R.id.app_bar_settings, false)
        );

        binding.btnSignOut.setOnClickListener(v -> {
            NavigationView sideNav = requireActivity().findViewById(R.id.sideNavView);
            if (sideNav != null) {
                sideNav.getMenu().performIdentifierAction(R.id.app_bar_logout, 0);
                Toast.makeText(getContext(), "Signed out successfully", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void navigateWithBackStack(Fragment fragment, int menuItemId, boolean isBottomNav) {
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.loadData(1000);
            mainActivity.findViewById(R.id.app_bar_home).setVisibility(View.VISIBLE);
            if (isBottomNav) {
                BottomNavigationView bottomNav = mainActivity.findViewById(R.id.bottomNavView);
                if (bottomNav != null) bottomNav.getMenu().findItem(menuItemId).setChecked(true);
            } else {
                NavigationView sideNav = mainActivity.findViewById(R.id.sideNavView);
                if (sideNav != null) sideNav.getMenu().findItem(menuItemId).setChecked(true);
            }
        }
        getParentFragmentManager().beginTransaction()
                .replace(R.id.containerView, fragment)
                .addToBackStack("profile")
                .commit();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}