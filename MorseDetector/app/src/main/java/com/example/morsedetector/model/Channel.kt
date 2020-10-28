package com.example.morsedetector.model

import android.util.Log
import com.example.morsedetector.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

class Channel() {
    companion object {
        const val LOG_TAG = "Channel"
        val MAX_DURATION = 1000 // store max 2 sec
    }

    private var audioParams: AudioParams = AudioParams.createDefault()
    private var dataQueue: Queue<Pair<ByteArray, Int>> = ConcurrentLinkedQueue()//LinkedList()
    private var dataDuration: Int = 0

    var lastReadedTime = System.currentTimeMillis()

    fun setAudioParams(audioParams: AudioParams) {
        this.audioParams = audioParams
    }

    suspend fun sendData(data: ByteArray) {
        val dataDuration = data.size / audioParams.bytesPerMs
        var availableDuration = getAvailableDuration()
        var fromLastRead = System.currentTimeMillis() - lastReadedTime
        while (dataDuration > availableDuration) {
            //freeSpaceForDuration(dataDuration)
            availableDuration = getAvailableDuration()
            Log.d(LOG_TAG, "sendData() ${data} duration ${dataDuration} available ${availableDuration} from last read ${fromLastRead}")
            delay(10)
            //wait here for data readed
        }
        dataQueue.offer(Pair(data, dataDuration))
    }



    fun receiveData(): ByteArray? {
        lastReadedTime = System.currentTimeMillis()
        val data = dataQueue.poll()?.first
        updateDataDuration()
        return data
    }

    private fun updateDataDuration() {
        this.dataDuration = getDataDuration()
    }

    private fun getDataDuration(): Int {
        var duration = 0
        dataQueue.forEach {
            duration += it.second
        }
        return dataQueue.sumBy { it.second }
    }

    private fun getAvailableDuration(): Int {
        return MAX_DURATION - dataDuration
    }

    private fun freeSpaceForDuration(neededDuration: Int) {
        if (neededDuration > MAX_DURATION) return
        while(getAvailableDuration() < neededDuration) {
            dataQueue.poll()
        }
    }
}