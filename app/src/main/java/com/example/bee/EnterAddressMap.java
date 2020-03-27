package com.example.bee;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.constant.TransportMode;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.model.Info;
import com.akexorcist.googledirection.model.Leg;
import com.akexorcist.googledirection.model.Route;
import com.akexorcist.googledirection.util.DirectionConverter;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.GeocodingResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *  This class takes user input of addresses and show the route on the map
 */
public class EnterAddressMap extends FragmentActivity implements OnMapReadyCallback,
        SetCost.OnFragmentInteractionListener, NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "TAG";
    private static final int REQUEST_CODE = 100;
    private Location currentLocation;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private GoogleMap map;
    private EditText fromEditText;
    private EditText toEditText;
    private Button confirmBtn;
    private String originAddress;
    private String destAddress;
    private LatLng p1;
    private LatLng p2;
    private ArrayList<LatLng> pointList;
    private int markers;
    private Boolean drew = false;
    private String distance;
    private String time;
    private double dist;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private String userID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drawer);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        userID = user.getUid();
        UserProfile profile = new UserProfile(userID);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        fetchLastLocation();

        drawerLayout = findViewById(R.id.drawer);
        navigationView = findViewById(R.id.navigationView);
        TextView displayName = navigationView.getHeaderView(0).findViewById(R.id.profileName);
        navigationView.setNavigationItemSelectedListener((NavigationView.OnNavigationItemSelectedListener) this);
        fromEditText = findViewById(R.id.from_address);
        toEditText = findViewById(R.id.to_address);
        confirmBtn = findViewById(R.id.confirm_route);
        // Hide confirm route button
        confirmBtn.setVisibility(View.GONE);
        Button showBtn = findViewById(R.id.show_route);
        ImageView profileBtn = findViewById(R.id.profile_btn);

        showBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideSoftKeyboard(EnterAddressMap.this);
                originAddress = fromEditText.getText().toString();
                destAddress = toEditText.getText().toString();
                if (!originAddress.isEmpty() && !destAddress.isEmpty()){
                    if (map != null) {
                        map.clear();
                    }

                    // drew is true if no exception is shown and the route is drawn
                    drew = getPoints(originAddress, destAddress);
                    if (!drew){
                        String text = "Invalid Address";
                        Toast toast = Toast.makeText(EnterAddressMap.this, text, Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                        confirmBtn.setVisibility(View.GONE);
                    } else {
                        confirmBtn.setVisibility(View.VISIBLE);
                    }
                } else {
                    // At least one of the editText is empty
                    String text = "Please fill in the address";
                    Toast toast = Toast.makeText(EnterAddressMap.this, text, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
            }
        });

        profileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Display name in drawer activity
                displayName.setText(profile.getFirstName()+" "+profile.getLastName());
                drawerLayout.openDrawer(Gravity.LEFT);
            }
        });
        // Confirm button for confirming route, invokes set cost dialog
        confirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new SetCost(dist*2.30).show(getSupportFragmentManager(), "set_cost");
            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        switch (menuItem.getItemId()){
            case R.id.profile:
                startActivity(new Intent(EnterAddressMap.this, EditProfileActivity.class));
                return true;
            case R.id.rating:
                Toast.makeText(EnterAddressMap.this, "My Rating Selected", Toast.LENGTH_SHORT).show();
                break;
            case R.id.about:
                Toast.makeText(EnterAddressMap.this, "About us Selected", Toast.LENGTH_SHORT).show();
                break;
            case R.id.logout:
                Toast.makeText(EnterAddressMap.this, "Logout Selected", Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
        return false;
    }


    /*
    StackOverflow post by Navneeth G https://stackoverflow.com/users/1135909/navneeth-g
    Answer https://stackoverflow.com/a/11656129

    Hides keyboard after pressing show routes button
     */
    private static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) activity.getSystemService(
                        Activity.INPUT_METHOD_SERVICE);
        View focusedView = activity.getCurrentFocus();
        if (focusedView != null) {
            inputMethodManager.hideSoftInputFromWindow(
                    activity.getCurrentFocus().getWindowToken(), 0);
        }
    }

    /**
     * Get latitude and longitude from the input location and call draw route function
     * @param originString
     * @param destString
     * @return whether or not the route is successfully drawn
     */
    /*
    Github library for Google Maps API Web Services by Google Maps https://github.com/googlemaps
    Library page: https://github.com/googlemaps/google-maps-services-java
     */
    private boolean getPoints(String originString, String destString) {
        LocationConverter converter = new LocationConverter(getApplicationContext());
        p1 = converter.addressToLatlng(originString);
        p2 = converter.addressToLatlng(destString);

        if (p1 == null || p2 == null) {
            return false;
        }

        map.addMarker(new MarkerOptions()
                .position(p1)
                .icon(bitmapDescriptorFromVector(this, R.drawable.ic_red_placeholder)));

        map.addMarker(new MarkerOptions()
                .position(p2)
                .icon(bitmapDescriptorFromVector(this, R.drawable.ic_green_placeholder)));
        markers = 2;
        LatLngBounds latLngBounds = new LatLngBounds.Builder()
                .include(p1)
                .include(p2)
                .build();

        // Move camera to include both points
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 200));
        getRideInfo(p1, p2);

        return true;
    }

    private void getRideInfo(LatLng p1, LatLng p2) {
        Routes routes = new Routes(p1, p2);
        routes.getRoutes();
        // distance in double, remove comma if there's any, remove km
        String temp = distance.replaceAll(",", "");
        dist = Double.parseDouble(temp.substring(0, temp.length() - 3));
    }

    /*
    StackOverflow post by Leo Droidcoder https://stackoverflow.com/users/5730321/leo-droidcoder
    Answer https://stackoverflow.com/a/45564994

    Convert vector drawable to bitmap for markers
     */
    private BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
