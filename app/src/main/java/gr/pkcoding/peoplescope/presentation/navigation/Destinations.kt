package gr.pkcoding.peoplescope.presentation.navigation

sealed class Destinations(val route: String) {
    object UserList : Destinations("user_list")
    object UserDetail : Destinations("user_detail/{userId}") {
        fun createRoute(userId: String) = "user_detail/$userId"
    }
}