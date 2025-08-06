package gr.pkcoding.peoplescope.domain.usecase

import androidx.paging.PagingData
import gr.pkcoding.peoplescope.domain.model.User
import gr.pkcoding.peoplescope.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow

class GetUsersPagedUseCase(
    private val repository: UserRepository
) {
    operator fun invoke(): Flow<PagingData<User>> {
        return repository.getUsersPaged()
    }
}