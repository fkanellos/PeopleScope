package gr.pkcoding.peoplescope.di

import org.koin.dsl.module

val dataModule = module {
    // Network dependencies
    // single { provideOkHttpClient() }
    // single { provideRetrofit(get()) }
    // single { provideRandomUserApi(get()) }

    // Database dependencies
    // single { provideDatabase(get()) }
    // single { provideBookmarkDao(get()) }

    // Repository
    // single<UserRepository> { UserRepositoryImpl(get(), get()) }
}