/*
    *//**
     * Draw route on the map from the given p1 and p2. They are the latitude and longitude
     * for the start location and end location respectively.
     * @param p1
     * @param p2
     *//*
    *//*
    Github libray by Akexorcist https://github.com/akexorcist
    Library page: https://github.com/akexorcist/Android-GoogleDirectionLibrary
     *//*
    private void drawRoute(LatLng p1, LatLng p2) {
        GoogleDirection.withServerKey(getString(R.string.google_maps_key))
                .from(p1)
                .to(p2)
                .transportMode(TransportMode.DRIVING)
                .execute(new DirectionCallback() {
                    @Override
                    public void onDirectionSuccess(Direction direction) {
                        if (direction.isOK()) {
                            Route route = direction.getRouteList().get(0);
                            Leg leg = route.getLegList().get(0);
                            pointList = leg.getDirectionPoint();
                            Info distanceInfo = leg.getDistance();
                            Info durationInfo = leg.getDuration();
                            distance = distanceInfo.getText();
                            time = durationInfo.getText();
                            // distance in double, remove comma if there's any, remove km
                            String temp = distance.replaceAll(",", "");
                            dist = Double.parseDouble(temp.substring(0, temp.length() - 3));
                            PolylineOptions polylineOptions = DirectionConverter
                                    .createPolyline(EnterAddressMap.this, pointList, 5,
                                            getResources().getColor(R.color.route));
                            map.addPolyline(polylineOptions);
                        } else {
                            String text = direction.getStatus();
                            Toast toast = Toast.makeText(EnterAddressMap.this, text, Toast.LENGTH_SHORT);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                        }
                    }

                    @Override
                    public void onDirectionFailure(Throwable t) {
                        String text = "Failed to get direction";
                        Toast toast = Toast.makeText(EnterAddressMap.this, text, Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();

                    }
                });
    }*/

    /**
     * Show the last location of the device on the map
     */
    private void fetchLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);
            return;
        }
        Task<Location> task = fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null){
                    currentLocation = location;
                    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                            .findFragmentById(R.id.rider_initial_map);
                    mapFragment.getMapAsync(EnterAddressMap.this);
                }
            }
        });
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState)
    {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        LatLng latLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
        map.setPadding(     0,      400,      0,     50);
        map.setMyLocationEnabled(true);
        map.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,15));

        map.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                confirmBtn.setVisibility(View.GONE);
                // Fill in the address box
                String address;
                String inputFrom = fromEditText.getText().toString();
                String inputTo = toEditText.getText().toString();
                if (markers == 2) {
                    markers = 0;
                    map.clear();
                    fromEditText.setText("");
                    toEditText.setText("");
                }
                if (inputFrom.isEmpty()) {
                    // First text box is empty
                    // if both text box is empty, the first text box has priority
                    LocationConverter converter = new LocationConverter(getApplicationContext());
                    address = converter.latlngToAddress(latLng);
                    fromEditText.setText(address);
                    map.addMarker(new MarkerOptions()
                            .position(latLng)
                            .icon(bitmapDescriptorFromVector(EnterAddressMap.this, R.drawable.ic_red_placeholder)));
                    markers += 1;

                } else if (!inputFrom.isEmpty() && inputTo.isEmpty()) {
                    // destination text box is empty
                    LocationConverter converter = new LocationConverter(getApplicationContext());
                    address = converter.latlngToAddress(latLng);
                    toEditText.setText(address);
                    map.addMarker(new MarkerOptions()
                            .position(latLng)
                            .icon(bitmapDescriptorFromVector(EnterAddressMap.this, R.drawable.ic_green_placeholder)));
                    markers += 1;
                }

            }
        });
    }

    /**
     * Ask for user's permission to use their location
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    fetchLastLocation();
                }
                break;
            }
        }
    }

    /**
     * Post request to Firebase with possibly increased cost from user
     * @param cost
     */
    @Override
    public void postRequest(double cost) {

        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference requestRef = database.getReference("requests").child(userID);
        HashMap<String,Request> request = new HashMap<>();

        // Convert ArrayList of Latlng to ArrayList of String
        ArrayList<String> points = new ArrayList<>();
        for (LatLng latLng : pointList ){
            points.add(latLng.latitude+","+latLng.longitude);
        }
        // Convert Latlng to String
        String p1Latlng = String.format("%f,%f",p1.latitude,p1.longitude);
        String p2Latlng = String.format("%f,%f",p2.latitude,p2.longitude);
        request.put("request", new Request(userID, originAddress, destAddress, p1Latlng, p2Latlng,
                points, distance, time, cost));
        requestRef.setValue(request).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d(TAG, "onSuccess: request posted for"+ userID);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "onFailure: " + e.toString());
            }
        });

        startActivity(new Intent(EnterAddressMap.this, WaitingForDriver.class));
    }

    @Override
    public void onBackPressed() {} // Prevent activity from going back to login
}
