package com.example.bee;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;

/**
 * The class is the waiting page for the rider, it shows the pick up location and destination,
 * and the cost of the ride.
 */
public class WaitingForDriver extends AppCompatActivity {
    private static final String TAG = "TAG";
    private FirebaseUser user;
    private DatabaseReference ref;
    private String userID;
    private Request request;
    private TextView toText;
    private TextView fromText;
    private TextView costText;
    private String name;
    private String phone;
    private int thumbUp;
    private int thumbDown;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiting_for_driver);
        toText = findViewById(R.id.show_to);
        fromText = findViewById(R.id.show_from);
        costText = findViewById(R.id.show_cost);
        Button cancelRequestBtn = findViewById(R.id.cancel_request);

        user = FirebaseAuth.getInstance().getCurrentUser();
        userID = user.getUid();
        FirebaseDatabase database = Utils.getDatabase();
        ref = database.getReference("requests").child(userID).child("request");

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                request = dataSnapshot.getValue(Request.class);
                if (toText.getText().toString().isEmpty() && request != null) {
                    // Initialize the page with ride information
                    toText.setText(request.getDest());
                    fromText.setText(request.getOrigin());
                    costText.setText(String.format("%.2f", request.getCost()));
                }
                if (request != null) {
                    String driverID = request.getDriverID();
                    if (driverID != null) {
                        DatabaseReference newRef = database.getReference("users").child(driverID);
                        newRef.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                name = dataSnapshot.child("firstName").getValue(String.class)
                                        + " " + dataSnapshot.child("lastName").getValue(String.class);
                                phone = dataSnapshot.child("phone").getValue(String.class);
                                thumbUp = dataSnapshot.child("thumbUp").getValue(Integer.class);
                                thumbDown = dataSnapshot.child("thumbDown").getValue(Integer.class);
                                // Show confirm ride offer dialog
                                toConfirmOffer();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                Log.d(TAG, databaseError.toString());
                            }
                        });
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, databaseError.toString());
            }
        });

        cancelRequestBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // An dialog that asks the user to confirm their cancellation
                toConfirmCancel();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        CheckNetwork check = new CheckNetwork(getApplicationContext());
        boolean result = check.isNetworkAvailable();
        if (!result){
            String text = "You are offline, unable to update your status";
            Toast toast = Toast.makeText(WaitingForDriver.this, text, Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER,0,0);
            toast.show();
        }
    }

    /**
     * Shows a confirm message that ask the user to confirm their cancel of request
     */
    private void toConfirmCancel() {
        Dialog dialog = new Dialog(WaitingForDriver.this, android.R.style.Theme_Dialog);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.confirm_cancel_dialog);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setWindowAnimations(R.style.DialogAnimation);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        Button confirmBtn = dialog.findViewById(R.id.do_cancel_btn);
        Button cancelBtn = dialog.findViewById(R.id.not_cancel_btn);

        confirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Remove request from database
                ref.getParent().removeValue();
                dialog.dismiss();
                // Go back to EnterAddressMap activity
                startActivity(new Intent(WaitingForDriver.this, EnterAddressMap.class));
            }
        });

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }
   private void toConfirmOffer() {
        Dialog dialog = new Dialog(WaitingForDriver.this, android.R.style.Theme_Dialog);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.confirm_offer_fragment);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setWindowAnimations(R.style.DialogAnimation);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        TextView driverName = dialog.findViewById(R.id.driver_name1);
        TextView phoneNum = dialog.findViewById(R.id.phone_number);
        TextView rateUpText = dialog.findViewById(R.id.rate_up);
        TextView rateDownText = dialog.findViewById(R.id.rate_down);
        Button startBtn = dialog.findViewById(R.id.start_btn);
        Button rejectBtn = dialog.findViewById(R.id.reject_btn);
        driverName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(WaitingForDriver.this, DriverBasicInformation.class));
            }
        });

        if (driverName.getText().toString().isEmpty()) {
            // Initialize the dialog with driver's info
            driverName.setText(name);
            phoneNum.setText(phone);
            rateUpText.setText(String.valueOf(thumbUp));
            rateDownText.setText(String.valueOf(thumbDown));
        }

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Rider accepted the offer
                dialog.dismiss();
                acceptOffer();
            }
        });

        rejectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Rider declined the offer
                dialog.dismiss();
                ref.child("driverID").setValue(null);
            }
        });

        dialog.show();

    }

    /**
     * Rider side will notify the driver that the offer has been accepted
     */
    private void acceptOffer() {
        // Update request in FireBase with status = true;
        ref.child("status").setValue(true);
        startActivity(new Intent(WaitingForDriver.this, RiderAfterAcceptRequest.class));
    }

    @Override
    public void onBackPressed() {} // Prevent activity from going back to the last activity

}
