package com.diegocmar.booksearch.domain.usecase

import com.diegocmar.booksearch.domain.model.BookDetail
import com.diegocmar.booksearch.domain.model.BookError
import com.diegocmar.booksearch.domain.repository.BookRepository
import javax.inject.Inject

class GetBookDetailUseCase
@Inject
constructor(private val repository: BookRepository) {
    /**
     * @param id Open Library work id (e.g. `OL45804W`).
     * @return [Result.success] with the detail, [Result.failure] with [BookError.NotFound] if [id] is
     * blank, or the [BookError] reported by the repository.
     */
    suspend operator fun invoke(id: String): Result<BookDetail> {
        if (id.isBlank()) return Result.failure(BookError.NotFound)
        return repository.getBookDetail(id)
    }
}
