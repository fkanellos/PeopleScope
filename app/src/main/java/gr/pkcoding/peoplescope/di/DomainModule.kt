package gr.pkcoding.peoplescope.di

import gr.pkcoding.peoplescope.domain.usecase.GetUserDetailsUseCase
import gr.pkcoding.peoplescope.domain.usecase.GetUsersPagedUseCase
import gr.pkcoding.peoplescope.domain.usecase.GetUsersUseCase
import gr.pkcoding.peoplescope.domain.usecase.IsUserBookmarkedUseCase
import gr.pkcoding.peoplescope.domain.usecase.ToggleBookmarkUseCase
import org.koin.dsl.module

val domainModule = module {
     factory { GetUsersUseCase(get()) }
     factory { GetUserDetailsUseCase(get()) }
     factory { ToggleBookmarkUseCase(get()) }
     factory { IsUserBookmarkedUseCase(get()) }
     factory { GetUsersPagedUseCase(get()) }
}