package com.abhik.urbanmanagementplatform.fragments

import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.abhik.urbanmanagementplatform.R
import com.abhik.urbanmanagementplatform.models.Announcement
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Locale

class AnnouncementListFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NewsFeedAdapter
    private val newsList = mutableListOf<Announcement>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_announcement_list, container, false)

        recyclerView = view.findViewById(R.id.rv_full_feed)
        recyclerView.layoutManager = LinearLayoutManager(context)

        adapter = NewsFeedAdapter(newsList)
        recyclerView.adapter = adapter

        fetchFullHistory()

        return view
    }

    private fun fetchFullHistory() {
        FirebaseFirestore.getInstance().collection("announcements")
            .whereEqualTo("status", "published")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener

                newsList.clear()
                value?.forEach { doc ->
                    newsList.add(doc.toObject(Announcement::class.java))
                }
                adapter.notifyDataSetChanged()
            }
    }

    inner class NewsFeedAdapter(private val list: List<Announcement>) :
        RecyclerView.Adapter<NewsFeedAdapter.NewsHolder>() {

        inner class NewsHolder(v: View) : RecyclerView.ViewHolder(v) {
            val date: TextView = v.findViewById(R.id.tv_news_date)
            val title: TextView = v.findViewById(R.id.tv_news_title)
            val content: TextView = v.findViewById(R.id.tv_news_content)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_news_feed, parent, false)
            return NewsHolder(v)
        }

        override fun onBindViewHolder(holder: NewsHolder, position: Int) {
            val item = list[position]
            holder.title.text = item.title
            holder.content.text = item.content

            item.timestamp?.let {
                val cal = java.util.Calendar.getInstance(Locale.ENGLISH)
                cal.timeInMillis = it.seconds * 1000L
                val dateStr = DateFormat.format("MMM dd, yyyy • h:mm a", cal).toString()
                holder.date.text = dateStr
            } ?: run {
                holder.date.text = "Just now"
            }
        }

        override fun getItemCount() = list.size
    }
}