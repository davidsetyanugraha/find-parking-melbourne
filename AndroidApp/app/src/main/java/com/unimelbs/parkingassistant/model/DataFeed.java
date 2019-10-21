package com.unimelbs.parkingassistant.model;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterManager;
import com.unimelbs.parkingassistant.R;
import com.unimelbs.parkingassistant.parkingapi.ParkingApi;
import com.unimelbs.parkingassistant.parkingapi.SiteState;
import com.unimelbs.parkingassistant.parkingapi.SitesStateGetQuery;
import com.unimelbs.parkingassistant.ui.BayRenderer;
import com.unimelbs.parkingassistant.util.Timer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import io.reactivex.schedulers.Schedulers;

import static com.uber.autodispose.AutoDispose.autoDisposable;

public class DataFeed {
    private static final String TAG = "DataFeed";
    private static final String BAYS_FILE = "bays";
    private static final long DAY_TO_MILLIS = 1000*60*60*24;
    private static final long MINUTE_TO_MILLIS = 1000*60;
    private static final int FRESHNESS_INTERVAL_DAYS = 1;
    private final double STATE_API_CIRCLE_RADIUS = 1000;

    private Context context;
    private LifecycleOwner lifecycleOwner;
    private List<Bay> bays;
    private Hashtable<Integer,Bay> baysHashtable;
    private ParkingApi api;
    private ClusterManager<Bay> clusterManager;

    public ClusterManager<Bay> getClusterManager()
    {
        return clusterManager;
    }

    public void setClusterManager(ClusterManager<Bay> clusterManager)
    {
        this.clusterManager = clusterManager;
    }

    public DataFeed (LifecycleOwner mainActivity,
                     Context context) {
        this.lifecycleOwner = mainActivity;
        this.context = context;
        this.baysHashtable = new Hashtable<Integer,Bay>();
        this.api = ParkingApi.getInstance();
        //this.bayStateApi = new BayStateApi(this);
    }


    class BayDataApi extends AsyncTask<Void,Void,Void>
    {
        private static final String TAG = "BayDataApi";
        private BayAdapter bayAdapter;
        private DataFeed dataFeed;


        public BayDataApi(DataFeed dataFeed)
        {
            this.dataFeed = dataFeed;
        }
        private void fetchApiData()
        {
            Log.d(TAG, "fetchApiData: started on thread:"+Thread.currentThread().getName());
            this.bayAdapter = new BayAdapter(this.dataFeed);
            ParkingApi api = ParkingApi.getInstance();
            Timer timer = new Timer();
            timer.start();
            api.sitesGet()
                    .subscribeOn(Schedulers.io())
                    //.observeOn(AndroidSchedulers.mainThread())
                    //.as(autoDisposable(AndroidLifecycleScopeProvider.from(getLifecycle(), Lifecycle.Event.ON_STOP)))
                    .subscribe(value ->
                            {
                                timer.stop();
                                Log.d(TAG, "fetchApiData: completed in "+
                                        timer.getDurationInSeconds()+" seconds. # of Fetched sites:"+
                                        value.size());
                                bayAdapter.convertSites(value);
                            },
                            throwable -> Log.d(TAG+"-throwable", throwable.getMessage()));

        }
        @Override
        protected Void doInBackground(Void... voids) {
            this.fetchApiData();
            this.dataFeed.saveBaysToFile();
            return null;
        }
    }

    class BayStateApi extends AsyncTask<Void,Void,Void>
    {
        LatLng centre;
        ClusterManager<Bay> clusterManager;
        public BayStateApi(LatLng centre, ClusterManager<Bay> clusterManager)
        {
            this.centre = centre;
            this.clusterManager = clusterManager;
        }

        @Override
        protected void onPostExecute(Void aVoid)
        {
            super.onPostExecute(aVoid);
            Log.d(TAG, "onPostExecute: clearing current cluster items + re-adding them.");
            this.clusterManager.getMarkerCollection().clear();
            this.clusterManager.clearItems();
            this.clusterManager.addItems(getItems());
        }

        @Override
        protected Void doInBackground(Void... voids)
        {
            SitesStateGetQuery query = new SitesStateGetQuery(centre.latitude,centre.longitude,STATE_API_CIRCLE_RADIUS);
            api.sitesStateGet(query)
                .subscribeOn(Schedulers.io())
                //.observeOn(AndroidSchedulers.mainThread()) // to return to the main thread
                //.as(autoDisposable(AndroidLifecycleScopeProvider.from(this, Lifecycle.Event.ON_STOP))) //to dispose when the activity finishes
                .subscribe(value -> {updateStates(value);},
                        throwable -> Log.d(TAG, "BayStateApi: "+throwable.getMessage()) // do this on error
                );
            return null;
        }
    }

    public void loadData()
    {
        Log.d(TAG, "loadData: loading data started on thread: "+Thread.currentThread().getName());
        if (dataFilesExist())
        {
            if(isDataFresh())
            {
                Log.d(TAG, "loadData: data is fresh - loading from local file.");
                loadBaysFromFile();
            }
            else
            {
                Log.d(TAG, "loadData: data is stale, showing current data " +
                        "and fetching fresh data from API.");
                loadBaysFromFile();
                new BayDataApi(this).execute();
            }
        }
        else
        {
            Log.d(TAG, "loadData: data file does not exist. Attempting loading from raw/bays.");
            loadBaysFromRaw();
            new BayDataApi(this).execute();
        }
        this.bays = new ArrayList<>(this.baysHashtable.values());
    }

