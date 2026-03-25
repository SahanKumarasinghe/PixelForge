package com.sahan.app.pixelforge.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class WishlistManager {

    private static final String PREFS_NAME = "PixelForgeGuestData";
    private static final String WISHLIST_KEY = "GuestWishlist";

    public static void addToWishlist(Context context, String productId) {
        FirebaseAuth auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            String uid = auth.getCurrentUser().getUid();

            Map<String, Object> wishlistItem = new HashMap<>();
            wishlistItem.put("productID", productId);
            wishlistItem.put("addedAt", com.google.firebase.Timestamp.now());

            db.collection("users").document(uid).collection("wishlist")
                    .document(productId).set(wishlistItem)
                    .addOnSuccessListener(aVoid -> Toast.makeText(context, "Added to Wishlist", Toast.LENGTH_SHORT).show());
        } else {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            Set<String> guestWishlist = new HashSet<>(prefs.getStringSet(WISHLIST_KEY, new HashSet<>()));

            guestWishlist.add(productId);
            prefs.edit().putStringSet(WISHLIST_KEY, guestWishlist).apply();

            Toast.makeText(context, "Saved locally! Log in to sync.", Toast.LENGTH_SHORT).show();
        }
    }

    public static void removeFromWishlist(Context context, String productId) {
        FirebaseAuth auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            String uid = auth.getCurrentUser().getUid();
            db.collection("users").document(uid).collection("wishlist").document(productId).delete();
        } else {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            Set<String> guestWishlist = new HashSet<>(prefs.getStringSet(WISHLIST_KEY, new HashSet<>()));

            guestWishlist.remove(productId);
            prefs.edit().putStringSet(WISHLIST_KEY, guestWishlist).apply();
        }
    }

    public interface WishlistCheckCallback {
        void onResult(boolean isInWishlist);
    }

    public static void checkIfInWishlist(Context context, String productId, WishlistCheckCallback callback) {
        FirebaseAuth auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            String uid = auth.getCurrentUser().getUid();
            db.collection("users").document(uid).collection("wishlist").document(productId).get()
                    .addOnSuccessListener(documentSnapshot -> callback.onResult(documentSnapshot.exists()))
                    .addOnFailureListener(e -> callback.onResult(false));
        } else {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            Set<String> guestWishlist = prefs.getStringSet(WISHLIST_KEY, new HashSet<>());
            callback.onResult(guestWishlist.contains(productId));
        }
    }

    public static void mergeLocalWishlistToFirebase(Context context) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return; // Failsafe

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> guestWishlist = prefs.getStringSet(WISHLIST_KEY, new HashSet<>());

        if (!guestWishlist.isEmpty()) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            String uid = auth.getCurrentUser().getUid();

            for (String productId : guestWishlist) {
                Map<String, Object> item = new HashMap<>();
                item.put("productID", productId);
                item.put("addedAt", com.google.firebase.Timestamp.now());
                db.collection("users").document(uid).collection("wishlist").document(productId).set(item);
            }

            prefs.edit().remove(WISHLIST_KEY).apply();
        }
    }
}