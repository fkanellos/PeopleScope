package gr.pkcoding.peoplescope

import android.app.Application
import gr.pkcoding.peoplescope.di.appModule
import gr.pkcoding.peoplescope.di.dataModule
import gr.pkcoding.peoplescope.di.domainModule
import gr.pkcoding.peoplescope.di.presentationModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import timber.log.Timber



class PeopleScopeApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Initialize Koin
        startKoin {
            androidLogger()
            androidContext(this@PeopleScopeApp)
            modules(
                appModule,
                dataModule,
                domainModule,
                presentationModule
            )
        }
    }
}