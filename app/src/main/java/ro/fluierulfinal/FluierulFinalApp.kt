package ro.fluierulfinal

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import ro.fluierulfinal.data.remote.LogoPlaceholderInterceptor

/**
 * Application class. @HiltAndroidApp makes Hilt generate the app-wide dependency container,
 * so classes can receive their dependencies through @Inject instead of creating them by hand.
 *
 * It also configures Coil's image loader (SingletonImageLoader.Factory): its own OkHttp client
 * (not the API-Football one, which carries the API key) with [LogoPlaceholderInterceptor].
 */
@HiltAndroidApp
class FluierulFinalApp : Application(), SingletonImageLoader.Factory {

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val client = OkHttpClient.Builder()
            .addInterceptor(LogoPlaceholderInterceptor())
            .build()
        return ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { client })) }
            .crossfade(true)
            .build()
    }
}
