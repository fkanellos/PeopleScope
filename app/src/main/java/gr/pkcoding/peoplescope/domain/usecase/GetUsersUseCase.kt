package gr.pkcoding.peoplescope.domain.usecase

import gr.pkcoding.peoplescope.domain.model.DataError
import gr.pkcoding.peoplescope.domain.model.Result
import gr.pkcoding.peoplescope.domain.model.User
import gr.pkcoding.peoplescope.domain.repository.UserRepository
import gr.pkcoding.peoplescope.utils.Constants

class GetUsersUseCase(
    private val repository: UserRepository
) {
    suspend operator fun invoke(
        page: Int = Constants.INITIAL_PAGE,
        pageSize: Int = Constants.PAGE_SIZE
    ): Result<List<User>, DataError> {
        return repository.getUsers(page, pageSize)
    }
}
