package gr.pkcoding.peoplescope.data.remote.api

import gr.pkcoding.peoplescope.data.remote.dto.UserResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface RandomUserApi {

    @GET("api/")
    suspend fun getUsers(
        @Query("page") page: Int,
        @Query("results") results: Int = 25,
        @Query("seed") seed: String = "peoplescope"
    ): UserResponse
}