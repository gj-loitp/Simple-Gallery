package com.loitp.receiver

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.loitp.ext.updateDirectoryPath
import com.loitp.helper.MediaFetcher
import com.simplemobiletools.commons.helpers.ensureBackgroundThread

class BootCompletedReceiver : BroadcastReceiver() {
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        ensureBackgroundThread {
            MediaFetcher(context).getFoldersToScan().forEach {
                context.updateDirectoryPath(it)
            }
        }
    }
}
