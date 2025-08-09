package gr.pkcoding.peoplescope.di

import gr.pkcoding.peoplescope.data.network.NetworkConnectivityProvider
import gr.pkcoding.peoplescope.presentation.ui.userdetail.UserDetailViewModel
import gr.pkcoding.peoplescope.presentation.ui.userlist.UserListViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
    // ViewModels
    viewModel {
        UserListViewModel(
            getUsersPagedUseCase = get(),
            toggleBookmarkUseCase = get(),
            bookmarkDao = get(),
            networkProvider = get<NetworkConnectivityProvider>()
        )
    }

    viewModel { (userId: String) ->
        UserDetailViewModel(
            userId = userId,
            getUserDetailsUseCase = get(),
            toggleBookmarkUseCase = get(),
            isUserBookmarkedUseCase = get()
        )
    }}