package com.unimelbs.parkingassistant;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;

import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.maps.model.LatLng;
import com.unimelbs.parkingassistant.broadcastreceivers.ForegroundServiceStopper;
import com.unimelbs.parkingassistant.model.Bay;
import com.unimelbs.parkingassistant.parkingapi.ParkingSiteFollower;
import com.unimelbs.parkingassistant.parkingapi.SiteState;

import org.apache.commons.lang3.time.DateUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Calendar;
import java.util.Date;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


import static com.uber.autodispose.AutoDispose.autoDisposable;

public class BayUpdateService extends Service {

    // Binder given to clients
    private final IBinder binder = new bayUpdateServiceBinder();

    // These are Notification Channel IDs.
    // Notification Channel ID passed
    // as a parameter will be ignored
    // for all the Android versions below
    // 8.0. Notification channel should only
    // be created for devices running Android 26
    static final String NOTIFICATION_CHANNEL_ID_BAY_UPDATE = "channel_id_bay_update";
    static final String NOTIFICATION_CHANNEL_ID_BAY_FOLLOW = "channel_id_bay_follow";
    static final String NOTIFICATION_CHANNEL_ID_SERVICE_KILLED = "channel_id_service_killed";
    static final String NOTIFICATION_CHANNEL_ID_START_PARKING = "channel_id_start_parking";
    //User visible Channel Name
    static final String CHANNEL_NAME = "Notification Channel";

    static Boolean hasSubscribed = false;
    static Bay selectedBayId = null;
    private CompositeDisposable disposable;
    NotificationManager notificationManager;
    static boolean isServiceRunning = false;


    // Below are notifications ID.
    // These IDs will be used to identify
    // a notification and perform operation
    // on it depending upon requirement.

    final Integer START_PARKING_NOTIFICATION_ID = 5;
    final Integer BAY_STATUS_UPDATE_NOTIFICATION_ID = 2;
    final Integer SERVICE_KILLED_WHILE_SUBSCRIBED_TO_BAY_ID = 3;
    final Integer FOLLOWING_BAY_NOTIFICATION_ID = 4;

    private Handler handler = new Handler();
    private Runnable runnable;
    private Boolean parkingNotificationFirstRun ;
    String countDownTimer = null;
    long startParkingTime;
    Date endParkingDate;
    NotificationCompat.Builder firstParkingNotificationBuilder;
    String requiredParkingDuration;

    // The external map intent on which
    // navigation to parking bay will
    // start.
    Intent externalMapIntent;




    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same processRestrictionChecking as its clients, we don't need to deal with IPC.
     */
    class bayUpdateServiceBinder extends Binder {
        BayUpdateService getService() {
            // Return this instance of service so clients can call public methods
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

    }


    void stopMyService() {
        try{

            stopForeground(true);
            handler.removeCallbacks(runnable);
            stopSelf();
            isServiceRunning = false;
            Log.d("StopMyService", "stopMyService called to stop the serviceS");

        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d("service", "oNStartC0mmand called");
        if (intent != null && intent.getAction().equals("ACTION_START_SERVICE")) {
            startService();
        }
        else stopMyService();
        return START_STICKY;
    }
    @Override
    public void onDestroy() {
        stopForeground(true);
        isServiceRunning = false;
        handler.removeCallbacks(runnable);
        //this.serviceKilledWhileStillSubscribedNotification();
        if(hasSubscribed) {

            disposeSubscription();
            Log.d("ServiceOnDestroy", "Dispose Service Called From Service OnDEstroy");
        }
        super.onDestroy();
        Log.d("ServiceOnDestroy", "Service onDestroy executed.");
    }

    public void subscribeToServerForUpdatesAndRaiseNotify(@NotNull Bay selectedBay) {
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
                disposable.dispose();

            }


        }
        catch(Exception e)
        {
            Log.e("BayServerUnsubscrbe", "Failed to dispose registered bay.");
        }

    }


