package com.abhishek.inkora.di

import com.abhishek.inkora.data.repository.FolderRepositoryImpl
import com.abhishek.inkora.data.repository.NoteRepositoryImpl
import com.abhishek.inkora.domain.repository.FolderRepository
import com.abhishek.inkora.domain.repository.NoteRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun bindNotes(impl: NoteRepositoryImpl): NoteRepository

    @Binds @Singleton
    abstract fun bindFolders(impl: FolderRepositoryImpl): FolderRepository
}
