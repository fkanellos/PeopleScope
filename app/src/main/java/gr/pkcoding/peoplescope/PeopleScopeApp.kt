package gr.pkcoding.peoplescope

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import gr.pkcoding.peoplescope.di.appModule
import gr.pkcoding.peoplescope.di.dataModule
import gr.pkcoding.peoplescope.di.domainModule
import gr.pkcoding.peoplescope.di.presentationModule
import gr.pkcoding.peoplescope.utils.Constants
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

        val imageLoader = ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(Constants.MEMORY_CACHE_PERCENT)
                    .strongReferencesEnabled(false)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(this.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(Constants.DISK_CACHE_SIZE)
                    .build()
            }
            .crossfade(150) //false
            .respectCacheHeaders(false)
            .build()
        Coil.setImageLoader(imageLoader)
    }
}