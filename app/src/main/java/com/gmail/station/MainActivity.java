package com.gmail.station;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gmail.station.contact.ViewContactActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Objects;


public class MainActivity extends AppCompatActivity {

    private static final String EDIT_THE_FORM = "edit the form";
    private static final String COMPLETE_FORM = "complete the form";

    private Toolbar nMain_toolbar;
    private LinearLayout nForm_btn;
    private LinearLayout nForm_btn1;
    private CardView nContact_btn;
    private CardView nServices_btn;
    private TextView nEdit_form_tv;
    private TextView nForm_msg_tv;
    private TextView nMsg_notif_tv;
    private TextView nForm_status_tv;
    private TextView nManager_name;
    private ImageView nArraw_iv;

    private FirebaseUser mUser;

    private FirebaseFirestore firestore;
    private CollectionReference workingSchCollection;
    private CollectionReference notifCollection;
    private DocumentReference stationDocument;

    private DatabaseReference contactRef;

    private String formCheck = "";

    @Override
    public void onStart() {
        super.onStart();
        init();
        updateUI(mUser);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        showToolbar();
        buildContactRoomsDB();

        checkMyForm();
        nForm_btn.setOnClickListener(view -> openForm());
        nForm_btn1.setOnClickListener(view -> openForm());

        nContact_btn.setOnClickListener(view -> openContact());

        nServices_btn.setOnClickListener(view -> openServices());
        displayMsgCount();

        getManagerName();
    }

    private void init() {
        nMain_toolbar = findViewById(R.id.main_toolbar);
        nForm_btn = findViewById(R.id.form_btn);
        nForm_btn1 = findViewById(R.id.form_btn1);
        nContact_btn = findViewById(R.id.contact_btn);
        nServices_btn = findViewById(R.id.services_btn);
        nEdit_form_tv = findViewById(R.id.edit_form_tv);
        nForm_msg_tv = findViewById(R.id.form_msg_tv);
        nMsg_notif_tv = findViewById(R.id.msg_notif_tv);
        nForm_status_tv = findViewById(R.id.form_status_tv);
        nArraw_iv = findViewById(R.id.arraw_iv);
        nManager_name = findViewById(R.id.manager_name);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();

        firestore = FirebaseFirestore.getInstance();
        if (mUser != null){
            workingSchCollection = firestore.collection(MyConstants.STATION_COLLECTION)
                    .document(mUser.getUid())
                    .collection(MyConstants.WORKING_SCH_COLLECTION);
            notifCollection = firestore.collection(MyConstants.STATION_COLLECTION)
                    .document(mUser.getUid())
                    .collection(MyConstants.NOTIF_COLLECTION);
            stationDocument = firestore.collection(MyConstants.STATION_COLLECTION)
                    .document(mUser.getUid());
        }

        FirebaseDatabase contactDB = FirebaseDatabase.getInstance();
        contactRef = contactDB.getReference(MyConstants.CONTACT_REF_PATH);
    }

