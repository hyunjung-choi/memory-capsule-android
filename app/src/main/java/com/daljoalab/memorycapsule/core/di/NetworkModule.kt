package com.daljoalab.memorycapsule.core.di

import com.daljoalab.memorycapsule.BuildConfig
import com.daljoalab.memorycapsule.core.network.NetworkFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = NetworkFactory.json()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = NetworkFactory.okHttpClient(
        interceptors = buildList {
            if (BuildConfig.DEBUG) add(NetworkFactory.loggingInterceptor())
        },
    )

    @Provides
    @Singleton
    fun provideRetrofit(json: Json, okHttpClient: OkHttpClient): Retrofit =
        NetworkFactory.retrofit(BuildConfig.BASE_URL, json, okHttpClient)
}
