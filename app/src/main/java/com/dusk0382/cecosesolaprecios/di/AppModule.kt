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
    fun httpClients(@ApplicationContext context: Context, json: Json): HttpClients =
        HttpClients(context, json)

    @Provides
    @Singleton
    fun repoApi(http: HttpClients): RepoApi = RepoApi(http)

    @Provides
    @Singleton
    fun officialApi(http: HttpClients): OfficialApi = OfficialApi(http)

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            // v1→v2: columna `clase` (rubro derivado). Una linea de SQL preserva
            // favoritos y carrito; el valor default se recalcula al sincronizar.
            // Sin fallbackToDestructiveMigration(): favoritos y carrito son datos
            // del usuario. Sin migración, Room falla al abrir; es preferible a
            // perderlos en silencio.
            .addMigrations(*AppDatabase.MIGRATIONS)
            .build()
}
