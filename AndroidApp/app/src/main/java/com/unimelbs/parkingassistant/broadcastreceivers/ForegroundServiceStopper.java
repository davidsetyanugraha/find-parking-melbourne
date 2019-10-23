package com.unimelbs.parkingassistant.broadcastreceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import static android.content.Context.MODE_PRIVATE;
import static com.unimelbs.parkingassistant.util.PreferenceManager.PREFERENCE_NAME;
import static com.unimelbs.parkingassistant.util.PreferenceManager.clearPreference;

import com.unimelbs.parkingassistant.BayUpdateService;
import com.unimelbs.parkingassistant.util.PreferenceManager;

public class ForegroundServiceStopper extends BroadcastReceiver {
    public static final int REQUEST_CODE = 333;

    @Override
    public void onReceive(Context context, Intent intent) {
        try
        {
        Intent service = new Intent(context, BayUpdateService.class);

        context.stopService(service);
        Log.d("BRForeGround", "Broadcast received to stop the bayUpdateService");
        SharedPreferences prefs = context.getSharedPreferences(PREFERENCE_NAME, MODE_PRIVATE);
        if (PreferenceManager.isAvailable(prefs)) {
            clearPreference(prefs);
        }


        } catch(Exception e){
            e.printStackTrace();
        }
    }
}