package com.tr.feelingme;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface FeelingMe {
    @GET("feelingme")
    Call<List<List<String>>> search(@Query("q") String q);
}
