package com.example.bee;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import com.akexorcist.googledirection.DirectionCallback;
import com.akexorcist.googledirection.GoogleDirection;
import com.akexorcist.googledirection.constant.TransportMode;
import com.akexorcist.googledirection.model.Direction;
import com.akexorcist.googledirection.model.Info;
import com.akexorcist.googledirection.model.Leg;
import com.akexorcist.googledirection.model.Route;
import com.akexorcist.googledirection.util.DirectionConverter;
import com.google.android.gms.location.FusedLocationProviderClient;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Objects;

import static android.app.PendingIntent.getActivity;
import static android.content.ContentValues.TAG;



//partial citation can be found in readme
/**
 * Pop up the map windows to display locations and route
 */
public class PopUpMap extends FragmentActivity implements OnMapReadyCallback{
    private FusedLocationProviderClient client_device;
    LinearLayout linearLayout;
    TextView riderName, requestMoneyAmount;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser user;
    String userID;
    String riderNameString;
    private FirebaseDatabase firebaseDatabase;
    GoogleMap mapPop;
    MarkerOptions place1;
    private DatabaseReference ref,riderFirstNameRef, riderLastNameRef;
    String passRiderID;
    MarkerOptions place2;
    private Boolean drew = false;
    Button AcceptButton;
    Button CancelButton;


        @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            setContentView(R.layout.pop_up_map);
            linearLayout = findViewById(R.id.pop_up_layout);
            riderName = findViewById(R.id.rider_name);
            requestMoneyAmount = findViewById(R.id.money_amount_in_pop);


//            https://stackoverflow.com/questions/9998221/how-to-pass-double-value-to-a-textview-in-android

            initMap();
            AcceptButton = findViewById(R.id.accept_button);
            CancelButton = findViewById(R.id.cancel_button);
//        initialize the map as a pop up window
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

            int width = displayMetrics.widthPixels;
            int height = displayMetrics.heightPixels;
//            receive the value from previous activity
            Bundle bundle = getIntent().getExtras();
            String passMoneyAmount = bundle.getString("passMoneyAmount");
            passRiderID = bundle.getString("passRiderID");
            String passDriverID = bundle.getString("passDriverID");
//            set up the size of the pop up window
            getWindow().setLayout((int) (width * 0.8), (int) (height * .6));
            riderName.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(PopUpMap.this, RiderProfile.class);
                    intent.putExtra("passRiderID",passRiderID);
                    startActivity(intent);
                }
            });
//        set up the accept button
            AcceptButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    pass the value to the next activity
                    Intent show = new Intent(PopUpMap.this,WaitingForRider.class);
                    show.putExtra("passMoneyAmount",passMoneyAmount);
                    show.putExtra("passRiderID",passRiderID);
                    show.putExtra("passRiderName",riderNameString);

                    DatabaseReference driverIDRef = ref.child("driverID");
                    driverIDRef.setValue(passDriverID);


                        startActivity(show);
                }
            });
//        set up the cancel button
            CancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    go to previous activity
                    final Intent cancel = new Intent(PopUpMap.this, SearchRide.class);
                    startActivity(cancel);
                }
            });


        }
//        load the bitmap
    private BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    /**
     * This initialize the map to start
     */
    private void initMap(){
        Log.d(TAG, "Initializing map");
        SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager()
        .findFragmentById(R.id.map_pop);
//        synchronize the map in the activity
        supportMapFragment.getMapAsync(PopUpMap.this);

    }
    /**
     * This locate the current location for the user device
     */

