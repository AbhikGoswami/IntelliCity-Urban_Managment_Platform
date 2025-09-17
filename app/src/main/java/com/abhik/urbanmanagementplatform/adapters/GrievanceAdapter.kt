package com.abhik.urbanmanagementplatform.adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.abhik.urbanmanagementplatform.R
import com.abhik.urbanmanagementplatform.models.Grievance
import java.text.SimpleDateFormat
import java.util.*

class GrievanceAdapter(private val grievanceList: List<Grievance>) :
    RecyclerView.Adapter<GrievanceAdapter.GrievanceViewHolder>() {

    class GrievanceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.tv_grievance_title)
        val departmentTextView: TextView = itemView.findViewById(R.id.tv_grievance_department)
        val dateTextView: TextView = itemView.findViewById(R.id.tv_grievance_date)
        val statusTextView: TextView = itemView.findViewById(R.id.tv_grievance_status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GrievanceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.grievancecardlayout, parent, false)
        return GrievanceViewHolder(view)
    }

    override fun onBindViewHolder(holder: GrievanceViewHolder, position: Int) {
        val grievance = grievanceList[position]

        holder.titleTextView.text = grievance.title
        holder.departmentTextView.text = "Department: ${grievance.department}"

        // Format the timestamp to a readable date
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        holder.dateTextView.text = "Submitted on: ${sdf.format(grievance.timestamp.toDate())}"

        holder.statusTextView.text = grievance.status
        setStatusColor(holder.statusTextView, grievance.status, holder.itemView.context)
    }

    override fun getItemCount() = grievanceList.size

    private fun setStatusColor(statusView: TextView, status: String, context: Context) {
        val color = when (status.lowercase(Locale.ROOT)) {
            "submitted" -> "#0288D1" // Blue
            "in progress" -> "#FFA000" // Orange
            "resolved" -> "#388E3C" // Green
            "rejected" -> "#D32F2F" // Red
            else -> "#757575" // Gray
        }
        statusView.background.setTint(Color.parseColor(color))
    }
}
