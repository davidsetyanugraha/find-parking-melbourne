package com.unimelbs.parkingassistant.parkingapi;

import com.unimelbs.parkingassistant.util.Constants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.http.QueryMap;

/**
 * Handles the calls to the API.
 */
public class ParkingApi {

    private static ParkingApi instance;

    /**
     * Required interface for defining the retrofit calls.
     */
    public interface Api {
        @GET("sites") //Check urls here https://inthecheesefactory.com/blog/retrofit-2.0/en?fb_comment_id=885081521586565_886605554767495
        Single<List<Site>> sitesGet();

        @GET("sites/state")
        Single<List<SiteState>> sitesStateGet(@QueryMap() Map<String, String> query);

        @POST("sites/state/connection/follow")
        Single<SiteState> follow(@Body FollowCommand params);

        @POST("sites/state/connection/unfollow")
        Completable unfollow(@Body UnfollowCommand params);
    }

    private Api api;

    private ParkingApi() {
        //Defines the connection with the timeaout
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        RxJava2CallAdapterFactory rxAdapter = RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io());

        //Builds a retrofit client
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Constants.API_URL)
                .addConverterFactory(GsonConverterFactory.create()) // Converter library used to convert response into POJO, can be replaced by moshi with MoshiConverterFactory
                .client(okHttpClient) // check if logging is needed later https://futurestud.io/tutorials/retrofit-2-log-requests-and-responses
                .addCallAdapterFactory(rxAdapter) //https://github.com/square/retrofit/tree/master/retrofit-adapters/rxjava2
                .build();
        api = retrofit.create(Api.class);
    }

    /**
     * Returns a singleton.
     */
    public static ParkingApi getInstance() {

        //If condition to ensure we don't create multiple instances in a single application
        if (instance == null) {

            instance = new ParkingApi();
        }

        return instance;
    }

    /**
     * Returns all the parking bays.
     */
    public Single<List<Site>> sitesGet() {
        return api.sitesGet();
    }

    /**
     * Returns the state of the bays according to the query.
     */
    public Single<List<SiteState>> sitesStateGet(SitesStateGetQuery query) {
        Map<String,String> parameters = new HashMap<>();
        parameters.put("latitude", String.valueOf(query.getLatitude()));
        parameters.put("longitude", String.valueOf(query.getLongitude()));
        if (query.getDistance() != null) {
            parameters.put("distance", query.getDistance().toString());
        }

        return api.sitesStateGet(parameters);
    }

    /**
     * Starts following a parking bay.
     */
    Single<SiteState> follow(FollowCommand command) {
        return api.follow(command);
    }

    /**
     * Unfollows the parking bay.
     */
    Completable unfollow(UnfollowCommand command) {
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
//                        System.out.println("Value:" + value.get(0).getDescription()); // sample, other values are id, status, location, zone, recordState
//                    },
//                    throwable -> Log.d("debug", throwable.getMessage()) // do this on error
//                );
//
//
//        SitesStateGetQuery query = new SitesStateGetQuery(-37.796201, 144.958266, null);
//        api.sitesStateGet(query)
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread()) // to return to the main thread
//                .as(autoDisposable(AndroidLifecycleScopeProvider.from(this, Lifecycle.Event.ON_STOP))) //to dispose when the activity finishes
//                .subscribe(value -> {
//                        System.out.println("Value:" + value.get(0).getStatus()); // sample, other values are id, status, location, zone, recordState
//                    },
//                    throwable -> Log.d("debug", throwable.getMessage()) // do this on error
//                );
