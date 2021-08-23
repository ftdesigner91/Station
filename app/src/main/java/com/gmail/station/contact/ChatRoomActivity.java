package com.gmail.station.contact;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.gmail.station.MyConstants;
import com.gmail.station.MyTouchEvent;
import com.gmail.station.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Objects;

public class ChatRoomActivity extends AppCompatActivity {

    private static final String CONTACT_REF_PATH = "contact_station";

    private String customer_id;
    private String customer_name;

    private DatabaseReference contactRef;

    private FirebaseUser mUser;
    private String my_id;

    private DocumentReference notifCollection;

    private MessageAdapter adapter;

    private Toolbar nMsg_toolbar;
    private TextView nCustomer_name;
    private RecyclerView nMsg_rv;
    private EditText nMsg_et;
    private TextView nSend_msg_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);
        init();
        showToolbar();
        new MyTouchEvent(this).collapseKeyboard(nMsg_rv);

        nSend_msg_btn.setOnClickListener(view -> sendMsg());

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        nMsg_rv.setHasFixedSize(true);
        nMsg_rv.setLayoutManager(layoutManager);
        nMsg_rv.setAdapter(adapter);
    }
    private void init() {
        Bundle bundle = getIntent().getExtras();
        customer_id = bundle.getString(MyConstants.CUSTOMER_ID);
        customer_name = bundle.getString(MyConstants.CUSTOMER_NAME);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        my_id = Objects.requireNonNull(mUser).getUid();

        FirebaseDatabase contactDB = FirebaseDatabase.getInstance();
        contactRef = contactDB.getReference(CONTACT_REF_PATH);

        Query query = contactRef.child("vqSuUjmU4GfmTvGzyHCukdPq2Ev2").child("TIxc1GszxrdmlGGXLD9mTWyJkKm2");
        FirebaseRecyclerOptions<ContactModel> options = new FirebaseRecyclerOptions.Builder<ContactModel>()
                .setQuery(query, ContactModel.class)
                .build();
        adapter = new MessageAdapter(options);

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        notifCollection = firestore.collection("stations").document(customer_id)
                .collection("my_notifications").document(my_id);

        nMsg_toolbar = findViewById(R.id.msg_toolbar);
        nCustomer_name = findViewById(R.id.customer_name);
        nMsg_rv = findViewById(R.id.msg_rv);
        nMsg_et = findViewById(R.id.msg_et);
        nSend_msg_btn = findViewById(R.id.send_msg_btn);
    }

    private void showToolbar(){
        setSupportActionBar(nMsg_toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        nCustomer_name.setText(customer_name);
    }

    private void sendMsg(){
        String message = String.valueOf(nMsg_et.getText()).trim();
        DatabaseReference myMsgRef = contactRef.child(my_id).child(customer_id);
        HashMap<String, Object> myMsgMap = new HashMap<>();
        myMsgMap.put(MyConstants.STATION_NAME, mUser.getDisplayName());
        myMsgMap.put(MyConstants.STATION_ID, my_id);
        myMsgMap.put("message", message);
        if (!TextUtils.isEmpty(message)){
            myMsgRef.push().setValue(myMsgMap).addOnCompleteListener(task -> {
                if (task.isComplete()){
                    if (task.isSuccessful()){
                        nMsg_et.getText().clear();
                        nMsg_rv.smoothScrollToPosition(Objects.requireNonNull(nMsg_rv.getAdapter()).getItemCount());
                        sendNotification(message);
                    }
                    else {
                        nMsg_et.getText().clear();
                        Toast.makeText(this, Objects.requireNonNull(task.getException()).getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                }
            });
        }else {nMsg_et.getText().clear();}
    }
    private void sendNotification(String message) {
        notifCollection.get().addOnCompleteListener(task1 -> {
            if (task1.isComplete()){
                if (!task1.getResult().exists()){
                    HashMap<String,String> notifMap = new HashMap<>();
                    notifMap.put("customer_name", mUser.getDisplayName());
                    notifMap.put("customer_id", my_id);
                    notifMap.put("message", message);
                    notifCollection.set(notifMap);
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        adapter.startListening();
    }
    @Override
    public void onStop() {
        super.onStop();
        adapter.stopListening();
    }
}