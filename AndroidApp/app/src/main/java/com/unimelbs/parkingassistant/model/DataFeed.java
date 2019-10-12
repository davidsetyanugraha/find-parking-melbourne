package com.unimelbs.parkingassistant.model;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider;
import com.unimelbs.parkingassistant.parkingapi.ParkingApi;
import com.unimelbs.parkingassistant.parkingapi.Site;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static com.uber.autodispose.AutoDispose.autoDisposable;

public class DataFeed implements DataFeeder {
    private static final String TAG = "DataFeed";
    private static final String BAYS_FILE = "bays.dat";
    private List<Site> sites;
    Context context;
    File baysFile;
    //private LifecycleObserver lifecycleObserver;
    private LifecycleOwner lifecycleOwner;
    private List<ClusterItem> bayList;
    public DataFeed (LifecycleOwner mainActivity,
                     Context context) {
        this.lifecycleOwner = mainActivity;
        this.context = context;
    }
    public void addBays()
    {

        loadBayList();
        if (sites==null)
        {
            ParkingApi api = ParkingApi.getInstance();
            api.sitesGet()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .as(autoDisposable(AndroidLifecycleScopeProvider.from(getLifecycle(), Lifecycle.Event.ON_STOP)))
                    .subscribe(value ->
                            {
                                Log.d(TAG, "addBays: value:"+value.size());
                                saveBayList(value);
                            },
                            throwable -> Log.d(TAG+"-throwable", throwable.getMessage()));
        }
        else
        {
            Log.d(TAG, "addBays: "+sites.size());
        }

    }

    public List<ClusterItem> getBayList() {
        addBays();
        return bayList;
    }

    @Override
    public List<ClusterItem> getItems() {
        addBays();
        return this.bayList;
    }

    @NonNull
    public Lifecycle getLifecycle() {
        return this.lifecycleOwner.getLifecycle();
    }

    private void loadBayList()
    {
        Log.d(TAG, "loadBayList: ");
        try {

            FileInputStream fileInputStream = context.openFileInput(BAYS_FILE);
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            sites = (List<Site>) objectInputStream.readObject();
            Log.d(TAG, "loadBayList: num of sites: "+sites.size());
            fileInputStream.close();
            objectInputStream.close();
        }  catch (FileNotFoundException e) {
            Log.d(TAG, "loadBayList:"+e.getMessage());
        } catch (Exception e) {
            Log.d(TAG, "loadBayList:"+e.getMessage());
        }
    }
    private void saveBayList(List<Site> list)
    {
        Log.d(TAG, "saveBayList: ");
        try {

            FileOutputStream fileOutputStream =  context.openFileOutput(BAYS_FILE, Context.MODE_PRIVATE);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            Log.d(TAG, "saveBayList: ");
            objectOutputStream.writeObject(list);
            objectOutputStream.close();
            fileOutputStream.close();
        } catch (Exception e) {
            Log.d("MainActivity", "onCreate: savePerson:"+e.getMessage());
        }
    }

}
