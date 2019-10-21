package com.unimelbs.parkingassistant;

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


import androidx.core.app.NotificationCompat;

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
    public static final String NOTIFICATION_CHANNEL_ID_BAY_UPDATE = "channel_id_bay_update";
    public static final String NOTIFICATION_CHANNEL_ID_BAY_FOLLOW = "channel_id_bay_follow";
    public static final String NOTIFICATION_CHANNEL_ID_SERVICE_KILLED = "channel_id_service_killed";
    //User visible Channel Name
    public static final String CHANNEL_NAME = "Notification Channel";

    private Boolean hasSubscribed = false;
    private CompositeDisposable disposable;
    NotificationManager notificationManager;
    public static boolean isServiceRunning = false;
    final Integer START_SERVICE_NOTIFICATION_ID = 1;
    final Integer BAY_STATUS_UPDATE_NOTIFICATION_ID = 2;
    final Integer SERVICE_KILLED_WHILE_SUBSCRIBED_TO_BAY_ID = 3;
    final Integer FOLLOWING_BAY_NOTIFICATION_ID = 4;


    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    class bayUpdateServiceBinder extends Binder {
        BayUpdateService getService() {
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
        startService();
    }

    void startService() {
        if (isServiceRunning) return;
        isServiceRunning = true;
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        /*
        try {
            String title = "Parking Assistant";
            String body = "Parking Assistant Is Running";


            //notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            // Notification Channel ID passed
            // as a parameter here will be
            // ignored for all the
            // Android versions below 8.0
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
            Intent notificationIntent = new Intent(getApplicationContext(), MapsActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            PendingIntent contentPendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                int importance = NotificationManager.IMPORTANCE_LOW;

                NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, CHANNEL_NAME, importance);
                //Boolean value to set if lights are enabled for Notifications from this Channel
                notificationChannel.enableLights(true);
                //Sets the color of Notification Light
                notificationChannel.setLightColor(Color.GREEN);
                //Sets whether notifications from these Channel should be visible on Lockscreen or not
                notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                notificationManager.createNotificationChannel(notificationChannel);
            }

            builder.setContentTitle(title);
            builder.setContentText(body);
            builder.setSmallIcon(R.mipmap.green_exclaimination);
            builder.setPriority(NotificationCompat.PRIORITY_LOW);
            builder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE);
            builder.setContentIntent(contentPendingIntent);

            Notification notification = builder.build();

            notificationManager.notify(START_SERVICE_NOTIFICATION_ID, notification);

            startForeground(START_SERVICE_NOTIFICATION_ID, notification);

            //Toast.makeText(this, "Selected Bay Status Has Been Changed", Toast.LENGTH_LONG).show();
        }
        catch(Exception e)
        {
            Log.e("Error", "Error occured in raising Start Service notification");
        } */

    }

    void followingBayIdNotification(String bayId) {


        try {
            String title = "Following Bay";
            String body = "Parking Assistant is tracking bay "+bayId+ " for updates.";


            //notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            // Notification Channel ID passed
            // as a parameter here will be
            // ignored for all the
            // Android versions below 8.0
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID_BAY_FOLLOW);
            Intent notificationIntent = new Intent(getApplicationContext(), MapsActivity.class);
            //notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            PendingIntent contentPendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                int importance = NotificationManager.IMPORTANCE_LOW;

                NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID_BAY_FOLLOW, CHANNEL_NAME, importance);
                //Boolean value to set if lights are enabled for Notifications from this Channel
                notificationChannel.enableLights(true);
                //Sets the color of Notification Light
                notificationChannel.setLightColor(Color.GREEN);
                //Sets whether notifications from these Channel should be visible on Lockscreen or not
                notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                notificationManager.createNotificationChannel(notificationChannel);
            }

            builder.setContentTitle(title);
            builder.setContentText(body);
            builder.setSmallIcon(R.mipmap.green_exclaimination);
            builder.setPriority(NotificationCompat.PRIORITY_LOW);
            builder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE);
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));

            // No need to tap enable this notification
            //builder.setContentIntent(contentPendingIntent);

            Notification notification = builder.build();

            try {
                notificationManager.cancel(BAY_STATUS_UPDATE_NOTIFICATION_ID);
            }catch (Exception e){
                Log.e("CancelNotifyService", "Failed to cancel notification BAY_STATUS_UPDATE_NOTIFICATION_ID ");
            }

            notificationManager.notify(FOLLOWING_BAY_NOTIFICATION_ID , notification);

            startForeground(FOLLOWING_BAY_NOTIFICATION_ID , notification);



            //Toast.makeText(this, "Selected Bay Status Has Been Changed", Toast.LENGTH_LONG).show();
        }
        catch(Exception e)
        {
            Log.e("Error", "Error occured in raising Following Bay ID Notification" + e.getMessage());
        }

    }

    void stopMyService() {
        stopForeground(true);
        stopSelf();
        isServiceRunning = false;
        Log.d("StopMyService", "stopMyService called to stop the serviceS");

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction().equals("ACTION_START_SERVICE")) {
            startService();
        }
        else stopMyService();
        return START_STICKY;
    }
    @Override
    public void onDestroy() {

        isServiceRunning = false;
        if(hasSubscribed) {

            disposeSubscription();
            this.serviceKilledWhileStillSubscribedNotification();
            Log.d("ServiceOnDestroy", "Dispose Service Called From Service OnDEstroy");
        }
        super.onDestroy();
        Log.d("ServiceOnDestroy", "Service onDestroy executed.");
    }

    public void subscribeToServerForUpdates(@NotNull Bay selectedBay) {
        try
        {
            // If after selecting a bay,
            // user changes his mind and select
            // a new bay, preious bay must be
            // disposed off.
            if(hasSubscribed){
                this.disposeSubscription();
            }

            //Connect the follower to a parking bay
            disposable = new CompositeDisposable();
            ParkingSiteFollower follower = ParkingSiteFollower.getInstance();
            Disposable d = follower.createParkingBayFollower(Integer.toString(selectedBay.getBayId()))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread()) // to return to the main thread
                    //.as(autoDisposable(AndroidLifecycleScopeProvider.from(BayUpdateService.this, Lifecycle.Event.ON_DESTROY))) //to dispose when the activity finishes
                    .subscribe(bay -> {
                                //Log.v("Notification", "Value:" + bay.getId() + " status of the bay is: " + bay.getStatus()); // sample, other values are id, status, location, zone, recordState
                                bayStatusChangeHandlerWithNotification(bay);
                            },
                            throwable -> Log.d("debug", throwable.getMessage()), // do this on error
                            () -> Log.d("debug", "complete"));
            disposable.add(d);
            hasSubscribed = true;
            followingBayIdNotification(Integer.toString(selectedBay.getBayId()));

        }
        catch(Exception e)
        {
            Log.e("BayServerSubscription", "Failed to subscribe the bay to server");
        }

    }

    public void disposeSubscription(){

        //Dispose when disposing the service or when not needed
        try {
            if (!disposable.isDisposed())
            {
                hasSubscribed = false;
                Log.d("Service Dispose", "Disposing Registered Bay");
                // For this version of app
                // when we dispose a subscription
                // we close the service as well
                // and does not wait for the
                // bay to become available.
                //stopMyService();
                disposable.dispose();

            }


        }
        catch(Exception e)
        {
            Log.e("BayServerUnsubscrbe", "Failed to dispose registered bay.");
        }

    }
    private void bayStatusChangeHandlerWithNotification(SiteState bay) {
        //call bay.status ..... present and occupied

        if (bay.getStatus().toLowerCase().equals("present")) { //present means a car is present so not available
            disposeSubscription();

            try {

                String title = "Bay Status";
                String subject = "Parking Bay Status Changed ";
                String body = "Selected bay "+ bay.getId()+ " has been occupied. Tap to select new bay.";

                //notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                // Notification Channel ID passed
                // as a parameter here will be
                // ignored for all the
                // Android versions below 8.0
                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID_BAY_UPDATE);

                Intent notificationIntent = getPackageManager()
                        .getLaunchIntentForPackage(getPackageName())
                        .setPackage(null)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

                /* If you need to set broadcast receiver to update app*/
                //PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 1002, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                //builder.addAction(R.drawable.ic_launcher_background, "Redirect to Nearest Bay", pendingIntent);
                //builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.icon));

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    int importance = NotificationManager.IMPORTANCE_HIGH;

                    NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID_BAY_UPDATE, CHANNEL_NAME, importance);
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

                builder.setContentTitle(title);
                builder.setContentText(body);
                builder.setSmallIcon(R.mipmap.exclaimination);
                builder.setPriority(NotificationCompat.PRIORITY_HIGH);
                builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
                builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
                //Following will set the tap action
                builder.setContentIntent(pendingIntent);


                Notification notification = builder.build();

                try {
                    notificationManager.cancel(FOLLOWING_BAY_NOTIFICATION_ID);
                }catch (Exception e){
                    Log.e("CancelNotifyService", "Failed to cancel notification FOLLOWING_BAY_NOTIFICATION_ID ");
                }

                notificationManager.notify(BAY_STATUS_UPDATE_NOTIFICATION_ID, notification);

                startForeground(BAY_STATUS_UPDATE_NOTIFICATION_ID , notification);




            } catch (Exception e) {
                Log.e("Error", "Error occured in raising Bay Status Changed notification" + e.getMessage());
            }

        }
    }

    private void serviceKilledWhileStillSubscribedNotification ()
    {
            try {
                String title = "Bay Status";
                String subject = "Parking Bay Status Changed ";
                String body = "The Application Has Been Killed. Not tracking any parking bay for updates";

                //Notification channel should only be created for devices running Android 26
                //notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

                // Notification Channel ID passed
                // as a parameter here will be
                // ignored for all the
                // Android versions below 8.0
                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID_SERVICE_KILLED);


                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    int importance = NotificationManager.IMPORTANCE_HIGH;

                    NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID_SERVICE_KILLED, CHANNEL_NAME, importance);
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
                builder.setContentTitle(title);
                builder.setContentText(body);
                builder.setSmallIcon(R.mipmap.exclaimination);
                builder.setPriority(NotificationCompat.PRIORITY_HIGH);
                builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
                builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));

                Notification notification = builder.build();

                try {
                    notificationManager.cancelAll();
                }catch (Exception e){
                    Log.e("CancelNotifyService", "Failed to cancel all notification while " +
                            "raising application killed notification ");
                }

                notificationManager.notify(SERVICE_KILLED_WHILE_SUBSCRIBED_TO_BAY_ID, notification);



            }
            catch (Exception e)
            {
                Log.e("Error", "Error occurred in raising Service Killed While Still Subscribed notification");
            }



    }
}
