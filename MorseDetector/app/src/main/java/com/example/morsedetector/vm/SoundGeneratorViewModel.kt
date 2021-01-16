package com.example.morsedetector.vm

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.morsedetector.model.*
import com.example.morsedetector.util.audio.file.WavFileWriter
import com.example.morsedetector.util.Constants
import com.example.morsedetector.util.audio.player.SamplesPlayer
import kotlinx.coroutines.*

class SoundGeneratorViewModel : ViewModel() {
    companion object {
        const val LOG_TAG = "SoundGeneratorViewModel"
    }

    val generator = MorseCodeSignalGenerator()
    val samplesPlayer =
        SamplesPlayer()
    val audioFileWriter =
        WavFileWriter()

    val playing = MutableLiveData<Boolean>()

    var currentVolume: Volume = Volume.fromRatio(1f)
    var currentFrequency: Float = 300f
    var currentWaveformType: WaveformType = WaveformType.SINE
    var currentWaveformTypes: List<WaveformType> = initWaveformTypes()
    var currentGroupsPerMinute: Float = 12f

    val frequency = MutableLiveData<Float>(currentFrequency)
    val volume = MutableLiveData<Volume>(currentVolume)
    val waveformType = MutableLiveData<WaveformType>(currentWaveformType)
    val waveformTypes = MutableLiveData<List<WaveformType>>(currentWaveformTypes)
    val groupsPerMinute = MutableLiveData<Float>(currentGroupsPerMinute)
    var currentText = MutableLiveData<SymbolsText>(SymbolsText())

    init {
        setWaveformType(WaveformType.SINE)
        updateParams()
    }

    var job: Job? = null

    fun start() {
        stop()
        samplesPlayer.start()
    }
    var channel: Channel? = null

    fun play() {
//        val textString = "ЗЬЫВФ  ЛГОЖЙ  ЦШДЙП  ЦДТМЦ  ЫЩЧЬМ  ЖЫЖВК  ЦЕЛКЦ  ТВЯДЬ  КБЩЦП  ЖРЕЫЖ  ЮКЗОЮ  ФМЩДА  ТСДЕР  ЮЛАЕУ  ЩЫГЖЭ  ЬДЦЕБ  САЭВЫ  ЩЮЧЦХ  ЫТТЛЙ  ВФВКВ  ОЫШВВ  БЧСНЕ  ЭВЗХШ  ЭАЩЖВ  ЬЗДСС  ТЮДБЩ  БЦЙФЛ  ЖИЫКШ  ЛОДРС  ЦЯВБУ  \n"
        val text = currentText.value ?: return
        text.addAlphabet(Constants.russianAlphabet)
        text.generateRandom(60)
        generator.clear()
        generator.addText(text)
        channel = generator.addReceiver()
        generator.start(viewModelScope)

        job = viewModelScope.launch (Dispatchers.Default){
            Log.d(LOG_TAG, "start receiving")
            while(isActive) {
                val data = channel?.receiveData()
                Log.d(LOG_TAG, "received data ${data}")
                if (data != null) {
                    Log.d(LOG_TAG, "received data size ${data.size}")
                    samplesPlayer.play(data)
//                    audioFileWriter.write(data)
                }
            }
            pause()
            Log.d(LOG_TAG, "end receiving")
        }
        playing.postValue(true)
    }

    fun pause() {
        job?.cancel()
        generator.stop()

        playing.postValue(false)

//        val dir = App.instance.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return
//        val file = File(dir, "record.wav")
//        dir.mkdirs()
//        file.createNewFile()
//        Log.d(LOG_TAG, "saving into file ${file.absolutePath}")
//        audioFileWriter.complete(file, AudioParams.createDefault())
    }

    fun stop() {
        pause()
        samplesPlayer.stop()
//        audioFileWriter.release()
    }

    private fun initWaveformTypes(): List<WaveformType> {
        return listOf(
            WaveformType.SINE,
            WaveformType.TRIANGLE,
            WaveformType.SAW_TOOTH,
            WaveformType.SQUARE
        )
    }

    private fun updateParams() {
        generator.currentFrequency = currentFrequency
        generator.currentVolume = currentVolume
        generator.currentWaveformType = currentWaveformType
        generator.currentGroupPerMinute = currentGroupsPerMinute

        waveformTypes.postValue(currentWaveformTypes)
        waveformType.postValue(currentWaveformType)
        frequency.postValue(currentFrequency)
        volume.postValue(currentVolume)
        groupsPerMinute.postValue(currentGroupsPerMinute)
    }

    fun setWaveformType(type: WaveformType) {
        currentWaveformType = type
        currentWaveformTypes.forEach {
            it.selected = it.id == type.id
        }
        updateParams()
    }

    fun setFrequency(newFrequency: Float) {
        currentFrequency = newFrequency
        updateParams()
    }

    fun setVolume(newVolume: Volume) {
        currentVolume = newVolume
        updateParams()
    }

    fun setGroupsPerMinute(groupsPerMinute: Float) {
        currentGroupsPerMinute = groupsPerMinute
        updateParams()
    }
}