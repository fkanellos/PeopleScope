package gr.pkcoding.peoplescope.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import gr.pkcoding.peoplescope.utils.collectAsEffect
import gr.pkcoding.peoplescope.utils.showToast
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

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

            viewModel.effect.collectAsEffect { effect ->
                when (effect) {
                    is UserListEffect.NavigateToUserDetail -> {
                        navController.navigate(
                            Destinations.UserDetail.createRoute(effect.user.id)
                        )
                    }
                    is UserListEffect.ShowError -> {
                        context.showToast(effect.message.asString(context))
                    }
                    is UserListEffect.ShowBookmarkToggled -> {
                        val message = if (effect.isBookmarked) {
                            "User bookmarked successfully"
                        } else {
                            "Bookmark removed"
                        }
                        context.showToast(message)
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
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            val viewModel: UserDetailViewModel = koinViewModel(parameters = { parametersOf(userId) })
            val state by viewModel.state.collectAsStateWithLifecycle()
            val context = LocalContext.current

            viewModel.effect.collectAsEffect { effect ->
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