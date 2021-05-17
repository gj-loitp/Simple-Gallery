package com.loitp.ui.activity

import android.os.Bundle
import com.loitp.ui.activity.PhotoVideoActivity

class VideoActivity : PhotoVideoActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        mIsVideo = true
        super.onCreate(savedInstanceState)
    }
}
