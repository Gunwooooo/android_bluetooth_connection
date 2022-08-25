package com.example.andbluetooth

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RecyclerView2Adapter(var context: Context, var data: ArrayList<Data>) : RecyclerView.Adapter<RecyclerView2Adapter.VH>() {


    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private lateinit var data1: TextView
        private lateinit var data2: TextView
        private lateinit var num: TextView
        fun setTextView(tmpData: Data, i: Int) {
            data1 = itemView.findViewById(R.id.data1)
            data2 = itemView.findViewById(R.id.data2)
            num = itemView.findViewById(R.id.num)
            data1.text = "${tmpData.data1}bpm"
            data2.text = "${tmpData.data2}℃"
            num.text = i.toString()
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView2Adapter.VH {
        return VH(LayoutInflater.from(context).inflate(R.layout.recyclerview2_item, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView2Adapter.VH, position: Int) {
        val tmpData = data[position]
        holder.setTextView(tmpData, position + 1)
    }

    override fun getItemCount(): Int {
        return data.size
    }
}
data class Data(var data1: String, var data2: String)