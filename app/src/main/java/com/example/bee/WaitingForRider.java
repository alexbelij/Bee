package com.example.bee;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

import static android.content.ContentValues.TAG;
/**
 * waiting for the rider response
 */
public class WaitingForRider extends FragmentActivity implements OnMapReadyCallback {
    private FirebaseFirestore db;
    private FirebaseUser user;
    private Request request;
    private String driverID;

    private String originAddress;
    private String destAddress;
    TextView riderName;
    String riderNameString;
    private DatabaseReference ref, riderFirstNameRef,riderLastNameRef;
    TextView RequestMoneyAmount;
    GoogleMap map;
    TextView RequestStatus;
//    RiderDecision riderDecision;
    Boolean riderResponse = true;
//    ArrayList<Request> request;
    RelativeLayout riderCard;
    Boolean myLocationPermission = false;
    MarkerOptions place1;
    MarkerOptions place2;
    Button finishButton;
    String riderFullName;
    Boolean drew = false;
    private String distance;
    private String time;
    double requestAmount;
    String passRiderName;
    String passRiderID;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.waiting_for_rider);
        // set up the map to the activity
        riderCard = findViewById(R.id.rider_card);

        RequestMoneyAmount = findViewById(R.id.request_money_amount2);
        initMap();
        riderResponse = false;
        Bundle bundle = getIntent().getExtras();
        passRiderName = bundle.getString("passRiderName");
        String passMoneyAmount = bundle.getString("passMoneyAmount");
        passRiderID = bundle.getString("passRiderID");

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        ref = database.getReference("requests").child(passRiderID).child("request");
        riderName = findViewById(R.id.rider_name);
        //        hide the finish button until the rider make response
        finishButton = findViewById(R.id.finish_button);
        finishButton.setVisibility(View.GONE);

        //hide the finish button until the rider make response
        RequestStatus = findViewById(R.id.request_status);
        RequestMoneyAmount.setText("$" + passMoneyAmount);
        if(passRiderName != null){
            riderName.setText(passRiderName);
        }else{
            riderName.setText("Invalid rider name");
        }
        if(passMoneyAmount != null){
            RequestMoneyAmount.setText("$" + passMoneyAmount);
        }else{
            RequestMoneyAmount.setText("Invalid amount");

        }

        riderName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WaitingForRider.this, RiderProfile.class);
                intent.putExtra("passRiderID",passRiderID);
                startActivity(intent);
            }
        });
        // Depends rider response to process to next activity
        DatabaseReference statusRef = database.getReference("requests").child(passRiderID).child("request").child("status");
        statusRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                 String riderResponseString = dataSnapshot.getValue(String.class);
//                riderResponse = Boolean.parseBoolean(riderResponseString);
                    riderResponse = dataSnapshot.getValue(Boolean.class);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        DatabaseReference cancelRef = ref.child("cancel");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                request = dataSnapshot.getValue(Request.class);
                if (request != null) {
                    driverID = request.getDriverID();
                    if (driverID == null) {
                        finishButton.setVisibility(View.VISIBLE);
                        RequestStatus.setText("Declined offer");
                        finishButton.setText("BACK");

                        finishButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                startActivity(new Intent(WaitingForRider.this, SearchRide.class));

                            }
                        });

                    }else{
                        if(riderResponse){
                            cancelRef.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    Boolean cancelValue = dataSnapshot.getValue(Boolean.class);
                                    if(cancelValue){
                                        ref.getParent().removeValue();
                                        finishButton.setVisibility(View.VISIBLE);
                                        RequestStatus.setText("Request has been cancelled");
                                        finishButton.setText("BACK");
                                        finishButton.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                startActivity(new Intent(WaitingForRider.this, SearchRide.class));

                                            }
                                        });
                                    }
                                    else{
                                        RequestStatus.setText("Confirmed ride offer");
                                        finishButton.setVisibility(View.VISIBLE);
                                        DatabaseReference reachRef = ref.child("reached");
                                        finishButton.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                reachRef.setValue(true);
                                                Intent intent = new Intent(WaitingForRider
                                                        .this, DriverPayActivity
                                                        .class);
                                                intent.putExtra("Rider", passRiderName);
                                                intent.putExtra("RiderID", passRiderID);
                                                intent.putExtra("amount", Double.parseDouble(passMoneyAmount));
                                                startActivity(intent);
                                            }
                                        });


                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });

                        }
                        if(!riderResponse){
                            finishButton.setVisibility(View.GONE);

                            RequestStatus.setText("Waiting for comfirmation......");

                        }
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, databaseError.toString());
            }
        });

        riderCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WaitingForRider.this, PopUpMap.class);
                intent.putExtra("passMoneyAmount",passMoneyAmount);
                intent.putExtra("passRiderID",passRiderID);
                intent.putExtra("passRiderName",riderFullName);
                startActivity(intent);


            }
        });


    }

    /**
     * This initialize the bitmap
     * @param context
     * context to load the bitmap
     * @param vectorResId
     * The Id to pass the vector into the bitmap
     */
    private BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
    /**
     * This inititiazlize the map fragment
     */
    private void initMap(){
        Log.d(TAG, "Initializing map");
        SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_requestAccepted);
