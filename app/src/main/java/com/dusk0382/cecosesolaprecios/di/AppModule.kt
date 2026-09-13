package com.dusk0382.cecosesolaprecios.di

import android.content.Context
import androidx.room.Room
import com.dusk0382.cecosesolaprecios.data.local.AppDatabase
import com.dusk0382.cecosesolaprecios.data.remote.HttpClients
import com.dusk0382.cecosesolaprecios.data.remote.OfficialApi
import com.dusk0382.cecosesolaprecios.data.remote.RepoApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun json(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun httpClients(json: Json): HttpClients = HttpClients(json)

    @Provides
    @Singleton
    fun repoApi(http: HttpClients): RepoApi = RepoApi(http)

    @Provides
    @Singleton
    fun officialApi(http: HttpClients): OfficialApi = OfficialApi(http)

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME).build()
}
