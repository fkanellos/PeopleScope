package gr.pkcoding.peoplescope.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import gr.pkcoding.peoplescope.presentation.ui.userdetail.UserDetailEffect
import gr.pkcoding.peoplescope.presentation.ui.userdetail.UserDetailScreen
import gr.pkcoding.peoplescope.presentation.ui.userdetail.UserDetailViewModel
import gr.pkcoding.peoplescope.presentation.ui.userlist.UserListEffect
import gr.pkcoding.peoplescope.presentation.ui.userlist.UserListScreen
import gr.pkcoding.peoplescope.presentation.ui.userlist.UserListViewModel
import gr.pkcoding.peoplescope.utils.CollectAsEffect
import gr.pkcoding.peoplescope.utils.Constants
import gr.pkcoding.peoplescope.utils.showToast
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import timber.log.Timber

@Composable
fun PeopleScopeNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Destinations.UserList.route
    ) {
        composable(route = Destinations.UserList.route) {
            val viewModel: UserListViewModel = koinViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            val context = LocalContext.current

            // Debounce state για toast messages
            var lastToastTime by remember { mutableLongStateOf(0L) }

            viewModel.effect.CollectAsEffect { effect ->
                when (effect) {
                    is UserListEffect.NavigateToUserDetail -> {
                        // ✅ Safe navigation με null check
                        val userId = effect.user.id
                        if (!userId.isNullOrBlank()) {
                            Timber.d("🚀 Navigating to user detail: $userId")
                            navController.navigate(
                                Destinations.UserDetail.createRoute(userId)
                            )
                        } else {
                            Timber.w("❌ Cannot navigate: user ID is null")
                            context.showToast("Cannot view details for this user")
                        }
                    }
                    is UserListEffect.ShowError -> {
                        Timber.e("❌ Showing error: ${effect.message}")
                        context.showToast(effect.message.asString(context))
                    }
                    is UserListEffect.ShowBookmarkToggled -> {
                        // Debounce toast messages (500ms)
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastToastTime > 500) {
                            val message = if (effect.isBookmarked) {
                                "User bookmarked ⭐"
                            } else {
                                "Bookmark removed"
                            }
                            Timber.d("📱 Showing bookmark toast: $message")
                            context.showToast(message)
                            lastToastTime = currentTime
                        }
                    }
                }
            }

            UserListScreen(
                state = state,
                onIntent = viewModel::processIntent,
                viewModel = viewModel
            )
        }

        composable(route = Destinations.UserDetail.route) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString(Constants.NAV_USER_ID_KEY)
                ?: return@composable

            val viewModel: UserDetailViewModel = koinViewModel(parameters = { parametersOf(userId) })
            val state by viewModel.state.collectAsStateWithLifecycle()
            val context = LocalContext.current

            viewModel.effect.CollectAsEffect { effect ->
                when (effect) {
                    is UserDetailEffect.NavigateBack -> {
                        navController.popBackStack()
                    }
                    is UserDetailEffect.ShowError -> {
                        context.showToast(effect.message.asString(context))
                    }
                    is UserDetailEffect.ShowBookmarkToggled -> {
                        val message = if (effect.isBookmarked) {
                            "User bookmarked successfully"
                        } else {
                            "Bookmark removed"
                        }
                        context.showToast(message)
                    }
                }
            }

            UserDetailScreen(
                state = state,
                onIntent = viewModel::processIntent,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}