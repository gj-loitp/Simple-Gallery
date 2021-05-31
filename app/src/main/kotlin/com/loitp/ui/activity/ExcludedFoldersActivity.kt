package com.loitp.ui.activity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.loitp.pro.R
import com.loitp.adapter.ManageFoldersAdapter
import com.loitp.ext.config
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.extensions.beVisibleIf
import com.simplemobiletools.commons.interfaces.RefreshRecyclerViewListener
import kotlinx.android.synthetic.main.activity_manage_folders.*

class ExcludedFoldersActivity : SimpleActivity(), RefreshRecyclerViewListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_folders)
        updateFolders()
    }

    private fun updateFolders() {
        val folders = ArrayList<String>()
        config.excludedFolders.mapTo(folders) { it }
        tvManageFoldersPlaceHolder.apply {
            text = getString(R.string.excluded_activity_placeholder)
            beVisibleIf(folders.isEmpty())
            setTextColor(config.textColor)
        }

        val adapter = ManageFoldersAdapter(
            activity = this,
            folders = folders,
            isShowingExcludedFolders = true,
            listener = this,
            recyclerView = rvManageFolders
        ) {}
        rvManageFolders.adapter = adapter
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_add_folder, menu)
        updateMenuItemColors(menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add_folder -> addFolder()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun refreshItems() {
        updateFolders()
    }

    private fun addFolder() {
        FilePickerDialog(
            activity = this,
            currPath = config.lastFilepickerPath,
            pickFile = false,
            showHidden = config.shouldShowHidden,
            showFAB = false,
            canAddShowHiddenButton = true,
            forceShowRoot = true
        ) {
            config.lastFilepickerPath = it
            config.addExcludedFolder(it)
            updateFolders()
        }
    }
}
