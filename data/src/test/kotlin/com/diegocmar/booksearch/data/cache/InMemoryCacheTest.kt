package com.diegocmar.booksearch.data.cache

import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TestTimeSource
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class InMemoryCacheTest {
    private val timeSource = TestTimeSource()
    private val cache = InMemoryCache<String, String>(ttl = 3.minutes, timeSource = timeSource)

    @Test
    fun `returns null for a missing key`() = runTest {
        assertNull(cache.get("missing"))
    }

    @Test
    fun `returns the stored value before the ttl expires`() = runTest {
        cache.put("key", "value")
        timeSource += 3.minutes - 1.milliseconds

        assertEquals("value", cache.get("key"))
    }

    @Test
    fun `returns null once the ttl has expired`() = runTest {
        cache.put("key", "value")
        timeSource += 3.minutes

        assertNull(cache.get("key"))
    }

    @Test
    fun `put refreshes the value and its expiration`() = runTest {
        cache.put("key", "old")
        timeSource += 2.minutes
        cache.put("key", "new")
        timeSource += 2.minutes

        assertEquals("new", cache.get("key"))
    }

    @Test
    fun `clear removes every entry`() = runTest {
        cache.put("a", "1")
        cache.put("b", "2")

        cache.clear()

        assertNull(cache.get("a"))
        assertNull(cache.get("b"))
    }
}
