package ro.fluierulfinal

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class. @HiltAndroidApp makes Hilt generate the app-wide dependency container,
 * so classes can receive their dependencies through @Inject instead of creating them by hand.
 */
@HiltAndroidApp
class FluierulFinalApp : Application()