    private boolean dataFilesExist()
    {
        boolean result=true;
        File baysFile = context.getFileStreamPath(BAYS_FILE);
        if(baysFile==null||!baysFile.exists())result=false;
        return result;
    }

    private boolean isDataFresh()
    {
        boolean result=true;
        long currentTime = System.currentTimeMillis();
        File baysFile = context.getFileStreamPath(BAYS_FILE);
        if((currentTime-baysFile.lastModified())/DAY_TO_MILLIS>FRESHNESS_INTERVAL_DAYS)result=false;
        return result;
    }


    public List<Bay> getItems() {
        if (bays==null)
        {
            bays = new ArrayList<>(baysHashtable.values());
            Log.d(TAG, "getItems: bays list is empty, creating it. "+bays.size()+ " bays added.");
        }
        return bays;
    }


    private void loadBaysFromFile()
    {
        Timer timer = new Timer();
        timer.start();
        try
        {
            FileInputStream fileInputStream = context.openFileInput(BAYS_FILE);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
            ObjectInputStream objectInputStream = new ObjectInputStream(bufferedInputStream);
            this.baysHashtable = (Hashtable<Integer, Bay>) objectInputStream.readObject();
            timer.stop();
            Log.d(TAG, "loadBaysFromFile: ended in "+
                    timer.getDurationInSeconds()+
                    " seconds. Number of bays loaded:"+
                    this.baysHashtable.size());
            fileInputStream.close();
            objectInputStream.close();
        }  catch (FileNotFoundException e) {
            Log.d(TAG, "loadBaysFromFile: FileNotFoundException: "+e.getMessage());
        } catch (Exception e) {
            Log.d(TAG, "loadBaysFromFile: general exception: "+e.getMessage());
        }
    }
    private void loadBaysFromRaw()
    {
        Timer timer = new Timer();
        timer.start();
        int rawFileId = context.getResources().
                getIdentifier("bays",
                        "raw",
                        "com.unimelbs.parkingassistant");
        if (rawFileId!=0)
        {
            try
            {
                BufferedInputStream bufferedInputStream =
                        new BufferedInputStream (context.getResources().openRawResource(rawFileId));
                ObjectInputStream objectInputStream = new ObjectInputStream(bufferedInputStream);
                //bays = (List<Bay>) objectInputStream.readObject();
                baysHashtable = (Hashtable<Integer, Bay>) objectInputStream.readObject();
                timer.stop();
                Log.d(TAG, "loadBaysFromRaw: completed in "+timer.getDurationInSeconds()+" seconds. Bays loaded: "+baysHashtable.size());

                bufferedInputStream.close();
                objectInputStream.close();
            }  catch (FileNotFoundException e) {
                Log.d(TAG, "loadBaysFromRaw: FileNotFoundException: "+e.getMessage());
            } catch (Exception e) {
                Log.d(TAG, "loadBaysFromRaw: general exception: "+e.getMessage());
            }
        }
        else
        {
            Log.d(TAG, "loadBaysFromRaw: bays file does not exist.");
        }
    }


    private void saveBaysToFile()
    {
        Log.d(TAG, "saveBaysToFile: ");
        File file = new File(context.getFilesDir()+"/"+BAYS_FILE);
        if (file.exists())
        {
            Log.d(TAG, "saveBaysToFile: a file exists, deleting it.");
            file.delete();
        }


        try {
            FileOutputStream fileOutputStream =  context.openFileOutput(BAYS_FILE, Context.MODE_PRIVATE);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            Log.d(TAG, "saveBaysToFile: num of bays: "+baysHashtable.size());
            //objectOutputStream.writeObject(this.bays);
            objectOutputStream.writeObject(baysHashtable);
            objectOutputStream.close();
            fileOutputStream.close();
        } catch (Exception e) {
            Log.d(TAG, "saveBaysToFile: exception:"+e.getMessage());
        }
    }


    public void addBay(Bay bay)
    {
        this.baysHashtable.put(bay.getBayId(),bay);
    }

    private void updateStates(List<SiteState> list)
    {
        Log.d(TAG, "updateStates: table includes:"+baysHashtable.size());
        for (SiteState siteState: list)
        {
            String strState = siteState.getStatus();

            int id = Integer.parseInt(siteState.getId());
            boolean state = (strState.equals("Present"))?false:true;
            Log.d(TAG, "updateStates: api_id:"+siteState.getId()+
                    "raw state:"+ strState+
                    " state:"+state);
            if (baysHashtable.get(id)!=null)
            {
                Log.d(TAG, "updateStates: site ("+id+") found.");
                baysHashtable.get(Integer.parseInt(siteState.getId())).setAvailable(state);
            }
            else
            {
                Log.d(TAG, "updateStates: site ("+id+") NOT FOUND.");
            }

        }
    }

    public void updateStates(LatLng centre)
    {
        new BayStateApi(centre,clusterManager).execute();
    }

}
