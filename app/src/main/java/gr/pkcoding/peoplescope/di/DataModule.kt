package gr.pkcoding.peoplescope.di

import androidx.room.Room
import gr.pkcoding.peoplescope.data.local.database.AppDatabase
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

    // Database dependencies
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).build()
    }
    single { get<AppDatabase>().bookmarkDao() }

    // Repository
    single<UserRepository> { UserRepositoryImpl(get(), get()) }
}