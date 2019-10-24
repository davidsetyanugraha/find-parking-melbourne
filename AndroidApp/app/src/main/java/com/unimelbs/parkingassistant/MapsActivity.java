package com.unimelbs.parkingassistant;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.maps.android.clustering.ClusterManager;
import com.unimelbs.parkingassistant.model.Bay;
import com.unimelbs.parkingassistant.model.DataFeed;
import com.unimelbs.parkingassistant.ui.BayRenderer;
import com.unimelbs.parkingassistant.util.PermissionManager;
import com.unimelbs.parkingassistant.util.PreferenceManager;
import com.unimelbs.parkingassistant.util.RestrictionsHelper;

import org.apache.commons.lang3.SerializationUtils;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.unimelbs.parkingassistant.util.PreferenceManager.PREFERENCE_NAME;

public class MapsActivity extends AppCompatActivity
        implements OnMapReadyCallback,
        ClusterManager.OnClusterItemClickListener<Bay> {

    private GoogleMap mMap;
    public static final String SECONDS = "com.unimelbs.parkingassistant.SECONDS";
    public static final String SELECTED_BAY = "com.unimelbs.parkingassistant.selectedBay";
    private static final String TAG = "MapActivity";
    private static String apiKey;
    private Bay selectedBay;
    private DataFeed data;

    BayUpdateService bayUpdateService;
    boolean bayUpdateServiceBound = false;
    int year, month, day, hour, minute;

    private boolean mLocationPermissionGranted;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private Location mLastKnownLocation;
    private final LatLng mDefaultLocation = new LatLng(-33.8523341, 151.2106085);
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

    private static final int ZOOM_DEFAULT = 15;
    private static final int ZOOM_CURRENTLOCATION = 15;
    private static final int ZOOM_PLACE = 18;
    private static final int ZOOM_BAY = 20;

    @BindView(R.id.restrictionLayout)
    LinearLayout layoutRestrictions;

    //Bottom sheet and StartParking impl
    @BindView(R.id.bottom_sheet_maps)
    LinearLayout layoutBottomSheet;

    @BindView(R.id.bay_title)
    TextView bayTitle;

    @BindView(R.id.bay_snippet)
    TextView baySnippet;

    @BindView(R.id.bay_status)
    TextView bayStatus;

    @BindView(R.id.btn_direction)
    Button direction;

    @BindView(R.id.btn_parking)
    Button startParkingButton;

    BottomSheetBehavior sheetBehavior;
    SharedPreferences prefs;
    RestrictionsHelper restrictionsHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(data==null)
        {
            Log.d(TAG, "onCreate: data is null, creating new object.");
            data = new DataFeed(getApplicationContext());
            data.loadData();
        }

        // Bind to BayUpdateService
        bindToBayUpdateService();

        setContentView(R.layout.activity_maps);

        //ButterKnife is java version of https://developer.android.com/topic/libraries/view-binding
        ButterKnife.bind(this);

        initializeGoogleMapsPlacesApis();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
//        PermissionManager.reqPermission(this, this);
        // Prompt the user for permission.
        getLocationPermission();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        activateAutoCompleteFragment();

        initBottomSheetUI();

        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

    }


    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // We've bound to BayUpdateServiceService, cast the IBinder and get service instance
            BayUpdateService.bayUpdateServiceBinder binder = (BayUpdateService.bayUpdateServiceBinder) service;
            bayUpdateService = binder.getService();
            bayUpdateServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

            // Should not dispose the
            // subscription here.
            // Should only be disposed
            // when asked or when the service stops.
            bayUpdateServiceBound = false;


        }
    };

    private void showAlertDialog(String title, String question, DialogInterface.OnClickListener yesListener, DialogInterface.OnClickListener noListener) {
        AlertDialog alertDialog;
        final AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);

        builder.setTitle(title);
        builder.setMessage(question)
                .setPositiveButton("Yes", yesListener)
                .setNegativeButton("No", noListener);

        builder.setCancelable(true);
        alertDialog = builder.create();
        alertDialog.show();
    }



    /** Defines callbacks for service binding, passed to bindService() */








    private void initBottomSheetUI() {
        sheetBehavior = BottomSheetBehavior.from(layoutBottomSheet);
        sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    private void goToParkingActivity() {
        Intent intent = new Intent(this, ParkingActivity.class);
        startActivity(intent);
    }

    private void goToParkingActivity(String seconds) {
        Log.d(TAG, "GOTOPARKING");
        Intent intent = new Intent(this, ParkingActivity.class);
        intent.putExtra(SECONDS, seconds);
        intent.putExtra(SELECTED_BAY, selectedBay);
        startActivity(intent);
    }

    /**
     * Bottom screen Button Direction OnClick
     */
    @OnClick(R.id.btn_direction)
    public void direction() {
        //todo: Add Direction Impl from other Service
        Log.d("Direction", "direction button clicked");

        if(this.selectedBay.isAvailable())
        {
            bayUpdateService.navigateToTheSelectedBayWithSubscription(this.selectedBay, true);

        }
        else
            {

            AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);

            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // In this version of app we do not support
                    // occupied to present status switch over.
                    // Hence if a bay is occupied, we wont monitor it.
                    bayUpdateService.navigateToTheSelectedBayWithSubscription(selectedBay, false);

                }
            });
            builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User cancelled the dialog
                }
            });
            builder.setMessage("The Bay is occupied. Do you Still want to Navigate?")
                    .setTitle("Bay Status");


            AlertDialog dialog = builder.create();
            dialog.show();

            //Toast.makeText(this, "Selected Bay Is Occupied.", Toast.LENGTH_LONG).show();

        }

    }


    @Override
    protected void onDestroy() {
        // Should not dispose the
        // subscription here.
        // Should only be disposed
        // when asked or when the service stops.
        unbindService(connection);
        bayUpdateServiceBound = false;
        //data.saveBaysToFile();

        super.onDestroy();
        Log.d("MapActivityDestroy", "Map Activity On Destroy Has Been Called");
    }
    @Override
    protected void onPause()
    {
        Log.d(TAG, "onPause: ");
        super.onPause();
        data.saveBaysToFile();
    }
    @Override
    protected void onStart() {
        Log.d(TAG, "onStart: ");
        super.onStart();
    }

    @Override
    protected void onResume(){
        Log.d(TAG, "onResume: ");
        super.onResume();
    }

    @Override
    protected void onStop()
    {
        //data.saveBaysToFile();
        super.onStop();
    }

    /**
     * Bottom screen Button Start Parking OnClick
     */
    @OnClick(R.id.btn_parking)
    public void startParking() {
        //bayStatusChangeNotification();

        renderParkingDialog();

    }

    private void renderParkingDialog() {
        AlertDialog alertDialog;
        final AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
        final View startParkingFormView = getLayoutInflater().inflate(R.layout.dialog_parking, null);
        Button continueButton = startParkingFormView.findViewById(R.id.formSubmitButton);

        Date currentTime = new Date();
        builder.setTitle("Set return time");
        builder.setView(startParkingFormView);
        alertDialog = builder.create();

        int mYear = Integer.parseInt(new SimpleDateFormat("yyyy").format(currentTime));
        int mMonth = Integer.parseInt(new SimpleDateFormat("MM").format(currentTime));
        int mDay = Integer.parseInt(new SimpleDateFormat("dd").format(currentTime));
        int mHour = Integer.parseInt(new SimpleDateFormat("HH").format(currentTime));
        int mMinute = Integer.parseInt(new SimpleDateFormat("mm").format(currentTime));

        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.YEAR, year);
                    cal.set(Calendar.MONTH, month);
                    cal.set(Calendar.DAY_OF_MONTH, day);
                    cal.set(Calendar.HOUR_OF_DAY, hour);
                    cal.set(Calendar.MINUTE, minute);

                    Date toDate = cal.getTime();
                    Log.d(TAG, new SimpleDateFormat("dd-M-yyyy hh:mm:ss").format(toDate));
                    Log.d(TAG, new SimpleDateFormat("dd-M-yyyy hh:mm:ss").format(currentTime));

                    long diffInMillies = toDate.getTime() - currentTime.getTime();
                    Long seconds = TimeUnit.SECONDS.convert(diffInMillies,TimeUnit.MILLISECONDS);

                    this.processValidation(currentTime, toDate, seconds);
                    Log.d(TAG, "diff (seconds) =" +seconds);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            private void processValidation(Date currentTime, Date toDate, Long seconds) {
                if (seconds <= 0) {
                    Toast.makeText(getApplicationContext(),
                            "Invalid Duration",
                            Toast.LENGTH_LONG).show();
                } else {
                    restrictionsHelper.processRestrictionChecking(seconds, currentTime, toDate);
                    if (! restrictionsHelper.isValid()){
                        DialogInterface.OnClickListener yesListener = new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                String strSeconds = seconds.toString();
                                goToParkingActivity(strSeconds);
                            }
                        };

                        DialogInterface.OnClickListener noListener = new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        };
                        String invalidReason = restrictionsHelper.getInvalidReason();
                        showAlertDialog("Violation Warning",  invalidReason +" \n \n  Are you sure to continue?", yesListener, noListener);
                    } else {

                        String strSeconds = seconds.toString();
                        goToParkingActivity(strSeconds);

                    }
                }
            }
        });

        Button cancelButton = startParkingFormView.findViewById(R.id.formCancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    alertDialog.cancel();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        Button selectDateButton = startParkingFormView.findViewById(R.id.btn_date);
        EditText txtDate = startParkingFormView.findViewById(R.id.in_date);
        selectDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatePickerDialog datePickerDialog = new DatePickerDialog(MapsActivity.this,
                        new DatePickerDialog.OnDateSetListener() {

                            @Override
                            public void onDateSet(DatePicker view, int year,
                                                  int monthOfYear, int dayOfMonth) {

                                txtDate.setText(dayOfMonth + "-" + (monthOfYear + 1) + "-" + year);
                                day = dayOfMonth;
                                month = monthOfYear;
                                MapsActivity.this.year = year;
                            }
                        }, mYear, mMonth, mDay);
                datePickerDialog.show();
            }
        });

        Button selectTimeButton = startParkingFormView.findViewById(R.id.btn_time);
        EditText txtTime = startParkingFormView.findViewById(R.id.in_time);
        selectTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Launch Time Picker Dialog
                TimePickerDialog timePickerDialog = new TimePickerDialog(MapsActivity.this,
                        new TimePickerDialog.OnTimeSetListener() {

                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay,
                                                  int minute) {

                                String am_pm = "";

                                Calendar datetime = Calendar.getInstance();
                                datetime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                datetime.set(Calendar.MINUTE, minute);

                                if (datetime.get(Calendar.AM_PM) == Calendar.AM)
                                    am_pm = "AM";
                                else if (datetime.get(Calendar.AM_PM) == Calendar.PM)
                                    am_pm = "PM";

                                String strHrsToShow = (datetime.get(Calendar.HOUR) == 0) ?"12":datetime.get(Calendar.HOUR)+"";

                                txtTime.setText(strHrsToShow + ":" + String.format("%02d", minute) + " " + am_pm);
                                hour = hourOfDay;
                                MapsActivity.this.minute = minute;
                            }
                        }, mHour, mMinute, false);
                timePickerDialog.show();

            }
        });

        builder.setCancelable(false);
        alertDialog.show();
    }


    private void activateAutoCompleteFragment() {

        List<Place.Field> placeFields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG);

        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        autocompleteFragment.setCountry("AU");


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
        //DataFeed data = new DataFeed(getApplicationContext());
        //ExtendedClusterManager<Bay> extendedClusterManager = new ExtendedClusterManager<>(this, mMap, data);
        ClusterManager<Bay> extendedClusterManager = new ClusterManager<>(this,mMap);

        //Added this to improve performance.
        extendedClusterManager.setAnimation(false);
        extendedClusterManager.setRenderer(new BayRenderer(this,
                mMap,
                extendedClusterManager,
                data));
        data.setClusterManager(extendedClusterManager);



        mMap.setOnCameraIdleListener(extendedClusterManager);

        mMap.setOnMarkerClickListener(extendedClusterManager);

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng arg0) {
                Log.d(TAG, "onMapClick");
                if (sheetBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                }
            }
        });


        extendedClusterManager.addItems(data.getItems());
        extendedClusterManager.setOnClusterItemClickListener(this);

        mMap.getUiSettings().setZoomControlsEnabled(true);
        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        // Go to the first position depending on if the user is starting the application or it is
        // returning from another session
        Bay focusPoint;
        int zoomLevel = ZOOM_DEFAULT ;
        if (data.getItems().size() > 0) {
            focusPoint = data.getItems().get(0);

            // When resuming from a previously
            // selected bay, zoom to previously
            // selected bay.
            if(BayUpdateService.selectedBayId != null){
                focusPoint = BayUpdateService.selectedBayId;
                zoomLevel = ZOOM_PLACE;
            }

            Log.d(TAG, "onMapReady: Zoomed bay:"+
                    focusPoint.getBayId()+" "+
                    focusPoint.getPosition()+" "+
                    focusPoint.isAvailable());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(focusPoint.getPosition(), zoomLevel));
        }
        else
        {
            // Get the current location of the device and set the position of the map.
            getDeviceLocation();
        }

        checkIfThereIsParking();

    }

    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    /**
     * Prompts the user for permission to use the device location.
     */
    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (mLocationPermissionGranted) {
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = task.getResult();
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(mLastKnownLocation.getLatitude(),
                                            mLastKnownLocation.getLongitude()), ZOOM_CURRENTLOCATION));
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            mMap.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(mDefaultLocation, ZOOM_DEFAULT));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    /**
     * Handles the result of the request for location permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[],
                                           int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }

    private void checkIfThereIsParking() {
        Log.d(TAG, "OnStart");
        //todo: Add check if data2 exists
        this.prefs = getSharedPreferences(PREFERENCE_NAME, MODE_PRIVATE);
        if (PreferenceManager.isAvailable(this.prefs)) {
            Log.d(TAG, "User is having ongoing parking!");

            DialogInterface.OnClickListener yesListener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    goToParkingActivity();
                }
            };

            DialogInterface.OnClickListener noListener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    PreferenceManager.clearPreference(prefs);
                }
            };

            showAlertDialog("Continue Parking", "Do you want to return into your Parking page?", yesListener, noListener);
        } else {
            Log.d(TAG, "User doesn't have ongoing parking!");
        }
    }

    @Override
    public boolean onClusterItemClick(Bay bay) {
        Log.d(TAG, "onClusterItemClick: bay clicked:" + bay.getBayId());
        selectedBay = SerializationUtils.clone(bay);

        reRenderBottomSheet(selectedBay);
        return false;
    }

    private void reRenderBottomSheet(@NotNull Bay bay) {
        //update bay
        String bayStatusMsg = (bay.isAvailable()) ? "Available" : "Occupied";
        String position = bay.getPosition().latitude + " , " + bay.getPosition().longitude;
        String title = (bay.getTitle().isEmpty()) ? position : bay.getTitle();
        bayTitle.setText(title);
        bayStatus.setText(bayStatusMsg);

        //update restriction
        layoutRestrictions.removeAllViews();
        for (int i = 0; i < bay.getRestrictions().size(); i++) {
            Button tv = new Button(getApplicationContext());
            tv.setText(bay.getRestrictions().get(i).getDescription());
            layoutRestrictions.addView(tv);
        }

        if (sheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
            sheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }

        restrictionsHelper = new RestrictionsHelper(bay.getRestrictions());
    }


    private boolean isInternetEnabled() {
        boolean result = false;
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        //ConnectivityManager.NetworkCallback networkCallback

        return result;
    }


    boolean doubleBackToExitPressedOnce = false;
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
                if (doubleBackToExitPressedOnce) {
                    super.onBackPressed();
                    return;
                }

                this.doubleBackToExitPressedOnce = true;
                Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

                new Handler().postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        doubleBackToExitPressedOnce=false;
                    }
                }, 2000);
            }
            //sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        } else {
            getSupportFragmentManager().popBackStack();
        }

    }

    private void bindToBayUpdateService(){

        Intent bayMonitorServiceIntent = new Intent(this, BayUpdateService.class);
        bayMonitorServiceIntent.setAction("ACTION_START_SERVICE");
        startService(bayMonitorServiceIntent);
        bindService(bayMonitorServiceIntent, connection, Context.BIND_AUTO_CREATE);

    }

}
