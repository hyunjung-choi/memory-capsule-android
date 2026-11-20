package com.daljoalab.memorycapsule.core.di

import com.daljoalab.memorycapsule.BuildConfig
import com.daljoalab.memorycapsule.core.network.NetworkFactory
import com.daljoalab.memorycapsule.core.network.auth.AuthInterceptor
import com.daljoalab.memorycapsule.core.network.auth.FirebaseIdTokenProvider
import com.daljoalab.memorycapsule.core.network.auth.IdTokenProvider
import com.daljoalab.memorycapsule.core.network.auth.TokenAuthenticator
import dagger.Binds
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
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
    ): OkHttpClient = NetworkFactory.okHttpClient(
        interceptors = buildList {
            add(authInterceptor)
            if (BuildConfig.DEBUG) add(NetworkFactory.loggingInterceptor())
        },
        authenticator = tokenAuthenticator,
    )

    @Provides
    @Singleton
    fun provideRetrofit(json: Json, okHttpClient: OkHttpClient): Retrofit =
        NetworkFactory.retrofit(BuildConfig.BASE_URL, json, okHttpClient)
}

@Module
@InstallIn(SingletonComponent::class)
interface AuthTokenModule {

    @Binds
    fun bindIdTokenProvider(impl: FirebaseIdTokenProvider): IdTokenProvider
}
