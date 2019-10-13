package com.unimelbs.parkingassistant.model;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider;
import com.unimelbs.parkingassistant.parkingapi.ParkingApi;
import java.io.BufferedInputStream;
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

public class DataFeed {
    private static final String TAG = "DataFeed";
    private static final String BAYS_FILE = "bays.dat";
    private static final String BAYS_DETAILS_FILE = "bays_details.dat";

    Context context;
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
            FileInputStream fileInputStream = context.openFileInput(BAYS_FILE);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
            ObjectInputStream objectInputStream = new ObjectInputStream(bufferedInputStream);

            long startTime = System.currentTimeMillis();
            bays = (List<Bay>) objectInputStream.readObject();
            long duration = (System.currentTimeMillis()-startTime)/1000;
            Log.d(TAG, "loadBaysFromFile: ended, duration:"+duration+" number of sites:"+bays.size());


            Bay bay = (Bay) bays.get(0);
            Log.d(TAG, "loadBaysFromFile: first Bay:"+ bay.getBayId());
            Log.d(TAG, "loadBaysFromFile: firt Bay's position:"+bay.getRawPosition()[0]);

            fileInputStream.close();
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
}
