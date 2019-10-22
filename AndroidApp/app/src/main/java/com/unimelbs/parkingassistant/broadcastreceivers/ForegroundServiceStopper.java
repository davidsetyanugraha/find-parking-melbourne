package com.unimelbs.parkingassistant.broadcastreceivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.unimelbs.parkingassistant.BayUpdateService;

public class ForegroundServiceStopper extends BroadcastReceiver {
    public static final int REQUEST_CODE = 333;

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent service = new Intent(context, BayUpdateService.class);
        context.stopService(service);
        Log.d("BRForeGround", "Broadcast received to stop the bayUpdateService");
    }
}