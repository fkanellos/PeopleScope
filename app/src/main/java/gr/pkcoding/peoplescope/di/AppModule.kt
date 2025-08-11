package gr.pkcoding.peoplescope.di

import gr.pkcoding.peoplescope.utils.Constants
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appModule = module {
    single<String>(named("BaseUrl")) { Constants.BASE_URL }

    // Future: Other app-wide configs will go here
}