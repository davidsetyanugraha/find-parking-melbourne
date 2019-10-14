package com.unimelbs.parkingassistant;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.unimelbs.parkingassistant.model.Bay;
import com.unimelbs.parkingassistant.model.DataFeed;
import com.unimelbs.parkingassistant.model.ExtendedClusterManager;
import com.unimelbs.parkingassistant.util.PermissionManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private static final String TAG = "MapActivity";
    private static String apiKey;

    //Bottom sheet and StartParking impl
    @BindView(R.id.btn_bottom_sheet)
    Button btnBottomSheet;

    @BindView(R.id.btn_start_parking)
    Button btnStartParking;

    @BindView(R.id.bottom_sheet)
    LinearLayout layoutBottomSheet;

    BottomSheetBehavior sheetBehavior;

    //alarm stuff
    private String EVENT_DATE_TIME = "2019-12-31 10:30:00";
    private String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private LinearLayout linear_layout_1, linear_layout_2;
    private TextView tv_hour, tv_minute, tv_second;
    private Handler handler = new Handler();
    private Runnable runnable;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //todo: Add check if data exists
        setContentView(R.layout.activity_maps);
        ButterKnife.bind(this);

        initializeGoogleMapsPlacesApis();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        PermissionManager.reqPermission(this,this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        activateAutoCompleteFragment();

        //bottom sheet behavior
        sheetBehavior = BottomSheetBehavior.from(layoutBottomSheet);

        /**
         * bottom sheet state change listener
         * we are changing button text when sheet changed state
         * */
        sheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_HIDDEN:
                    case BottomSheetBehavior.STATE_DRAGGING:
                    case BottomSheetBehavior.STATE_SETTLING:
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED: {
                        btnBottomSheet.setText("Close Bay");
                    }
                    break;
                    case BottomSheetBehavior.STATE_COLLAPSED: {
                        btnBottomSheet.setText("Click Bay");
                    }
                    break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });

        initUI();
        countDownStart();

        TextView textView = (TextView)findViewById(R.id.alertDialogTextView);

        this.showCustomViewAlertDialog(textView);

    }

    private void showCustomViewAlertDialog(TextView textView) {
        final TextView textViewTmp = textView;

        Button alertDialogButton = (Button)findViewById(R.id.btn_start_parking);

        alertDialogButton.setOnClickListener(new View.OnClickListener() {

            // Store the created AlertDialog instance.
            // Because only AlertDialog has cancel method.
            private AlertDialog alertDialog = null;

            @Override
            public void onClick(View view) {
                // Create a alert dialog builder.
                final AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);

                // Set title value.
                builder.setTitle("Start Parking");

                // Get custom login form view.
                final View loginFormView = getLayoutInflater().inflate(R.layout.dialog_parking, null);
                // Set above view in alert dialog.
                builder.setView(loginFormView);

                // Continue button click listener.
                Button continueButton = (Button)loginFormView.findViewById(R.id.formContinueButton);
                continueButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        try {
                            // Close Alert Dialog.
                            alertDialog.cancel();
                            textViewTmp.setText("Parking success.");
                        }catch(Exception ex)
                        {
                            ex.printStackTrace();
                        }
                    }
                });

                // Reset button click listener.
                Button resetButton = (Button)loginFormView.findViewById(R.id.formResetButton);
                resetButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        try {
                            EditText duration = (EditText)loginFormView.findViewById(R.id.parkingFormDuration);

                            duration.setText("");
                        }catch(Exception ex)
                        {
                            ex.printStackTrace();
                        }
                    }
                });

                builder.setCancelable(true);
                alertDialog = builder.create();
                alertDialog.show();
            }
        });
    }

    /**
     * manually opening / closing bottom sheet on button click
     */
    @OnClick(R.id.btn_bottom_sheet)
    public void toggleBottomSheet() {
        if (sheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
            sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        } else {
            sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
    }

    /**
     * manually opening / closing bottom sheet on button click
     */
    @OnClick(R.id.btn_start_parking)
    public void startParking() {
        Log.d("Parking", "Start Parking!");
    }


    /**** Alarm Impl */
    private void initUI() {
        linear_layout_1 = findViewById(R.id.linear_layout_1);
        linear_layout_2 = findViewById(R.id.linear_layout_2);
        tv_hour = findViewById(R.id.tv_hour);
        tv_minute = findViewById(R.id.tv_minute);
        tv_second = findViewById(R.id.tv_second);
    }

    private void countDownStart() {
        runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    handler.postDelayed(this, 1000);
                    SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
                    Date event_date = dateFormat.parse(EVENT_DATE_TIME);
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

    /**** End of Alarm Impl */

    private void activateAutoCompleteFragment(){

        List<Place.Field> placeFields = Arrays.asList(Place.Field.ID, Place.Field.NAME,Place.Field.LAT_LNG);

        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);



        try {
            autocompleteFragment.setPlaceFields(placeFields);
        } catch (NullPointerException e) {
            Log.d(TAG, "Null Pointer Exception while setting Place Fields to Retrieve");
            e.printStackTrace();
        }

        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {

                String placeName = place.getName();
                LatLng placeLatLng = place.getLatLng();
                mMap.addMarker(new MarkerOptions().position(placeLatLng).title(placeName));
                mMap.moveCamera(CameraUpdateFactory.newLatLng(placeLatLng));
                mMap.moveCamera(CameraUpdateFactory.zoomTo(18));
                Log.i(TAG, "Place: " + placeName + ", " + place.getId()+ ", " + placeLatLng);
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.i(TAG, "An error occurred: " + status);
            }
        });

    }

    private void initializeGoogleMapsPlacesApis(){
        MapsActivity.apiKey = getResources().getString(R.string.google_maps_key);
        // Initialize the Places SDK
        Places.initialize(getApplicationContext(), apiKey);
        // Create a new Places client instance
        PlacesClient placesClient = Places.createClient(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        Log.d(TAG, "onMapReady: ");
        DataFeed data = new DataFeed(this, getApplicationContext());

        data.fetchBays();
        ExtendedClusterManager<Bay> extendedClusterManager = new ExtendedClusterManager<>(this,mMap,data);
        List<Bay> testBays = new ArrayList<>();
        double[] p1 = {-37.80556999462847,144.95412584486132};
        double[] p2 = {-37.80508340024678,144.94844545541983};
        testBays.add(new Bay(1001,p1));
        testBays.add(new Bay(1002,p2));
        Log.d(TAG, "onMapReady: data.getItem().size:"+data.getItems().size());


        //ClusterManager<Bay> clusterManager = new ClusterManager<Bay>(this,this.mMap,data);

        mMap.setOnCameraIdleListener(extendedClusterManager);
        mMap.setOnMarkerClickListener(extendedClusterManager);
        //List<Bay> itemList = data.getItems();
        //Log.d(TAG, "onMapReady: clusterItems size:"+itemList.size()+" first:"+itemList.get(0).getPosition().longitude);
        LatLng zoom = new LatLng(p1[0],p1[1]);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(zoom,10));
        extendedClusterManager.addItems(data.getItems());


        /*
        mMap.setOnCameraIdleListener(clusterManager);
        mMap.setOnMarkerClickListener(clusterManager);
        List<Bay> itemList = data.getItems();
        Log.d(TAG, "onMapReady: clusterItems size:"+itemList.size()+" first:"+itemList.get(0).getPosition().longitude);
        clusterManager.addItems(itemList);
        */

    }
}
