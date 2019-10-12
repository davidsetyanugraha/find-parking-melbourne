package com.unimelbs.parkingassistant.model;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;
import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider;
import com.unimelbs.parkingassistant.parkingapi.ParkingApi;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static com.uber.autodispose.AutoDispose.autoDisposable;

public class DataFeed implements DataFeeder, LifecycleOwner {
    private static final String TAG = "TE-DataFeed";
    private List<ClusterItem> bayList;
    public void addBays()
    {
        ParkingApi api = ParkingApi.getInstance();
        api.sitesGet()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .as(autoDisposable(AndroidLifecycleScopeProvider.from(this, Lifecycle.Event.ON_STOP)))
                .subscribe(value -> {System.out.println("Value:" + value.get(0).getDescription());},
                        throwable -> Log.d(TAG+"-throwable", throwable.getMessage()));
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
    @Override
    public Lifecycle getLifecycle() {
        return null;
    }
}