//        synchronize the map in the activity
        supportMapFragment.getMapAsync(WaitingForRider.this);

    }

    /**
     * This set up the map
     * @param googleMap
     * google map to display the map content
     */
    @Override
    public void onMapReady(GoogleMap googleMap){
        Toast.makeText(this, "Map is Ready", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onMapReady: Map is ready");
        map = googleMap;
        Bundle bundle = getIntent().getExtras();
        passRiderID = bundle.getString("passRiderID");
        if(passRiderID != null){
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            ref = database.getReference("requests").child(passRiderID).child("request");
            DatabaseReference originLatlngRef = ref.child("originLatlng");
//            set up the origin on the map
            originLatlngRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists()) {
                        String originString = dataSnapshot.getValue(String.class);
                        String[] afterSplitLoc = originString.split(",");
                        double originLatitude = Double.parseDouble(afterSplitLoc[0]);
                        double originLongitude = Double.parseDouble(afterSplitLoc[1]);
                        LatLng originCoordinate = new LatLng(originLatitude, originLongitude);
                        place1 = new MarkerOptions().position(originCoordinate).title("Starting position");
                    }
                    else{
                        boolean result = isNetworkAvailable();
                        if (!result) {
                            Toast toast = Toast.makeText(WaitingForRider.this, "You are offline", Toast.LENGTH_SHORT);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            riderCard.setEnabled(false);
                            RequestStatus.setText("Please check internet activity");
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
//            retreive destination coordinate from firebase
            DatabaseReference destLatlngRef = ref.child("destLatlng");
            destLatlngRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists()) {
                        String destStringTemp = dataSnapshot.getValue(String.class);
                        String[] afterSplitLoc1 = destStringTemp.split(",");
                        double destLatitude = Double.parseDouble(afterSplitLoc1[0]);
                        double destLongitude = Double.parseDouble(afterSplitLoc1[1]);
                        LatLng destCoordinate = new LatLng(destLatitude, destLongitude);
                        place2 = new MarkerOptions().position(destCoordinate).title("Destination");
                    }
                    else{
                        boolean result = isNetworkAvailable();
                        if (!result) {
                            Toast toast = Toast.makeText(WaitingForRider.this, "You are offline", Toast.LENGTH_SHORT);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            riderCard.setEnabled(false);
                            RequestStatus.setText("Please check internet activity");
                        }
                    }

                    FirebaseDatabase database = FirebaseDatabase.getInstance();
                    riderFirstNameRef = database.getReference("users").child(passRiderID).child("firstName");;
                    riderFirstNameRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            String riderFirstNameString = dataSnapshot.getValue(String.class);
                            if(riderFirstNameString != null){
                                riderLastNameRef = database.getReference("users").child(passRiderID).child("lastName");
                                riderLastNameRef.addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        String riderLastNameString = dataSnapshot.getValue(String.class);
                                        if(riderLastNameString != null) {
                                            riderFullName = riderFirstNameString + " " + riderLastNameString;
                                            riderName.setText(riderFullName);
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


                    drew = getPoints(place1, place2);

                    if (!drew) {
                        String text = "Invalid Address";
                        Toast toast = Toast.makeText(WaitingForRider.this, text, Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();

                    }else{
                        String tripTime = ref.child("time").toString();
//                        mapPop.addMarker()
//                        mapPop.addMarker(toAddress.position(to_position)
//                                .icon(bitmapDescriptorFromVector(this, R.drawable.ic_green_placeholder)));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
//                String tripTime = ref.child("time").toString();
//                LatLng destLatlng = ref.child("destLaglng");
//                String originStringTemp = ref.child("originLatlng").get();
//                riderID.setText(originStringTemp);
//                String originString = originStringTemp.substring(1).substring(0, originStringTemp.length() - 2 );
//
//
//
//                String destString = destStringTemp.substring(1).substring(0, destStringTemp.length() - 2 );
//

//
//


        }


        }
    /**
     * This check the network is connected or not
     */

    private boolean isNetworkAvailable() {

        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    /**
     * This locate the current location for the user device
     */
    private void getDeviceLocation(){
        Log.d(TAG, "getDeviceLocation: getting the devices current location");

        FusedLocationProviderClient client_device = LocationServices.getFusedLocationProviderClient(this);
//        locate the current driver position
        try{
            if(myLocationPermission){
                final Task location = client_device.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if(task.isSuccessful()){
                            Log.d(TAG, "onComplete: found location!");
                            Location currentLocation = (Location) task.getResult();

                        }else{
                            Log.d(TAG, "onComplete: current location is null");
                            Toast.makeText(WaitingForRider.this, "unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }catch (SecurityException e){
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage() );
        }
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

            map.addMarker(fromAddress.position(from_position)
                    .icon(bitmapDescriptorFromVector(this, R.drawable.ic_red_placeholder)));
            map.addMarker(toAddress.position(to_position)
                    .icon(bitmapDescriptorFromVector(this, R.drawable.ic_green_placeholder)));
//            display the two locations as the marker in the map
            LatLngBounds latLngBounds = new LatLngBounds.Builder()
                    .include(from_position)
                    .include(to_position)
                    .build();
            map.setPadding(0, 150, 0, 0);
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 200));
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
                .execute(new DirectionCallback() {
                    @Override
                    public void onDirectionSuccess(Direction direction) {
                        if(direction.isOK()) {

                            Route route = direction.getRouteList().get(0);
                            Leg leg = route.getLegList().get(0);
                            ArrayList<LatLng> pointList = leg.getDirectionPoint();
                            Info distanceInfo = leg.getDistance();
                            String distance = distanceInfo.getText();
                            Toast.makeText(WaitingForRider.this, distance, Toast.LENGTH_SHORT).show();
                            PolylineOptions polylineOptions = DirectionConverter
                                    .createPolyline(WaitingForRider.this, pointList, 5,
                                            getResources().getColor(R.color.route));
                            map.addPolyline(polylineOptions);
//                            display the route as line on the map
//                            Route route = direction.getRouteList().get(0);
//                            Leg leg = route.getLegList().get(0);
//                            ArrayList<LatLng> pointList = leg.getDirectionPoint();
//                            PolylineOptions polylineOptions = DirectionConverter
//                                    .createPolyline(PopUpMap.this, pointList, 5,
//                                            getResources().getColor(R.color.yellow));
//                            mapPop.addPolyline(polylineOptions);
                        } else {
                            String text = direction.getStatus();
                            Toast toast = Toast.makeText(WaitingForRider.this, text, Toast.LENGTH_SHORT);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                        }
                    }

                    @Override
                    public void onDirectionFailure(Throwable t) {
                        String text = "Failed to get direction";
                        Toast toast = Toast.makeText(WaitingForRider.this, text, Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();

                    }
                });

    }
}
