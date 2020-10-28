package com.example.morsedetector.ui

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.example.morsedetector.App
import com.example.morsedetector.R
import com.example.morsedetector.adaptor.WaveformAdaptor
import com.example.morsedetector.model.Volume
import com.example.morsedetector.model.WaveformType
import com.example.morsedetector.vm.SoundGeneratorViewModel
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import kotlinx.android.synthetic.main.fragment_sound_generator.*

class SoundGeneratorFragment : BaseFragment() {
    companion object {
        const val LOG_TAG = "SoundGeneratorFragment"
        const val VOLUME_MAX = 1000
    }

    val generatorViewModel: SoundGeneratorViewModel by activityViewModels()
    var waveformAdaptor: WaveformAdaptor? = null
    var waveformAdaptorListener = object : WaveformAdaptor.AdaptorListener {
        override fun onClicked(type: WaveformType, position: Int) {
            generatorViewModel.setWaveformType(type)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_sound_generator, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        waveformAdaptor = WaveformAdaptor()
        waveformAdaptor?.listener = waveformAdaptorListener

        rvWaveformType.adapter = waveformAdaptor
        val layoutManager = FlexboxLayoutManager(context)
        layoutManager.flexDirection = FlexDirection.ROW
        layoutManager.justifyContent = JustifyContent.CENTER
        rvWaveformType.layoutManager = layoutManager

        pbGeneratorPlayback.setOnClickListener {
            if (generatorViewModel.playing.value == true) {
                generatorViewModel.pause()
            } else {
                generatorViewModel.play()
            }
        }
        ivGeneratorShotPlayback.setOnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    Log.d(LOG_TAG, "ACTION_DOWN")
                    generatorViewModel.play()
                }
                MotionEvent.ACTION_UP -> {
                    Log.d(LOG_TAG, "ACTION_UP")
                    generatorViewModel.pause()

                }
            }
            true
        }
        sbFrequency.max = 8000
        sbVolume.max = VOLUME_MAX
        sbGroupsPerMinute.max = 40
        sbFrequency.progress = generatorViewModel.currentFrequency.toInt()
        sbVolume.progress =
            (generatorViewModel.currentVolume.getDb() * (VOLUME_MAX / Volume.VOLUME_MAX_DB)).toInt()
        sbGroupsPerMinute.progress = (generatorViewModel.currentGroupsPerMinute.toInt())

        sbFrequency.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                generatorViewModel.setFrequency(p1.toFloat())
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })
        sbVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, volume: Int, p2: Boolean) {
                generatorViewModel.setVolume(Volume.fromDb(volume * (Volume.VOLUME_MAX_DB / VOLUME_MAX)))
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })
        sbGroupsPerMinute.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, groupsPerMinute: Int, p2: Boolean) {
                generatorViewModel.setGroupsPerMinute(groupsPerMinute.toFloat())
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val groupsPerMinute = seekBar?.progress ?: return
                if (groupsPerMinute < 5) seekBar.progress = groupsPerMinute
            }
        })

        generatorViewModel.waveformTypes.observe(viewLifecycleOwner, Observer {
            it?.let {
                waveformAdaptor?.items = it
            }
        })
        generatorViewModel.playing.observe(viewLifecycleOwner, Observer {
            it?.let {
                if (it) {
                    pbGeneratorPlayback.setState(PlaybackButton.State.STOP)
                } else {
                    pbGeneratorPlayback.setState(PlaybackButton.State.PLAY)
                }
            }
        })
        generatorViewModel.frequency.observe(viewLifecycleOwner, Observer {
            it?.let {
                val frequency = String.format("%.2f", it)
                tvFrequency.setText(getString(R.string.frequency, frequency))
            }
        })
        generatorViewModel.volume.observe(viewLifecycleOwner, Observer {
            it?.let {
                val volume = String.format("%.2f", it.getDb())
                tvVolume.setText(getString(R.string.volume, volume))
            }
        })
        generatorViewModel.groupsPerMinute.observe(viewLifecycleOwner, Observer {
            it?.let {
                val volume = String.format("%.2f", it)
                tvGroupsPerMinute.setText(getString(R.string.groupsPerMinute, volume))
            }
        })
    }

    override fun onStart() {
        super.onStart()
        generatorViewModel.start()
        App.instance.requestPermissions(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    override fun onStop() {
        super.onStop()
        generatorViewModel.stop()
    }


}