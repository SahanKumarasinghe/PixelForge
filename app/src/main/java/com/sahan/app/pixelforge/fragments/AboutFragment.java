package com.sahan.app.pixelforge.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.sahan.app.pixelforge.R;
import com.sahan.app.pixelforge.databinding.FragmentAboutBinding;

public class AboutFragment extends Fragment implements OnMapReadyCallback {

    private FragmentAboutBinding binding;
    private GoogleMap mMap;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAboutBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getActivity() != null && getActivity().findViewById(R.id.app_bar_home) != null) {
            getActivity().findViewById(R.id.app_bar_home).setVisibility(View.GONE);
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map_container);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        binding.contactEmail.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_SENDTO);
            intent.setData(android.net.Uri.parse("mailto:support@pixelforge.lk"));
            startActivity(intent);
        });

        binding.contactPhone.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_DIAL);
            intent.setData(android.net.Uri.parse("tel:+94765662243"));
            startActivity(intent);
        });

        binding.contactWebsite.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
            intent.setData(android.net.Uri.parse("https://www.pixelforge.lk"));
            startActivity(intent);
        });

    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        LatLng colomboBranch = new LatLng(6.9271, 79.8612);
        mMap.addMarker(new MarkerOptions()
                .position(colomboBranch)
                .title("PixelForge Colombo")
                .snippet("Main Headquarters"));

        LatLng kandyBranch = new LatLng(7.2906, 80.6337);
        mMap.addMarker(new MarkerOptions()
                .position(kandyBranch)
                .title("PixelForge Kandy")
                .snippet("Central Province Store"));

        LatLng buttalaBranch = new LatLng(6.7567, 81.2483);
        mMap.addMarker(new MarkerOptions()
                .position(buttalaBranch)
                .title("PixelForge Buttala")
                .snippet("Uva Province Store"));

        LatLng centerOfSriLanka = new LatLng(7.8731, 80.7718);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(centerOfSriLanka, 7f));

        mMap.getUiSettings().setZoomControlsEnabled(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (getActivity() != null && getActivity().findViewById(R.id.app_bar_home) != null) {
            getActivity().findViewById(R.id.app_bar_home).setVisibility(View.VISIBLE);
        }
        binding = null;
    }
}