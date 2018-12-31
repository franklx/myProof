package com.example.fabio.myproof

import android.Manifest.permission
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.app.Activity
import android.support.v4.app.ActivityCompat
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat
import android.os.Build
import android.annotation.TargetApi



/**
 * Created by fabio on 21/06/2017.
 */

class Other {
    init {
        startTime = System.currentTimeMillis()
        endTime = startTime
        duration = endTime - startTime
    }

    companion object {
        private var startTime: Long = 0
        private var endTime: Long = 0
        var duration: Long = 0
        fun time() {
            startTime = endTime
            endTime = System.currentTimeMillis() //System.nanoTime();
            duration = endTime - startTime
        }

        fun getDuration(): String {
            return duration.toString()
        }

        internal fun getExternalName(name: String): String {
            return name.replace("_", " ")
        }

        internal fun checkName(name: String): Boolean {
            return name.matches("\\w+".toRegex())
        }

        internal fun isConstant(name: String): Boolean {
            if (name.startsWith("\\")) return true
            if (name.startsWith("§")) return true
            return if (name.startsWith("#")) true else false
        }

    }
}
