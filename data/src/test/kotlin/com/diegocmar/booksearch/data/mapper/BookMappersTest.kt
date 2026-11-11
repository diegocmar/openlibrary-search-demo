package com.diegocmar.booksearch.data.mapper

import com.diegocmar.booksearch.data.remote.dto.SearchDocDto
import com.diegocmar.booksearch.data.remote.dto.WorkDto
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test

class BookMappersTest {
    @Test
    fun `search doc is mapped to Book`() {
        val book =
            SearchDocDto(
                key = "/works/OL893414W",
                title = "Dune",
                authorNames = listOf("Frank Herbert"),
                firstPublishYear = 1965,
                editionCount = 155,
                coverId = 11481354
            ).toDomain()

        assertEquals("OL893414W", book.id)
        assertEquals("Dune", book.title)
        assertEquals(listOf("Frank Herbert"), book.authors)
        assertEquals(1965, book.firstPublishYear)
        assertEquals("https://covers.openlibrary.org/b/id/11481354-M.jpg", book.coverUrl)
    }

    @Test
    fun `search doc without cover has no cover url`() {
        assertNull(SearchDocDto(key = "/works/OL1W").toDomain().coverUrl)
    }

    @Test
    fun `work description can be a plain string`() {
        val detail = WorkDto(key = "/works/OL1W", description = JsonPrimitive("A book")).toDomain()

        assertEquals("A book", detail.description)
    }

    @Test
    fun `work description can be a typed object`() {
        val description =
            buildJsonObject {
                put("type", "/type/text")
                put("value", "A book")
            }

        assertEquals(
            "A book",
            WorkDto(key = "/works/OL1W", description = description).toDomain().description
        )
    }

    @Test
    fun `invalid cover ids are skipped`() {
        val detail = WorkDto(key = "/works/OL1W", covers = listOf(-1, 42)).toDomain()

        assertEquals("https://covers.openlibrary.org/b/id/42-L.jpg", detail.coverUrl)
    }

    @Test
    fun `real work payload is parsed and mapped`() {
        val json =
            """
            {"key":"/works/OL45804W","title":"Fantastic Mr Fox","first_publish_date":"1970",
             "description":{"type":"/type/text","value":"A clever fox"},
             "subjects":["Animals","Foxes"],"covers":[6498519],"unknown":"ignored"}
            """.trimIndent()

        val detail = Json { ignoreUnknownKeys = true }.decodeFromString<WorkDto>(json).toDomain()

        assertEquals("OL45804W", detail.id)
        assertEquals("Fantastic Mr Fox", detail.title)
        assertEquals("1970", detail.firstPublishDate)
        assertEquals("A clever fox", detail.description)
        assertEquals(listOf("Animals", "Foxes"), detail.subjects)
    }
}
