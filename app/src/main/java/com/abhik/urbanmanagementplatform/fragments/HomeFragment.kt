package com.abhik.urbanmanagementplatform.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.abhik.urbanmanagementplatform.R
import com.abhik.urbanmanagementplatform.adapters.DepartmentAdapter
import com.abhik.urbanmanagementplatform.models.Department
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HomeFragment : Fragment() {

    private lateinit var departmentAdapter: DepartmentAdapter
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.homefragmentlayout, container, false)
        db = FirebaseFirestore.getInstance()

        val layoutLatest = view.findViewById<LinearLayout>(R.id.layout_latest_announcement)
        val tvHomeTitle = view.findViewById<TextView>(R.id.tv_home_ann_title)

        setupLatestAnnouncementTicker(layoutLatest, tvHomeTitle)

        val rvDepartments = view.findViewById<RecyclerView>(R.id.rv_departments)
        setupDepartmentList(rvDepartments)

        return view
    }

    private fun setupLatestAnnouncementTicker(layout: LinearLayout, textView: TextView) {

        db.collection("announcements")
            .whereEqualTo("status", "published")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.e("HomeFragment", "Error fetching latest announcement", error)
                    textView.text = "Error loading updates"
                    return@addSnapshotListener
                }

                if (value != null && !value.isEmpty) {

                    val title = value.documents[0].getString("title") ?: "New Announcement"
                    textView.text = title
                } else {
                    textView.text = "No active announcements"
                }
            }

        layout.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AnnouncementListFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun setupDepartmentList(recyclerView: RecyclerView) {
        val departments = listOf(
            Department("Electricity", R.drawable.ic_electric),
            Department("Water Supply", R.drawable.ic_water),
            Department("Roads & Potholes", R.drawable.ic_road),
            Department("Waste Management", R.drawable.ic_waste)
        )

        departmentAdapter = DepartmentAdapter(departments) { selectedDepartment ->
            val grievanceFragment = GrievanceFormFragment.newInstance(selectedDepartment.name)

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, grievanceFragment)
                .addToBackStack(null)
                .commit()
        }

        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = departmentAdapter
    }
}