package com.gmail.station;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;

import android.Manifest;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class StationFormActivity extends AppCompatActivity {

    private static final String START_TAG = "start_time";
    private static final String END_TAG = "end_time";
    private static final int PERMISSION_TO_GET_LOCATION = 100;

    private Toolbar nForm_toolbar;
    private ScrollView nParent_layout;
    private TextView nForm_hint_tv;
    private EditText nStation_name_et;
    private TextView nDays_tv;
    private Button nSet_days_btn;
    private TextView nHours_tv;
    private Button nSet_hours_btn;
    private EditText nStation_address_et;
    private Button nGet_location_btn;
    private Button nApply_btn;

    private FirebaseFirestore firestore;
    private CollectionReference workingSchCollection;

    private FirebaseUser mUser;

    // DAY DIALOG
    private CheckBox nAll_day_cb;
    private CheckBox nSun_cb;
    private CheckBox nMon_cb;
    private CheckBox nTue_cb;
    private CheckBox nWed_cb;
    private CheckBox nThu_cb;
    private CheckBox nFri_cb;
    private CheckBox nSat_cb;
    // TIME DIALOG
    private TextView nTime_start_tv;
    private TextView nTime_end_tv;

    private FusedLocationProviderClient fusedLocationProviderClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_station_form);
        init();
        showToolbar();
        new MyTouchEvent(this).collapseKeyboard(nParent_layout);

        nSet_days_btn.setOnClickListener(view -> daysDialog());
        nSet_hours_btn.setOnClickListener(view -> timeDialog());
        nGet_location_btn.setOnClickListener(view -> getStationLocationInfo());
        nApply_btn.setOnClickListener(view -> sendStationInfoToFirestore());

        displayWorkingHours();
        displayWorkingDays();
        displayInfoInHints();
    }

    private void init() {
        nForm_toolbar = findViewById(R.id.form_toolbar);
        nParent_layout = findViewById(R.id.parent_layout);
        nForm_hint_tv = findViewById(R.id.form_hint_tv);
        nStation_name_et = findViewById(R.id.station_name_et);
        nDays_tv = findViewById(R.id.days_tv);
        nSet_days_btn = findViewById(R.id.set_days_btn);
        nHours_tv = findViewById(R.id.hours_tv);
        nSet_hours_btn = findViewById(R.id.set_hours_btn);
        nStation_address_et = findViewById(R.id.station_address_et);
        nGet_location_btn = findViewById(R.id.get_location_btn);
        nApply_btn = findViewById(R.id.apply_btn);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mUser = mAuth.getCurrentUser();

        firestore = FirebaseFirestore.getInstance();
        workingSchCollection = firestore.collection(MyConstants.STATION_COLLECTION)
                .document(mUser.getUid())
                .collection(MyConstants.WORKING_SCH_COLLECTION);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
    }

    private void showToolbar() {
        setSupportActionBar(nForm_toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        nForm_hint_tv.setText("Fill Later!");
    }

    private void displayInfoInHints() {
        firestore.collection(MyConstants.STATION_COLLECTION).document(mUser.getUid())
                .addSnapshotListener((value, error) -> {
            if (Objects.requireNonNull(value).exists()){
                if (value.get(MyConstants.STATION_ADDRESS) != null){
                    String address = String.valueOf(value.get(MyConstants.STATION_ADDRESS));
                    nStation_address_et.setHint(address);
                }
                if (value.get(MyConstants.STATION_NAME) != null){

                    String name = String.valueOf(value.get(MyConstants.STATION_NAME));
                    nStation_name_et.setHint(name);
                }
            }
        });
    }

    private void sendStationInfoToFirestore() {
        HashMap<String, String > stationMap = new HashMap<>();
        if (!TextUtils.isEmpty(nStation_address_et.getText().toString().trim())
                &&!TextUtils.isEmpty(nStation_name_et.getText().toString().trim())){
            stationMap.put("station_name", getStringFromET(nStation_name_et));
            stationMap.put("station_address", getStringFromET(nStation_address_et));
            firestore.collection(MyConstants.STATION_COLLECTION).document(mUser.getUid())
                    .set(stationMap,SetOptions.merge()).addOnCompleteListener(task -> {
                        if (task.isComplete()){
                            if (task.isSuccessful()){
                                Toast.makeText(StationFormActivity.this,
                                        "Form completed successfully",
                                        Toast.LENGTH_LONG).show();
                                Intent mainIntent = new Intent(StationFormActivity.this, MainActivity.class);
                                startActivity(mainIntent);
                                finish();
                            }
                        }
            });
        }else {
            Toast.makeText(this, "check your inputs", Toast.LENGTH_LONG).show();
        }
    }
    private String getStringFromET(EditText editText){
        return String.valueOf(editText.getText()).trim();
    }

    private void getStationLocationInfo() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            fusedLocationProviderClient.getLastLocation().addOnCompleteListener(task -> {
                Location location = task.getResult();
                if (location != null) {
                    try {
                        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                        List<Address> addresses = geocoder.getFromLocation(
                                location.getLatitude(), location.getLongitude(), 1);
                        String addressLine = addresses.get(0).getAddressLine(0);
                        nStation_address_et.setText(addressLine);

                        firestore.collection(MyConstants.STATION_COLLECTION).document(mUser.getUid());
                        HashMap<String, Double> usersMap = new HashMap<>();
                        usersMap.put(MyConstants.LATITUDE, location.getLatitude());
                        usersMap.put(MyConstants.LONGITUDE, location.getLongitude());

                        firestore.collection(MyConstants.STATION_COLLECTION).document(mUser.getUid())
                                .set(usersMap, SetOptions.merge()).addOnCompleteListener(task1 -> {
                            if (task1.isComplete()) {
                                if (!task1.isSuccessful()) {
                                    Toast.makeText(StationFormActivity.this,
                                            "" + Objects.requireNonNull(task1.getException()).getMessage(),
                                            Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSION_TO_GET_LOCATION);
        }
    }

    private void daysDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.day_picker_dialog, null, false);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        nAll_day_cb = view.findViewById(R.id.all_day_cb);
        nSun_cb = view.findViewById(R.id.sun_cb);
        nMon_cb = view.findViewById(R.id.mon_cb);
        nTue_cb = view.findViewById(R.id.tue_cb);
        nWed_cb = view.findViewById(R.id.wed_cb);
        nThu_cb = view.findViewById(R.id.thu_cb);
        nFri_cb = view.findViewById(R.id.fri_cb);
        nSat_cb = view.findViewById(R.id.sat_cb);

        Button nDy_ok_btn = view.findViewById(R.id.dy_ok_btn);
        Button nDy_cancel_btn = view.findViewById(R.id.dy_cancel_btn);

        checkboxListener(nAll_day_cb);
        checkboxListener(nSun_cb);
        checkboxListener(nMon_cb);
        checkboxListener(nTue_cb);
        checkboxListener(nWed_cb);
        checkboxListener(nThu_cb);
        checkboxListener(nFri_cb);
        checkboxListener(nSat_cb);


        nDy_ok_btn.setOnClickListener(view1 ->
                firestore.collection(MyConstants.STATION_COLLECTION)
                .document(mUser.getUid())
                .collection(MyConstants.WORKING_SCH_COLLECTION)
                .document(MyConstants.DAY_DOC).get().addOnCompleteListener(task -> {
            if (task.isComplete()){ if (task.isSuccessful()){ dialog.dismiss(); } }
        }));
        nDy_cancel_btn.setOnClickListener(view1 -> { /*if (daysList.size() > 0){ daysList.clear();}*/ dialog.dismiss(); });
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
    }
    private void checkboxListener(CheckBox checkBox) {
        checkBox.setOnCheckedChangeListener((compoundButton, checked) -> {
            if (checkBox.getId() == R.id.all_day_cb){
                checkAllBoxes(checked);
            }
            if (
                    nSun_cb.isChecked()||
                    nMon_cb.isChecked() ||
                    nTue_cb.isChecked() ||
                    nWed_cb.isChecked() ||
                    nThu_cb.isChecked() ||
                    nFri_cb.isChecked() ||
                    nSat_cb.isChecked()
            )
            {
                nAll_day_cb.setChecked(false);
            }
            daysChecker(nSun_cb);
            daysChecker(nMon_cb);
            daysChecker(nTue_cb);
            daysChecker(nWed_cb);
            daysChecker(nThu_cb);
            daysChecker(nFri_cb);
            daysChecker(nSat_cb);
        });
    }
    private void daysChecker(CheckBox checkBox) {
        HashMap<String, String> daysMap = new HashMap<>();
        if (checkBox.isChecked()){
            daysMap.put(String.valueOf(checkBox.getText()), "ON");
        }if (!checkBox.isChecked()){
            daysMap.put(String.valueOf(checkBox.getText()), "OFF");
        }
        workingSchCollection.document(MyConstants.DAY_DOC).set(daysMap, SetOptions.merge());
    }
    private void displayWorkingDays(){
        workingSchCollection.document(MyConstants.DAY_DOC).addSnapshotListener((value, error) -> {
            String sunday = String.valueOf(value.get(MyConstants.SUN));
            String monday = String.valueOf(value.get(MyConstants.MON));
            String tuesday = String.valueOf(value.get(MyConstants.TUE));
            String wednesday = String.valueOf(value.get(MyConstants.WED));
            String thursday = String.valueOf(value.get(MyConstants.THU));
            String friday = String.valueOf(value.get(MyConstants.FRI));
            String saturday = String.valueOf(value.get(MyConstants.SAT));
            String[] onDays = new String[7];

            if (sunday.equals("ON")) {onDays[0] = MyConstants.SUN+", ";}
            else if (sunday.equals("OFF")) {onDays[0] = "";}
            if (monday.equals("ON")) {onDays[1] = MyConstants.MON+", ";}
            else if (monday.equals("OFF")) {onDays[1] = "";}
            if (tuesday.equals("ON")) {onDays[2] = MyConstants.TUE+", ";}
            else if (tuesday.equals("OFF")) {onDays[2] = "";}
            if (wednesday.equals("ON")) {onDays[3] = MyConstants.WED+", ";}
            else if (wednesday.equals("OFF")) {onDays[3] = "";}
            if (thursday.equals("ON")) {onDays[4] = MyConstants.THU+", ";}
            else if (thursday.equals("OFF")) {onDays[4] = "";}
            if (friday.equals("ON")) {onDays[5] = MyConstants.FRI+", ";}
            else if (friday.equals("OFF")) {onDays[5] = "";}
            if (saturday.equals("ON")) {onDays[6] = MyConstants.SAT;}
            else if (saturday.equals("OFF")) {onDays[6] = "";}
            if (!sunday.equals("null") && !monday.equals("null") && !tuesday.equals("null") && !wednesday.equals("null")
                    && !thursday.equals("null") && !friday.equals("null") && !saturday.equals("null")){
                nDays_tv.setText(onDays[0]+onDays[1]+onDays[2]+onDays[3]+onDays[4]+onDays[5]+onDays[6]);
            }
        });
    }
    private void checkAllBoxes(boolean checked) {
        nSun_cb.setChecked(checked);
        nMon_cb.setChecked(checked);
        nTue_cb.setChecked(checked);
        nWed_cb.setChecked(checked);
        nThu_cb.setChecked(checked);
        nFri_cb.setChecked(checked);
        nSat_cb.setChecked(checked);
    }

    private void timeDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.time_dialog, null, false);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        nTime_start_tv = view.findViewById(R.id.start_time_tv);
        nTime_end_tv = view.findViewById(R.id.end_time_tv);
        TextView nTime_apply_btn = view.findViewById(R.id.time_apply_btn);
        Button nTime_start_btn = view.findViewById(R.id.time_start_btn);
        Button nTime_end_btn = view.findViewById(R.id.time_end_btn);

        nTime_start_btn.setOnClickListener(view1 -> setHourDialog(nTime_start_tv, nTime_end_tv, START_TAG));
        nTime_end_btn.setOnClickListener(view2 -> setHourDialog(nTime_end_tv, nTime_end_tv, END_TAG));
        nTime_apply_btn.setOnClickListener(view3 -> {
            if (nTime_start_tv.getText().equals(getString(R.string.start_at_0_0))){
                Toast.makeText(this, "set start time", Toast.LENGTH_SHORT).show();
            }else if (nTime_end_tv.getText().equals(getString(R.string.end_at_0_0))){
                Toast.makeText(this, "set end time", Toast.LENGTH_SHORT).show();
            }else {
                sendWorkingHoursToFirebase(String.valueOf(nTime_start_tv.getText()), String.valueOf(nTime_end_tv.getText()));
                dialog.dismiss();
            }
        });

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
    }
    private void setHourDialog(TextView nTime_start_tv, TextView nTime_end_tv, String tag) {
        DialogFragment newFragment = new TimePickerFragment(nTime_start_tv, nTime_end_tv, tag);
        newFragment.show(getSupportFragmentManager(), tag);
    }
    public void sendWorkingHoursToFirebase(String start, String end) {
        HashMap<String, String> hoursMap = new HashMap<>();
        hoursMap.put(MyConstants.START_KEY, start);
        hoursMap.put(MyConstants.END_KEY, end);
        workingSchCollection.document(MyConstants.HOURS_DOC).set(hoursMap, SetOptions.merge());
    }
    public void sendStartEndHoursToFirebase(String startOrEndHr, int hour, String startOrEndMin, double minute) {
        HashMap<String, Double> hoursMap = new HashMap<>();
        hoursMap.put(startOrEndHr, (double) hour);
        hoursMap.put(startOrEndMin, minute);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        FirebaseFirestore firestore1 = FirebaseFirestore.getInstance();
        if (user != null){
            firestore1.collection(MyConstants.STATION_COLLECTION)
                    .document(user.getUid())
                    .collection(MyConstants.WORKING_SCH_COLLECTION)
                    .document(MyConstants.HOURS_DOC).set(hoursMap, SetOptions.merge());
        }else {
            Toast.makeText(this, user+" = null", Toast.LENGTH_LONG).show();
        }
    }
    private void displayWorkingHours() {
        workingSchCollection.document(MyConstants.HOURS_DOC).addSnapshotListener((value, error) -> {
            if (Objects.requireNonNull(value).exists()){
                String startValue = String.valueOf(value.get(MyConstants.START_KEY));
                String endValue = String.valueOf(value.get(MyConstants.END_KEY));
                nHours_tv.setText(startValue+" - "+endValue);
            }
        });
    }
    public static class TimePickerFragment extends DialogFragment
            implements TimePickerDialog.OnTimeSetListener {
        final TextView start;
        final TextView end;
        final String tag;
        public TimePickerFragment(TextView start, TextView end, String tag) {
            this.start = start;
            this.end = end;
            this.tag = tag;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            final Calendar c = Calendar.getInstance();
            int hour = c.get(Calendar.HOUR_OF_DAY);
            int minute = c.get(Calendar.MINUTE);

            return new TimePickerDialog(getActivity(), this, hour, minute,
                    DateFormat.is24HourFormat(getActivity()));
        }
        private int convert24to12(int hr){
            if (hr > 12){
                return hr - 12;
            }else if (hr == 0){
                return hr+12;
            }
                else return hr;
        }
        private double convertMinuteToDecimal(int minute){
            if (minute >= 0 && minute <= 9){
                return minute * 0.1;
            }else{
                return Double.parseDouble(new DecimalFormat("#.##").format(minute * 0.01));
            }
        }
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            String timeFormat = convert24to12(hourOfDay) + ":" + minute;
            String startTag = getFragmentManager().findFragmentByTag(tag).getTag();
            if (startTag.equals(START_TAG)){
                new StationFormActivity().sendStartEndHoursToFirebase(MyConstants.START_HR, hourOfDay,
                        MyConstants.START_MIN, minute);
                if (hourOfDay >= 0 && hourOfDay <= 11){ start.setText("start at "+timeFormat+" A.M."); }
                if (hourOfDay >= 12 && hourOfDay <= 24){ start.setText("start at "+timeFormat+" P.M."); }
            }
            if (startTag.equals(END_TAG)){
                new StationFormActivity().sendStartEndHoursToFirebase(MyConstants.END_HR, hourOfDay,
                        MyConstants.END_MIN, minute);
                if (hourOfDay >= 0 && hourOfDay <= 11){ end.setText("end at "+timeFormat+" A.M."); }
                if (hourOfDay >= 12 && hourOfDay <= 24){ end.setText("end at "+timeFormat+" P.M."); }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_TO_GET_LOCATION
                && (grantResults.length > 0)
                && (grantResults[0] == PackageManager.PERMISSION_GRANTED)){
            getStationLocationInfo();
        }
        else {
            Toast.makeText(StationFormActivity.this,
                    "Failed to get permission\nPlease try again",
                    Toast.LENGTH_LONG).show();
        }
    }
}