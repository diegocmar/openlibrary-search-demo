package com.diegocmar.booksearch.data.remote

import com.diegocmar.booksearch.data.remote.dto.SearchResponseDto
import com.diegocmar.booksearch.data.remote.dto.WorkDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Open Library public API (https://openlibrary.org/developers/api). No API key required.
 *
 * Retrofit `suspend` functions are main-safe and cancel the underlying OkHttp call when the calling
 * coroutine is cancelled.
 */
interface OpenLibraryApi {
    @GET("search.json")
    suspend fun searchBooks(
        @Query("q") query: String,
        @Query("limit") limit: Int = SEARCH_LIMIT,
        @Query("fields") fields: String = SEARCH_FIELDS
    ): SearchResponseDto

    @GET("works/{id}.json")
    suspend fun getDetail(@Path("id") id: String): WorkDto

    companion object {
        const val BASE_URL = "https://openlibrary.org/"
        const val SEARCH_LIMIT = 30
        const val SEARCH_FIELDS = "key,title,author_name,first_publish_year,cover_i,edition_count"
    }
}
