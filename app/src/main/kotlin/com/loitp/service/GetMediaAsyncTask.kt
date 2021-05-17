package com.loitp.service

import android.content.Context
import android.os.AsyncTask
import com.loitp.ext.config
import com.loitp.ext.getFavoritePaths
import com.loitp.pro.helpers.*
import com.loitp.pro.models.Medium
import com.loitp.pro.models.ThumbnailItem
import com.simplemobiletools.commons.helpers.FAVORITES
import com.simplemobiletools.commons.helpers.SORT_BY_DATE_MODIFIED
import com.simplemobiletools.commons.helpers.SORT_BY_DATE_TAKEN
import com.simplemobiletools.commons.helpers.SORT_BY_SIZE
import java.util.*

@Suppress("DEPRECATION")
class GetMediaAsyncTask(
    val context: Context,
    val mPath: String,
    val isPickImage: Boolean = false,
    val isPickVideo: Boolean = false,
    val showAll: Boolean,
    val callback: (media: ArrayList<ThumbnailItem>) -> Unit
) :
    AsyncTask<Void, Void, ArrayList<ThumbnailItem>>() {
    private val mediaFetcher = MediaFetcher(context)

    override fun doInBackground(vararg params: Void): ArrayList<ThumbnailItem> {
        val pathToUse = if (showAll) SHOW_ALL else mPath
        val folderGrouping = context.config.getFolderGrouping(pathToUse)
        val fileSorting = context.config.getFolderSorting(pathToUse)
        val getProperDateTaken = fileSorting and SORT_BY_DATE_TAKEN != 0 ||
            folderGrouping and GROUP_BY_DATE_TAKEN_DAILY != 0 ||
            folderGrouping and GROUP_BY_DATE_TAKEN_MONTHLY != 0

        val getProperLastModified = fileSorting and SORT_BY_DATE_MODIFIED != 0 ||
            folderGrouping and GROUP_BY_LAST_MODIFIED_DAILY != 0 ||
            folderGrouping and GROUP_BY_LAST_MODIFIED_MONTHLY != 0

        val getProperFileSize = fileSorting and SORT_BY_SIZE != 0
        val favoritePaths = context.getFavoritePaths()
        val getVideoDurations = context.config.showThumbnailVideoDuration
        val lastModifieds =
            if (getProperLastModified) mediaFetcher.getLastModifieds() else HashMap()
        val dateTakens = if (getProperDateTaken) mediaFetcher.getDateTakens() else HashMap()

        val media = if (showAll) {
            val foldersToScan = mediaFetcher.getFoldersToScan().filter {
                it != RECYCLE_BIN && it != FAVORITES && !context.config.isFolderProtected(it)
            }
            val media = ArrayList<Medium>()
            foldersToScan.forEach {
                val newMedia = mediaFetcher.getFilesFrom(
                    curPath = it,
                    isPickImage = isPickImage,
                    isPickVideo = isPickVideo,
                    getProperDateTaken = getProperDateTaken,
                    getProperLastModified = getProperLastModified,
                    getProperFileSize = getProperFileSize,
                    favoritePaths = favoritePaths,
                    getVideoDurations = getVideoDurations,
                    lastModifieds = lastModifieds,
                    dateTakens = dateTakens
                )
                media.addAll(newMedia)
            }

            mediaFetcher.sortMedia(media, context.config.getFolderSorting(SHOW_ALL))
            media
        } else {
            mediaFetcher.getFilesFrom(
                curPath = mPath,
                isPickImage = isPickImage,
                isPickVideo = isPickVideo,
                getProperDateTaken = getProperDateTaken,
                getProperLastModified = getProperLastModified,
                getProperFileSize = getProperFileSize,
                favoritePaths = favoritePaths,
                getVideoDurations = getVideoDurations,
                lastModifieds = lastModifieds,
                dateTakens = dateTakens
            )
        }

        return mediaFetcher.groupMedia(media, pathToUse)
    }

    override fun onPostExecute(media: ArrayList<ThumbnailItem>) {
        super.onPostExecute(media)
        callback(media)
    }

    fun stopFetching() {
        mediaFetcher.shouldStop = true
        cancel(true)
    }
}
