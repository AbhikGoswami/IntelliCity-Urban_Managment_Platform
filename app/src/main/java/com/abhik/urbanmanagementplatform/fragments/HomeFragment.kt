package com.abhik.urbanmanagementplatform.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.abhik.urbanmanagementplatform.R
import com.abhik.urbanmanagementplatform.adapters.DepartmentAdapter
import com.abhik.urbanmanagementplatform.models.Department

class HomeFragment : Fragment() {

    private lateinit var departmentAdapter: DepartmentAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.homefragmentlayout, container, false)
        recyclerView = view.findViewById(R.id.rv_departments)
        setupRecyclerView()
        return view
    }

    private fun setupRecyclerView() {
        // 1. Create the data source for the list
        val departments = listOf(
            Department("Electricity", R.drawable.ic_electric),
            Department("Water Supply", R.drawable.ic_water),
            Department("Roads & Potholes", R.drawable.ic_road),
            Department("Waste Management", R.drawable.ic_waste),
            //Department("Public Parks", R.drawable.ic_park)
            // You can add more departments here
        )

        // 2. Initialize the adapter and define what happens when an item is clicked
        departmentAdapter = DepartmentAdapter(departments) { selectedDepartment ->
            // Create an instance of the grievance form, passing the selected department's name
            val grievanceFragment = GrievanceFormFragment.newInstance(selectedDepartment.name)

            // Navigate to the grievance form fragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, grievanceFragment)
                .addToBackStack(null) // Allows the user to press 'back' to return to this screen
                .commit()
        }

        // 3. Attach the adapter and a layout manager to the RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = departmentAdapter
    }
}

