package com.unimelbs.parkingassistant.parkingapi;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.google.gson.internal.LinkedTreeMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
//import retrofit2.Call;
//import retrofit2.Callback;
//import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.http.QueryMap;


public class ParkingApi {

    private static final String url = "https://parkingappapi.azurewebsites.net/api/";
//    private static final String url = "http://10.8.8.8:7071/api/";

    private static ParkingApi instance;

    public interface Api {
        @GET("sites") //Check urls here https://inthecheesefactory.com/blog/retrofit-2.0/en?fb_comment_id=885081521586565_886605554767495
        Observable<List<Site>> sitesGet();

        @GET("sites/state")
        Observable<List<SiteState>> sitesStateGet(@QueryMap() Map<String, String> query);

        @POST("sites/state/connection/follow")
        Single<SiteState> follow(@Body FollowCommand params);

        @POST("sites/state/connection/unfollow")
        Completable unfollow(@Body UnfollowCommand params);
    }

    private Api api;

    public ParkingApi() {
        RxJava2CallAdapterFactory rxAdapter = RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io());

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create()) // Converter library used to convert response into POJO, can be replaced by moshi with MoshiConverterFactory
                //.client(httpClient.build()) // check if logging is needed later https://futurestud.io/tutorials/retrofit-2-log-requests-and-responses
                .addCallAdapterFactory(rxAdapter) //https://github.com/square/retrofit/tree/master/retrofit-adapters/rxjava2
                .build();
        api = retrofit.create(Api.class);
    }

    public static ParkingApi getInstance() {

        //If condition to ensure we don't create multiple instances in a single application
        if (instance == null) {

            instance = new ParkingApi();
        }

        return instance;
    }

    public Observable<List<Site>> sitesGet() {
        return api.sitesGet();
    }

    public Observable<List<SiteState>> sitesStateGet(SitesStateGetQuery query) {
        Map<String,String> parameters = new HashMap<>();
        parameters.put("latitude", String.valueOf(query.getLatitude()));
        parameters.put("longitude", String.valueOf(query.getLongitude()));
        if (query.getDistance() != null) {
            parameters.put("distance", query.getDistance().toString());
        }

        return api.sitesStateGet(parameters);
    }

    public Single<SiteState> follow(FollowCommand command) {
        return api.follow(command);
    }

    public Completable unfollow(UnfollowCommand command) {
        return api.unfollow(command);
    }
}

// Sample usage

//        ParkingApi api = ParkingApi.getInstance();
//        api.sitesGet()
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread()) // to return to the main thread
//                .as(autoDisposable(AndroidLifecycleScopeProvider.from(this, Lifecycle.Event.ON_STOP))) //to dispose when the activity finishes
//                .subscribe(value -> {
//                System.out.println("Value:" + value.get(0).getDescription()); // sample, other values are id, status, location, zone, recordState
//                },
//                throwable -> Log.d("debug", throwable.getMessage()), // do this on error
//                () -> Log.d("debug", "complete")); // do this on completion
//
//
//        SitesStateGetQuery query = new SitesStateGetQuery(-37.796201, 144.958266, null);
//        api.sitesStateGet(query)
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread()) // to return to the main thread
//                .as(autoDisposable(AndroidLifecycleScopeProvider.from(this, Lifecycle.Event.ON_STOP))) //to dispose when the activity finishes
//                .subscribe(value -> {
//                System.out.println("Value:" + value.get(0).getStatus()); // sample, other values are id, status, location, zone, recordState
//                },
//                throwable -> Log.d("debug", throwable.getMessage()), // do this on error
//                () -> Log.d("debug", "complete")); // do this on completion
