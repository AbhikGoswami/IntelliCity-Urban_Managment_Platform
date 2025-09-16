package com.abhik.urbanmanagementplatform.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.abhik.urbanmanagementplatform.R
import com.abhik.urbanmanagementplatform.models.Department

class DepartmentAdapter(
    private val departments: List<Department>,
    private val onItemClick: (Department) -> Unit
) : RecyclerView.Adapter<DepartmentAdapter.DepartmentViewHolder>() {

    class DepartmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.iv_department_icon)
        val name: TextView = itemView.findViewById(R.id.tv_department_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DepartmentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.departmentcardlayout, parent, false)
        return DepartmentViewHolder(view)
    }

    override fun onBindViewHolder(holder: DepartmentViewHolder, position: Int) {
        val department = departments[position]
        holder.name.text = department.name
        holder.icon.setImageResource(department.iconResId)

        holder.itemView.setOnClickListener {
            onItemClick(department)
        }
    }

    override fun getItemCount() = departments.size
}
