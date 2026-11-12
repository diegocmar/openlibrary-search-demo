package com.diegocmar.booksearch.data.repository

import com.diegocmar.booksearch.data.remote.OpenLibraryApi
import com.diegocmar.booksearch.data.remote.dto.SearchDocDto
import com.diegocmar.booksearch.data.remote.dto.SearchResponseDto
import com.diegocmar.booksearch.data.remote.dto.WorkDto

/** Records calls and lets each test decide what the network answers. */
class FakeOpenLibraryApi : OpenLibraryApi {
    val searchCalls = mutableListOf<String>()
    var searchAnswer: suspend (query: String) -> SearchResponseDto = { query -> responseFor(query) }
    var workAnswer: suspend (
        id: String
    ) -> WorkDto = { id -> WorkDto(key = "/works/$id", title = "Title $id") }

    override suspend fun searchBooks(query: String, limit: Int, fields: String): SearchResponseDto {
        searchCalls += query
        return searchAnswer(query)
    }

    override suspend fun getDetail(id: String): WorkDto = workAnswer(id)

    companion object {
        fun responseFor(query: String, version: Int = 1) = SearchResponseDto(
            docs = listOf(
                SearchDocDto(key = "/works/OL${query.hashCode()}W", title = "$query v$version")
            )
        )
    }
}
