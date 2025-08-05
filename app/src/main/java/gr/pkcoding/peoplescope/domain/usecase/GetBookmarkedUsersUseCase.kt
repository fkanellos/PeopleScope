package gr.pkcoding.peoplescope.domain.usecase

import gr.pkcoding.peoplescope.domain.model.DataError
import gr.pkcoding.peoplescope.domain.model.Result
import gr.pkcoding.peoplescope.domain.model.User
import gr.pkcoding.peoplescope.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow

class GetBookmarkedUsersUseCase(
    private val repository: UserRepository
) {
    operator fun invoke(): Flow<Result<List<User>, DataError.Local>> {
        return repository.getBookmarkedUsers()
    }
}