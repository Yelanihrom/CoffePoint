package ru.coffeepoint.core.sync.google.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.coffeepoint.core.sync.google.api.GoogleSheetsSyncApi

object GoogleSheetsApiProvider {

    private const val BASE_URL = "https://script.google.com/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val syncApi: GoogleSheetsSyncApi =
        retrofit.create(GoogleSheetsSyncApi::class.java)

}
