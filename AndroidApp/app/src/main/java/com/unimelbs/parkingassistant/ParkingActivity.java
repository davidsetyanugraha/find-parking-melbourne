/*
This activity has been deprecated
and all its functionality has been moved
to display as a user notification.
It will be deleted later on
if the development team find no
use of it.
 */


package com.unimelbs.parkingassistant;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.unimelbs.parkingassistant.model.Bay;
import com.unimelbs.parkingassistant.util.PreferenceManager;

import org.apache.commons.lang3.time.DateUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

import androidx.appcompat.app.AppCompatActivity;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.unimelbs.parkingassistant.util.PreferenceManager.PREFERENCE_NAME;
import static com.unimelbs.parkingassistant.util.PreferenceManager.clearPreference;


public class ParkingActivity extends AppCompatActivity {

    //alarm stuff (deprecated)
    private String EVENT_DATE_TIME = "2019-12-31 10:30:00";
    private String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    //alarm stuff
    private static final String TAG = "ParkingActivity";
    private Bay selectedBay;

    //bottom sheet view

    @BindView(R.id.restrictionLayout)
    LinearLayout layoutRestrictions;

    @BindView(R.id.bottom_sheet_parking)
    LinearLayout layoutBottomSheet;

    @BindView(R.id.bay_title)
    TextView bayTitle;

    @BindView(R.id.bay_status)
    TextView bayStatus;

    @BindView(R.id.btn_parking)
    Button stopParkingButton;

    @BindView(R.id.btn_direction)
    Button directionButton;


    //count down view
    @BindView(R.id.linear_layout_1)
    LinearLayout linear_layout_1;

    @BindView(R.id.linear_layout_2)
    LinearLayout linear_layout_2;

    @BindView(R.id.tv_day)
    TextView tv_day;

    @BindView(R.id.tv_hour)
    TextView tv_hour;

    @BindView(R.id.tv_minute)
    TextView tv_minute;

    @BindView(R.id.tv_second)
    TextView tv_second;

    Date endParkingDate;


    private Handler handler = new Handler();
    private Runnable runnable;