    private void showToolbar() {
        setSupportActionBar(nMain_toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setTitle("My Station");
        if (mUser != null){
            stationDocument.get().addOnCompleteListener(task -> {
                if (task.isComplete()){
                    if (task.isSuccessful()){
                        if (task.getResult().exists()){
                            if (task.getResult().get(MyConstants.STATION_NAME) != null){
                                String stationName = String.valueOf(task.getResult().get(MyConstants.STATION_NAME));
                                getSupportActionBar().setTitle(stationName);
                            }else {
                                getSupportActionBar().setTitle("My Station");
                            }
                        }else {
                            getSupportActionBar().setTitle("My Station");
                        }
                    }
                }
            });
        }
    }

    private void displayMsgCount() {
        if (notifCollection != null){
            notifCollection.addSnapshotListener((value, error) -> {
                for (int i = 0; i <= value.size(); i++){
                    try {
                        if (value.getDocuments().get(i-1).get(MyConstants.MSG_SEEN) != null){
                            if (value.getDocuments().get(i-1).getBoolean(MyConstants.MSG_SEEN)){
                                if (i > 1){
                                    nMsg_notif_tv.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
                                    nMsg_notif_tv.setTextColor(getColor(R.color.design_default_color_error));
                                    nMsg_notif_tv.setText("you have "+i+" messages");
                                }
                                if (i == 1){
                                    nMsg_notif_tv.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
                                    nMsg_notif_tv.setTextColor(getColor(R.color.design_default_color_error));
                                    nMsg_notif_tv.setText("you have "+i+" message");
                                }
                            }
                            else {
                                nMsg_notif_tv.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
                                nMsg_notif_tv.setTextColor(getColor(R.color.c_black));
                                nMsg_notif_tv.setText("no new messages");
                            }
                        }
                    } catch (IndexOutOfBoundsException e){ e.printStackTrace(); }
                }
            });
        }
    }

    private void checkMyForm() {
        try {
            nArraw_iv.setImageResource(R.drawable.ic_baseline_arrow_right_24);
            if (workingSchCollection != null){
                workingSchCollection.addSnapshotListener((value, error) -> {
                    if (!Objects.requireNonNull(value).isEmpty()){
                        stationDocument.addSnapshotListener((value1, error1) -> {
                            if (Objects.equals(value1.get(MyConstants.STATION_ADDRESS), "") ||
                                    Objects.equals(value1.get(MyConstants.STATION_NAME), "")){
                                formCheck = "NOT ";
                                nForm_status_tv.setText("You have NOT completed the form");
                                nEdit_form_tv.setText(COMPLETE_FORM);
                                nForm_msg_tv.setText("it is best to "+COMPLETE_FORM+" at the station location");
                            }else {
                                nForm_status_tv.setText("You have "+formCheck+"completed the form");
                                nEdit_form_tv.setText(EDIT_THE_FORM);
                                nForm_msg_tv.setText("it is best to "+EDIT_THE_FORM+" at the station location");
                            }
                        });
                    }else {
                        Toast.makeText(this, ""+value.isEmpty(), Toast.LENGTH_SHORT).show();
                        formCheck = "NOT ";
                        nForm_status_tv.setText("You have NOT completed the form");
                        nEdit_form_tv.setText(COMPLETE_FORM);
                        nForm_msg_tv.setText("it is best to "+COMPLETE_FORM+" at the station location");
                    }
                });
            }
        }catch (NullPointerException e){
            e.printStackTrace();
        }
    }

    private void openForm() {
        Intent formIntent = new Intent(MainActivity.this, StationFormActivity.class);
        startActivity(formIntent);
    }

    private void openContact() {
        Intent contactIntent = new Intent(MainActivity.this, ViewContactActivity.class);
        startActivity(contactIntent);
    }

    private void openServices() {
        Intent serviceIntent = new Intent(MainActivity.this, AddServicesActivity.class);
        startActivity(serviceIntent);
    }

    private void updateUI(FirebaseUser currentUser) {
        if (currentUser == null){
            Intent stationFormIntent = new Intent(MainActivity.this, SignInActivity.class);
            startActivity(stationFormIntent);
            finish();
        }
    }

    private void buildContactRoomsDB() {
        contactRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (mUser != null){
                    if (!snapshot.hasChild(mUser.getUid())){
                        contactRef.child(mUser.getUid()).setValue(mUser.getUid()).addOnCompleteListener(task -> {
                            if (task.isComplete()){
                                if (!task.isSuccessful()){ Log.e("chat room", task.getException().getMessage()); }
                            }
                        });
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private String getFirstName(String fullName){
        String[] firstName = fullName.split(" ", 2);
        //String substring = fullName.substring(0, 0).toUpperCase();
        String cap1stLetter = firstName[0].substring(0, 1).toUpperCase();
        int fisrtNameLength = firstName[0].length();
        String get1stName =firstName[0].substring(1, fisrtNameLength);
        return cap1stLetter+get1stName;
    }
    private void getManagerName() {
        if (mUser != null){
            stationDocument.addSnapshotListener((value, error) -> {
                if (Objects.requireNonNull(value).exists()){
                    if (value.get(MyConstants.STATION_MANAGER_NAME) != null){
                        String managerName = String.valueOf(value.get(MyConstants.STATION_MANAGER_NAME));
                        nManager_name.setText(getFirstName(managerName));
                    }
                }

            });
        }
    }
}