package com.abhik.urbanmanagementplatform.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.abhik.urbanmanagementplatform.R
import com.abhik.urbanmanagementplatform.adapters.GrievanceAdapter
import com.abhik.urbanmanagementplatform.models.Grievance
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class TrackGrievancesFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var grievancesRecyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var noGrievancesTextView: TextView
    private lateinit var grievanceAdapter: GrievanceAdapter
    private val grievanceList = mutableListOf<Grievance>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.trackgrievanceslayout, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        grievancesRecyclerView = view.findViewById(R.id.rv_grievances)
        progressBar = view.findViewById(R.id.track_progress_bar)
        noGrievancesTextView = view.findViewById(R.id.tv_no_grievances)

        setupRecyclerView()
        loadGrievances()

        return view
    }

    private fun setupRecyclerView() {
        grievanceAdapter = GrievanceAdapter(grievanceList)
        grievancesRecyclerView.layoutManager = LinearLayoutManager(context)
        grievancesRecyclerView.adapter = grievanceAdapter
    }

    private fun loadGrievances() {
        setLoading(true)
        val userId = auth.currentUser?.uid

        if (userId == null) {
            Toast.makeText(context, "You need to be logged in.", Toast.LENGTH_SHORT).show()
            setLoading(false)
            return
        }

        db.collection("grievances")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING) // Show newest first
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    noGrievancesTextView.visibility = View.VISIBLE
                } else {
                    grievanceList.clear()
                    for (document in documents) {
                        val grievance = document.toObject(Grievance::class.java).copy(id = document.id)
                        grievanceList.add(grievance)
                    }
                    grievanceAdapter.notifyDataSetChanged()
                    noGrievancesTextView.visibility = View.GONE
                }
                setLoading(false)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(context, "Error getting grievances: ${exception.message}", Toast.LENGTH_LONG).show()
                setLoading(false)
            }
    }

    private fun setLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}
