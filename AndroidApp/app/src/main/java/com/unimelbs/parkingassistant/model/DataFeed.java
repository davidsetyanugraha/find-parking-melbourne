package com.unimelbs.parkingassistant.model;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterManager;
import com.unimelbs.parkingassistant.R;
import com.unimelbs.parkingassistant.parkingapi.ParkingApi;
import com.unimelbs.parkingassistant.parkingapi.SiteState;
import com.unimelbs.parkingassistant.parkingapi.SitesStateGetQuery;
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

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;



/**
 * Represents data repository of bay and bay states data.
 */
public class DataFeed {
    private static final String TAG = "DataFeed";
    private static final String BAYS_FILE = "bays";
    private static final long DAY_TO_MILLIS = 1000*60*60*24;
    private static final int FRESHNESS_INTERVAL_DAYS = 1;
    private final double STATE_API_CIRCLE_RADIUS = 1000;

    private Context context;
    private List<Bay> bays;
    private Hashtable<Integer,Bay> baysHashtable;
    private ParkingApi api;
    private PublishSubject<List<Bay>> baysSubject = PublishSubject.create();
    //private BayStateApi bayStateApi;
    private ClusterManager<Bay> clusterManager;


    /**
     * Constructor.
     * @param context
     */
    public DataFeed(Context context) {
        this.context = context;
        this.baysHashtable = new Hashtable<>();
        this.api = ParkingApi.getInstance();
    }

    public Observable<List<Bay>> getBaysObservable() {
        return baysSubject;
        //TODO: Call baysSubject.onNext(<<<the new array here>>); when needed
    }

/*
    class BayStateApi extends AsyncTask<Void,Void,List<SiteState>>
    {
        private static final String TAG = "BayStateApi";
        private DataFeed dataFeed;
        private List<SiteState> baysStates;
        public BayStateApi(DataFeed dataFeed, LatLng centrePoint)
        {
            this.dataFeed = dataFeed;
        }
        private void fetchApiData()
        {
            SitesStateGetQuery query = new SitesStateGetQuery(-37.796201, 144.958266, null);
            dataFeed.api.sitesStateGet(query)
                .subscribeOn(Schedulers.io())
                //.observeOn(AndroidSchedulers.mainThread()) // to return to the main thread
                //.as(autoDisposable(AndroidLifecycleScopeProvider.from(this, Lifecycle.Event.ON_STOP))) //to dispose when the activity finishes
                .subscribe(value ->
                        {
                            baysStates = value;
                            System.out.println("Value:" + value.get(0).getStatus()); // sample, other values are id, status, location, zone, recordState
                        },
                    throwable -> Log.d("debug", throwable.getMessage()) // do this on error
                );
        }


        @Override
        protected Void doInBackground(Void...params) {
            fetchApiData();
            return null; //new LatLng(50,60);//null;
        }
    }
 */

