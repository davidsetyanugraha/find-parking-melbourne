package com.unimelbs.parkingassistant;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    public static final String EXTRA_HOUR = "com.unimelbs.parkingassistant.HOUR";
    private static final String TAG = "MapActivity";
    private static String apiKey;

    //Bottom sheet and StartParking impl
    @BindView(R.id.btn_parking_bay)
    Button btnBottomSheet;

    @BindView(R.id.bottom_sheet)
    LinearLayout layoutBottomSheet;

    @BindView(R.id.btn_direction)
    Button direction;

    @BindView(R.id.btn_start_parking)
    Button startParkingButton;

    BottomSheetBehavior sheetBehavior;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //todo: Add check if data exists
        setContentView(R.layout.activity_maps);

        //ButterKnife is java version of https://developer.android.com/topic/libraries/view-binding
        ButterKnife.bind(this);

        initializeGoogleMapsPlacesApis();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        PermissionManager.reqPermission(this, this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        activateAutoCompleteFragment();

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
    }

    private void triggerIntent(String hour) {
        Intent intent = new Intent(this, ParkingActivity.class);
        intent.putExtra(EXTRA_HOUR, hour);
        startActivity(intent);
    }


    /**
     * OnClickLocation ParkingBay
     */
    @OnClick(R.id.btn_parking_bay)
    public void onClickParkingBay() {
        //todo: add onClick Parking bay
        if (sheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
            sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        } else {
            sheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
    }

    /**
     * Bottom screen Button Start Parking OnClick
     */
    @OnClick(R.id.btn_direction)
    public void direction() {
        //todo: Add Direction Impl from other Service
        Log.d("Direction", "direction button clicked");
    }

    /**
     * Bottom screen Button Start Parking OnClick
     */
    @OnClick(R.id.btn_start_parking)
    public void startParking() {
        AlertDialog alertDialog;
        final AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
        final View startParkingFormView = getLayoutInflater().inflate(R.layout.dialog_parking, null);
        Button continueButton = startParkingFormView.findViewById(R.id.formContinueButton);

        builder.setTitle("Start Parking");
        builder.setView(startParkingFormView);

        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    EditText hour = startParkingFormView.findViewById(R.id.parkingFormDuration);
                    String strHour = hour.getText().toString();
                    triggerIntent(strHour);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        Button resetButton = startParkingFormView.findViewById(R.id.formResetButton);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    EditText duration = startParkingFormView.findViewById(R.id.parkingFormDuration);
                    duration.setText("");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        builder.setCancelable(true);
        alertDialog = builder.create();
        alertDialog.show();
    }


    private void activateAutoCompleteFragment() {

        List<Place.Field> placeFields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG);

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
                Log.i(TAG, "Place: " + placeName + ", " + place.getId() + ", " + placeLatLng);
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.i(TAG, "An error occurred: " + status);
            }
        });

    }

    private void initializeGoogleMapsPlacesApis() {
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
        ExtendedClusterManager<Bay> extendedClusterManager = new ExtendedClusterManager<>(this, mMap, data);
        List<Bay> testBays = new ArrayList<>();
        double[] p1 = {-37.80556999462847, 144.95412584486132};
        double[] p2 = {-37.80508340024678, 144.94844545541983};
        testBays.add(new Bay(1001, p1));
        testBays.add(new Bay(1002, p2));
        Log.d(TAG, "onMapReady: data.getItem().size:" + data.getItems().size());


        //ClusterManager<Bay> clusterManager = new ClusterManager<Bay>(this,this.mMap,data);

        mMap.setOnCameraIdleListener(extendedClusterManager);
        mMap.setOnMarkerClickListener(extendedClusterManager);
        //List<Bay> itemList = data.getItems();
        //Log.d(TAG, "onMapReady: clusterItems size:"+itemList.size()+" first:"+itemList.get(0).getPosition().longitude);
        LatLng zoom = new LatLng(p1[0], p1[1]);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(zoom, 10));
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
