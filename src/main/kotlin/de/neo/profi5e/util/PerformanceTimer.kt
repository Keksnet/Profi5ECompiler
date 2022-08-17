package de.neo.profi5e.util

import java.util.concurrent.locks.ReentrantLock

class PerformanceTimer {

    var running = ReentrantLock()

    private var startTime: Long = 0
    private var startTimeNano: Long = 0

    private var stopTime: Long = 0
    private var stopTimeNano: Long = 0

    var millis: Long = 0
        private set

    var nanos: Long = 0
        private set

    var elapsedTime: String = "0ms 0ns"
        private set

    fun start() {
        if (!running.tryLock()) error("Timer is already running")
        startTime = System.currentTimeMillis()
        startTimeNano = System.nanoTime()
    }

    fun stop() {
        if (!running.isLocked) error("Timer is not running")
        stopTime = System.currentTimeMillis()
        stopTimeNano = System.nanoTime()

        millis = stopTime - startTime
        nanos = stopTimeNano - startTimeNano
        elapsedTime = "${millis}ms ${nanos}ns"

        running.unlock()
    }

}