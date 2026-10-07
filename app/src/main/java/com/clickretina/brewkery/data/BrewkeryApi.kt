package com.clickretina.brewkery.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url

interface BrewkeryApi {
    @GET("data.json") suspend fun menu(): MenuResponse
    @GET suspend fun item(@Url url: String): MenuItem

    companion object {
        const val BASE = "https://raw.githubusercontent.com/VivekShah138/Brewkery/main/"
        val service: BrewkeryApi by lazy {
            Retrofit.Builder().baseUrl(BASE).addConverterFactory(GsonConverterFactory.create()).build().create(BrewkeryApi::class.java)
        }
    }
}
