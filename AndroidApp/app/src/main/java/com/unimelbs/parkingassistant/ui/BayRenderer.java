package com.unimelbs.parkingassistant.ui;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;

import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;
import com.unimelbs.parkingassistant.model.Bay;
import com.unimelbs.parkingassistant.model.DataFeed;
import com.unimelbs.parkingassistant.util.Constants;
import com.unimelbs.parkingassistant.util.DistanceUtil;
import com.unimelbs.parkingassistant.util.Timer;

import java.util.Collection;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;

/**
 * Custom cluster renderer, used to implement the logic of displaying markers
 * representing bays, and to control model update in an efficient way.
 */
public class BayRenderer extends DefaultClusterRenderer<Bay>
        implements GoogleMap.OnCameraIdleListener
{
    private final float AVAILABLE_BAY_COLOR = BitmapDescriptorFactory.HUE_GREEN;
    private final float OCCUPIED_BAY_COLOR = BitmapDescriptorFactory.HUE_RED;
    private final double STATE_API_CIRCLE_RADIUS = 1000;
    private final double STREET_VIEW_RADIUS = 250;
    private final int STATUS_FRESHNESS_INTERVAL=120;
    private float currentZoom;

    private Context context;
    private GoogleMap mMap;
    private ClusterManager<Bay> clusterManager;
    private static final String TAG="BayRenderer";
    private DataFeed dataFeed;
    private LatLng circleCentre;
    private long lastBayStatusUpdateTime;




    /**
     * Constructors for BayRenderer.
     * @param context
     * @param mMap
     * @param clusterManager
     */
    public BayRenderer(Context context, GoogleMap mMap, ClusterManager<Bay> clusterManager)
    {
        super(context,mMap,clusterManager);
        this.context = context;
        this.mMap = mMap;
        this.clusterManager = clusterManager;
    }
    public BayRenderer(Context context,
                       GoogleMap mMap, 
                       ClusterManager<Bay> clusterManager,
                       DataFeed dataFeed)
    {
        this(context,mMap,clusterManager);
        this.dataFeed = dataFeed;
        this.dataFeed.getBaysObservable().observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    value -> updateBaysMarkers(value),
                    throwable -> {
                        Log.d(TAG, "BayRenderer: throwable: "+throwable.getMessage());
                    }
                );
    }

    /**
     * Checks bay information before rendering it on the map, changes marker properties
     * Accordingly.
     * @param item
     * @param markerOptions
     */
    @Override
    protected void onBeforeClusterItemRendered(Bay item, MarkerOptions markerOptions) {
        BitmapDescriptor newIcon = null;
        switch (item.getStatus())
        {
            case AVAILABLE: {newIcon= Constants.AVAILABLE_ICON;break;}
            case OCCUPIED: {newIcon=Constants.UNAVAILABLE_ICON;break;}
            case UNAVAILABLE: {newIcon=Constants.UNKNOWN_ICON;break;}
        }
        markerOptions.icon(newIcon);

    }

    @Override
    protected void onClusterItemRendered(Bay clusterItem, Marker marker) {
        super.onClusterItemRendered(clusterItem, marker);
    }


    /**
     * This is executed when the user has finished interacting with the map.
     * i.e. zoom or move.
     */
    @Override
    public void onCameraIdle()
    {

        LatLng cameraFocus = mMap.getCameraPosition().target;
        //Calculating the radius of the circle including the Visible rectangle of the map.
        double radius = DistanceUtil.getRadius(mMap);

        currentZoom = mMap.getCameraPosition().zoom;


/*
        if (currentZoom<Constants.MAP_DO_NOT_CLUSTER_ZOOM_LEVEL)
        {
            Log.d(TAG, "onCameraIdle: Removing marker collection.");
            //clusterManager.getMarkerManager().getCollection(Constants.BAY_COLLECTION_ID).clear();
            //clusterManager.getMarkerManager().getCollection(Constants.BAY_COLLECTION_ID).hideAll();
            Log.d(TAG, "onCameraIdle: markers size:"+clusterManager.getMarkerCollection().getMarkers().size());
        }
*/

        Log.d(TAG, "onCameraIdle: current view radius:"+radius+
                "zoom:"+currentZoom);
        //Checks if radius (in meters) of the shown part of the map is < the defined street view
        //radius. This is the point when Bay status API is called to show it on the map.
        if (radius<STREET_VIEW_RADIUS)
        {
            //If this is the first use of the app, set a centre for a circle that bounds
            //an area for which bay status is updated.
            if (circleCentre==null)
            {
                circleCentre = mMap.getCameraPosition().target;

                lastBayStatusUpdateTime = System.currentTimeMillis();
                Log.d(TAG, "onCameraIdle: initial circle set. Position:"+
                        circleCentre.toString()+" updating bays + refreshing map at "+
                        Timer.convertToTimestamp(lastBayStatusUpdateTime));
                dataFeed.fetchBaysStates(circleCentre);
            }
            //This is the case that bay status has been updated in the same session.
            else
            {
                //Calculates bay status information validity (freshness).
                long dataLifeInSeconds = (System.currentTimeMillis()-lastBayStatusUpdateTime)/1000;
                Log.d(TAG, "onCameraIdle: data life: "+dataLifeInSeconds+" seconds.");

                //If the data is old (sensor data is updated every 2 minutes). Call back-end
                //API to get current status information.
                if (dataLifeInSeconds>STATUS_FRESHNESS_INTERVAL)
                {
                    Log.d(TAG, "onCameraIdle: current data timestamp: "+
                            Timer.convertToTimestamp(lastBayStatusUpdateTime)+
                            " system time: "+Timer.convertToTimestamp(System.currentTimeMillis())+
                            ". State data is old, refreshing it.");
                    dataFeed.fetchBaysStates(circleCentre);
                    lastBayStatusUpdateTime = System.currentTimeMillis();
                }
                //The case that the data is fresh.
                else
                {
                    //Calculates the physical distance corresponding to user navigation
                    //on the map from the centre of the last updated circle area.
                    double cameraMoveDistance =
                            Math.round(DistanceUtil.getDistanceS(circleCentre,cameraFocus));
                    double boundary = cameraMoveDistance+radius;

                    //Checks if the user has moved on the map within the area of the last updated circle
                    //If the user move outside the boundary.
                    if (boundary>STATE_API_CIRCLE_RADIUS)
                    {
                        Log.d(TAG, "onCameraIdle: Moved out of the boundary Need to call the state API again");
                        //Setting a new circle.
                        circleCentre=cameraFocus;
                        lastBayStatusUpdateTime=System.currentTimeMillis();
                        dataFeed.fetchBaysStates(circleCentre);
                    }
                    else
                    {
                        Log.d(TAG, "onCameraIdle: moving within boundaries."+
                                "State data timestamp:"+Timer.convertToTimestamp(lastBayStatusUpdateTime));
                    }
                }
            }
        }
    }

    /**
     * Updates all visible markers on the screen for markers that has been rendered already.
     * This is called on a specified zoom level only.
     * @param changedBays
     */
    private void updateBaysMarkers(List<Bay> changedBays)
    {
        if(currentZoom>=Constants.MAP_DO_NOT_CLUSTER_ZOOM_LEVEL)
        {
            int updatedMarkers=0;

            for (Bay bay: changedBays)
            {
                updatedMarkers+=(updateBayMarker(bay))?1:0;
            }
            Log.d(TAG, "updateMarkers: markers updated:"+updatedMarkers);
        }
    }

    /**
     * Keep clustering items until appropriate zoom level occurs.
     * @param cluster
     * @return
     */
    @Override
    protected boolean shouldRenderAsCluster(Cluster<Bay> cluster) {
//        return cluster.getSize() > this.mMinClusterSize;

//        final float currentMaxZoom = mMap.getMaxZoomLevel();
        return currentZoom < Constants.MAP_DO_NOT_CLUSTER_ZOOM_LEVEL;
//        return currentZoom < currentMaxZoom && cluster.getSize() >= 10;
    }

    /**
     * Updates a Bay's marker on the map to the appropriate visual cue.
     * @param bay
     * @return
     */
    private boolean updateBayMarker(Bay bay)
    {
        BitmapDescriptor newIcon=null;
        switch (bay.getStatus())
        {
            case AVAILABLE: {newIcon=Constants.AVAILABLE_ICON;break;}
            case OCCUPIED: {newIcon=Constants.UNAVAILABLE_ICON;break;}
            case UNAVAILABLE: {newIcon=Constants.UNKNOWN_ICON;break;}
        }
        Marker m = getMarker(bay);
        if (m!=null)
        {
            m.setIcon(newIcon);
            return true;
        }
        else return false;
    }
}
