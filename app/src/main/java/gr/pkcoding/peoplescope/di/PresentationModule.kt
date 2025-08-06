package gr.pkcoding.peoplescope.di

import gr.pkcoding.peoplescope.presentation.ui.userdetail.UserDetailViewModel
import gr.pkcoding.peoplescope.presentation.ui.userlist.UserListViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val presentationModule = module {
    // ViewModels
     viewModel { UserListViewModel(get(), get()) }
     viewModel { (userId: String) -> UserDetailViewModel(userId, get(), get(), get()) }
}