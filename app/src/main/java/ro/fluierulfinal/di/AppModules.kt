package ro.fluierulfinal.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import ro.fluierulfinal.BuildConfig
import ro.fluierulfinal.data.cache.CacheDataSource
import ro.fluierulfinal.data.fake.FakeAuthRepository
import ro.fluierulfinal.data.fake.FakeFavoritesRepository
import ro.fluierulfinal.data.fake.FakeFixtureRepository
import ro.fluierulfinal.data.fake.FakeLeagueRepository
import ro.fluierulfinal.data.fake.FakeStandingsRepository
import ro.fluierulfinal.data.fake.FakeTeamRepository
import ro.fluierulfinal.data.repository.AuthRepository
import ro.fluierulfinal.data.repository.AuthRepositoryImpl
import ro.fluierulfinal.data.repository.FavoritesRepository
import ro.fluierulfinal.data.repository.FavoritesRepositoryImpl
import ro.fluierulfinal.data.repository.FixtureRepository
import ro.fluierulfinal.data.repository.FixtureRepositoryImpl
import ro.fluierulfinal.data.repository.LeagueRepository
import ro.fluierulfinal.data.repository.LeagueRepositoryImpl
import ro.fluierulfinal.data.repository.StandingsRepository
import ro.fluierulfinal.data.repository.StandingsRepositoryImpl
import ro.fluierulfinal.data.repository.TeamRepository
import ro.fluierulfinal.data.repository.TeamRepositoryImpl
import ro.fluierulfinal.data.supabase.SupabaseCacheDataSource
import java.time.Clock
import javax.inject.Provider
import javax.inject.Qualifier
import javax.inject.Singleton

/** Marks the app-wide CoroutineScope (lives as long as the app, unlike a ViewModel's scope). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /** Injected instead of calling Instant.now() directly, so tests can use a fixed clock. */
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemUTC()

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    /** JSON for Supabase rows: unknown columns (e.g. user_id) are ignored, default values are always sent. */
    private val supabaseJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        check(BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()) {
            "SUPABASE_URL / SUPABASE_ANON_KEY missing from local.properties (or set USE_FAKE_DATA=true)"
        }
        return createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_ANON_KEY) {
            defaultSerializer = KotlinXSerializer(supabaseJson)
            install(Auth)
            install(Postgrest)
        }
    }

    @Provides
    @Singleton
    fun provideCacheDataSource(impl: SupabaseCacheDataSource): CacheDataSource = impl
}

/**
 * Chooses real or fake repositories from BuildConfig.USE_FAKE_DATA.
 * Provider<...> creates only the implementation actually chosen, so in fake mode
 * no Supabase client or Retrofit service is ever built.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    private val fake = BuildConfig.USE_FAKE_DATA

    @Provides @Singleton
    fun auth(real: Provider<AuthRepositoryImpl>, fakeImpl: Provider<FakeAuthRepository>): AuthRepository =
        if (fake) fakeImpl.get() else real.get()

    @Provides @Singleton
    fun teams(real: Provider<TeamRepositoryImpl>, fakeImpl: Provider<FakeTeamRepository>): TeamRepository =
        if (fake) fakeImpl.get() else real.get()

    @Provides @Singleton
    fun leagues(real: Provider<LeagueRepositoryImpl>, fakeImpl: Provider<FakeLeagueRepository>): LeagueRepository =
        if (fake) fakeImpl.get() else real.get()

    @Provides @Singleton
    fun fixtures(real: Provider<FixtureRepositoryImpl>, fakeImpl: Provider<FakeFixtureRepository>): FixtureRepository =
        if (fake) fakeImpl.get() else real.get()

    @Provides @Singleton
    fun standings(real: Provider<StandingsRepositoryImpl>, fakeImpl: Provider<FakeStandingsRepository>): StandingsRepository =
        if (fake) fakeImpl.get() else real.get()

    @Provides @Singleton
    fun favorites(real: Provider<FavoritesRepositoryImpl>, fakeImpl: Provider<FakeFavoritesRepository>): FavoritesRepository =
        if (fake) fakeImpl.get() else real.get()
}
