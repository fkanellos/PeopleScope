package gr.pkcoding.peoplescope.domain.usecase

import gr.pkcoding.peoplescope.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow

class IsUserBookmarkedUseCase(
    private val repository: UserRepository
) {
    operator fun invoke(userId: String): Flow<Boolean> {
        return repository.isUserBookmarked(userId)
    }
}