    /**
     * Calls a back-end API that caches Bay data from the city of
     * Melbourne Open Data API. This task runs on a separate thread.
     */
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
        protected void onPostExecute(Void aVoid)
        {
            super.onPostExecute(aVoid);
            Log.d(TAG, "onPostExecute: after saving.");
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Log.d(TAG, "doInBackground: started"+Thread.currentThread().getName());
            fetchApiData();
            return null;
        }
    }

    /**
     * Calls a back-end API that caches Bay sensor status data from the
     * city of Melbourne Open Data API. The data is refreshed at the back-end
     * every 2 minutes.
     * This task runs on a separate thread.
     */
    class BayStateApi extends AsyncTask<Void,Void,Void>
    {
        private LatLng centre;
        private ClusterManager<Bay> clusterManager;

        /**
         * Constructor that takes a circle centre point and a radius to query
         * bays within the circle.
         * @param centre
         * @param clusterManager
         */
        public BayStateApi(LatLng centre, ClusterManager<Bay> clusterManager)
        {
            Log.d(TAG, "BayStateApi: centre is"+centre);
            this.centre = centre;
            this.clusterManager = clusterManager;
        }

        @Override
        protected void onPostExecute(Void aVoid)
        {
            super.onPostExecute(aVoid);
            refreshMap();
            Log.d(TAG, "onPostExecute: clearing current cluster items + re-adding them.");

        }

        @Override
        protected Void doInBackground(Void... voids)
        {
            SitesStateGetQuery query = new SitesStateGetQuery(centre.latitude,centre.longitude,STATE_API_CIRCLE_RADIUS);
            api.sitesStateGet(query)
                .subscribeOn(Schedulers.io())
                //.observeOn(AndroidSchedulers.mainThread()) // to return to the main thread
                //.as(autoDisposable(AndroidLifecycleScopeProvider.from(this, Lifecycle.Event.ON_STOP))) //to dispose when the activity finishes
                .subscribe(value ->
                        {
                            updateBaysStates(value);
                        },
                        throwable -> Log.d(TAG, "BayStateApi: "+throwable.getMessage()) // do this on error
                );
            return null;
        }

        /**
         * Removes all bays from the map, re add them to display current status
         */
        protected void refreshMap()
        {
            this.clusterManager.getMarkerCollection().clear();
            this.clusterManager.clearItems();
            this.clusterManager.addItems(getItems());
        }
    }

    /**
     * Loads Bay data from file(s) and fetch data from the API if necessary.
     */
    public void loadData()
    {
        Log.d(TAG, "loadData: loading data started.");
        // Checks if local bays file exists.
        if (dataFilesExist())
        {
            // Checks if the bays local file is fresh according to a freshness constant.
            if(isDataFresh())
            {
                Log.d(TAG, "loadData: data is fresh, loading from local file.");
                loadBaysFromFile();
            }
            //If data is not fresh, call back-end API to save a newer version of the data.
            else
            {
                Log.d(TAG, "loadData: data is stale, showing current data " +
                        "and fetching fresh data from API.");
                loadBaysFromFile();
                new BayDataApi(this).execute();

            }
        }
        // If local bays data file doesn't exist, load from raw bays file that is packaged with the app
        else
        {
            Log.d(TAG, "loadData: data file does not exist. Attempting loading from raw/bays.");
            loadBaysFromRaw();
            new BayDataApi(this).execute();
        }
        this.bays = new ArrayList<>(this.baysHashtable.values());
    }

    /**
     * Checks if a local bays file exists.
     * @return
     */
    private boolean dataFilesExist()
    {
        boolean result=true;
        File baysFile = context.getFileStreamPath(BAYS_FILE);
        if(baysFile==null||!baysFile.exists())result=false;
        return result;
    }

    /**
     * Checks if the local bays file is fresh.
     * @return
     */
    private boolean isDataFresh()
    {
        boolean result=true;
        long currentTime = System.currentTimeMillis();
        File baysFile = context.getFileStreamPath(BAYS_FILE);
        if((currentTime-baysFile.lastModified())/DAY_TO_MILLIS>FRESHNESS_INTERVAL_DAYS)result=false;
        return result;
    }


    /**
     * Returns a list of Bays to be drawn on the map.
     * @return
     */
    public List<Bay> getItems() {
        if (bays==null)
        {
            bays = new ArrayList<>(baysHashtable.values());
            Log.d(TAG, "getItems: bays list is empty, creating it. "+bays.size()+ " bays added.");
        }
        return bays;
    }


    /**
     * Loads bays data from local file.
     */
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
            File file = new File(context.getFilesDir()+"/"+BAYS_FILE);
            if (file.exists())
            {
                Log.d(TAG, "loadBaysFromFile: exception:"+e.getMessage());
                file.delete();
                Log.d(TAG, "loadBaysFromFile: file deleted");
                new BayDataApi(this).execute();
            }
        }
    }

    /**
     * Loads bay data from raw file.
     */
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


    /**
     * Saves Bays list as serialised java object to a local file.
     */
    public synchronized void  saveBaysToFile()
    {
        Log.d(TAG, "saveBaysToFile: datafeed:"+this);
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
            objectOutputStream.writeObject(baysHashtable);
            objectOutputStream.close();
            fileOutputStream.close();
        } catch (Exception e) {
            Log.d(TAG, "saveBaysToFile: exception:"+e.getMessage());
        }
    }

    /**
     * Adds a bay object to the bays hashtable.
     * @param bay
     */
    public void addBay(Bay bay)
    {
        baysHashtable.put(bay.getBayId(),bay);
    }

    /**
     * Updates bays hashtable with current status (Available, Unavailable).
     * @param list returned from calling the back-end API.
     */
    private void updateBaysStates(List<SiteState> list)
    {
        int found=0;
        int notFound=0;
        int availableBays=0;
        Timer timer=new Timer();
        Log.d(TAG, "fetchBaysStates: Bay table includes:"+baysHashtable.size()+" "+
                "State data includes: "+list.size());
        timer.start();
        for (SiteState siteState: list)
        {
            String strState = siteState.getStatus();

            int id = Integer.parseInt(siteState.getId());
            boolean state = (strState.equals("Present"))?false:true;
            if (baysHashtable.get(id)!=null)
            {
                found++;
                availableBays+=(state)?1:0;
                baysHashtable.get(Integer.parseInt(siteState.getId())).setAvailable(state);
            }
            else
            {
                notFound++;

            }
        }
        timer.stop();
        Log.d(TAG, "fetchBaysStates: completed in "+timer.getDurationInSeconds()+" "+
                found+" states found. "+notFound+" states not found."+
                " Available bays: "+availableBays);
    }

    /**
     * Calls the back-end API to retrieve bay statuses within a circle defined by a
     * centre point and a radius.
     * @param centre a point on the map (Latitude and Longitude)
     */
    public void fetchBaysStates(LatLng centre)
    {
        if (centre!=null)
            new BayStateApi(centre,clusterManager).execute();
        else
            Log.d(TAG, "fetchBaysStates: centre is null");
    }

    /**
     * Setter for map cluster manager.
     * @param clusterManager
     */
    public void setClusterManager(ClusterManager<Bay> clusterManager)
    {
        this.clusterManager = clusterManager;
    }

}
