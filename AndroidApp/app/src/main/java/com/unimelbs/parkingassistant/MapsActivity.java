package com.unimelbs.parkingassistant;

import androidx.fragment.app.FragmentActivity;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private static final String TAG = "TEPA";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        //PermissionManager permissionManager = new PermissionManager();
        //permissionManager.requestPermission(this);
        //PermissionManager.requestPermission(this);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        PermissionManager.reqPermission(this,this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
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
        // Add a marker in Sydney and move the camera
        LatLng uom = new LatLng(-37.798122, 144.960814);
        mMap.addMarker(new MarkerOptions().position(uom).title("Our Uni"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(uom));
        mMap.moveCamera(CameraUpdateFactory.zoomTo(18));


    }
}
