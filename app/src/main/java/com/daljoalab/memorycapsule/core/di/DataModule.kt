package com.daljoalab.memorycapsule.core.di

import com.daljoalab.memorycapsule.core.data.repository.AuthRepository
import com.daljoalab.memorycapsule.core.data.repository.ConversationRepository
import com.daljoalab.memorycapsule.core.data.repository.DefaultConversationRepository
import com.daljoalab.memorycapsule.core.data.repository.DefaultPairingRepository
import com.daljoalab.memorycapsule.core.data.repository.DefaultUserRepository
import com.daljoalab.memorycapsule.core.data.repository.FirebaseAuthRepository
import com.daljoalab.memorycapsule.core.data.repository.PairingRepository
import com.daljoalab.memorycapsule.core.data.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {

    @Binds
    fun bindConversationRepository(impl: DefaultConversationRepository): ConversationRepository

    @Binds
    fun bindAuthRepository(impl: FirebaseAuthRepository): AuthRepository

    @Binds
    fun bindUserRepository(impl: DefaultUserRepository): UserRepository

    @Binds
    fun bindPairingRepository(impl: DefaultPairingRepository): PairingRepository
}
