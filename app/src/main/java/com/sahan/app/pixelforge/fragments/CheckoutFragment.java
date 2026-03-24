package com.sahan.app.pixelforge.fragments;

import android.app.Activity;
import android.content.Intent;
import android.icu.text.NumberFormat;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.sahan.app.pixelforge.R;
import com.sahan.app.pixelforge.databinding.ActivityMainBinding;
import com.sahan.app.pixelforge.databinding.FragmentCartBinding;
import com.sahan.app.pixelforge.databinding.FragmentCheckoutBinding;
import com.sahan.app.pixelforge.listener.FireStoreCallBack;
import com.sahan.app.pixelforge.models.CartItem;
import com.sahan.app.pixelforge.models.Order;
import com.sahan.app.pixelforge.models.Product;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lk.payhere.androidsdk.PHConstants;
import lk.payhere.androidsdk.PHMainActivity;
import lk.payhere.androidsdk.PHResponse;
import lk.payhere.androidsdk.model.InitRequest;
import lk.payhere.androidsdk.model.StatusResponse;

public class CheckoutFragment extends Fragment {

    private FragmentCheckoutBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private double total;
    private boolean isPaymentActive;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigationView;
    private ActivityMainBinding homeBinding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.homeBinding = ActivityMainBinding.inflate(getLayoutInflater());
        this.navigationView = homeBinding.sideNavView;
        this.bottomNavigationView = homeBinding.bottomNavView;
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCheckoutBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    private void updateShipping(String type) {
        if (type.equals("standard")) {
            binding.orderShipping.setText("500.00");
        } else {
            binding.orderShipping.setText("2,500.00");
        }

        updateTotal();
    }

