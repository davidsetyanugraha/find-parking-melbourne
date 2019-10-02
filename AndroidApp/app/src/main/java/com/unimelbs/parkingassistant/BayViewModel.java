package com.unimelbs.parkingassistant;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

public class BayViewModel extends AndroidViewModel {
    private BayRepository mRepository;
    private LiveData<List<Bay>> mAllBays;

    public BayViewModel (Application application) {
        super(application);
        mRepository = new BayRepository(application);
        mAllBays = mRepository.getAllBays();
    }

    LiveData<List<Bay>> getAllBays() { return mAllBays; }

    public void insert(Bay bay) { mRepository.insert(bay); }

}