//    https://stackoverflow.com/questions/30708036/delete-the-last-two-characters-of-the-string
    @Override
    public void onMapReady(GoogleMap googleMap){
        Toast.makeText(this, "Map is Ready", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onMapReady: Map is ready");
        mapPop = googleMap;

        user = firebaseAuth.getInstance().getCurrentUser();
        userID = user.getUid();
        Bundle bundle = getIntent().getExtras();
        String passMoneyAmount = bundle.getString("passMoneyAmount");
        passRiderID = bundle.getString("passRiderID");
        requestMoneyAmount.setText("$" + passMoneyAmount);
//        check the passing riderID is whether exists or not
        if(passRiderID != null){
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            ref = database.getReference("requests").child(passRiderID).child("request");
            DatabaseReference originLatlngRef = ref.child("originLatlng");
//              initialize the reference for original location
            originLatlngRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    String originString = dataSnapshot.getValue(String.class);
                    String[] afterSplitLoc = originString.split(",");
                    double originLatitude = Double.parseDouble(afterSplitLoc[0]);
                    double originLongitude = Double.parseDouble(afterSplitLoc[1]);
                    LatLng originCoordinate = new LatLng(originLatitude,originLongitude);
//                    set up the origin location on the map
                    place1 = new MarkerOptions().position(originCoordinate).title("Starting position");

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
            DatabaseReference destLatlngRef = ref.child("destLatlng");
//            retrieve the database reference for destination coordinate
            destLatlngRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                    set up the destination coordiante on the map displayed
                    String destStringTemp = dataSnapshot.getValue(String.class);
                    String[] afterSplitLoc1 = destStringTemp.split(",");

                    double destLatitude = Double.parseDouble(afterSplitLoc1[0]);
                    double destLongitude = Double.parseDouble(afterSplitLoc1[1]);
                    LatLng destCoordinate = new LatLng(destLatitude,destLongitude);
                    FirebaseDatabase database = FirebaseDatabase.getInstance();
                    riderFirstNameRef = database.getReference("users").child(passRiderID).child("firstName");;
                    riderFirstNameRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            String riderFirstNameString = dataSnapshot.getValue(String.class);
//                            check the first name is valid or not
                            if(riderFirstNameString != null){
//                                set up the rider first name and last name
                                riderLastNameRef = database.getReference("users").child(passRiderID).child("lastName");
                                riderLastNameRef.addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        String riderLastNameString = dataSnapshot.getValue(String.class);
//                                  check the last name is valid or not
                                        if(riderLastNameString != null) {
                                            riderName.setText(riderFirstNameString + " " + riderLastNameString);
                                        }else{
                                            riderName.setText("Invalid rider name");
                                        }
                                    }
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                            }else{
                                riderName.setText("Invalid rider name");
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
//                             set up the destination location on the map
                    place2 = new MarkerOptions().position(destCoordinate).title("Destination");
                    drew = getPoints(place1, place2);
//                  if the route cannot be generated, address can be invalid
                    if (!drew) {
                        String text = "Invalid Address";
                        Toast toast = Toast.makeText(PopUpMap.this, text, Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });

        }
        boolean result = isNetworkAvailable();
//        offline mode initialization when the network is unavailable
        if (!result){
            Toast toast = Toast.makeText(PopUpMap.this, "You are offline", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER,0,0);
            toast.show();
            CancelButton.setEnabled(false);
            riderName.setText("Please check internet activity");

        }

//        initialize the starting position and destination

    }
    /**
     * This check the network is whether connected or not
     */
    private boolean isNetworkAvailable() {

        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    /**
     * This set up the marker points on the map
     * @param fromAddress
     * address of starting position
     * @param toAddress
     * address of the destination
     */

    private boolean getPoints(MarkerOptions fromAddress, MarkerOptions toAddress) {

        try {
            // May throw an IOException
            LatLng from_position = fromAddress.getPosition();
            LatLng to_position = toAddress.getPosition();

            mapPop.addMarker(fromAddress.position(from_position)
                    .icon(bitmapDescriptorFromVector(this, R.drawable.ic_red_placeholder)));
            mapPop.addMarker(toAddress.position(to_position)
                    .icon(bitmapDescriptorFromVector(this, R.drawable.ic_green_placeholder)));
//            display the two locations as the marker in the map
            LatLngBounds latLngBounds = new LatLngBounds.Builder()
                    .include(from_position)
                    .include(to_position)
                    .build();
            mapPop.setPadding(0, 150, 0, 0);
            mapPop.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 200));
            drawRoute(from_position, to_position);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
    /**
     * This return draw the route between two locations
     * @param p1
     * coordinate of starting location
     * @param p2
     * coordinate of destination
     */

    private void drawRoute(LatLng p1, LatLng p2) {
        GoogleDirection.withServerKey(getString(R.string.google_maps_key))
                .from(p1)
                .to(p2)
                .transportMode(TransportMode.DRIVING)
//                retrieve the key for traverse route direction
                .execute(new DirectionCallback() {
                    @Override
                    public void onDirectionSuccess(Direction direction) {
                        if(direction.isOK()) {

                            Route route = direction.getRouteList().get(0);
                            Leg leg = route.getLegList().get(0);
                            ArrayList<LatLng> pointList = leg.getDirectionPoint();
                            Info distanceInfo = leg.getDistance();
                            String distance = distanceInfo.getText();
                            Toast.makeText(PopUpMap.this, distance, Toast.LENGTH_SHORT).show();
                            PolylineOptions polylineOptions = DirectionConverter
                                    .createPolyline(PopUpMap.this, pointList, 5,
                                            getResources().getColor(R.color.route));
                            mapPop.addPolyline(polylineOptions);
//                            display the route as line on the map

                        } else {
                            String text = direction.getStatus();
                            Toast toast = Toast.makeText(PopUpMap.this, text, Toast.LENGTH_SHORT);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                        }
                    }
//fail to generate route between locations
                    @Override
                    public void onDirectionFailure(Throwable t) {
                        String text = "Failed to get direction";
                        Toast toast = Toast.makeText(PopUpMap.this, text, Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();

                    }
                });

    }
}
