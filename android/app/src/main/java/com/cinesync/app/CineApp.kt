package com.cinesync.app

import android.app.Application
import com.cinesync.app.data.Session

class CineApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Session.init(this)
    }
}
