package com.gmail.station;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class SignInActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 120;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    private FirebaseFirestore firestore;

    private GoogleSignInClient mGoogleSignInClient;
    private SignInButton nG_sign_in;

    private ProgressBar signin_progress_bar;

    private SimpleDateFormat simpleDateFormat;
    private Date date;
    private String currentDateTime;

    @Override
    public void onStart() {
        super.onStart();
        init();
        updateUI(currentUser);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        init();

        nG_sign_in.setOnClickListener(view -> signIn());
    }

    private void init() {
        signin_progress_bar = findViewById(R.id.signin_progress_bar);

        nG_sign_in = findViewById(R.id.g_sign_in);

        GoogleSignInOptions gso = new GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        firestore = FirebaseFirestore.getInstance();

        simpleDateFormat = new SimpleDateFormat("hh:mm:ss dd/MM/yyyy", Locale.getDefault());
        date = Calendar.getInstance().getTime();
        currentDateTime = simpleDateFormat.format(date);
    }

    private void progressBarVisibility() {
        if (signin_progress_bar.getVisibility() == View.INVISIBLE) {
            signin_progress_bar.setVisibility(View.VISIBLE);
        } else {
            signin_progress_bar.setVisibility(View.INVISIBLE);
        }
    }

    private void signIn() {
        progressBarVisibility();
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d("errd fire", "signInWithCredential:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        addStationToFirestore(user);
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w("errw fire", "signInWithCredential:failure", task.getException());
                        updateUI(null);
                        progressBarVisibility();
                    }
                });
    }
    private void addStationToFirestore(FirebaseUser currentUser) {
        if (currentUser != null){
            HashMap<String, String> stationMap = new HashMap<>();
            stationMap.put(MyConstants.STATION_ID, currentUser.getUid());
            stationMap.put(MyConstants.STATION_MANAGER_NAME, currentUser.getDisplayName());
            stationMap.put(MyConstants.JOINED_AT, currentDateTime);
            stationMap.put(MyConstants.STATION_MANAGER_EMAIL, currentUser.getEmail());

            firestore.collection(MyConstants.STATION_COLLECTION).document(currentUser.getUid())
                    .set(stationMap, SetOptions.merge()).addOnCompleteListener(task -> {
                if (task.isComplete()){
                    progressBarVisibility();
                    if (task.isSuccessful()){ updateUI(currentUser); }
                    else {
                        Toast.makeText(SignInActivity.this,
                                "something went wrong! please try again",
                                Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }
    private void updateUI(FirebaseUser currentUser) {
        if (currentUser != null){
            Intent stationFormIntent = new Intent(SignInActivity.this, StationFormActivity.class);
            startActivity(stationFormIntent);
            finish();
        }
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d("err,d", "firebaseAuthWithGoogle:" + account.getId());
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.w("err,d", "Google sign in failed", e);
            }
        }else {progressBarVisibility();}
    }
}