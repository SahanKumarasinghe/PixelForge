package com.sahan.app.pixelforge.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.sahan.app.pixelforge.R;
import com.sahan.app.pixelforge.adapters.OrderItemsAdapter;
import com.sahan.app.pixelforge.databinding.FragmentOrdersBinding;
import com.sahan.app.pixelforge.models.Order;
import com.sahan.app.pixelforge.adapters.OrdersAdapter;

import java.util.ArrayList;
import java.util.List;

public class OrdersFragment extends Fragment {

    private FragmentOrdersBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private List<Order> orderList;
    private ListenerRegistration orderListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentOrdersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loadUserOrders();
    }

    private void loadUserOrders() {
        FirebaseUser user = auth.getCurrentUser();

        if (user == null) {
            showEmptyState();
            return;
        }
        String uid = user.getUid();

        this.orderListener = db.collection("orders")
                .whereEqualTo("userId", uid)
                .addSnapshotListener((value, error) -> {

                    if (error != null) {
                        android.util.Log.e("OrdersFragment", "Listen failed.", error);
                        return;
                    }

                    if (value != null && !value.isEmpty()) {
                        orderList = new ArrayList<>();
                        showOrdersContent();

                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Order order = doc.toObject(Order.class);
                            if (order != null) {
                                orderList.add(order);
                            }
                        }
                        orderList.sort((o1, o2) -> {
                            if (o1.getOrderDate() == null || o2.getOrderDate() == null) return 0;
                            return o2.getOrderDate().compareTo(o1.getOrderDate()); // Descending
                        });

                        setupRecyclerView();

                    } else {
                        showEmptyState();
                    }
                });
    }

    private void setupRecyclerView() {
        binding.ordersRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        OrdersAdapter adapter = new OrdersAdapter(orderList);

        adapter.setOnOrderClickListener(this::showOrderItemsPopup);

        binding.ordersRecyclerView.setAdapter(adapter);
    }

    private void showEmptyState() {
        binding.emptyOrdersLayout.setVisibility(View.VISIBLE);
        binding.ordersContentLayout.setVisibility(View.GONE);
    }

    private void showOrdersContent() {
        binding.emptyOrdersLayout.setVisibility(View.GONE);
        binding.ordersContentLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (orderListener != null) {
            orderListener.remove();
        }
        binding = null;
    }

    private void showOrderItemsPopup(Order order) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_order_items, null);
        bottomSheetDialog.setContentView(dialogView);

        TextView title = dialogView.findViewById(R.id.popup_order_title);
        androidx.recyclerview.widget.RecyclerView recyclerView = dialogView.findViewById(R.id.popup_recycler_view);
        com.google.android.material.button.MaterialButton btnClose = dialogView.findViewById(R.id.popup_btn_close);

        title.setText("Items for Order #" + order.getOrderId());

        recyclerView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(getContext()));

        OrderItemsAdapter adapter = new OrderItemsAdapter(order.getOrderItems());
        recyclerView.setAdapter(adapter);

        btnClose.setOnClickListener(v -> bottomSheetDialog.dismiss());
        bottomSheetDialog.show();
    }
}