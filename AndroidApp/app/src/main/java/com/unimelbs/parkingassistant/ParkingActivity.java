package com.unimelbs.parkingassistant;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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

import org.apache.commons.lang3.time.DateUtils;

import java.util.Date;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ParkingActivity extends AppCompatActivity {

    //alarm stuff (deprecated)
    private String EVENT_DATE_TIME = "2019-12-31 10:30:00";
    private String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    //alarm stuff

    private static final String TAG = "ParkingActivity";
    private Bay selectedBay;
    
    @BindView(R.id.bottom_sheet_parking)
    LinearLayout layoutBottomSheet;

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

    @BindView(R.id.btn_stop_parking)
    Button stopParkingButton;

    private Handler handler = new Handler();
    private Runnable runnable;

    BottomSheetBehavior sheetBehavior;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parking);

        Intent intent = getIntent();
        String message = intent.getStringExtra(MapsActivity.HOUR);
        this.selectedBay = (Bay) intent.getSerializableExtra(MapsActivity.SELECTED_BAY);

        Log.d(TAG,"park at: "+selectedBay.getTitle());

        //ButterKnife is java version of https://developer.android.com/topic/libraries/view-binding
        ButterKnife.bind(this);

        countDownStart(Integer.parseInt(message));

        initBottomSheetUI();
    }

    private void initBottomSheetUI() {

        sheetBehavior = BottomSheetBehavior.from(layoutBottomSheet);

        sheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_HIDDEN:
                    case BottomSheetBehavior.STATE_DRAGGING:
                    case BottomSheetBehavior.STATE_SETTLING:
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED: {
                        Log.d("sheetBehavior", "expanded");
                    }
                    break;
                    case BottomSheetBehavior.STATE_COLLAPSED: {
                        Log.d("sheetBehavior", "collapsed");
                    }
                    break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            }
        });
    }

    private void countDownStart(int hour) {
        Date event_date = DateUtils.addHours(new Date(), hour);

        runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    handler.postDelayed(this, 1000);

                    Date current_date = new Date();

                    if (!current_date.after(event_date)) {
                        long diff = event_date.getTime() - current_date.getTime();
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
