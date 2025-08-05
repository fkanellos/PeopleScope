package gr.pkcoding.peoplescope.domain.usecase

import gr.pkcoding.peoplescope.domain.model.Result
import gr.pkcoding.peoplescope.domain.model.User
import gr.pkcoding.peoplescope.domain.model.UserError
import gr.pkcoding.peoplescope.domain.repository.UserRepository

class GetUserDetailsUseCase(
    private val repository: UserRepository
) {
    suspend operator fun invoke(userId: String): Result<User, UserError> {
        return repository.getUserById(userId)
    }
}