package gr.pkcoding.peoplescope.domain.usecase

import gr.pkcoding.peoplescope.domain.model.DataError
import gr.pkcoding.peoplescope.domain.model.Result
import gr.pkcoding.peoplescope.domain.model.User
import gr.pkcoding.peoplescope.domain.repository.UserRepository

class BookmarkUserUseCase(
    private val repository: UserRepository
) {
    suspend operator fun invoke(user: User): Result<Unit, DataError.Local> {
        return repository.bookmarkUser(user)
    }
}