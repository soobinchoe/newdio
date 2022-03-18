package com.traydcorp.newdio.utils.retrofitAPI

import com.google.gson.GsonBuilder
import com.traydcorp.newdio.utils.retofitService.NullOnEmptyConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


object RetrofitClient {
    private var instant: Retrofit? = null
    private var gson = GsonBuilder().setLenient().create()

    //private const val BASE_URL = "http://10.0.1.19/"
    private const val BASE_URL = "https://traydnewdioglobal.com"

    fun getInstance(): Retrofit {
        val interceptor = HttpLoggingInterceptor()
        interceptor.apply {
            interceptor.level = HttpLoggingInterceptor.Level.BODY
        }

        val client: OkHttpClient =  OkHttpClient
            .Builder()
            .addInterceptor(interceptor).connectTimeout(5, TimeUnit.SECONDS)
            .build()
        if(instant == null) {
            instant = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(NullOnEmptyConverterFactory())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
        }
        return instant!!
    }
}