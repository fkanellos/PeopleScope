package gr.pkcoding.peoplescope.data.remote.dto

import com.google.gson.annotations.SerializedName

data class UserResponse(
    @SerializedName("results")
    val results: List<UserDto>,
    @SerializedName("info")
    val info: InfoDto
)

data class InfoDto(
    @SerializedName("seed")
    val seed: String,
    @SerializedName("results")
    val results: Int,
    @SerializedName("page")
    val page: Int,
    @SerializedName("version")
    val version: String
)

