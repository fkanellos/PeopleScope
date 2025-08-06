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
        // Configure Coil for better performance
        val imageLoader = ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // Use 25% of app memory for images
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(this.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50 * 1024 * 1024) // 50MB disk cache
                    .build()
            }
            .crossfade(false) // Disable crossfade for smoother scrolling
            .respectCacheHeaders(false) // Don't check cache headers for faster loading
            .build()
        Coil.setImageLoader(imageLoader)
    }
}