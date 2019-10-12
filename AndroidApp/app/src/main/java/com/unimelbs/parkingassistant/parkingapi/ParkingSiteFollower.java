package com.unimelbs.parkingassistant.parkingapi;

import android.util.Log;

import com.microsoft.signalr.HubConnection;
import com.microsoft.signalr.HubConnectionBuilder;
import com.microsoft.signalr.HubConnectionState;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class ParkingSiteFollower {
    private static final String connectionUrl = "https://parkingappapi.azurewebsites.net/api/sites/state/connection/";
//    private static final String connectionUrl = "http://10.8.8.8:7071/api/sites/state/connection/";

    private static ParkingSiteFollower instance;
    private ParkingApi api;

    private ParkingSiteFollower() {
        this.api = ParkingApi.getInstance();
    }

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

    public static ParkingSiteFollower getInstance() {
        //If condition to ensure we don't create multiple instances in a single application
        if (instance == null) {
            instance = new ParkingSiteFollower();
        }
        return instance;
    }

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

    private Observable<SiteState> createHubObservable(String bayId, InnerHubConnection hubConnection) {
        return Observable.create(subscriber -> {
            hubConnection.hub.on("SitesState",
                    (message) -> {
                        Log.d("PSF:SignalRConnection", message.toString());
                        if (!subscriber.isDisposed()) {
                            subscriber.onNext(message);
                        }
                    },
                    SiteState.class);

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

            Single<String> connectionIdObservable = Single.fromCallable(() -> hubConnection.hub.getConnectionId());

            CompositeDisposable disposable = new CompositeDisposable();
            Disposable d = hubConnection.hub.start()
                    .andThen(connectionIdObservable)
                    .flatMap(connectionId -> {
                        FollowCommand command = new FollowCommand(connectionId, bayId);
                        return api.follow(command);
                    })
                    //Different ways to subscribe (lambda) https://guides.codepath.com/android/Lambda-Expressions
                    .subscribe(
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

    private InnerHubConnection createHub() {
        HubConnection hubConnection = HubConnectionBuilder.create(connectionUrl)
                .build();

        return new InnerHubConnection(hubConnection);
    }

    private void closeConnection(String bayId, InnerHubConnection hubConnection) {
        if (hubConnection.isBayFollowed()) {
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

//    private void Connect(String bayId) {
//        hubConnection = HubConnectionBuilder.create(connectionUrl)
//                .build();
//
//        //Type targetClassType = new TypeToken<ArrayList<Object>>() { }.getType();
//        hubConnection.on("SitesState",
//                (message) -> Log.d("PSF:SignalRConnection", message.toString()), //do here the message to outside
//                Object.class);
//
//        hubConnection.onClosed((error) -> Log.d("PSF:SignalRConnection", error.getMessage()));
//
////        hubConnection.start().blockingAwait();
////        hubConnection.start().doOnComplete(() -> hubConnection.invoke(Void.class, "GetConnectionId"));
//
//        hubConnection.start().doOnComplete(() -> {
//            connectionId = hubConnection.getConnectionId();
//            FollowCommand command = new FollowCommand(connectionId, "6126");
//            //Different ways to subscribe (lambda) https://guides.codepath.com/android/Lambda-Expressions
//            api.follow(command)
//                    //.subscribeOn(Schedulers.io())
//                    .observeOn(AndroidSchedulers.mainThread())
//                    .as(autoDisposable(AndroidLifecycleScopeProvider.from(this, Lifecycle.Event.ON_STOP)))
//                    .subscribe(value -> Log.d("PSF:Api", "here the value but it is void in this case"),
//                            throwable -> Log.d("PSF:Api", throwable.getMessage()),
//                            () -> Log.d("PSF:Api", "complete"));
//        });
//
//    }
}
