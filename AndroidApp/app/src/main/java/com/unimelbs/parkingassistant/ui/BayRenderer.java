package com.unimelbs.parkingassistant.ui;

import android.content.Context;
import android.media.TimedMetaData;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.unimelbs.parkingassistant.model.Bay;
import com.unimelbs.parkingassistant.model.DataFeed;
import com.unimelbs.parkingassistant.util.DistanceUtil;
import com.unimelbs.parkingassistant.util.Timer;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

public class BayRenderer extends DefaultClusterRenderer<Bay>
    implements GoogleMap.OnCameraIdleListener    
    {
    private Context context;
    private GoogleMap mMap;
    private ClusterManager<Bay> clusterManager;
    private static final String TAG="BayRenderer";
    private DataFeed dataFeed;
    private final float AVAILABLE_BAY_COLOR = BitmapDescriptorFactory.HUE_GREEN;
    private final float OCCUPIED_BAY_COLOR = BitmapDescriptorFactory.HUE_RED;
    private final double STATE_API_CIRCLE_RADIUS = 1000;
    private final double STREET_VIEW_RADIUS = 250;
    private final int STATUS_FRESHNESS_INTERVAL=120;
    private LatLng circleCentre;
    private long lastBayStatusUpdateTime;


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
        
    }


    @Override
    protected void onBeforeClusterItemRendered(Bay item, MarkerOptions markerOptions) {
        super.onBeforeClusterItemRendered(item, markerOptions);
        if (item.isAvailable())
        {

            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(AVAILABLE_BAY_COLOR));
        }
        else
        {
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(OCCUPIED_BAY_COLOR));
        }
    }

    @Override
    protected void onClusterItemRendered(Bay clusterItem, Marker marker) {
        super.onClusterItemRendered(clusterItem, marker);
    }

        @Override
        public void onCameraIdle() {


            Log.d(TAG, "onCameraIdle: ");
            LatLng topRight = mMap.getProjection().getVisibleRegion().latLngBounds.northeast;
            LatLng bottomLeft = mMap.getProjection().getVisibleRegion().latLngBounds.southwest;
            LatLng cameraFocus = mMap.getCameraPosition().target;
            //Calculating the radius of the circle including the Visible rectangle of the map.
            long radius =Math.round(DistanceUtil.getDistanceS(topRight,bottomLeft)/2);
            Log.d(TAG, "onCameraIdle: current view radius:"+radius);
            if (radius<STREET_VIEW_RADIUS)
            {
                if (circleCentre==null)
                {
                    circleCentre = mMap.getCameraPosition().target;
                    Log.d(TAG, "onCameraIdle: initial circle set. Position:"+
                            circleCentre.toString()+" updating bays + refreshing map");
                    lastBayStatusUpdateTime = System.currentTimeMillis();

                    //String lastTimeStr = new SimpleDateFormat(new Date(lastBayStatusUpdateTime),"")
                    Log.d(TAG, "onCameraIdle: initial circle set. Position:"+
                            circleCentre.toString()+" updating bays + refreshing map at "+
                            Timer.convertToTimestamp(lastBayStatusUpdateTime));
                    dataFeed.updateStates(circleCentre);
                }
                else
                {
                    long dataLifeInSeconds = (System.currentTimeMillis()-lastBayStatusUpdateTime)/1000;
                    Log.d(TAG, "onCameraIdle: data life: "+dataLifeInSeconds);
                    if (dataLifeInSeconds>STATUS_FRESHNESS_INTERVAL)
                    {
                        Log.d(TAG, "onCameraIdle: current data timestamp: "+
                                Timer.convertToTimestamp(lastBayStatusUpdateTime)+
                                " system time: "+Timer.convertToTimestamp(System.currentTimeMillis())+
                                ". State data is old, refreshing it.");
                        dataFeed.updateStates(circleCentre);
                        lastBayStatusUpdateTime = System.currentTimeMillis();
                    }
                    else
                    {
                        long cameraMoveDistance =
                                Math.round(DistanceUtil.getDistanceS(circleCentre,cameraFocus));
                        long boundary = cameraMoveDistance+radius;
                        if (boundary>STATE_API_CIRCLE_RADIUS)
                        {
                            Log.d(TAG, "onCameraIdle: Moved out of the boundary Need to call the state API again");
                            circleCentre=cameraFocus;
                            lastBayStatusUpdateTime=System.currentTimeMillis();
                            dataFeed.updateStates(circleCentre);
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
    }
