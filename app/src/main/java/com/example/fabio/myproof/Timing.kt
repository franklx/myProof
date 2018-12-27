package com.example.fabio.myproof

/**
 * Created by fabio on 21/06/2017.
 */

class Timing {
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
    }
}
