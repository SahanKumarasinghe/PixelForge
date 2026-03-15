package com.sahan.app.pixelforge.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sahan.app.pixelforge.R;
import com.sahan.app.pixelforge.databinding.ActivityMainBinding;
import com.sahan.app.pixelforge.databinding.SideNavHeaderBinding;
import com.sahan.app.pixelforge.fragments.CartFragment;
import com.sahan.app.pixelforge.fragments.CategoryFragment;
import com.sahan.app.pixelforge.fragments.HomeFragment;
import com.sahan.app.pixelforge.fragments.OrdersFragment;
import com.sahan.app.pixelforge.fragments.ProfileFragment;
import com.sahan.app.pixelforge.fragments.SettingsFragment;
import com.sahan.app.pixelforge.fragments.WishlistFragment;
import com.sahan.app.pixelforge.models.User;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, NavigationBarView.OnItemSelectedListener {

    private static final String TAG = "MainActivity";
    private DrawerLayout drawerLayout;
    private MaterialToolbar toolbar;
    private NavigationView navigationView;
    private BottomNavigationView bottomNavigationView;
    private SideNavHeaderBinding sideNavHeaderBinding;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firebaseDB;
    private ActivityMainBinding homeBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.homeBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(homeBinding.getRoot());

        this.drawerLayout = homeBinding.drawerLayout;
//        this.toolbar = homeBinding.toolBar;
        this.navigationView = homeBinding.sideNavView;
        this.bottomNavigationView = homeBinding.bottomNavView;
        View headerView = homeBinding.sideNavView.getHeaderView(0);
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.firebaseDB = FirebaseFirestore.getInstance();
        this.sideNavHeaderBinding = SideNavHeaderBinding.bind(headerView);

        setSupportActionBar(toolbar);
//        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.drawerOpen, R.string.drawerClose);
//        toggle.syncState();

        ImageView menuToggler = findViewById(R.id.menuToggler);
        menuToggler.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.closeDrawer(GravityCompat.END);
            } else {
                drawerLayout.openDrawer(GravityCompat.END);
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    drawerLayout.closeDrawer(GravityCompat.END);
                } else {
                    finish();
                }
            }
        });

        navigationView.setNavigationItemSelectedListener(this);
        bottomNavigationView.setOnItemSelectedListener(this);

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
            navigationView.getMenu().findItem(R.id.app_bar_home).setChecked(true);
        }

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser != null) {
            firebaseDB.collection("users")
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            if (documentSnapshot.exists()) {
                                User user = documentSnapshot.toObject(User.class);
                                assert user != null;
                                sideNavHeaderBinding.sideNavUsername.setText(user.getName());
                                sideNavHeaderBinding.sideNavEmail.setText(user.getEmail());

                                if (user.getProfilePicUrl() == null) {
                                    Glide.with(MainActivity.this)
                                            .load(R.drawable.empty_profile_img)
                                            .centerCrop()
                                            .circleCrop()
                                            .into(sideNavHeaderBinding.sideNavPofilePic);
                                } else {
                                    Glide.with(MainActivity.this)
                                            .load(user.getProfilePicUrl())
                                            .centerCrop()
                                            .circleCrop()
                                            .into(sideNavHeaderBinding.sideNavPofilePic);
                                }
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(MainActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                            Log.e(TAG, e.toString().trim());
                        }
                    });

            navigationView.getMenu().findItem(R.id.app_bar_profile).setVisible(true);
            navigationView.getMenu().findItem(R.id.app_bar_orders).setVisible(true);
            navigationView.getMenu().findItem(R.id.app_bar_wishlist).setVisible(true);
            navigationView.getMenu().findItem(R.id.app_bar_cart).setVisible(true);
            navigationView.getMenu().findItem(R.id.app_bar_message).setVisible(true);
            navigationView.getMenu().findItem(R.id.app_bar_login).setVisible(false);
            navigationView.getMenu().findItem(R.id.app_bar_logout).setVisible(true);

        } else {
            navigationView.removeHeaderView(sideNavHeaderBinding.getRoot());
            navigationView.setPadding(0, 50, 0, 0);
        }

    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

        int itemId = menuItem.getItemId();

        Menu menu = navigationView.getMenu();

        for (int i = 0; i < menu.size(); i++) {
            menu.getItem(i).setChecked(false);
        }

        for (int i = 0; i < bottomNavigationView.getMenu().size(); i++) {
            bottomNavigationView.getMenu().getItem(i).setChecked(false);
        }

        if (itemId == R.id.app_bar_home || itemId == R.id.bottom_nav_home) {
            homeBinding.topBar.setVisibility(View.VISIBLE);
            loadFragment(new HomeFragment());
            navigationView.getMenu().findItem(R.id.app_bar_home).setChecked(true);
            bottomNavigationView.getMenu().findItem(R.id.bottom_nav_home).setChecked(true);

        } else if (itemId == R.id.app_bar_profile || itemId == R.id.bottom_nav_profile) {

            if (firebaseAuth.getCurrentUser() == null) {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
            }

            homeBinding.topBar.setVisibility(View.GONE);
            loadFragment(new ProfileFragment());
            navigationView.getMenu().findItem(R.id.app_bar_profile).setChecked(true);
            bottomNavigationView.getMenu().findItem(R.id.bottom_nav_profile).setChecked(true);


        } else if (itemId == R.id.app_bar_orders) {
            homeBinding.topBar.setVisibility(View.VISIBLE);
            loadFragment(new OrdersFragment());
            navigationView.getMenu().findItem(R.id.app_bar_orders).setChecked(true);

        } else if (itemId == R.id.app_bar_wishlist) {
            homeBinding.topBar.setVisibility(View.VISIBLE);
            loadFragment(new WishlistFragment());
            navigationView.getMenu().findItem(R.id.app_bar_wishlist).setChecked(true);

        } else if (itemId == R.id.app_bar_cart || itemId == R.id.bottom_nav_cart) {
            homeBinding.topBar.setVisibility(View.VISIBLE);
            if (firebaseAuth.getCurrentUser() == null) {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
            }

            loadFragment(new CartFragment());
            navigationView.getMenu().findItem(R.id.app_bar_cart).setChecked(true);
            bottomNavigationView.getMenu().findItem(R.id.bottom_nav_cart).setChecked(true);

        } else if (itemId == R.id.app_bar_message) {
            homeBinding.topBar.setVisibility(View.VISIBLE);
//            loadFragment(new MessageFragment());
//            navigationView.getMenu().findItem(R.id.app_bar_message).setChecked(true);

        } else if (itemId == R.id.app_bar_settings) {
            homeBinding.topBar.setVisibility(View.VISIBLE);
            loadFragment(new SettingsFragment());
            navigationView.getMenu().findItem(R.id.app_bar_settings).setChecked(true);

        } else if (itemId == R.id.bottom_nav_category) {
            homeBinding.topBar.setVisibility(View.VISIBLE);
            loadFragment(new CategoryFragment());
            bottomNavigationView.getMenu().findItem(R.id.bottom_nav_category).setChecked(true);

        } else if (itemId == R.id.app_bar_login) {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);

        } else if (itemId == R.id.app_bar_logout) {
            firebaseAuth.signOut();
            loadFragment(new HomeFragment());
            navigationView.getMenu().clear();
            navigationView.inflateMenu(R.menu.home_nav_menu);
            navigationView.removeHeaderView(sideNavHeaderBinding.getRoot());
            navigationView.setPadding(0, 50, 0, 0);
//            navigationView.inflateHeaderView(R.layout.side_nav_header);

        }
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
        }
        return false;

    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.containerView, fragment);
        transaction.commit();
    }
}