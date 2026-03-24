package com.sahan.app.pixelforge.fragments;

import android.app.Activity;
import android.content.Intent;
import android.icu.text.NumberFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sahan.app.pixelforge.R;
import com.sahan.app.pixelforge.databinding.FragmentCheckoutBinding;
import com.sahan.app.pixelforge.models.Order;
import com.sahan.app.pixelforge.models.Product;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import lk.payhere.androidsdk.PHConstants;
import lk.payhere.androidsdk.PHMainActivity;
import lk.payhere.androidsdk.PHResponse;
import lk.payhere.androidsdk.model.InitRequest;
import lk.payhere.androidsdk.model.StatusResponse;

public class BuyNowCheckoutFragment extends Fragment {

    private FragmentCheckoutBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String productId;
    private int quantity;
    private List<Order.OrderItem.Attribute> selectedAttributes;
    private double total;
    private List<Order.OrderItem> currentOrderItems;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            this.productId = getArguments().getString("productId");
            this.quantity = getArguments().getInt("quantity");
            this.selectedAttributes = (ArrayList<Order.OrderItem.Attribute>) getArguments().getSerializable("attributes");
        }
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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

        binding.orderTotal.setText(String.format("LKR " + formatter.format(total) + ".00"));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getActivity().findViewById(R.id.bottomNavView).setVisibility(View.GONE);

        if (productId != null && quantity > 0) {
            // Buy Now flow: load the product
            db.collection("products")
                    .whereEqualTo("productID", productId)
                    .get()
                    .addOnSuccessListener(qs -> {
                        if (!qs.isEmpty()) {
                            Product product = qs.getDocuments().get(0).toObject(Product.class);
                            if (product != null) {
                                double subtotal = product.getPrice() * quantity;
                                binding.orderSubtotal.setText(String.format(Locale.US, "%.2f", subtotal));
                                updateTotal();

                                List<Order.OrderItem.Attribute> attributes = selectedAttributes != null ? selectedAttributes : new ArrayList<>();
                                currentOrderItems = new ArrayList<>();
                                currentOrderItems.add(Order.OrderItem.builder()
                                        .productId(product.getProductID())
                                        .quantity(quantity)
                                        .unitPrice(product.getPrice())
                                        .attributes(attributes)
                                        .build());
                            }
                        }
                    });
        }

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
        });

        binding.optionExpress.setOnClickListener(v -> {
            binding.rbStandard.setChecked(false);
            binding.rbExpress.setChecked(true);
            updateShipping("express");
        });

        if (auth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnContinueToPayment.setOnClickListener(v -> {
            if (currentOrderItems == null || currentOrderItems.isEmpty()) {
                Toast.makeText(getContext(), "Loading product data, please wait...", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isFormValid()) {
                return;
            }

            String name = binding.shippingName.getText().toString();
            String email = binding.shippingEmail.getText().toString();
            String phone = binding.shippingPhoneNumber.getText().toString();
            String addressLine1 = binding.shippingAddressLine1.getText().toString();
            String addressLine2 = binding.shippingAddressLine2.getText().toString();
            String city = binding.shippingCity.getText().toString();

            InitRequest req = new InitRequest();
            req.setSandBox(true);
            req.setMerchantId("1224973");
            req.setMerchantSecret("NDIzNTA0MTg4NTM4NDQ4NTE2NTYxNzcyMzQ4NzIzNzAzMTg2NTk2");
            req.setCurrency("LKR");
            req.setAmount(this.total);
            req.setOrderId(String.valueOf(System.currentTimeMillis()));
            req.setItemsDescription("PixelForge Buy Now Order");

            String[] parts = name.trim().split(" ", 2);
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
        });
    }

    private final ActivityResultLauncher<Intent> payhereLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            Intent data = result.getData();

            // Corrected to look for INTENT_EXTRA_RESULT
            if (data.hasExtra(PHConstants.INTENT_EXTRA_RESULT)) {
                PHResponse<StatusResponse> response = (PHResponse<StatusResponse>) data.getSerializableExtra(PHConstants.INTENT_EXTRA_RESULT);

                if (response != null && response.isSuccess()) {
                    Log.i("PAYHERE", "Payment Successful");
                    saveOrder(response.getData());
                } else {
                    Log.e("PAYHERE", "Payment Failed");
                    Toast.makeText(getContext(), "Payment Failed!", Toast.LENGTH_LONG).show();
                }
            } else {
                Log.e("PAYHERE", "Intent is missing the result data!");
            }
        } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
            Log.e("PAYHERE", "Payment Canceled");
        }
    });

    private void saveOrder(StatusResponse statusResponse) {
        String uid = this.auth.getCurrentUser().getUid();

        Order.Address shippingAddress = Order.Address.builder()
                .name(binding.shippingName.getText().toString())
                .email(binding.shippingEmail.getText().toString())
                .phoneNumber(binding.shippingPhoneNumber.getText().toString())
                .addressLine1(binding.shippingAddressLine1.getText().toString())
                .addressLine2(binding.shippingAddressLine2.getText().toString())
                .city(binding.shippingCity.getText().toString())
                .postCode(binding.shippingPostalCode.getText().toString())
                .build();

        Order.Address billingAddress;
        if (binding.switchBillingSameAsShipping.isChecked()) {
            billingAddress = shippingAddress;
        } else {
            billingAddress = Order.Address.builder()
                    .name(binding.billingName.getText().toString())
                    .email(binding.billingEmail.getText().toString())
                    .phoneNumber(binding.billingPhoneNumber.getText().toString())
                    .addressLine1(binding.billingAddressLine1.getText().toString())
                    .addressLine2(binding.billingAddressLine2.getText().toString())
                    .city(binding.billingCity.getText().toString())
                    .postCode(binding.billingPostalCode.getText().toString())
                    .build();
        }

        Order order = new Order();
        order.setOrderId(String.valueOf(System.currentTimeMillis()));
        order.setUserId(uid);
        order.setTotalAmount(total);
        order.setStatus("Payment Completed");
        order.setOrderDate(Timestamp.now());
        order.setShippingAddress(shippingAddress);
        order.setBillingAddress(billingAddress);
        order.setOrderItems(currentOrderItems);

        db.collection("orders").document().set(order)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Order placed successfully!", Toast.LENGTH_SHORT).show();

                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.containerView, new HomeFragment())
                            .commit();
                })
                .addOnFailureListener(e -> {
                    Log.e("FirestoreError", "Failed to create order", e);
                    Toast.makeText(getContext(), "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private boolean isFormValid() {
        String name = binding.shippingName.getText().toString();
        String email = binding.shippingEmail.getText().toString();
        String phone = binding.shippingPhoneNumber.getText().toString();
        String addressLine1 = binding.shippingAddressLine1.getText().toString();
        String city = binding.shippingCity.getText().toString();
        String postalCode = binding.shippingPostalCode.getText().toString();

        if (name.isEmpty() || name.length() < 3) {
            binding.shippingName.setError("Valid name is required");
            binding.shippingName.requestFocus();
            return false;
        }
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.shippingEmail.setError("Enter a valid email");
            binding.shippingEmail.requestFocus();
            return false;
        }
        if (phone.isEmpty() || !phone.matches("^[0-9]{10}$")) {
            binding.shippingPhoneNumber.setError("Enter valid 10-digit phone number");
            binding.shippingPhoneNumber.requestFocus();
            return false;
        }
        if (addressLine1.isEmpty()) {
            binding.shippingAddressLine1.setError("Address Line 1 is required");
            binding.shippingAddressLine1.requestFocus();
            return false;
        }
        if (city.isEmpty()) {
            binding.shippingCity.setError("City is required");
            binding.shippingCity.requestFocus();
            return false;
        }
        if (postalCode.isEmpty() || !postalCode.matches("^[0-9]{4,6}$")) {
            binding.shippingPostalCode.setError("Invalid postal code");
            binding.shippingPostalCode.requestFocus();
            return false;
        }

        if (!binding.switchBillingSameAsShipping.isChecked()) {
            String bName = binding.billingName.getText().toString();
            String bEmail = binding.billingEmail.getText().toString();
            String bPhone = binding.billingPhoneNumber.getText().toString();
            String bAddress = binding.billingAddressLine1.getText().toString();
            String bCity = binding.billingCity.getText().toString();
            String bPostal = binding.billingPostalCode.getText().toString();

            if (bName.isEmpty() || bName.length() < 3) {
                binding.billingName.setError("Valid name is required");
                binding.billingName.requestFocus();
                return false;
            }
            if (bEmail.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(bEmail).matches()) {
                binding.billingEmail.setError("Enter a valid email");
                binding.billingEmail.requestFocus();
                return false;
            }
            if (bPhone.isEmpty() || !bPhone.matches("^[0-9]{10}$")) {
                binding.billingPhoneNumber.setError("Enter valid 10-digit phone number");
                binding.billingPhoneNumber.requestFocus();
                return false;
            }
            if (bAddress.isEmpty()) {
                binding.billingAddressLine1.setError("Address required");
                binding.billingAddressLine1.requestFocus();
                return false;
            }
            if (bCity.isEmpty()) {
                binding.billingCity.setError("City required");
                binding.billingCity.requestFocus();
                return false;
            }
            if (bPostal.isEmpty() || !bPostal.matches("^[0-9]{4,6}$")) {
                binding.billingPostalCode.setError("Invalid postal code");
                binding.billingPostalCode.requestFocus();
                return false;
            }
        }
        return true;
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