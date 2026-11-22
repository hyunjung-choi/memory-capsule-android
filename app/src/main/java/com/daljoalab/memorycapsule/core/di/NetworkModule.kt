package com.daljoalab.memorycapsule.core.di

import com.daljoalab.memorycapsule.BuildConfig
import com.daljoalab.memorycapsule.core.network.NetworkFactory
import com.daljoalab.memorycapsule.core.network.api.ConsentApi
import com.daljoalab.memorycapsule.core.network.api.ConversationApi
import com.daljoalab.memorycapsule.core.network.api.PairingApi
import com.daljoalab.memorycapsule.core.network.api.UserApi
import com.daljoalab.memorycapsule.core.network.auth.AuthInterceptor
import com.daljoalab.memorycapsule.core.network.auth.FirebaseIdTokenProvider
import com.daljoalab.memorycapsule.core.network.auth.IdTokenProvider
import com.daljoalab.memorycapsule.core.network.auth.TokenAuthenticator
import com.daljoalab.memorycapsule.core.network.timeout.ReadTimeoutInterceptor
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
        readTimeoutInterceptor: ReadTimeoutInterceptor,
    ): OkHttpClient = NetworkFactory.okHttpClient(
        interceptors = buildList {
            add(readTimeoutInterceptor)
            add(authInterceptor)
            if (BuildConfig.DEBUG) add(NetworkFactory.loggingInterceptor())
        },
        authenticator = tokenAuthenticator,
    )

    @Provides
    @Singleton
    fun provideRetrofit(json: Json, okHttpClient: OkHttpClient): Retrofit =
        NetworkFactory.retrofit(BuildConfig.BASE_URL, json, okHttpClient)

    @Provides
    @Singleton
    fun provideUserApi(retrofit: Retrofit): UserApi = retrofit.create(UserApi::class.java)

    @Provides
    @Singleton
    fun provideConsentApi(retrofit: Retrofit): ConsentApi = retrofit.create(ConsentApi::class.java)

    @Provides
    @Singleton
    fun provideConversationApi(retrofit: Retrofit): ConversationApi =
        retrofit.create(ConversationApi::class.java)

    @Provides
    @Singleton
    fun providePairingApi(retrofit: Retrofit): PairingApi = retrofit.create(PairingApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
interface AuthTokenModule {

    @Binds
    fun bindIdTokenProvider(impl: FirebaseIdTokenProvider): IdTokenProvider
}
