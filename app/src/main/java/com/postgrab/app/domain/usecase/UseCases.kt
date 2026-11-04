package com.postgrab.app.domain.usecase

import android.net.Uri
import com.postgrab.app.domain.model.ImportedImage
import com.postgrab.app.domain.model.SaveResult
import com.postgrab.app.domain.model.SourceType
import com.postgrab.app.domain.repository.ImageRepository

class ImportImageUseCase(private val repo: ImageRepository) {
    suspend fun fromUrl(url: String, onProgress: (Float) -> Unit): List<ImportedImage> =
        listOf(repo.importUrl(url, onProgress))

    suspend fun fromUris(uris: List<Uri>, type: SourceType): List<ImportedImage> =
        repo.importUris(uris, type)
}

class SaveImageUseCase(private val repo: ImageRepository) {
    suspend operator fun invoke(
        image: ImportedImage,
        allowDuplicate: Boolean,
        onProgress: (Float) -> Unit
    ): SaveResult = repo.save(image, allowDuplicate, onProgress)
}

class DeleteSavedImagesUseCase(private val repo: ImageRepository) {
    suspend operator fun invoke(uris: List<Uri>): List<Uri> = repo.deleteSaved(uris)
}
