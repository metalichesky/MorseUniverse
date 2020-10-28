package com.example.morsedetector.adaptor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.morsedetector.R
import com.example.morsedetector.model.WaveformType
import kotlinx.android.synthetic.main.item_waveform_type.view.*

class WaveformAdaptor: RecyclerView.Adapter<WaveformAdaptor.WaveformViewHolder>() {
    var listener: AdaptorListener? = null
    var items: List<WaveformType> = emptyList()
    set(value) {
        notifyDataSetChanged()
        field = value
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WaveformViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_waveform_type, parent, false)
        return WaveformViewHolder(view)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: WaveformViewHolder, position: Int) {
        val realPosition = holder.adapterPosition
        if (realPosition != RecyclerView.NO_POSITION) {
            items.getOrNull(realPosition)?.let {
                holder.bind(it, realPosition)
            }
        }
    }

    inner class WaveformViewHolder(val view: View): RecyclerView.ViewHolder(view) {

        fun bind(type: WaveformType, position: Int) {
            val context = view.context
            view.ivWaveform.setImageDrawable(ContextCompat.getDrawable(context, type.drawableRes))
            view.tvWaveform.setText(context.getText(type.nameRes))
            view.cbxWaveform.isChecked = type.selected
            view.cbxWaveform.isClickable = false
            view.flWaveform.setOnClickListener {
                listener?.onClicked(type, position)
            }
        }
    }

    interface AdaptorListener {
        fun onClicked(type: WaveformType, position: Int)

    }
}