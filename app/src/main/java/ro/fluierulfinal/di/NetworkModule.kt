package ro.fluierulfinal.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import ro.fluierulfinal.BuildConfig
import ro.fluierulfinal.data.remote.ApiFootballService
import ro.fluierulfinal.data.remote.ApiJson
import ro.fluierulfinal.data.remote.ApiKeyInterceptor
import ro.fluierulfinal.data.remote.RateLimitLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Tells Hilt how to build the networking objects. Each @Provides function is a recipe;
 * @Singleton means one shared instance for the whole app (one connection pool, one Retrofit).
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val API_FOOTBALL_URL = "https://v3.football.api-sports.io/"

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(ApiKeyInterceptor(BuildConfig.API_FOOTBALL_KEY))
        .addInterceptor(RateLimitLoggingInterceptor())
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(API_FOOTBALL_URL)
        .client(client)
        .addConverterFactory(ApiJson.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideApiFootballService(retrofit: Retrofit): ApiFootballService =
        retrofit.create(ApiFootballService::class.java)
}
