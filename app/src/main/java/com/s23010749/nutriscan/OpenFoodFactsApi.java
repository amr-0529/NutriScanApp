package com.s23010749.nutriscan;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;

public interface OpenFoodFactsApi {
    @GET("api/v2/product/{barcode}.json")
    Call<ProductResponse> getProductByBarcode(
            @Path("barcode") String barcode,
            @Header("User-Agent") String userAgent
    );
}