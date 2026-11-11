package com.diegocmar.booksearch.data.repository

import com.diegocmar.booksearch.domain.model.BookError
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

private const val HTTP_NOT_FOUND = 404

/**
 * Runs [block] and maps any failure into a [BookError].
 *
 * [CancellationException] is always rethrown so structured concurrency keeps working: a cancelled
 * search must stop, not turn into an error state.
 */
internal suspend inline fun <T> safeApiCall(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e.toBookError())
}

internal fun Throwable.toBookError(): BookError = when (this) {
    is BookError -> this

    is IOException -> BookError.Network

    is HttpException -> if (code() ==
        HTTP_NOT_FOUND
    ) {
        BookError.NotFound
    } else {
        BookError.Server(code())
    }

    is SerializationException -> BookError.Unknown(this)

    else -> BookError.Unknown(this)
}
