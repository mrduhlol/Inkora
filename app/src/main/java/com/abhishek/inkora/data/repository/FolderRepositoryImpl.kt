package com.abhishek.inkora.data.repository

import com.abhishek.inkora.data.local.database.FolderDao
import com.abhishek.inkora.data.local.database.NoteDao
import com.abhishek.inkora.data.local.database.entities.FolderEntity
import com.abhishek.inkora.domain.model.Folder
import com.abhishek.inkora.domain.repository.FolderRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FolderRepositoryImpl @Inject constructor(
    private val folderDao: FolderDao,
    private val noteDao: NoteDao
) : FolderRepository {
    override fun observeFolders(): Flow<List<Folder>> =
        folderDao.observe().map { list -> list.map { Folder(it.id, it.name, it.createdAt) } }

    override suspend fun create(name: String): Long =
        folderDao.upsert(FolderEntity(name = name.trim()))

    override suspend fun rename(id: Long, name: String) = folderDao.rename(id, name.trim())

    override suspend fun delete(id: Long) {
        // Keep notes, just unfile them — never cascade-delete user content.
        noteDao.clearFolder(id)
        folderDao.delete(id)
    }
}
