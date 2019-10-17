package com.unimelbs.parkingassistant.model;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider;
import com.unimelbs.parkingassistant.R;
import com.unimelbs.parkingassistant.parkingapi.ParkingApi;
import com.unimelbs.parkingassistant.util.Timer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static com.uber.autodispose.AutoDispose.autoDisposable;

public class DataFeed extends AsyncTask<Void,Void,Void> {
    private static final String TAG = "DataFeed";
    private static final String BAYS_FILE = "bays.dat";
    private static final String BAYS_DETAILS_FILE = "bays_details.dat";
    private static final String BAYS_JSON_FILE = "bays.json";
    private static final String BAYS_DETAILS_JSON_FILE = "bays_details.json";
    private static final long DAY_TO_MILLIS = 1000*60*60*24;
    private static final long MINUTE_TO_MILLIS = 1000*60;
    private static final int FRESHNESS_INTERVAL_DAYS = 0;

    private Context context;
    private LifecycleOwner lifecycleOwner;
    private List<Bay> bays;
    private Hashtable<Integer,BayDetails> baysDetailsHashtable;

    public DataFeed (LifecycleOwner mainActivity,
                     Context context) {
        this.lifecycleOwner = mainActivity;
        this.context = context;
        bays = new ArrayList<>();
        baysDetailsHashtable = new Hashtable<>();
    }
    class OnlineData extends AsyncTask<Void,Void,Void>
    {
        private static final String TAG = "OnlineData";
        private List<Bay> bays;
        private Hashtable<Integer,BayDetails> baysDetailsHashtable;
        private void fetchApiData()
        {
            Log.d(TAG, "fetchApiData: started on thread:"+Thread.currentThread().getName());
            this.bays = new ArrayList<>();
            this.baysDetailsHashtable = new Hashtable<>();
            BayAdapter bayAdapter = new BayAdapter();
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
                                Log.d(TAG, "fetchApiData: ended: duration: "+
                                        timer.getDuration()+" fetched sites:"+
                                        value.size());
                                ArrayList<Object> conversionResults = bayAdapter.convertSites(value);
                                this.bays = (ArrayList<Bay>)conversionResults.get(0);
                                this.baysDetailsHashtable = (Hashtable<Integer, BayDetails>) conversionResults.get(1);
                                saveBaysToFile(this.bays);
                                saveBayDetailsToFile(this.baysDetailsHashtable);
                            },
                            throwable -> Log.d(TAG+"-throwable", throwable.getMessage()));

        }
        @Override
        protected Void doInBackground(Void... voids) {
            this.fetchApiData();
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
                loadBayDetailsFromFile();
            }
            else
            {
                Log.d(TAG, "loadData: data is stale, showing current data " +
                        "and fetching fresh data from API.");
                //This should run in the background
                //new OnlineData().execute();
                loadBaysFromFile();
                loadBayDetailsFromFile();
                new OnlineData().execute();
                //fetchApiData();
            }
        }
        else
        {

            //new OnlineData().execute();

            loadBaysFromRaw();
            loadBayDetailsFromRaw();
            new OnlineData().execute();
            Log.d(TAG, "loadData: data files don't exist. Showing data from res/raw folder"+
                    " and calling the API async to download data");
        }
    }

    private boolean dataFilesExist()
    {
        boolean result=true;
        File baysFile = context.getFileStreamPath(BAYS_FILE);
        File bayDetailsFile = context.getFileStreamPath(BAYS_DETAILS_FILE);
        if(baysFile==null||
                !baysFile.exists()||
                bayDetailsFile==null||
                !bayDetailsFile.exists())result=false;
        return result;
    }

    private boolean isDataFresh()
    {
        boolean result=true;
        long currentTime = System.currentTimeMillis();
        File baysFile = context.getFileStreamPath(BAYS_FILE);
        File bayDetailsFile = context.getFileStreamPath(BAYS_DETAILS_FILE);
        if((currentTime-baysFile.lastModified())/DAY_TO_MILLIS>FRESHNESS_INTERVAL_DAYS||
               (currentTime-bayDetailsFile.lastModified())/DAY_TO_MILLIS>FRESHNESS_INTERVAL_DAYS)result=false;
        return result;
    }


    private void loadBayDetailsFromRaw()
    {
        Timer timer = new Timer();
        timer.start();

        try
        {
            BufferedInputStream bufferedInputStream =
                    new BufferedInputStream (context.getResources().openRawResource(R.raw.bays_details));
            ObjectInputStream objectInputStream = new ObjectInputStream(bufferedInputStream);
            baysDetailsHashtable = (Hashtable<Integer, BayDetails>) objectInputStream.readObject();

            Log.d(TAG, "loadBayDetailsFromRaw: completed in "+timer.getDuration()+" seconds. Bay details loaded: "+baysDetailsHashtable.size());
            bufferedInputStream.close();
            objectInputStream.close();
        }  catch (FileNotFoundException e) {
            Log.d(TAG, "loadBayDetailsFromRaw: FileNotFoundException: "+e.getMessage());
        } catch (Exception e) {
            Log.d(TAG, "loadBayDetailsFromRaw: general exception: "+e.getMessage());
        }
    }

    private void loadBaysFromRaw()
    {
        Timer timer = new Timer();
        timer.start();
        try
        {
            BufferedInputStream bufferedInputStream =
                    new BufferedInputStream (context.getResources().openRawResource(R.raw.bays));
            ObjectInputStream objectInputStream = new ObjectInputStream(bufferedInputStream);
            bays = (List<Bay>) objectInputStream.readObject();

            timer.stop();
            Log.d(TAG, "loadBaysFromRaw: completed in "+timer.getDuration()+" seconds. Bays loaded: "+bays.size());

            bufferedInputStream.close();
            objectInputStream.close();
        }  catch (FileNotFoundException e) {
            Log.d(TAG, "loadBaysFromFile: FileNotFoundException: "+e.getMessage());
        } catch (Exception e) {
            Log.d(TAG, "loadBaysFromFile: general exception: "+e.getMessage());
        }
    }


    public List<Bay> getItems() {
        return this.bays;
    }

    @NonNull
    public Lifecycle getLifecycle() {
        return this.lifecycleOwner.getLifecycle();
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

            bays = (List<Bay>) objectInputStream.readObject();
            timer.stop();
            Log.d(TAG, "loadBaysFromFile: ended in "+timer.getDuration()+" seconds. Number of bays loaded:"+bays.size());
            fileInputStream.close();
            objectInputStream.close();
        }  catch (FileNotFoundException e) {
            Log.d(TAG, "loadBaysFromFile: FileNotFoundException: "+e.getMessage());
        } catch (Exception e) {
            Log.d(TAG, "loadBaysFromFile: general exception: "+e.getMessage());
        }
    }

    private void loadBayDetailsFromFile()
    {
        Timer timer = new Timer();
        timer.start();
        try
        {
            FileInputStream fileInputStream = context.openFileInput(BAYS_DETAILS_FILE);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
            ObjectInputStream objectInputStream = new ObjectInputStream(bufferedInputStream);

            this.baysDetailsHashtable = (Hashtable<Integer,BayDetails>) objectInputStream.readObject();
            timer.stop();
            Log.d(TAG, "loadBayDetailsFromFile: ended in "+timer.getDuration()+" seconds. Number of bays loaded:"+bays.size());
            Log.d(TAG, "loadBayDetailsFromFile: first item:"+this.baysDetailsHashtable.values().iterator().next().getRestrictions().get(0));
            fileInputStream.close();
            objectInputStream.close();
        }  catch (FileNotFoundException e) {
            Log.d(TAG, "\"loadBayDetailsFromFile: FileNotFoundException: "+e.getMessage());
        } catch (Exception e) {
            Log.d(TAG, "\"loadBayDetailsFromFile: general exception: "+e.getMessage());
        }
    }

    private void saveBaysToFile(List<Bay> list)
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
            Log.d(TAG, "saveBaysToFile: num of bays: "+list.size());
            objectOutputStream.writeObject(list);
            objectOutputStream.close();
            fileOutputStream.close();
        } catch (Exception e) {
            Log.d(TAG, "saveBaysToFile: "+e.getMessage());
        }
    }

    private void saveBayDetailsToFile(Hashtable<Integer,BayDetails> bayDetailsHashtable)
    {
        Log.d(TAG, "saveBayDetailsToFile: ");
        File file = new File(context.getFilesDir()+"/"+BAYS_DETAILS_FILE);
        if (file.exists())
        {
            Log.d(TAG, "saveBayDetailsToFile: a file exists, deleting it.");
            file.delete();
        }
        try {
            FileOutputStream fileOutputStream =  context.openFileOutput(BAYS_DETAILS_FILE, Context.MODE_PRIVATE);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            Log.d(TAG, "saveBaysToFile: num of site details: "+bayDetailsHashtable.size());
            objectOutputStream.writeObject(baysDetailsHashtable);
            objectOutputStream.close();
            fileOutputStream.close();
        } catch (Exception e) {
            Log.d(TAG, "saveBayDetailsToFile: ");
        }
    }

    public void saveAsJson()
    {

        Log.d(TAG, "saveAsJson: started");
        try {
            GsonBuilder gsonBuilder = new GsonBuilder();
            Gson bayGson = gsonBuilder.create();


            FileOutputStream baysFos =  context.openFileOutput(BAYS_JSON_FILE, Context.MODE_PRIVATE);
            FileOutputStream bayDetailsFos =  context.openFileOutput(BAYS_DETAILS_JSON_FILE, Context.MODE_PRIVATE);

            BufferedOutputStream bos = new BufferedOutputStream(baysFos);
            bos.write(bayGson.toJson(bays).getBytes());
            bos.flush();
            bos = new BufferedOutputStream(bayDetailsFos);
            bos.write(bayGson.toJson(baysDetailsHashtable).getBytes());

            baysFos.close();
            bayDetailsFos.close();
            bos.close();

        } catch (Exception e) {
            Log.d(TAG, "saveBayDetailsToFile: ");
        }

    }
    private void fetchApiData()
    {
        this.bays = new ArrayList<>();
        this.baysDetailsHashtable = new Hashtable<>();
        BayAdapter bayAdapter = new BayAdapter();
        ParkingApi api = ParkingApi.getInstance();
        Log.d(TAG, "fetchApiData: started");
        long startTime = System.currentTimeMillis();
        api.sitesGet()
                .subscribeOn(Schedulers.io())
                //.observeOn(AndroidSchedulers.mainThread())
                //.as(autoDisposable(AndroidLifecycleScopeProvider.from(getLifecycle(), Lifecycle.Event.ON_STOP)))
                .subscribe(value ->
                        {
                            double duration = (double) (System.currentTimeMillis()-startTime)/1000;
                            Log.d(TAG, "fetchApiData: ended: duration: "+
                                    duration+" fetched sites:"+
                                    value.size());
                            ArrayList<Object> conversionResults = bayAdapter.convertSites(value);
                            this.bays = (ArrayList<Bay>)conversionResults.get(0);
                            this.baysDetailsHashtable = (Hashtable<Integer, BayDetails>) conversionResults.get(1);
                            saveBaysToFile(this.bays);
                            saveBayDetailsToFile(this.baysDetailsHashtable);
                        },
                        throwable -> Log.d(TAG+"-throwable", throwable.getMessage()));

    }

    @Override
    protected Void doInBackground(Void... voids) {
        loadData();
        return null;
    }
}
