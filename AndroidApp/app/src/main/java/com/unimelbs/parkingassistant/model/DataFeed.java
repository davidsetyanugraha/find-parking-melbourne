package com.unimelbs.parkingassistant.model;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider;
import com.unimelbs.parkingassistant.R;
import com.unimelbs.parkingassistant.parkingapi.ParkingApi;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static com.uber.autodispose.AutoDispose.autoDisposable;

public class DataFeed {
    private static final String TAG = "DataFeed";
    private static final String BAYS_FILE = "bays.dat";
    private static final String BAYS_DETAILS_FILE = "bays_details.dat";
    private static final String BAYS_JSON_FILE = "bays.json";
    private static final String BAYS_DETAILS_JSON_FILE = "bays_details.json";
    private static final long DAY_TO_MILLIS = 1000*60*60*24;
    private static final long MINUTE_TO_MILLIS = 1000*60;
    private static final int FRESHNESS_INTERVAL_DAYS = 1;

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

    public void loadData()
    {
        if (dataFilesExist())
        {
            if(isDataUptodate())
            {
                Log.d(TAG, "loadData: data is uptodate - loading from local file");
            }
            else
            {
                Log.d(TAG, "loadData: data is stale, showing current data " +
                        "and fetching fresh data from API Asynchronously");
            }
        }
        else
        {
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

    private boolean isDataUptodate()
    {
        boolean result=true;
        long currentTime = System.currentTimeMillis();
        File baysFile = context.getFileStreamPath(BAYS_FILE);
        File bayDetailsFile = context.getFileStreamPath(BAYS_DETAILS_FILE);
        if((currentTime-baysFile.lastModified())/DAY_TO_MILLIS>FRESHNESS_INTERVAL_DAYS||
               (currentTime-bayDetailsFile.lastModified())/DAY_TO_MILLIS>FRESHNESS_INTERVAL_DAYS)result=false;
        return result;
    }

    public void fetchBays()
    {
        loadBaysFromFile();
        if (bays.size()==0)
        {
            BayAdapter bayAdapter = new BayAdapter();
            ParkingApi api = ParkingApi.getInstance();
            Log.d(TAG, "fetchBays: started");
            long startTime = System.currentTimeMillis();
            api.sitesGet()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(autoDisposable(AndroidLifecycleScopeProvider.from(getLifecycle(), Lifecycle.Event.ON_STOP)))
                    .subscribe(value ->
                            {
                                double duration = (double) (System.currentTimeMillis()-startTime)/1000;
                                Log.d(TAG, "fetchBays: ended: duration: "+
                                        duration+" fetched sites:"+
                                        value.size());
                                bayAdapter.convertSites(value,this.bays,baysDetailsHashtable);
                                saveBaysToFile(this.bays);
                                saveBayDetailsToFile(this.baysDetailsHashtable);
                            },
                            throwable -> Log.d(TAG+"-throwable", throwable.getMessage()));

         }
        else
        {
            loadBayDetailsFromFile();
        }
    }

    public void loadBayDetailsFromFile()
    {
        long startTime = System.currentTimeMillis();
        try
        {
            BufferedInputStream bis =
                    new BufferedInputStream (context.getResources().openRawResource(R.raw.bays));

            //context.getResources().openRawResource(R.raw.)
            //InputStream inputStream = getResources().openRawResource(R.raw.radar_search);
            //context.getResources().openRawResource()

            FileInputStream fileInputStream = context.openFileInput(BAYS_DETAILS_FILE);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
            ObjectInputStream objectInputStream = new ObjectInputStream(bufferedInputStream);
            baysDetailsHashtable = (Hashtable<Integer, BayDetails>) objectInputStream.readObject();
            //BayDetails bayDetails = baysDetailsHashtable.values().iterator().next();
            long duration = (System.currentTimeMillis()-startTime)/1000;
            Log.d(TAG, "loadBayDetailsFromFile: load ended, duration:"+duration);

            fileInputStream.close();
            bufferedInputStream.close();
            objectInputStream.close();
        }  catch (FileNotFoundException e) {
            Log.d(TAG, "loadBaysFromFile: FileNotFoundException: "+e.getMessage());
        } catch (Exception e) {
            Log.d(TAG, "loadBaysFromFile: general exception: "+e.getMessage());
        }
    }


    public List<Bay> getItems() {
        //fetchBays();
        return this.bays;
    }

    @NonNull
    public Lifecycle getLifecycle() {
        return this.lifecycleOwner.getLifecycle();
    }

    private void loadBaysFromFile()
    {
        try
        {
            //FileInputStream fileInputStream = context.openFileInput(BAYS_FILE);
            //BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
            BufferedInputStream bufferedInputStream =
                    new BufferedInputStream (context.getResources().openRawResource(R.raw.bays));
            ObjectInputStream objectInputStream = new ObjectInputStream(bufferedInputStream);

            long startTime = System.currentTimeMillis();
            bays = (List<Bay>) objectInputStream.readObject();
            long duration = (System.currentTimeMillis()-startTime)/1000;
            Log.d(TAG, "loadBaysFromFile: ended, duration:"+duration+" number of sites:"+bays.size());


            Bay bay = (Bay) bays.get(0);
            Log.d(TAG, "loadBaysFromFile: first Bay:"+ bay.getBayId());
            Log.d(TAG, "loadBaysFromFile: firt Bay's position:"+bay.getRawPosition()[0]);

            //fileInputStream.close();
            objectInputStream.close();
        }  catch (FileNotFoundException e) {
            Log.d(TAG, "loadBaysFromFile: FileNotFoundException: "+e.getMessage());
        } catch (Exception e) {
            Log.d(TAG, "loadBaysFromFile: general exception: "+e.getMessage());
        }
    }

    private void saveBaysToFile(List<Bay> list)
    {
        Log.d(TAG, "saveBaysToFile: ");
        try {
            FileOutputStream fileOutputStream =  context.openFileOutput(BAYS_FILE, Context.MODE_PRIVATE);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            Log.d(TAG, "saveBaysToFile: num of serialisable sites"+list.size());
            objectOutputStream.writeObject(list);
            objectOutputStream.close();
            fileOutputStream.close();
        } catch (Exception e) {
            Log.d("MainActivity", "onCreate: savePerson:"+e.getMessage());
        }
    }

    private void saveBayDetailsToFile(Hashtable<Integer,BayDetails> bayDetailsHashtable)
    {
        Log.d(TAG, "saveBaysToFile: ");
        try {
            FileOutputStream fileOutputStream =  context.openFileOutput(BAYS_DETAILS_FILE, Context.MODE_PRIVATE);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            Log.d(TAG, "saveBaysToFile: num of serialisable sites"+bayDetailsHashtable.size());
            objectOutputStream.writeObject(baysDetailsHashtable);
            objectOutputStream.close();
            fileOutputStream.close();
        } catch (Exception e) {
            Log.d(TAG, "saveBayDetailsToFile: ");
        }
    }

    public void saveAsJson()
    {
        long startTime = System.currentTimeMillis();
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
        Log.d(TAG, "saveAsJson: duration:"+(System.currentTimeMillis()-startTime)/1000);
    }

}