    /*
    The method below will create an external
    map intent that will be used for turn by
    turn navigation to selected bay.
     */
    void navigateToTheSelectedBayWithSubscription(Bay selectedBay, Boolean serverSubscriptionRequired)
    {
        if (serverSubscriptionRequired)
        {   //In the last catch it is disposed
            // if no maps method is available
            subscribeToServerForUpdatesAndRaiseNotify(selectedBay);

        }
        else
        {
            Toast.makeText(this, "No Bay Is Being Tracked", Toast.LENGTH_LONG).show();
            notificationManager.cancelAll();
        }
        // Placed outside of Subscription
        // so that even if a bay is selected
        // without subscription, Maps Activity
        // will focus to that location
        BayUpdateService.selectedBayId = selectedBay;
        LatLng selectedBayLatLng = selectedBay.getPosition();
        String lat = String.valueOf(selectedBayLatLng.latitude);
        String lon = String.valueOf(selectedBayLatLng.longitude);


        // Part of the code below is taken from
        // https://stackoverflow.com/questions/
        // 2662531/launching-google-maps-directions
        // -via-an-intent-on-android?rq=1

        Uri navigationIntentUri = Uri.parse("google.navigation:q=" + lat + "," + lon);
        Log.d("Navigation Uri", "Navigation URI is " + navigationIntentUri);


        externalMapIntent = new Intent(Intent.ACTION_VIEW, navigationIntentUri);
        externalMapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        externalMapIntent.setPackage("com.google.android.apps.maps");


        try
        {
            startActivity(externalMapIntent);
        }
        catch (ActivityNotFoundException ex)
        {
            try
            {
                Intent unrestrictedIntent = new Intent(Intent.ACTION_VIEW, navigationIntentUri);
                startActivity(unrestrictedIntent);
            }
            catch (ActivityNotFoundException innerEx)
            {
                Toast.makeText(this, "No Map Application Found, Please install one", Toast.LENGTH_LONG).show();
                Log.d("Failure", "Failed To Open any navigation method " );
                disposeSubscription();

            }
        }
    }

    /*
    If dut to extreme memory constraints situation,
    the foreground enabled service notification is
    closed, This notification will alert the user
    that further monitoring is not possible.
     */
    private void serviceKilledWhileStillSubscribedNotification ()
    {
        try {
            String title = "Bay Status";
            //String subject = "Parking Bay Status Changed ";
            String body = "Bay Tracking has stopped";



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

                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build();
                notificationChannel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                        audioAttributes);
                //Sets whether notifications from these Channel should be visible on Lockscreen or not
                notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                notificationManager.createNotificationChannel(notificationChannel);
            }
            builder.setContentTitle(title);
            builder.setContentText(body);
            builder.setSmallIcon(R.drawable.ic_green_exclamation_mark);
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


    private void bayStatusChangeHandlerWithNotification(SiteState bay) {
        //call bay.status ..... present and occupied

        if (bay.getStatus().toLowerCase().equals("present")) { //present means a car is present so not available
            disposeSubscription();

            try {

                String title = "Bay Status";
                String subject = "Parking Bay Status Changed ";
                String body = "Selected bay "+ bay.getId()+ " occupied. Tap to select new bay. ";

                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID_BAY_UPDATE);

                Intent applicationIntent = getPackageManager()
                        .getLaunchIntentForPackage(getPackageName())
                        .setPackage(null)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                PendingIntent pendingIntentToGoApp = PendingIntent.getActivity(this, 0, applicationIntent, 0);



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
                    AudioAttributes audioAttributes = new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .build();
                    notificationChannel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                            audioAttributes);

                    //Sets whether notifications from these Channel should be visible on Lockscreen or not
                    notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                    notificationManager.createNotificationChannel(notificationChannel);
                }

                builder.setContentTitle(title);
                builder.setContentText(body);
                builder.setSmallIcon(R.drawable.ic_red_exclamation_mark);
                builder.setPriority(NotificationCompat.PRIORITY_HIGH);
                builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
                builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
                //Following will set the tap action
                builder.setContentIntent(pendingIntentToGoApp);


                Notification notification = builder.build();

                try {
                    stopForeground(true);
                    notificationManager.cancel(FOLLOWING_BAY_NOTIFICATION_ID);
                }catch (Exception e){
                    Log.e("CancelNotifyService", "Failed to cancel notification FOLLOWING_BAY_NOTIFICATION_ID ");
                }

                notificationManager.notify(BAY_STATUS_UPDATE_NOTIFICATION_ID, notification);

