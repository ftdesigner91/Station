package com.gmail.station.contact;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.gmail.station.MyConstants;
import com.gmail.station.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.HashMap;
import java.util.Objects;

public class ViewContactActivity extends AppCompatActivity {

    private Toolbar nView_contact_toolbar;
    private RecyclerView nContact_rv;

    private FirebaseFirestore firestore;
    private CollectionReference notifCollection;
    private FirestoreRecyclerAdapter<ContactModel, ContactViewHolder> adapter;
    private FirestoreRecyclerOptions<ContactModel> options;
    private Query query;

    private FirebaseAuth mAuth;
    private FirebaseUser mUser;

    private LinearLayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_contact);
        init();
        showToolbar();

        adapter = new FirestoreRecyclerAdapter<ContactModel, ContactViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull ContactViewHolder holder, int position, @NonNull ContactModel model) {
             holder.nSender_iv.setImageResource(R.drawable.ic_person_24px);
             holder.nSender_name_tv.setText(getFirstName(model.getCustomer_name()));
             holder.nSender_msg_tv.setText(model.getMessage());
             notifCollection.get().addOnCompleteListener(task -> {
                 if (task.isComplete()){
                     if (task.isSuccessful()){
                         Boolean aBoolean = task.getResult().getDocuments().get(position).getBoolean(MyConstants.MSG_SEEN);
                         if (aBoolean){ holder.nNotif_frame.setVisibility(View.VISIBLE); }
                     }
                 }
             });
             holder.nMsg_btn.setOnClickListener(view -> openChatRoom(model, model.getCustomer_id()));
            }
            @NonNull
            @Override
            public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.contact_layout, parent, false);
                return new ContactViewHolder(view);
            }
        };
        nContact_rv.setHasFixedSize(true);
        nContact_rv.setLayoutManager(layoutManager);
        nContact_rv.setAdapter(adapter);
    }

    private void init() {
        nView_contact_toolbar = findViewById(R.id.view_contact_toolbar);
        nContact_rv = findViewById(R.id.contact_rv);

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();

        firestore = FirebaseFirestore.getInstance();
        notifCollection = firestore.collection(MyConstants.STATION_COLLECTION).document(mUser.getUid())
                .collection(MyConstants.NOTIF_COLLECTION);
        query = notifCollection;
        options = new FirestoreRecyclerOptions.Builder<ContactModel>()
                .setQuery(query, ContactModel.class)
                .build();

        layoutManager = new LinearLayoutManager(this);
    }

    private void showToolbar(){
        setSupportActionBar(nView_contact_toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(true);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

    }

    private String getFirstName(String firstName){
        String[] blankSpacefinder = firstName.split(" ", 2);
        return firstName = blankSpacefinder[0];
    }

    private void openChatRoom(@NonNull ContactModel model, String customer_id) {
        HashMap<String, Object> notifMap = new HashMap<>();
        notifMap.put(MyConstants.MSG_SEEN, false);
        notifCollection.document(customer_id).update(notifMap).addOnCompleteListener(task -> {
            if (task.isComplete()){
                if (task.isSuccessful()){
                    Intent msgIntent = new Intent(ViewContactActivity.this, ChatRoomActivity.class);
                    msgIntent.putExtra(MyConstants.CUSTOMER_ID, model.getCustomer_id());
                    msgIntent.putExtra(MyConstants.CUSTOMER_NAME, model.getCustomer_name());
                    startActivity(msgIntent);
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (adapter != null){ adapter.startListening(); }
    }
    @Override
    public void onStop() {
        super.onStop();
        if (adapter != null){ adapter.stopListening(); }
    }

    private static class ContactViewHolder extends RecyclerView.ViewHolder {

        private final CardView nMsg_btn;
        private final ImageView nSender_iv;
        private final TextView nSender_name_tv;
        private final TextView nSender_msg_tv;
        private final FrameLayout nNotif_frame;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            nMsg_btn = itemView.findViewById(R.id.msg_btn);
            nSender_iv = itemView.findViewById(R.id.sender_iv);
            nSender_name_tv = itemView.findViewById(R.id.sender_name_tv);
            nSender_msg_tv = itemView.findViewById(R.id.sender_msg_tv);
            nNotif_frame = itemView.findViewById(R.id.notif_frame);

        }
    }
}