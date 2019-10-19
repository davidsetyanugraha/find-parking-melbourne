package com.unimelbs.parkingassistant;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.unimelbs.parkingassistant.model.Bay;
import com.unimelbs.parkingassistant.util.PreferenceManager;
import com.unimelbs.parkingassistant.util.RestrictionsHelper;

import org.apache.commons.lang3.time.DateUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

import androidx.appcompat.app.AppCompatActivity;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.unimelbs.parkingassistant.util.PreferenceManager.clearPreference;

public class ParkingActivity extends AppCompatActivity {

    //alarm stuff (deprecated)
    private String EVENT_DATE_TIME = "2019-12-31 10:30:00";
    private String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    //alarm stuff
    private static final String TAG = "ParkingActivity";
    private Bay selectedBay;

    //bottom sheet view
    @BindView(R.id.bottom_sheet_parking)
    LinearLayout layoutBottomSheet;

    @BindView(R.id.bay_title)
    TextView bayTitle;

    @BindView(R.id.bay_snippet)
    TextView baySnippet;

    @BindView(R.id.bay_status)
    TextView bayStatus;

    @BindView(R.id.bay_restriction)
    TextView bayRestriction;

    @BindView(R.id.btn_stop_parking)
    Button stopParkingButton;

    //count down view
    @BindView(R.id.linear_layout_1)
    LinearLayout linear_layout_1;

    @BindView(R.id.linear_layout_2)
    LinearLayout linear_layout_2;

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
        String hourMsg = intent.getStringExtra(MapsActivity.HOUR);

        this.prefs = getPreferences(MODE_PRIVATE);
        this.selectedBay = PreferenceManager.getBayFromSharedPreference(this.prefs);
        this.endParkingDate = PreferenceManager.getEndDateFromSharedPreference(this.prefs);

        if ((this.selectedBay == null) && (this.endParkingDate == null)) {
            Log.d(TAG, "User doesn't have ongoing parking!");
            this.selectedBay = (Bay) intent.getSerializableExtra(MapsActivity.SELECTED_BAY);
            this.endParkingDate = (Date) DateUtils.addHours(new Date(), Integer.parseInt(hourMsg));
            PreferenceManager.saveBayToSharedPreferences(this.selectedBay, this.prefs);
            PreferenceManager.saveEndDateToSharedPreferences(this.endParkingDate, this.prefs);
        } else {
            Log.d(TAG, "User is having ongoing parking!");
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
        renderBottomSheet(selectedBay);
    }

    private void renderBottomSheet(@NotNull Bay bay) {
        String bayStatusMsg = (bay.isAvailable()) ? "Available" : "Occupied";
        String position = bay.getPosition().latitude + " , " + bay.getPosition().longitude;
        String title = (bay.getTitle().isEmpty()) ? position : bay.getTitle();
        bayTitle.setText(title);
        bayStatus.setText(bayStatusMsg);

        String bayRestrictionString = RestrictionsHelper.convertRestrictionsToString(bay.getRestrictions());
        bayRestriction.setText(bayRestrictionString);
        baySnippet.setText("BayId = " + Integer.toString(bay.getBayId()));

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
                        long Hours = diff / (60 * 60 * 1000) % 24;
                        long Minutes = diff / (60 * 1000) % 60;
                        long Seconds = diff / 1000 % 60;
                        //
                        tv_hour.setText(String.format("%02d", Hours));
                        tv_minute.setText(String.format("%02d", Minutes));
                        tv_second.setText(String.format("%02d", Seconds));
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
    @OnClick(R.id.btn_stop_parking)
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

    private void goToMapsActivity() {
        Intent intent = new Intent(this, MapsActivity.class);
        startActivity(intent);
    }

}
