package com.unimelbs.parkingassistant;

import android.app.Application;
import android.os.AsyncTask;

import androidx.lifecycle.LiveData;

import java.util.List;

public class BayRepository {
    private BayDao mBayDao;
    private LiveData<List<Bay>> mAllBays;

    public BayRepository(Application application)
    {
        BayRoomDatabase db = BayRoomDatabase.getDatabase(application);
        mBayDao = db.bayDao();
        mAllBays = mBayDao.getAllBays();
    }
    LiveData<List<Bay>> getAllBays()
    {
        return mAllBays;
    }
    private static class insertAsyncTask extends AsyncTask<Bay, Void, Void> {

        private BayDao mAsyncTaskDao;

        insertAsyncTask(BayDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final Bay... params) {
            mAsyncTaskDao.insert(params[0]);
            return null;
        }
    }

    public void insert (Bay bay)
    {
        new insertAsyncTask(mBayDao).execute(bay);
    }
}
