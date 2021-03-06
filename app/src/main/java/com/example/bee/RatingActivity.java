package com.example.bee;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
/**
 *  This class supports user to see the ratings they got
 */
public class RatingActivity extends AppCompatActivity {
    public static final String TAG = "TAG";
    private ImageView logo;
    private TextView username, thumbUp, thumbDown, driverHint, ratingHint;
    private Button back;

    private ProgressDialog progressDialog;
    private FirebaseAuth firebaseAuth;
    private FirebaseDatabase firebaseDatabase;
    FirebaseFirestore db;
    String userID;
    ProgressBar progressBar;
    FirebaseUser mUser;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rating);

        initializeGUI();
        driverHint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(RatingActivity.this,DriverMain.class));
            }
        });




    }

    /**
     *  This method initializes GUI
     */
    private void initializeGUI() {


        username = findViewById(R.id.ratingName);
        thumbUp = findViewById(R.id.thumbUp);
        thumbDown = findViewById(R.id.thumbDown);
        driverHint = findViewById(R.id.tvDriverHint);
        progressDialog = new ProgressDialog(this);

        firebaseAuth = FirebaseAuth.getInstance();
        userID = firebaseAuth.getCurrentUser().getUid();
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference ref = database.getReference("users");
        ref.child(userID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot snap: dataSnapshot.getChildren()){
                    String name = dataSnapshot.child("firstName").getValue(String.class)
                            + " " + dataSnapshot.child("lastName").getValue(String.class);
                    username.setText(name);
                    thumbUp.setText(dataSnapshot.child("thumbUp").getValue().toString());
                    thumbDown.setText(dataSnapshot.child("thumbDown").getValue().toString());

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }


}



