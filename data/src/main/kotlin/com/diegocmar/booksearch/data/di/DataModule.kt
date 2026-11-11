package com.diegocmar.booksearch.data.di

import com.diegocmar.booksearch.data.cache.InMemoryCache
import com.diegocmar.booksearch.data.remote.OpenLibraryApi
import com.diegocmar.booksearch.data.repository.BookRepositoryImpl
import com.diegocmar.booksearch.domain.model.Book
import com.diegocmar.booksearch.domain.repository.BookRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindBookRepository(impl: BookRepositoryImpl): BookRepository

    companion object {
        /** Search results stay valid for 3 minutes. */
        private val SEARCH_CACHE_TTL = 3.minutes

        // Open Library asks clients to identify themselves.
        private const val USER_AGENT = "BookSearchDemo/1.0 (Android)"

        @Provides
        @Singleton
        fun provideOkHttpClient(): OkHttpClient = OkHttpClient
            .Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                chain.proceed(
                    chain
                        .request()
                        .newBuilder()
                        .header("User-Agent", USER_AGENT)
                        .build()
                )
            }.build()

        @Provides
        @Singleton
        fun provideOpenLibraryApi(okHttpClient: OkHttpClient): OpenLibraryApi {
            val json = Json { ignoreUnknownKeys = true }
            return Retrofit
                .Builder()
                .baseUrl(OpenLibraryApi.BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(OpenLibraryApi::class.java)
        }

        @Provides
        @Singleton
        fun provideSearchCache(): InMemoryCache<String, List<Book>> =
            InMemoryCache(ttl = SEARCH_CACHE_TTL)
    }
}
