package com.example.jo

import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    private const val BASE_URL = "https://api.openai.com/v1/"  // Correct base URL for OpenAI
    private const val API_KEY = "API_KEY"  // Replace with your actual API key
    private val client = OkHttpClient.Builder().addInterceptor { chain ->
        val originalRequest: Request = chain.request()
        val newRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $API_KEY")  // Dynamically include API key here
            .build()
        chain.proceed(newRequest)
    }.build()

    private val retrofitInstance: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: OpenAIService by lazy {
        retrofitInstance.create(OpenAIService::class.java)
    }
}
