package gr.pkcoding.peoplescope.domain.usecase

import gr.pkcoding.peoplescope.domain.model.DataError
import gr.pkcoding.peoplescope.domain.model.Result
import gr.pkcoding.peoplescope.domain.repository.UserRepository

class RemoveBookmarkUseCase(
    private val repository: UserRepository
) {
    suspend operator fun invoke(userId: String): Result<Unit, DataError.Local> {
        return repository.removeBookmark(userId)
    }
}