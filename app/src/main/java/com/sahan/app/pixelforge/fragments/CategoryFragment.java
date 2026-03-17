package com.sahan.app.pixelforge.fragments;

import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.sahan.app.pixelforge.R;
import com.sahan.app.pixelforge.activities.MainActivity;
import com.sahan.app.pixelforge.adapters.CategoryAdapter;
import com.sahan.app.pixelforge.databinding.FragmentCategoryBinding;
import com.sahan.app.pixelforge.models.Category;

import org.checkerframework.checker.units.qual.C;

import java.util.List;

public class CategoryFragment extends Fragment {

    private static final String TAG = "CategoryFragment";
    private FragmentCategoryBinding binding;
    private CategoryAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCategoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.categoryRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        FirebaseFirestore fireBaseDb = FirebaseFirestore.getInstance();
        fireBaseDb.collection("categories")
                .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        QuerySnapshot result = task.getResult();
                        List<Category> categoryList = result.toObjects(Category.class);
                        adapter = new CategoryAdapter(categoryList, category -> {

                            Bundle bundle = new Bundle();
                            bundle.putString("categoryId", category.getCatId());

                            ListingFragment fragment = new ListingFragment();
                            fragment.setArguments(bundle);

                            requireActivity().getSupportFragmentManager()
                                    .beginTransaction()
                                    .replace(R.id.containerView, fragment)  // This containerView is in MainActivity's layout
                                    .addToBackStack(null)  // Optional: add to backstack so user can go back to HomeFragment
                                    .commit();

                        });
                        binding.categoryRecyclerView.setAdapter(adapter);
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, e.toString().trim());
                    }
                });

        getActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {

            }
        });

    }
}