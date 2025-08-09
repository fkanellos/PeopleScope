package gr.pkcoding.peoplescope.di

import androidx.room.Room
import gr.pkcoding.peoplescope.data.local.database.AppDatabase
import gr.pkcoding.peoplescope.data.network.NetworkConnectivityManager
import gr.pkcoding.peoplescope.data.network.NetworkConnectivityProvider
import gr.pkcoding.peoplescope.data.paging.UserPagingSource
import gr.pkcoding.peoplescope.data.remote.NetworkModule
import gr.pkcoding.peoplescope.data.remote.api.RandomUserApi
import gr.pkcoding.peoplescope.data.repository.UserRepositoryImpl
import gr.pkcoding.peoplescope.domain.repository.UserRepository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import retrofit2.Retrofit

val dataModule = module {
    // Network dependencies
    single { NetworkModule.provideOkHttpClient() }
    single { NetworkModule.provideRetrofit(get()) }
    single<RandomUserApi> { get<Retrofit>().create(RandomUserApi::class.java) }

    single<NetworkConnectivityProvider> {
        NetworkConnectivityManager(androidContext())
    }

    // Database dependencies
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).build()
    }
    single { get<AppDatabase>().bookmarkDao() }

    // Paging Source
    factory { UserPagingSource(get(), get(), get<NetworkConnectivityProvider>()) }

    // Repository
    single<UserRepository> {
        UserRepositoryImpl(
            api = get(),
            bookmarkDao = get(),
            networkProvider = get<NetworkConnectivityProvider>()
        )
    }
}