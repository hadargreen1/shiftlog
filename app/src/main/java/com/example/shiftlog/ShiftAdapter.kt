package com.example.shiftlog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ShiftAdapter(private val shiftList: List<Shift>) : RecyclerView.Adapter<ShiftAdapter.ShiftViewHolder>() {

    class ShiftViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val startTimeTextView: TextView = itemView.findViewById(R.id.startTimeTextView)
        val endTimeTextView: TextView = itemView.findViewById(R.id.endTimeTextView)
        val durationTextView: TextView = itemView.findViewById(R.id.durationTextView)
        val salaryTextView: TextView = itemView.findViewById(R.id.salaryTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShiftViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_shift, parent, false)
        return ShiftViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShiftViewHolder, position: Int) {
        val shift = shiftList[position]
        holder.startTimeTextView.text = "Start: ${shift.startTime}"
        holder.endTimeTextView.text = "End: ${shift.endTime}"
        holder.durationTextView.text = "Duration: ${shift.duration} hrs"
        holder.salaryTextView.text = "Salary: $${shift.salary}"
    }

    override fun getItemCount(): Int {
        return shiftList.size
    }
}