    private void updateTotal() {

        NumberFormat formatter = NumberFormat.getNumberInstance();
        int shipping = binding.rbExpress.isChecked() ? 2500 : 500;

        String subtotalText = binding.orderSubtotal.getText().toString().replace(",", "");
        double subtotal = subtotalText.isEmpty() ? 0 : Double.parseDouble(subtotalText);

        this.total = subtotal + shipping;
        this.isPaymentActive = true;

        binding.orderTotal.setText(String.format("LKR " + formatter.format(total) + ".00"));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getActivity().findViewById(R.id.bottomNavView).setVisibility(View.GONE);
        getCartItems(cartItems -> {

            ArrayList<String> productIDs = new ArrayList<>();
            cartItems.forEach(cartItem -> {
                productIDs.add(cartItem.getProductID());
            });

            getProductsByIds(productIDs, products -> {
                double subTotal = 0;
                for (CartItem cartItem : cartItems) {
                    Product product = products.get(cartItem.getProductID());
                    if (product != null) {
                        subTotal += product.getPrice() * cartItem.getQuantity();
                    }
                }

                binding.orderSubtotal.setText(String.format(Locale.US, "%.2f", subTotal));
                updateTotal();
            });
        });

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                getParentFragmentManager().popBackStack();
            }
        });

        boolean isChecked = binding.switchBillingSameAsShipping.isChecked();

        binding.cardBillingAddress.setVisibility(isChecked ? View.GONE : View.VISIBLE);
        setBillingFieldsEnabled(!isChecked);

        binding.switchBillingSameAsShipping.setOnCheckedChangeListener((buttonView, isChecked1) -> {

            if (isChecked1) {
                binding.cardBillingAddress.setVisibility(View.GONE);
                setBillingFieldsEnabled(false);

                binding.billingName.setText(binding.shippingName.getText().toString());
                binding.billingEmail.setText(binding.shippingEmail.getText().toString());
                binding.billingPhoneNumber.setText(binding.shippingPhoneNumber.getText().toString());
                binding.billingAddressLine1.setText(binding.shippingAddressLine1.getText().toString());
                binding.billingAddressLine2.setText(binding.shippingAddressLine2.getText().toString());
                binding.billingCity.setText(binding.shippingCity.getText().toString());
                binding.billingPostalCode.setText(binding.shippingPostalCode.getText().toString());

            } else {
                binding.cardBillingAddress.setVisibility(View.VISIBLE);
                setBillingFieldsEnabled(true);
            }
        });

        updateShipping("standard");

        binding.optionStandard.setOnClickListener(v -> {
            binding.rbStandard.setChecked(true);
            binding.rbExpress.setChecked(false);

            updateShipping("standard");
            updateTotal();
        });

        binding.optionExpress.setOnClickListener(v -> {
            binding.rbStandard.setChecked(false);
            binding.rbExpress.setChecked(true);

            updateShipping("express");
            updateTotal();
        });


        if (auth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = auth.getCurrentUser().getUid();


        binding.btnContinueToPayment.setOnClickListener(v -> {

            if (isPaymentActive) {

                String name = binding.shippingName.getText().toString();
                String email = binding.shippingEmail.getText().toString();
                String phone = binding.shippingPhoneNumber.getText().toString();
                String addressLine1 = binding.shippingAddressLine1.getText().toString();
                String addressLine2 = binding.shippingAddressLine2.getText().toString();
                String city = binding.shippingCity.getText().toString();
                String postalCode = binding.shippingPostalCode.getText().toString();

                if (name.isEmpty()) {
                    binding.shippingName.setError("Name is required");
                    binding.shippingName.requestFocus();
                    return;
                }

                if (name.length() < 3) {
                    binding.shippingName.setError("Name must be at least 3 characters");
                    binding.shippingName.requestFocus();
                    return;
                }

                if (email.isEmpty()) {
                    binding.shippingEmail.setError("Email is required");
                    binding.shippingEmail.requestFocus();
                    return;
                }

                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    binding.shippingEmail.setError("Enter a valid email");
                    binding.shippingEmail.requestFocus();
                    return;
                }

                if (phone.isEmpty()) {
                    binding.shippingPhoneNumber.setError("Phone number is required");
                    binding.shippingPhoneNumber.requestFocus();
                    return;
                }

                if (!phone.matches("^[0-9]{10}$")) {
                    binding.shippingPhoneNumber.setError("Enter valid 10-digit phone number");
                    binding.shippingPhoneNumber.requestFocus();
                    return;
                }

                if (addressLine1.isEmpty()) {
                    binding.shippingAddressLine1.setError("Address Line 1 is required");
                    binding.shippingAddressLine1.requestFocus();
                    return;
                }

                if (city.isEmpty()) {
                    binding.shippingCity.setError("City is required");
                    binding.shippingCity.requestFocus();
                    return;
                }

                if (postalCode.isEmpty()) {
                    binding.shippingPostalCode.setError("Postal code is required");
                    binding.shippingPostalCode.requestFocus();
                    return;
                }

                if (!postalCode.matches("^[0-9]{4,6}$")) {
                    binding.shippingPostalCode.setError("Invalid postal code");
                    binding.shippingPostalCode.requestFocus();
                    return;
                }
                if (addressLine2.length() > 0 && addressLine2.length() < 3) {
                    binding.shippingAddressLine2.setError("Too short");
                    binding.shippingAddressLine2.requestFocus();
                    return;
                }

                if (!binding.switchBillingSameAsShipping.isChecked()) {

                    String billing_name = binding.billingName.getText().toString();
                    String billing_email = binding.billingEmail.getText().toString();
                    String billing_phone = binding.billingPhoneNumber.getText().toString();
                    String billing_addressLine1 = binding.billingAddressLine1.getText().toString();
                    String billing_addressLine2 = binding.billingAddressLine2.getText().toString();
                    String billing_city = binding.billingCity.getText().toString();
                    String billing_postalCode = binding.billingPostalCode.getText().toString();

                    if (billing_name.isEmpty()) {
                        binding.billingName.setError("Name is required");
                        binding.billingName.requestFocus();
                        return;
                    }

                    if (billing_name.length() < 3) {
                        binding.billingName.setError("Name must be at least 3 characters");
                        binding.billingName.requestFocus();
                        return;
                    }

                    if (billing_email.isEmpty()) {
                        binding.billingEmail.setError("Email is required");
                        binding.billingEmail.requestFocus();
                        return;
                    }

                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(billing_email).matches()) {
                        binding.billingEmail.setError("Enter a valid email");
                        binding.billingEmail.requestFocus();
                        return;
                    }

                    if (billing_phone.isEmpty()) {
                        binding.billingPhoneNumber.setError("Phone number is required");
                        binding.billingPhoneNumber.requestFocus();
                        return;
                    }

                    if (!billing_phone.matches("^[0-9]{10}$")) {
                        binding.billingPhoneNumber.setError("Enter valid 10-digit phone number");
                        binding.billingPhoneNumber.requestFocus();
                        return;
                    }

                    if (billing_addressLine1.isEmpty()) {
                        binding.billingAddressLine1.setError("Address Line 1 is required");
                        binding.billingAddressLine1.requestFocus();
                        return;
                    }

                    if (billing_city.isEmpty()) {
                        binding.billingCity.setError("City is required");
                        binding.billingCity.requestFocus();
                        return;
                    }

                    if (billing_postalCode.isEmpty()) {
                        binding.billingPostalCode.setError("Postal code is required");
                        binding.billingPostalCode.requestFocus();
                        return;
                    }

                    if (!billing_postalCode.matches("^[0-9]{4,6}$")) {
                        binding.billingPostalCode.setError("Invalid postal code");
                        binding.billingPostalCode.requestFocus();
                        return;
                    }

                    if (billing_addressLine2.length() > 0 && billing_addressLine2.length() < 3) {
                        binding.billingAddressLine2.setError("Too short");
                        binding.billingAddressLine2.requestFocus();
                        return;
                    }
                }

                InitRequest req = new InitRequest();
                req.setSandBox(true);

                req.setMerchantId("1224973");
                req.setMerchantSecret("NDIzNTA0MTg4NTM4NDQ4NTE2NTYxNzcyMzQ4NzIzNzAzMTg2NTk2");
                req.setCurrency("LKR");

                req.setAmount(this.total);
                req.setOrderId(String.valueOf(System.currentTimeMillis()));
                req.setItemsDescription("");

                String[] parts = name.split(" ", 2);
                String firstName = parts[0];
                String lastName = (parts.length > 1) ? parts[1] : "";

                req.getCustomer().setFirstName(firstName);
                req.getCustomer().setLastName(lastName);
                req.getCustomer().setEmail(email);
                req.getCustomer().setPhone(phone);
                req.getCustomer().getAddress().setAddress(addressLine1 + " " + addressLine2);
                req.getCustomer().getAddress().setCity(city);
                req.getCustomer().getAddress().setCountry("Sri Lanka");

                req.setNotifyUrl("https://pixelforge.requestcatcher.com/");

                Intent intent = new Intent(getActivity(), PHMainActivity.class);
                intent.putExtra(PHConstants.INTENT_EXTRA_DATA, req);

                payhereLauncher.launch(intent);

            }
        });
    }

    private final ActivityResultLauncher<Intent> payhereLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {

        if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
            Intent data = result.getData();

            if (data.hasExtra(PHConstants.INTENT_EXTRA_RESULT)) {
                PHResponse<StatusResponse> response = (PHResponse<StatusResponse>) data.getSerializableExtra(PHConstants.INTENT_EXTRA_RESULT);

                if (response != null && response.isSuccess()) {
                    StatusResponse statusResponse = response.getData();

                    saveOrder(statusResponse);

                    Log.i("PAYHERE", "Payment Successful");
                } else {
                    Log.e("PAYHERE", "Payment Failed");
                }

            }

        } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
            Log.e("PAYHERE", "Payment Canceled");
        }

    });

    private void saveOrder(StatusResponse statusResponse) {
        getCartItems(cartItems -> {

            String uid = this.auth.getCurrentUser().getUid();

            Order order = new Order();
            order.setOrderId(String.valueOf(System.currentTimeMillis()));
            order.setUserId(uid);
            order.setTotalAmount(total);
            order.setStatus("Payment Completed");
            order.setOrderDate(Timestamp.now());

            String name = binding.shippingName.getText().toString();
            String email = binding.shippingEmail.getText().toString();
            String phone = binding.shippingPhoneNumber.getText().toString();
            String addressLine1 = binding.shippingAddressLine1.getText().toString();
            String addressLine2 = binding.shippingAddressLine2.getText().toString();
            String city = binding.shippingCity.getText().toString();
            String postalCode = binding.shippingPostalCode.getText().toString();

            if (name.isEmpty()) {
                binding.shippingName.setError("Name is required");
                binding.shippingName.requestFocus();
                return;
            }

            if (name.length() < 3) {
                binding.shippingName.setError("Name must be at least 3 characters");
                binding.shippingName.requestFocus();
                return;
            }

            if (email.isEmpty()) {
                binding.shippingEmail.setError("Email is required");
                binding.shippingEmail.requestFocus();
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.shippingEmail.setError("Enter a valid email");
                binding.shippingEmail.requestFocus();
                return;
            }

            if (phone.isEmpty()) {
                binding.shippingPhoneNumber.setError("Phone number is required");
                binding.shippingPhoneNumber.requestFocus();
                return;
            }

            if (!phone.matches("^[0-9]{10}$")) {
                binding.shippingPhoneNumber.setError("Enter valid 10-digit phone number");
                binding.shippingPhoneNumber.requestFocus();
                return;
            }

            if (addressLine1.isEmpty()) {
                binding.shippingAddressLine1.setError("Address Line 1 is required");
                binding.shippingAddressLine1.requestFocus();
                return;
            }

            if (city.isEmpty()) {
                binding.shippingCity.setError("City is required");
                binding.shippingCity.requestFocus();
                return;
            }

            if (postalCode.isEmpty()) {
                binding.shippingPostalCode.setError("Postal code is required");
                binding.shippingPostalCode.requestFocus();
                return;
            }

            if (!postalCode.matches("^[0-9]{4,6}$")) {
                binding.shippingPostalCode.setError("Invalid postal code");
                binding.shippingPostalCode.requestFocus();
                return;
            }
            if (addressLine2.length() > 0 && addressLine2.length() < 3) {
                binding.shippingAddressLine2.setError("Too short");
                binding.shippingAddressLine2.requestFocus();
                return;
            }

            Order.Address shippingAddress = Order.Address.builder()
                    .name(name)
                    .email(email)
                    .phoneNumber(phone)
                    .addressLine1(addressLine1)
                    .addressLine2(addressLine2)
                    .city(city)
                    .postCode(postalCode)
                    .build();

            order.setShippingAddress(shippingAddress);

            if (!binding.switchBillingSameAsShipping.isChecked()) {

                String billing_name = binding.billingName.getText().toString();
                String billing_email = binding.billingEmail.getText().toString();
                String billing_phone = binding.billingPhoneNumber.getText().toString();
                String billing_addressLine1 = binding.billingAddressLine1.getText().toString();
                String billing_addressLine2 = binding.billingAddressLine2.getText().toString();
                String billing_city = binding.billingCity.getText().toString();
                String billing_postalCode = binding.billingPostalCode.getText().toString();

                if (billing_name.isEmpty()) {
                    binding.billingName.setError("Name is required");
                    binding.billingName.requestFocus();
                    return;
                }

                if (billing_name.length() < 3) {
                    binding.billingName.setError("Name must be at least 3 characters");
                    binding.billingName.requestFocus();
                    return;
                }

                if (billing_email.isEmpty()) {
                    binding.billingEmail.setError("Email is required");
                    binding.billingEmail.requestFocus();
                    return;
                }

                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(billing_email).matches()) {
                    binding.billingEmail.setError("Enter a valid email");
                    binding.billingEmail.requestFocus();
                    return;
                }

                if (billing_phone.isEmpty()) {
                    binding.billingPhoneNumber.setError("Phone number is required");
                    binding.billingPhoneNumber.requestFocus();
                    return;
                }

                if (!billing_phone.matches("^[0-9]{10}$")) {
                    binding.billingPhoneNumber.setError("Enter valid 10-digit phone number");
                    binding.billingPhoneNumber.requestFocus();
                    return;
                }

                if (billing_addressLine1.isEmpty()) {
                    binding.billingAddressLine1.setError("Address Line 1 is required");
                    binding.billingAddressLine1.requestFocus();
                    return;
                }

                if (billing_city.isEmpty()) {
                    binding.billingCity.setError("City is required");
                    binding.billingCity.requestFocus();
                    return;
                }

                if (billing_postalCode.isEmpty()) {
                    binding.billingPostalCode.setError("Postal code is required");
                    binding.billingPostalCode.requestFocus();
                    return;
                }

                if (!billing_postalCode.matches("^[0-9]{4,6}$")) {
                    binding.billingPostalCode.setError("Invalid postal code");
                    binding.billingPostalCode.requestFocus();
                    return;
                }

                if (billing_addressLine2.length() > 0 && billing_addressLine2.length() < 3) {
                    binding.billingAddressLine2.setError("Too short");
                    binding.billingAddressLine2.requestFocus();
                    return;
                }

                Order.Address billingAddress = Order.Address.builder()
                        .name(billing_name)
                        .email(billing_email)
                        .phoneNumber(billing_phone)
                        .addressLine1(billing_addressLine1)
                        .addressLine2(billing_addressLine2)
                        .city(billing_city)
                        .postCode(billing_postalCode)
                        .build();

                order.setBillingAddress(billingAddress);

            } else {
                order.setBillingAddress(shippingAddress);
            }

            List<Order.OrderItem> orderItems = new ArrayList<>();
            ArrayList<String> productIDs = new ArrayList<>();
            cartItems.forEach(cartItem -> {
                productIDs.add(cartItem.getProductID());
            });

            getProductsByIds(productIDs, data -> {

                for (CartItem cartItem : cartItems) {

                    Product product = data.get(cartItem.getProductID());

                    if (product != null) {
                        List<Order.OrderItem.Attribute> attributeList = new ArrayList<>();

                        for (CartItem.Attribute att : cartItem.getAttributes()) {
                            Order.OrderItem.Attribute attribute = Order.OrderItem.Attribute.builder()
                                    .name(att.getName())
                                    .value(att.getValue())
                                    .build();

                            attributeList.add(attribute);

                        }

                        Order.OrderItem orderItem = Order.OrderItem.builder()
                                .productId(cartItem.getProductID())
                                .quantity(cartItem.getQuantity())
                                .unitPrice(product.getPrice())
                                .attributes(attributeList)
                                .build();

                        orderItems.add(orderItem);


                        order.setOrderItems(orderItems);
                    }
                }

                db.collection("orders").document().set(order).addOnSuccessListener(AVoid -> {

                    Toast.makeText(getContext(), "Order placed successfully", Toast.LENGTH_SHORT).show();

                    db.collection("users")
                            .document(uid)
                            .collection("cart")
                            .get()
                            .addOnSuccessListener(qs -> {
                                qs.getDocuments().forEach(ds -> {
                                    ds.getReference().delete();
                                });
                            });
                    getParentFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    navigationView.setCheckedItem(R.id.app_bar_home);
                    bottomNavigationView.setSelectedItemId(R.id.bottom_nav_home);

                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.containerView, new HomeFragment())
                            .commit();

                }).addOnFailureListener(e -> {
                    android.util.Log.e("FirestoreError", "Failed to create order", e);
                    Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            });
        });
    }


    private void setBillingFieldsEnabled(boolean enabled) {
        binding.billingName.setEnabled(enabled);
        binding.billingEmail.setEnabled(enabled);
        binding.billingPhoneNumber.setEnabled(enabled);
        binding.billingAddressLine1.setEnabled(enabled);
        binding.billingAddressLine2.setEnabled(enabled);
        binding.billingCity.setEnabled(enabled);
        binding.billingPostalCode.setEnabled(enabled);
    }

    private void getCartItems(FireStoreCallBack<List<CartItem>> callBack) {
        String uid = auth.getCurrentUser().getUid();

        db.collection("users").document(uid).collection("cart").get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot qds) {
                if (!qds.isEmpty()) {
                    List<CartItem> cartItems = qds.toObjects(CartItem.class);
                    callBack.onCallBack(cartItems);

                }
            }
        });
    }

    private void getProductsByIds(List<String> ids, FireStoreCallBack<Map<String, Product>> callBack) {
        Map<String, Product> products = new HashMap<>();

        if (ids == null || ids.isEmpty()) {
            callBack.onCallBack(products);
            return;
        }


        db.collection("products")
                .whereIn("productID", ids)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot qds) {
                        qds.getDocuments().forEach(ds -> {
                            Product product = ds.toObject(Product.class);
                            if (product != null) {
                                products.put(product.getProductID(), product);
                            }
                        });

                        callBack.onCallBack(products);
                    }
                });
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