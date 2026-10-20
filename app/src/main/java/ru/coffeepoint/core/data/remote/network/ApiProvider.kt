package ru.coffeepoint.core.data.remote.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.coffeepoint.BuildConfig
import java.io.IOException
import ru.coffeepoint.core.data.remote.api.TerminalCommandApi

object ApiProvider {

    private const val BASE_URL = "https://api.vendista.ru:99/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            if (BuildConfig.IS_TEST_ENVIRONMENT) {
                throw IOException("Vendista terminal commands are disabled in the test environment")
            }
            chain.proceed(chain.request())
        }
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val terminalCommandApi: TerminalCommandApi =
        retrofit.create(TerminalCommandApi::class.java)

}
