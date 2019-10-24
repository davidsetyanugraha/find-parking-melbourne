package com.unimelbs.parkingassistant.parkingapi;

import android.util.Log;

import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;
import com.microsoft.signalr.HubConnectionState;
import com.unimelbs.parkingassistant.util.Constants;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

/**
 * Handles the interaction to follow a parking bay an listen to the server notifications
 */
public class ParkingSiteFollower {

    private static ParkingSiteFollower instance;
    private ParkingApi api;

    private ParkingSiteFollower() {
        this.api = ParkingApi.getInstance();
    }

    /**
     * Defines the connection with the SignalRHub.
     */
    private class InnerHubConnection {
        private HubConnection hub;
        private boolean bayFollowed;

        public  InnerHubConnection(HubConnection hub) {
            this.hub = hub;
            this.bayFollowed = false;
        }

        public HubConnection getHub() {
            return hub;
        }

        public boolean isBayFollowed() {
            return bayFollowed;
        }

        public void setBayFollowed(boolean bayFollowed) {
            this.bayFollowed = bayFollowed;
        }
    }

    /**
     * Returns a single instance of this class (singleton).
     */
    public static ParkingSiteFollower getInstance() {
        //If condition to ensure we don't create multiple instances in a single application
        if (instance == null) {
            instance = new ParkingSiteFollower();
        }
        return instance;
    }

    /**
     * Returns an observable that when subscribed notifies when the paking bay changes it state.
     */
    public Observable<SiteState> createParkingBayFollower(String bayId) {
        Observable<SiteState> obs = Observable.using(
                // resource factory:
                () -> createHub(),
                // observable factory:
                hubConnection -> createHubObservable(bayId, hubConnection),
                // dispose action:
                hubConnection -> closeConnection(bayId, hubConnection)
        );
        return obs;
    }

    /**
     * Creates the observable making the connection with the hub.
     */
    private Observable<SiteState> createHubObservable(String bayId, InnerHubConnection hubConnection) {
        return Observable.create(subscriber -> {
            //Define the callback method that responds to the hub
            hubConnection.hub.on("SitesState",
                    (message) -> {
                        Log.d("PSF:SignalRConnection", message.toString());
                        if (!subscriber.isDisposed()) {
                            // Notify the subscriber
                            subscriber.onNext(message);
                        }
                    },
                    SiteState.class);

            // Handle when the connection is closed
            hubConnection.hub.onClosed((error) -> {
                if (error != null) {
                    Log.d("PSF:SignalRConnection", error.getMessage());
                } else {
                    Log.d("PSF:SignalRConnection", "Connection closed");
                }
                if (!subscriber.isDisposed()) {
                    subscriber.onError(error);
                }
            });

            // Prepare to get the connection id
            Single<String> connectionIdObservable = Single.fromCallable(() -> hubConnection.hub.getConnectionId());

            CompositeDisposable disposable = new CompositeDisposable();
            Disposable d = hubConnection.hub.start()// Start the hub
                    .andThen(connectionIdObservable) //retrieve the connection id
                    .flatMap(connectionId -> {
                        FollowCommand command = new FollowCommand(connectionId, bayId);
                        // register the bay on the API using the follow method
                        return api.follow(command);
                    })
                    //Different ways to subscribe (lambda) https://guides.codepath.com/android/Lambda-Expressions
                    .subscribe( //Executes the connection
                            bay -> {
                                Log.d("PSF:Api", "Following " + bay.getId());
                                hubConnection.setBayFollowed(true);
                                disposable.clear();
                            },
                            throwable -> {
                                Log.d("PSF:Api", throwable.getMessage());
                                disposable.clear();
                                subscriber.onError(throwable);
                            });
            disposable.add(d);
        });


    }

    /**
     * Creates the hub object.
     */
    private InnerHubConnection createHub() {
        HubConnection hubConnection = HubConnectionBuilder.create(Constants.HUB_CONNECTION_URL)
                .build();

        return new InnerHubConnection(hubConnection);
    }

    /**
     * Closes all the conections.
     */
    private void closeConnection(String bayId, InnerHubConnection hubConnection) {
        if (hubConnection.isBayFollowed()) {
            // If it is following a bay, the unfollow method should be executed
            String connectionId = hubConnection.hub.getConnectionId();
            UnfollowCommand command = new UnfollowCommand(connectionId, bayId);
            ParkingApi api = ParkingApi.getInstance();
            CompositeDisposable disposable = new CompositeDisposable();
            Disposable d = api.unfollow(command)
                    .subscribe(
                            () -> {
                                Log.d("PSF:Api", "Unfollowed " + bayId);
                                disposable.clear();
                            },
                            throwable -> {
                                Log.d("PSF:Api", throwable.getMessage());
                                disposable.clear();
                            }
                    );
            disposable.add(d);
        }
        // If the hub have not been disposed, close the hub connection
        if (hubConnection.hub.getConnectionState() == HubConnectionState.CONNECTED) {
            CompositeDisposable disposable = new CompositeDisposable();
            Disposable d = hubConnection.hub.stop().subscribe(() -> {
                        Log.d("PSF:SignalRConnection", "Connection stopped");
                        disposable.clear();
                    },
                    throwable -> {
                        Log.d("PSF:SignalRConnection", throwable.getMessage());
                        disposable.clear();
                    });
            disposable.add(d);
        }
    }
}
//    //Create the disposable in the class
//    CompositeDisposable disposable = new CompositeDisposable();
//
//    //Connect the follower to a parking bay
//    ParkingSiteFollower follower = ParkingSiteFollower.getInstance();
//    Disposable d = follower.createParkingBayFollower("6126")
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread()) // to return to the main thread
//                .as(autoDisposable(AndroidLifecycleScopeProvider.from(this, Lifecycle.Event.ON_STOP))) //to dispose when the activity finishes
//                .subscribe(bay -> {
//                System.out.println("Value:" + bay.getId()); // sample, other values are id, status, location, zone, recordState
//                },
//                throwable -> Log.d("debug", throwable.getMessage()), // do this on error
//                () -> Log.d("debug", "complete"));
//
//    //Dispose when disposing the service or when not needed
//    disposable.add(d);



