package com.gmail.station;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import net.yslibrary.android.keyboardvisibilityevent.util.UIUtil;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;

import javax.annotation.Nonnull;

public class AddServicesActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private ScrollView nAdd_service_parent;
    private Toolbar nService_toolbar;
    private ImageView nService_iv;
    private EditText nTitle_et;
    private EditText nDes_et;
    private Button nAdd_btn;
    private ProgressBar nService_progress_bar;

    private FirebaseAuth mAuth;
    private FirebaseUser mUser;

    private FirebaseFirestore firestore;
    private DocumentReference servicesDocument;

    private StorageReference storageReference;
    private UploadTask uploadTask;

    private Uri image_uri;

    private SimpleDateFormat simpleDateFormat;
    private Date date;
    private String currentDateTime;

    private String millis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_services);
        init();
        showToolbar();

        new MyTouchEvent(AddServicesActivity.this).collapseKeyboard(nAdd_service_parent);

        nService_iv.setOnClickListener(view -> getImageFromGallery());

        nAdd_btn.setOnClickListener(view -> addMyService());
    }

    private void init() {
        nAdd_service_parent = findViewById(R.id.add_service_parent);
        nService_iv = findViewById(R.id.service_iv);
        nTitle_et = findViewById(R.id.title_et);
        nDes_et = findViewById(R.id.des_et);
        nAdd_btn = findViewById(R.id.add_btn);
        nService_toolbar = findViewById(R.id.service_toolbar);
        nService_progress_bar = findViewById(R.id.service_progress_bar);

        mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();
        if (image_uri != null){
            storageReference = FirebaseStorage.getInstance().getReference(MyConstants.STATIONS_IMG_LOCATION)
                    .child(mUser.getUid() + System.currentTimeMillis() + "." + getFileExtension(image_uri));
        }

        firestore = FirebaseFirestore.getInstance();
        millis = String.valueOf(System.currentTimeMillis());
        servicesDocument = firestore.collection(MyConstants.STATION_COLLECTION).document(mUser.getUid())
                .collection(MyConstants.MY_SERVICES_COLLECTION).document(millis);

        simpleDateFormat = new SimpleDateFormat("hh:mm:ss dd/MM/yyyy", Locale.getDefault());
        date = Calendar.getInstance().getTime();
    }

    private void showToolbar() {
        setSupportActionBar(nService_toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
    }

    private String getStringFromET(EditText editText){
        return String.valueOf(editText.getText()).trim();
    }

    private void getImageFromGallery() {
        Intent galleryIntent = new Intent();
        galleryIntent.setType("image/*");
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST);
    }

    private String getFileExtension(Uri uri) {
        ContentResolver cr = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cr.getType(uri));
    }

    private String getCurrentDateTime(){ return currentDateTime = simpleDateFormat.format(date); }

    private void addMyService() {
        nService_progress_bar.setVisibility(View.VISIBLE);
        if (!TextUtils.isEmpty(String.valueOf(nTitle_et.getText()).trim())){
            if (!TextUtils.isEmpty(String.valueOf(nDes_et.getText()).trim())){
                if (image_uri != null){
                    storageReference = FirebaseStorage.getInstance().getReference(MyConstants.STATIONS_IMG_LOCATION)
                            .child(mUser.getUid() + System.currentTimeMillis() + "." + getFileExtension(image_uri));
                    uploadTask = storageReference.putFile(image_uri);

                    uploadTask.continueWithTask(task -> {
                        if (!task.isSuccessful()){throw Objects.requireNonNull(task.getException());}
                        return storageReference.getDownloadUrl();

                    }).addOnCompleteListener(task -> {
                        if (task.isComplete()){
                            if (task.isSuccessful()){
                                if (mUser != null){
                                    Uri result = task.getResult();
                                    HashMap<String, String> serviceMap = new HashMap<>();
                                    serviceMap.put(MyConstants.STATION_ID, mUser.getUid());
                                    serviceMap.put("millis", millis);
                                    serviceMap.put(MyConstants.ADDED_AT, getCurrentDateTime());
                                    serviceMap.put(MyConstants.SERVICE_TITLE, getStringFromET(nTitle_et));
                                    serviceMap.put(MyConstants.SERVICE_DESC, getStringFromET(nDes_et));
                                    serviceMap.put(MyConstants.SERVICE_IMG, String.valueOf(result));
                                    servicesDocument.set(serviceMap, SetOptions.merge()).addOnCompleteListener(task1 -> {
                                        if (task1.isComplete()){
                                            if (task1.isSuccessful()){
                                                nService_progress_bar.setVisibility(View.GONE);
                                                Toast.makeText(this, "service added successfully", Toast.LENGTH_SHORT).show();
                                            }else {nService_progress_bar.setVisibility(View.GONE);}
                                        }
                                    });
                                }else {
                                    Toast.makeText(this, "you are not signed-in", Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                    });

                }else {
                    HashMap<String, String> serviceMap1 = new HashMap<>();
                    serviceMap1.put(MyConstants.STATION_ID, mUser.getUid());
                    serviceMap1.put("millis", millis);
                    serviceMap1.put(MyConstants.ADDED_AT, getCurrentDateTime());
                    serviceMap1.put(MyConstants.SERVICE_TITLE, getStringFromET(nTitle_et));
                    serviceMap1.put(MyConstants.SERVICE_DESC, getStringFromET(nDes_et));
                    servicesDocument.set(serviceMap1, SetOptions.merge()).addOnCompleteListener(task -> {
                        if (task.isComplete()){
                            if (task.isSuccessful()){
                                nService_progress_bar.setVisibility(View.GONE);
                                Toast.makeText(this, "service added successfully", Toast.LENGTH_SHORT).show();
                            }
                            else {nService_progress_bar.setVisibility(View.GONE);}
                        }
                    });
                }
            }else {
                nService_progress_bar.setVisibility(View.GONE);
                Toast.makeText(this, "Add description", Toast.LENGTH_LONG).show();
            }

        }else {
            Toast.makeText(this, "Add title", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST
        &&resultCode == RESULT_OK
        && data != null
        && data.getData() != null){
            image_uri = data.getData();
            Picasso.get().load(image_uri).into(nService_iv);
        }
    }
}