package com.unimelbs.parkingassistant.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;


import androidx.core.app.NotificationCompat;
import androidx.lifecycle.Lifecycle;

import com.uber.autodispose.android.lifecycle.AndroidLifecycleScopeProvider;
import com.unimelbs.parkingassistant.MapsActivity;
import com.unimelbs.parkingassistant.R;
import com.unimelbs.parkingassistant.model.Bay;
import com.unimelbs.parkingassistant.parkingapi.ParkingSiteFollower;
import com.unimelbs.parkingassistant.parkingapi.SiteState;

import org.jetbrains.annotations.NotNull;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


import static com.uber.autodispose.AutoDispose.autoDisposable;

public class BayUpdateService extends Service {

    // Binder given to clients
    private final IBinder binder = new bayUpdateServiceBinder();
    // This is the Notification Channel ID.
    public static final String NOTIFICATION_CHANNEL_ID = "channel_id";
    //User visible Channel Name
    public static final String CHANNEL_NAME = "Notification Channel";
    final int NOTIFICATION_ID = 101;

    public CompositeDisposable disposable;
    NotificationManager notificationManager;






    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class bayUpdateServiceBinder extends Binder {
        public BayUpdateService getService() {
            // Return this instance of LocalService so clients can call public methods
            return BayUpdateService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

    }

    @Override
    public void onDestroy() {

        disposeSubscription();
        try{
            notificationManager.cancel(NOTIFICATION_ID);
        } catch(Exception e){
            Log.e("ServiceOnDestroy", "notification was not cancelled onServiceDestroy");
        }

        super.onDestroy();
        Log.d("BayUpdateService", "onDestroy executed with bay disposed");
    }

    public void subscribeToServerForUpdates(@NotNull Bay selectedBay) {

        //Connect the follower to a parking bay
        disposable = new CompositeDisposable();
        ParkingSiteFollower follower = ParkingSiteFollower.getInstance();
        Disposable d = follower.createParkingBayFollower(Integer.toString(selectedBay.getBayId()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()) // to return to the main thread
                //.as(autoDisposable(AndroidLifecycleScopeProvider.from(BayUpdateService.this, Lifecycle.Event.ON_DESTROY))) //to dispose when the activity finishes
                .subscribe(bay -> {
                            //Log.v("Notification", "Value:" + bay.getId() + " status of the bay is: " + bay.getStatus()); // sample, other values are id, status, location, zone, recordState
                            bayStatusChangeNotification(bay);
                        },
                        throwable -> Log.d("debug", throwable.getMessage()), // do this on error
                        () -> Log.d("debug", "complete"));
        disposable.add(d);


    }

    public void disposeSubscription(){

        //Dispose when disposing the service or when not needed
        try {
            if (!disposable.isDisposed())
                disposable.dispose();
            Log.d("Service Dispose", "Successfully disposed the registered bay.");
        }
        catch(Exception e)
        {
            Log.e("BayUpdateService error", "Failed to dispose registered bay.");
        }

    }
    public void bayStatusChangeNotification(SiteState bay) {
        //call bay.status ..... present and occupied

        if(bay.getStatus().toLowerCase().equals("present")){ //present means a car is present so not available
            disposeSubscription();

            String title = "Bay Status Changed";
            String subject = "Parking Bay Status Changed ";
            String body = "The status of selected bay has been changed, Tap to Redirect to app";

            // Importance applicable to all the notifications in this Channel

            notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            //Notification channel should only be created for devices running Android 26
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                int importance = NotificationManager.IMPORTANCE_HIGH;

                NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, CHANNEL_NAME, importance);
                //Boolean value to set if lights are enabled for Notifications from this Channel
                notificationChannel.enableLights(true);
                //Boolean value to set if vibration are enabled for Notifications from this Channel
                notificationChannel.enableVibration(true);
                //Sets the color of Notification Light
                notificationChannel.setLightColor(Color.GREEN);
                //Set the vibration pattern for notifications. Pattern is in milliseconds with the format {delay,play,sleep,play,sleep...}
                notificationChannel.setVibrationPattern(new long[]{
                        500,
                        500,
                        500,
                        500,
                        500
                });
                //Sets whether notifications from these Channel should be visible on Lockscreen or not
                notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                notificationManager.createNotificationChannel(notificationChannel);

            }

            // Notification Channel ID passed
            // as a parameter here will be
            // ignored for all the
            // Android versions below 8.0
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
            builder.setContentTitle(title);
            builder.setContentText(body);
            builder.setSmallIcon(R.mipmap.exclaimination);
            builder.setPriority(NotificationCompat.PRIORITY_MAX);
            builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));

            Intent intent = new Intent(this, MapsActivity.class);

            new Intent(this, MapsActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 1001, intent, 0);
            //Following will set the tap action
            builder.setContentIntent(pendingIntent);


            //PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 1002, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            //builder.addAction(R.drawable.ic_launcher_background, "Redirect to Nearest Bay", pendingIntent);
            //builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.icon));
            Notification notification = builder.build();

            try
            {
                notificationManager.notify(NOTIFICATION_ID, notification);
            }
            catch(NullPointerException e)
            {
                Log.e("Error", "Error occured in raising notification");
            }

            //Toast.makeText(this, "Selected Bay Status Has Been Changed", Toast.LENGTH_LONG).show();


        }

        }
}