    BottomSheetBehavior sheetBehavior;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parking);

        Intent intent = getIntent();
        String secondsMsg = intent.getStringExtra(MapsActivity.SECONDS);

        this.prefs = getSharedPreferences(PREFERENCE_NAME, MODE_PRIVATE);
        if (PreferenceManager.isAvailable(this.prefs)) {
            Log.d(TAG, "User is having ongoing parking!");
            this.selectedBay = PreferenceManager.getBayFromSharedPreference(this.prefs);
            this.endParkingDate = PreferenceManager.getEndDateFromSharedPreference(this.prefs);
        } else {
            Log.d(TAG, "User doesn't have ongoing parking!");
            this.selectedBay = (Bay) intent.getSerializableExtra(MapsActivity.SELECTED_BAY);
            this.endParkingDate = (Date) DateUtils.addSeconds(new Date(), Integer.parseInt(secondsMsg));
            PreferenceManager.saveBayToSharedPreferences(this.selectedBay, this.prefs);
            PreferenceManager.saveEndDateToSharedPreferences(this.endParkingDate, this.prefs);
        }

        //ButterKnife is java version of https://developer.android.com/topic/libraries/view-binding
        ButterKnife.bind(this);

        countDownStart();

        initBottomSheetUI();
    }

    private void initBottomSheetUI() {
        //change layout
        sheetBehavior = BottomSheetBehavior.from(layoutBottomSheet);
        sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        stopParkingButton.setText("Stop Parking");

        reRenderBottomSheet(selectedBay);
    }

    private void reRenderBottomSheet(@NotNull Bay bay) {
        String bayStatusMsg = (bay.isAvailable()) ? "Available" : "Occupied";
        String position = bay.getPosition().latitude + " , " + bay.getPosition().longitude;
        String title = (bay.getTitle().isEmpty()) ? position : bay.getTitle();
        bayTitle.setText(title);
        bayStatus.setText(bayStatusMsg);

        layoutRestrictions.removeAllViews();
        for (int i = 0; i < bay.getRestrictions().size(); i++) {
            Button tv = new Button(getApplicationContext());
            tv.setText(bay.getRestrictions().get(i).getDescription());
            layoutRestrictions.addView(tv);
        }

//        String bayRestrictionString = RestrictionsHelper.convertRestrictionsToString(bay.getRestrictions());

        if (sheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
            sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    private void countDownStart() {
        runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    handler.postDelayed(this, 1000);

                    Date current_date = new Date();

                    if (!current_date.after(endParkingDate)) {
                        long diff = endParkingDate.getTime() - current_date.getTime();
                        long days = diff / (24 * 60 * 60 * 1000);
                        long hours = diff / (60 * 60 * 1000) % 24;
                        long minutes = diff / (60 * 1000) % 60;
                        long seconds = diff / 1000 % 60;
                        //
                        tv_day.setText(String.format("%02d", days));
                        tv_hour.setText(String.format("%02d", hours));
                        tv_minute.setText(String.format("%02d", minutes));
                        tv_second.setText(String.format("%02d", seconds));
                    } else {
                        linear_layout_1.setVisibility(View.VISIBLE);
                        linear_layout_2.setVisibility(View.GONE);
                        handler.removeCallbacks(runnable);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        handler.postDelayed(runnable, 0);
    }

    protected void onStop() {
        super.onStop();
        handler.removeCallbacks(runnable);
    }

    /**
     * Bottom screen Button Stop Parking OnClick
     */
    @OnClick(R.id.btn_parking)
    public void stopParking() {
        AlertDialog alertDialog;
        final AlertDialog.Builder builder = new AlertDialog.Builder(ParkingActivity.this);

        builder.setTitle("Stop Parking");
        builder.setMessage("Are you sure to Stop Parking?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        clearPreference(prefs);
                        goToMapsActivity();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Context context = getApplicationContext();
                        int duration = Toast.LENGTH_SHORT;
                        Toast toast = Toast.makeText(context, "Cancel", duration);
                        toast.show();
                    }
                });

        builder.setCancelable(true);
        alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Bottom screen Button Direction OnClick
     */
    @OnClick(R.id.btn_direction)
    public void direction() {


        try
        {
            walkToTheSelectedBay();

        } catch (Exception e) {
            Toast.makeText(this, "Unable to show walking direction to parked car ", Toast.LENGTH_LONG).show();
        }
    }

    private void goToMapsActivity() {
        //Intent intent = new Intent(this, MapsActivity.class);
        //startActivity(intent);

        Intent openMapsActivity = new Intent(ParkingActivity.this, MapsActivity.class);
        openMapsActivity.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivityIfNeeded(openMapsActivity, 0);
    }

    private void walkToTheSelectedBay()
    {
        LatLng selectedBayLatLng = this.selectedBay.getPosition();
        String lat = String.valueOf(selectedBayLatLng.latitude);
        String lon = String.valueOf(selectedBayLatLng.longitude);


        // Part of the code below is taken from
        // https://stackoverflow.com/questions/
        // 2662531/launching-google-maps-directions
        // -via-an-intent-on-android?rq=1

        Uri walkIntentUri = Uri.parse("google.navigation:q=" + lat + "," + lon+"&mode=w");
        Log.d("Walk Uri", "Walk URI is " + walkIntentUri);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, walkIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");


        try {
            startActivity(mapIntent);

        } catch (ActivityNotFoundException ex) {
            try {
                Intent unrestrictedIntent = new Intent(Intent.ACTION_VIEW, walkIntentUri);
                startActivity(unrestrictedIntent);
            } catch (ActivityNotFoundException innerEx) {
                Toast.makeText(this, "No Map Application Found ", Toast.LENGTH_LONG).show();

            }
        }
    }

    @Override
    public void onBackPressed() {
        // Codes added with support
        // from StackOVerFlow :)
        int count = getSupportFragmentManager().getBackStackEntryCount();
        // Bottom sheet has not been defined as fragment
        // So count should be zero
        if (count == 0) {
            if (sheetBehavior!=null && sheetBehavior.getState() !=
                    BottomSheetBehavior.STATE_HIDDEN) {
                sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

            }
            // Double back press to exit app
            else {
                super.onBackPressed();

            }
            //sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        } else {
            getSupportFragmentManager().popBackStack();
        }

    }

}
