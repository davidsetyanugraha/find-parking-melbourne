package com.unimelbs.parkingassistant;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.maps.android.clustering.ClusterManager;
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider;
import com.unimelbs.parkingassistant.model.Bay;
import com.unimelbs.parkingassistant.model.DataFeed;
import com.unimelbs.parkingassistant.model.ExtendedClusterManager;
import com.unimelbs.parkingassistant.parkingapi.ParkingSiteFollower;
import com.unimelbs.parkingassistant.util.PermissionManager;
import com.unimelbs.parkingassistant.util.RestrictionsHelper;

import org.apache.commons.lang3.SerializationUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.Lifecycle;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.uber.autodispose.AutoDispose.autoDisposable;

public class MapsActivity extends AppCompatActivity
        implements OnMapReadyCallback,
        ClusterManager.OnClusterItemClickListener<Bay> {

    private GoogleMap mMap;
    public static final String HOUR = "com.unimelbs.parkingassistant.HOUR";
    public static final String SELECTED_BAY = "com.unimelbs.parkingassistant.selectedBay";
    private static final String TAG = "MapActivity";
    private static String apiKey;
    private Bay selectedBay;
    // This is the Notification Channel ID.
    public static final String NOTIFICATION_CHANNEL_ID = "channel_id";
    //User visible Channel Name
    public static final String CHANNEL_NAME = "Notification Channel";


    //Bottom sheet and StartParking impl
    @BindView(R.id.bottom_sheet_maps)
    LinearLayout layoutBottomSheet;

    @BindView(R.id.bay_title)
    TextView bayTitle;

    @BindView(R.id.bay_snippet)
    TextView baySnippet;

    @BindView(R.id.bay_status)
    TextView bayStatus;

    @BindView(R.id.bay_restriction)
    TextView bayRestriction;

    @BindView(R.id.btn_direction)
    Button direction;

    @BindView(R.id.btn_start_parking)
    Button startParkingButton;

    BottomSheetBehavior sheetBehavior;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: " + Thread.currentThread().getName());
        //todo: Add check if data2 exists
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
        sheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    private void triggerIntent(String hour) {
        Intent intent = new Intent(this, ParkingActivity.class);
        intent.putExtra(HOUR, hour);
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

        //if(this.selectedBay.isAvailable())
        if (true) //TODO USe the above line once the site statuses are clear. Right now all are occupied.

        {


            LatLng selectedBayLatLng = this.selectedBay.getPosition();
            String lat = String.valueOf(selectedBayLatLng.latitude);
            String lon = String.valueOf(selectedBayLatLng.longitude);


            // Part of the code below is taken from
            // https://stackoverflow.com/questions/
            // 2662531/launching-google-maps-directions
            // -via-an-intent-on-android?rq=1

            Uri navigationIntentUri = Uri.parse("google.navigation:q=" + lat + "," + lon);
            Log.d("Navigation Uri", "Navigation URI is " + navigationIntentUri);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, navigationIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");


            try {
                subscribeToServerForUpdates(this.selectedBay);
                startActivity(mapIntent);

            } catch (ActivityNotFoundException ex) {
                try {
                    Intent unrestrictedIntent = new Intent(Intent.ACTION_VIEW, navigationIntentUri);
                    startActivity(unrestrictedIntent);
                } catch (ActivityNotFoundException innerEx) {
                    Toast.makeText(this, "No Map Application Found, Opening In Browser", Toast.LENGTH_LONG).show();
                    try {
                        Uri.Builder builder = new Uri.Builder();
                        builder.scheme("https")
                                .authority("www.google.com")
                                .appendPath("maps")
                                .appendPath("dir")
                                .appendPath("")
                                .appendQueryParameter("api", "1")
                                .appendQueryParameter("destination", lat + "," + lon);
                        String url = builder.build().toString();
                        Log.d("Directions", url);
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        i.setData(Uri.parse(url));
                        startActivity(i);
                    } catch (Exception e) {
                        Log.d("Failure", "Failed To Open any navigation method " + e.getMessage());

                    }
                }
            }

        } else {
            Toast.makeText(this, "Selected Bay Is Occupied.", Toast.LENGTH_LONG).show();

        }

    }

    /**
     * Bottom screen Button Start Parking OnClick
     */
    @OnClick(R.id.btn_start_parking)
    public void startParking() {
        //bayStatusChangeNotification();
        AlertDialog alertDialog;
        final AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
        final View startParkingFormView = getLayoutInflater().inflate(R.layout.dialog_parking, null);
        Button continueButton = startParkingFormView.findViewById(R.id.formSubmitButton);

        builder.setTitle("Start Parking");
        builder.setView(startParkingFormView);
        alertDialog = builder.create();

        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    EditText hour = startParkingFormView.findViewById(R.id.parkingFormDuration);
                    String strHour = hour.getText().toString();

                    //@todo add validation from selectedBay
                    if (strHour.isEmpty()) {
                        Toast.makeText(getApplicationContext(),
                                "Input cannot be blank",
                                Toast.LENGTH_LONG).show();
                    } else if (! RestrictionsHelper.isValid(selectedBay.getRestrictions(), strHour)){
                        Toast.makeText(getApplicationContext(),
                                RestrictionsHelper.getInvalidReason(selectedBay.getRestrictions(), strHour),
                                Toast.LENGTH_LONG).show();
                    } else {
                        triggerIntent(strHour);
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        Button resetButton = startParkingFormView.findViewById(R.id.formCancelButton);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    EditText duration = startParkingFormView.findViewById(R.id.parkingFormDuration);
                    duration.setText("");
                    alertDialog.cancel();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        builder.setCancelable(false);
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
        data.loadData();
        List<Bay> bayList = data.getItems();
        //data.execute();
        //try {Thread.sleep(30000);}catch(Exception e){Log.d(TAG, "onMapReady: "+e.getMessage());}
        ExtendedClusterManager<Bay> extendedClusterManager = new ExtendedClusterManager<>(this, mMap, data);

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
        extendedClusterManager.addItems(bayList);
        extendedClusterManager.setOnClusterItemClickListener(this);
        LatLng zoomPoint;
        if (bayList.size() > 0) {

            Log.d(TAG, "onMapReady: Bay items loaded on map.");
            zoomPoint = data.getItems().get(0).getPosition();
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(zoomPoint, 15));
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


    private boolean isInternetEnabled() {
        boolean result = false;
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        //ConnectivityManager.NetworkCallback networkCallback

        return result;
    }


    private void subscribeToServerForUpdates(@NotNull Bay selectedBay) {

        CompositeDisposable disposable = new CompositeDisposable();

        //Connect the follower to a parking bay
        ParkingSiteFollower follower = ParkingSiteFollower.getInstance();
        Disposable d = follower.createParkingBayFollower(Integer.toString(selectedBay.getBayId()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()) // to return to the main thread
                .as(autoDisposable(AndroidLifecycleScopeProvider.from(this, Lifecycle.Event.ON_STOP))) //to dispose when the activity finishes
                .subscribe(bay -> {
                            //Log.v("Notification", "Value:" + bay.getId() + " status of the bay is: " + bay.getStatus()); // sample, other values are id, status, location, zone, recordState
                            bayStatusChangeNotification();
                        },
                        throwable -> Log.d("debug", throwable.getMessage()), // do this on error
                        () -> Log.d("debug", "complete"));

        //Dispose when disposing the service or when not needed
        //disposable.add(d);

    }

    public void bayStatusChangeNotification() {


        final int NOTIFICATION_ID = 101;
        String title = "Bay Status Changed";
        String subject = "Parking Bay Status Changed ";
        String body = "The status of selected bay has been changed, Tap to Redirect to app";
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // Importance applicable to all the notifications in this Channel


        //Notification channel should only be created for devices running Android 26
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, CHANNEL_NAME, importance);
            //Boolean value to set if lights are enabled for Notifications from this Channel
            notificationChannel.enableLights(true);
            //Boolean value to set if vibration are enabled for Notifications from this Channel
            notificationChannel.enableVibration(true);
            //Sets the color of Notification Light
            notificationChannel.setLightColor(Color.GREEN);
            //Set the vibration pattern for notifications. Pattern is in milliseconds with the format {delay,play,sleep,play,sleep...}
            notificationChannel.setVibrationPattern(new long[]{
                    500,
                    500,
                    500,
                    500,
                    500
            });
            //Sets whether notifications from these Channel should be visible on Lockscreen or not
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationManager.createNotificationChannel(notificationChannel);

        }

        // Notification Channel ID passed
        // as a parameter here will be
        // ignored for all the
        // Android versions below 8.0
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        builder.setContentTitle(title);
        builder.setContentText(body);
        builder.setSmallIcon(R.drawable.ic_launcher_background);
        builder.setPriority(NotificationCompat.PRIORITY_MAX);
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));

        Intent intent = new Intent(this, MapsActivity.class);

        new Intent(this, MapsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 1001, intent, 0);
        //Following will set the tap action
        builder.setContentIntent(pendingIntent);


        //PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 1002, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        //builder.addAction(R.drawable.ic_launcher_background, "Redirect to Nearest Bay", pendingIntent);
        //builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.icon));
        Notification notification = builder.build();


        notificationManager.notify(NOTIFICATION_ID, notification);


        Toast.makeText(this, "Selected Bay Status Has Been Changed", Toast.LENGTH_LONG).show();
    }


}
