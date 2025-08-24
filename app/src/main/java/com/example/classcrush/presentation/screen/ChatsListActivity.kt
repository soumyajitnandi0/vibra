package com.example.classcrush.presentation.screen

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.classcrush.R
import com.example.classcrush.data.model.ChatSummary
import com.example.classcrush.data.model.UserSession
import com.google.firebase.database.*

class ChatsListActivity : AppCompatActivity() {
    private lateinit var recycler: RecyclerView
    private val items = mutableListOf<ChatSummary>()
    private lateinit var adapter: ChatsAdapter
    private lateinit var ref: DatabaseReference
    private lateinit var myId: String
    private lateinit var myName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_matches)

        myId = UserSession.id(this)
        myName = UserSession.name(this)

        recycler = findViewById(R.id.matchesRecycler)
        adapter = ChatsAdapter(items) { s ->
            val i = Intent(this, ChatActivity::class.java)
            i.putExtra("otherUserId", s.otherUserId)
            i.putExtra("otherUserName", s.otherUserName)
            startActivity(i)
        }
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        ref = FirebaseDatabase.getInstance().getReference("chat_summaries").child(myId)
        ref.orderByChild("lastTimestamp").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                items.clear()
                for (sn in snapshot.children) {
                    sn.getValue(ChatSummary::class.java)?.let { items.add(it) }
                }
                items.sortByDescending { it.lastTimestamp }
                adapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private class ChatsAdapter(
        private val list: List<ChatSummary>,
        private val onClick: (ChatSummary) -> Unit
    ) : RecyclerView.Adapter<ChatsAdapter.VH>() {

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(R.id.rowName)
            val last: TextView = v.findViewById(R.id.rowLast)
            val time: TextView = v.findViewById(R.id.rowTime)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_match_row, parent, false)
            return VH(v)
        }

        override fun getItemCount() = list.size

        override fun onBindViewHolder(h: VH, pos: Int) {
            val chatSummary = list[pos]
            h.name.text = chatSummary.otherUserName
            h.last.text = chatSummary.lastMessage
            h.time.text = android.text.format.DateFormat.format("hh:mm a", chatSummary.lastTimestamp)
            h.itemView.setOnClickListener { onClick(chatSummary) }
        }
    }
}




