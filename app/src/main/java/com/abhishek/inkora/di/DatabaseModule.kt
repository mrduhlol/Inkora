package com.abhishek.inkora.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.abhishek.inkora.data.local.database.AttachmentDao
import com.abhishek.inkora.data.local.database.FolderDao
import com.abhishek.inkora.data.local.database.InkoraDatabase
import com.abhishek.inkora.data.local.database.NoteDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.inkoraStore by preferencesDataStore("inkora_settings")

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): InkoraDatabase =
        Room.databaseBuilder(ctx, InkoraDatabase::class.java, InkoraDatabase.NAME)
            .addMigrations(InkoraDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideNoteDao(db: InkoraDatabase): NoteDao = db.noteDao()
    @Provides fun provideFolderDao(db: InkoraDatabase): FolderDao = db.folderDao()
    @Provides fun provideAttachmentDao(db: InkoraDatabase): AttachmentDao = db.attachmentDao()

    @Provides @Singleton
    fun provideDataStore(@ApplicationContext ctx: Context): DataStore<Preferences> = ctx.inkoraStore
}
