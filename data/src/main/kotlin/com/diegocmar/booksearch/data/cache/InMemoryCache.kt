package com.diegocmar.booksearch.data.cache

import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Thread-safe in-memory cache whose entries expire [ttl] after being stored.
 *
 * Time is measured with an injectable [TimeSource] (monotonic by default, so it is not affected by
 * changes of the device clock) which lets tests control expiration deterministically.
 */
class InMemoryCache<K : Any, V : Any>(
    private val ttl: Duration,
    private val timeSource: TimeSource = TimeSource.Monotonic
) {
    private data class Entry<V>(val value: V, val storedAt: TimeMark)

    private val mutex = Mutex()
    private val entries = mutableMapOf<K, Entry<V>>()

    /** Returns the value for [key] if present and not expired; expired entries are evicted. */
    suspend fun get(key: K): V? = mutex.withLock {
        val entry = entries[key] ?: return@withLock null
        if (entry.storedAt.elapsedNow() < ttl) {
            entry.value
        } else {
            entries.remove(key)
            null
        }
    }

    suspend fun put(key: K, value: V) = mutex.withLock {
        entries[key] = Entry(value, timeSource.markNow())
    }

    suspend fun clear() = mutex.withLock { entries.clear() }
}