                //startForeground(BAY_STATUS_UPDATE_NOTIFICATION_ID , notification);




            } catch (Exception e) {
                Log.e("Error", "Error occured in raising Bay Status Changed notification" + e.getMessage());
            }

        }
    }


    private void followingBayIdNotification(String bayId) {


        try {

            try {
                //notificationManager.cancel(BAY_STATUS_UPDATE_NOTIFICATION_ID);
                //stopForeground(true);
                notificationManager.cancelAll();
            }catch (Exception e){
                Log.e("CancelNotifyService", "Failed to cancel notification BAY_STATUS_UPDATE_NOTIFICATION_ID ");
            }


            String title = "Following Bay";
            String body = "Parking Assistant is tracking bay "+bayId+ " for updates.";



            NotificationCompat.Builder builder = new NotificationCompat.Builder(this,
                    NOTIFICATION_CHANNEL_ID_BAY_FOLLOW);

            Intent intentHide = new Intent(this, ForegroundServiceStopper.class);
            PendingIntent exitNavigation = PendingIntent.getBroadcast(this,
                    (int) System.currentTimeMillis(), intentHide, PendingIntent.FLAG_UPDATE_CURRENT);;


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                int importance = NotificationManager.IMPORTANCE_HIGH;

                NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID_BAY_FOLLOW,
                        CHANNEL_NAME, importance);
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
            builder.setSmallIcon(R.drawable.ic_green_exclamation_mark);
            builder.setPriority(NotificationCompat.PRIORITY_MAX);
            builder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE);
            builder.setWhen(0);
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));

            builder.addAction(0, "Stop Following Bay", exitNavigation);

            // No need to tap enable this notification
            //builder.setContentIntent(contentPendingIntent);

            Notification notification = builder.build();



            notificationManager.notify(FOLLOWING_BAY_NOTIFICATION_ID , notification);
            startForeground(FOLLOWING_BAY_NOTIFICATION_ID , notification);



            //Toast.makeText(this, "Selected Bay Status Has Been Changed", Toast.LENGTH_LONG).show();
        }
        catch(Exception e)
        {
            Log.e("Error", "Error occured in raising Following Bay ID Notification" + e.getMessage());
        }

    }

    void startParkingNotification(Bay bay, Long startParkingTime, String parkingDuration){
        this.requiredParkingDuration = parkingDuration;
        this.startParkingTime = startParkingTime;
        BayUpdateService.selectedBayId = bay;
        this.parkingNotificationFirstRun = true;
        //int amount = (int) (Float.parseFloat(this.requiredParkingDuration) * 60);
        //this.endParkingDate = (Date) DateUtils.addMinutes(new Date(), amount);
        int amount = (int) (Float.parseFloat(this.requiredParkingDuration));
        this.endParkingDate = (Date) DateUtils.addSeconds(new Date(), amount);

        //Cancel previously active notification
        // close handler of previous notification
        try {
            if(parkingNotificationFirstRun) {
                stopForeground(true);
                handler.removeCallbacks(runnable);
                notificationManager.cancelAll();
                Log.d("notcancel", "All previous notifications have been cancelled before adding new");
            }
        } catch (Exception e) {
            Log.e("CancelNotifyService", "Failed to cancel notification START_PARKING_NOTIFICATION_ID ");
        }


        //Date currentTime = Calendar.getInstance().getTime();
        //displayParkingNotification(currentTime.toString());
        countDownStart();
    }

    private void countDownStart() {

        runnable = new Runnable() {


            @Override
            public void run() {
                try {

                    handler.postDelayed(this, 1000);
                    Date current_date = new Date();

                    long diff = endParkingDate.getTime()-current_date.getTime() ;
                    //long days = diff / (24 * 60 * 60 * 1000);
                    long Hours = diff / (60 * 60 * 1000) % 24;
                    long Minutes = diff / (60 * 1000) % 60;
                    long Seconds = diff / 1000 % 60;

                    //countDownTimer = days+":"+Hours + ":"+ Minutes+":"+Seconds;
                    countDownTimer = Hours + ":"+ Minutes+":"+Seconds;
                    //Log.d("parkingNot", "Parking Notification first Run Status "
                            //+ parkingNotificationFirstRun.toString() );

                    if (parkingNotificationFirstRun){
                        displayParkingNotification(countDownTimer, parkingNotificationFirstRun );
                        parkingNotificationFirstRun = false;
                    }
                    else
                    {

                        displayParkingNotification(countDownTimer, parkingNotificationFirstRun );
                    }



                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        handler.postDelayed(runnable, 0);
    }


    private void displayParkingNotification(String countDownTime, Boolean parkingNotificationFirstRun) {

        String bayId = Integer.toString(BayUpdateService.selectedBayId.getBayId());

        try {

             //Cancelling of previous notification is handled in
            // startParkingNotificationMethod
            if(parkingNotificationFirstRun) {

                this.firstParkingNotificationBuilder = new NotificationCompat.Builder(this,
                        NOTIFICATION_CHANNEL_ID_START_PARKING);
                Date currentTime = Calendar.getInstance().getTime();
                String title = "Parking Duration Left: " + countDownTime;
                String body = "Car parked on " +currentTime+ ".";

                Intent intentHide = new Intent(this, ForegroundServiceStopper.class);
                PendingIntent exitNavigation = PendingIntent.getBroadcast(this,
                        (int) System.currentTimeMillis(), intentHide, PendingIntent.FLAG_UPDATE_CURRENT);

                Intent walkUsingGmap = this.getWalkToBayIntent();
                PendingIntent startWalk = PendingIntent.getActivity(this, 100, walkUsingGmap, 0);


                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    int importance = NotificationManager.IMPORTANCE_HIGH;

                    NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID_START_PARKING,
                            CHANNEL_NAME, importance);
                    //Boolean value to set if lights are enabled for Notifications from this Channel
                    notificationChannel.enableLights(true);
                    //Sets the color of Notification Light
                    notificationChannel.setLightColor(Color.GREEN);
                    //Sets whether notifications from these Channel should be visible on Lockscreen or not
                    notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                    AudioAttributes audioAttributes = new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .build();
                    notificationChannel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                            audioAttributes);

                    notificationChannel.setVibrationPattern(new long[]{
                            500,
                            500,
                            500,
                            500,
                            500
                    });
                    notificationManager.createNotificationChannel(notificationChannel);
                }

                this.firstParkingNotificationBuilder.setContentTitle(title);
                this.firstParkingNotificationBuilder.setContentText(body);
                this.firstParkingNotificationBuilder.setSmallIcon(R.drawable.ic_green_exclamation_mark);
                this.firstParkingNotificationBuilder.setPriority(NotificationCompat.PRIORITY_MAX);
                this.firstParkingNotificationBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
                this.firstParkingNotificationBuilder.setWhen(0);

                this.firstParkingNotificationBuilder.setAutoCancel(false);
                this.firstParkingNotificationBuilder.setOngoing(true);
                this.firstParkingNotificationBuilder.setOnlyAlertOnce(true);
                this.firstParkingNotificationBuilder.addAction(0, "Walk To Parking Bay", startWalk);
                this.firstParkingNotificationBuilder.addAction(0, "Stop Parking", exitNavigation);
                this.firstParkingNotificationBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));

                notificationManager.notify(START_PARKING_NOTIFICATION_ID, this.firstParkingNotificationBuilder.build());
                startForeground(START_PARKING_NOTIFICATION_ID, this.firstParkingNotificationBuilder.build());

            }
            else
            {   int day_index_add = 0;
                String[] arrOfRemainingDuration = countDownTime.split(":");
                Integer hours = Integer.parseInt(arrOfRemainingDuration[0+ day_index_add]);
                Integer minutes = Integer.parseInt(arrOfRemainingDuration[1 + day_index_add]);
                Integer seconds = Integer.parseInt(arrOfRemainingDuration[2 + day_index_add]);

                Integer duration= hours * 60 + minutes; //duration in minutes

                int TEN_MINUTES_LEFT = 9;
                if (duration.equals(TEN_MINUTES_LEFT)) { // duration left == 10 mins specified by 9

                    this.firstParkingNotificationBuilder.setSmallIcon(R.drawable.ic_red_exclamation_mark);
                    this.firstParkingNotificationBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));

                 }

                if (!((int) (hours+minutes+seconds) <= 0)) {
                    //If duration left is not less than zero show duration left
                    this.firstParkingNotificationBuilder.setContentTitle("Parking Duration Left: " + countDownTime);
                    notificationManager.notify(START_PARKING_NOTIFICATION_ID, this.firstParkingNotificationBuilder.build());
                }
                else{// If duration left is less than zero, show overdue.

                    if ((hours + minutes + seconds) == 0)
                    {
                        this.firstParkingNotificationBuilder.setColor(Color.RED);
                        this.firstParkingNotificationBuilder.setSmallIcon(R.drawable.ic_red_exclamation_mark);
                        this.firstParkingNotificationBuilder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));

                    }


                    this.firstParkingNotificationBuilder.setContentTitle("PARKING OVER DUE BY: " + countDownTime.replace('-',' '));
                    notificationManager.notify(START_PARKING_NOTIFICATION_ID, this.firstParkingNotificationBuilder.build());

                }
            }


        }
        catch(Exception e)
            {
                Log.e("Error", "Error occured in raising Start PArkking Bay ID Notification: " + e.getMessage());
            }


    }

    private Intent getWalkToBayIntent()
    {
        LatLng selectedBayLatLng = BayUpdateService.selectedBayId.getPosition();
        String lat = String.valueOf(selectedBayLatLng.latitude);
        String lon = String.valueOf(selectedBayLatLng.longitude);


        // Part of the code below is taken from
        // https://stackoverflow.com/questions/
        // 2662531/launching-google-maps-directions
        // -via-an-intent-on-android?rq=1

        Uri walkIntentUri = Uri.parse("google.navigation:q=" + lat + "," + lon+"&mode=w");
        Log.d("Walk Uri", "Walk URI is " + walkIntentUri);
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, walkIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        return mapIntent;
    }



}
