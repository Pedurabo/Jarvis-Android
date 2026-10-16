package com.example.jarvis.core.network

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideKampalaPlacesApiService(
        okHttpClient: OkHttpClient
    ): KampalaPlacesApiService {
        val placesClient = okHttpClient.newBuilder()
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(90, java.util.concurrent.TimeUnit.SECONDS)
            .callTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl("https://jarvis-kampala-preview.onrender.com/")
            .client(placesClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(KampalaPlacesApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideGoogleMapsApiService(okHttpClient: OkHttpClient): GoogleMapsApiService {
        return Retrofit.Builder()
            .baseUrl("https://maps.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GoogleMapsApiService::class.java)
    }
}
