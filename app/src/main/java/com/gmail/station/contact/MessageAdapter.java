package com.gmail.station.contact;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.gmail.station.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class MessageAdapter extends FirebaseRecyclerAdapter<ContactModel, MessageAdapter.MessageViewHolder> {

    private static final int RECEIVER_LAYOUT = 2;
    private static final int SENDER_LAYOUT = 1;

    public MessageAdapter(@NonNull FirebaseRecyclerOptions<ContactModel> options) {
        super(options);
    }


    @Override
    protected void onBindViewHolder(@NonNull MessageViewHolder holder, int position, @NonNull ContactModel model) {
        if (holder.nSent_msg_tv != null){ holder.nSent_msg_tv.setText(model.getMessage()); }
        if (holder.nReceived_msg_tv != null){ holder.nReceived_msg_tv.setText(model.getMessage()); }
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;

        if (viewType == SENDER_LAYOUT){
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.msg_sent_layout, parent, false);
        }
        else { // RECEIVER_LAYOUT
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.msg_receive_layout, parent, false);
        }
        return new MessageViewHolder(view);
    }

    @Override
    public int getItemViewType(int position) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser mUser = mAuth.getCurrentUser();
        String my_id = Objects.requireNonNull(mUser).getUid();

        if (getItem(position).getStation_id() != null && getItem(position).getStation_id().equals(my_id)){
            return SENDER_LAYOUT;
        }else {
            return RECEIVER_LAYOUT;
        }

    }

    static class MessageViewHolder extends RecyclerView.ViewHolder{

        private final TextView nSent_msg_tv;
        private final TextView nReceived_msg_tv;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            nSent_msg_tv = itemView.findViewById(R.id.sent_msg_tv);
            nReceived_msg_tv = itemView.findViewById(R.id.received_msg_tv);
        }
    }
}
