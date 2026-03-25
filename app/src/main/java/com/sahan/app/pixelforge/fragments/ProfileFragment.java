package com.sahan.app.pixelforge.fragments;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.security.identity.AccessControlProfileId;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
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

    private ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result->{
                if (result.getResultCode() == Activity.RESULT_OK){
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
                        db.collection("users").document(auth.getUid()).update("profilePicUrl", imageID).addOnSuccessListener(Avoid->{
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
                            String profilePicUrl = documentSnapshot.getString("profilePicUrl");

                            binding.profileName.setText(name != null && !name.isEmpty() ? name : "PixelForge User");
                            binding.profileEmail.setText(email != null && !email.isEmpty() ? email : "No Email Provided");

                            User profileUser =  documentSnapshot.toObject(User.class);
                            assert profileUser != null;

                            FirebaseStorage storage = FirebaseStorage.getInstance();
                            storage.getReference("profile-images/"+profileUser.getProfilePicUrl()).getDownloadUrl()
                            .addOnSuccessListener(uri->{

                                Glide.with(requireContext())
                                        .load(uri)
                                        .placeholder(R.drawable.empty_profile_img)
                                        .into(binding.profileImage);

                                if (getActivity() instanceof MainActivity) {
                                    ((MainActivity) getActivity()).updateNavHeaderImage(uri);
                                }
                            });
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

        binding.rowGotoCart.setOnClickListener(v -> {
            BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottomNavView);
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(R.id.bottom_nav_cart);
            }
        });

        binding.rowWishlist.setOnClickListener(v -> {
            BottomNavigationView bottomNav = requireActivity().findViewById(R.id.bottomNavView);
            if (bottomNav != null) {
                bottomNav.setSelectedItemId(R.id.bottom_nav_wishlist);
            }
        });

        // Use the Side Navigation to route to Orders
        binding.rowOrders.setOnClickListener(v -> {
            NavigationView sideNav = requireActivity().findViewById(R.id.sideNavView);
            if (sideNav != null) {
                sideNav.getMenu().performIdentifierAction(R.id.app_bar_orders, 0);
            }
        });

        binding.rowSecurity.setOnClickListener(v -> {
            NavigationView sideNav = requireActivity().findViewById(R.id.sideNavView);
            if (sideNav != null) {
                sideNav.getMenu().performIdentifierAction(R.id.app_bar_settings, 0);
            }
        });
        binding.btnSignOut.setOnClickListener(v -> {
            NavigationView sideNav = requireActivity().findViewById(R.id.sideNavView);
            if (sideNav != null) {
                sideNav.getMenu().performIdentifierAction(R.id.app_bar_logout, 0);
                Toast.makeText(getContext(), "Signed out successfully", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}