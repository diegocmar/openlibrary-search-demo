package com.diegocmar.booksearch.domain.model

/**
 * Errors the domain layer knows about. The data layer maps any low-level failure into one of these.
 */
sealed class BookError : Exception() {
    /** No connectivity or the request timed out. */
    data object Network : BookError() {
        private fun readResolve(): Any = Network
    }

    /** The requested resource does not exist. */
    data object NotFound : BookError() {
        private fun readResolve(): Any = NotFound
    }

    /** The server answered with an unexpected HTTP status. */
    data class Server(val code: Int) : BookError()

    /** Anything else (malformed payload, unexpected exception...). */
    data class Unknown(override val cause: Throwable?) : BookError()
}
