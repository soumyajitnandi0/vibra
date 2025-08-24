package com.example.classcrush.presentation.screen

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.classcrush.R
import com.example.classcrush.data.model.ChatUtils
import com.example.classcrush.data.model.GossipModel
import com.example.classcrush.data.model.UserSession
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException

class ChatActivity : AppCompatActivity() {

    private lateinit var inputMsg: EditText
    private lateinit var btnSend: FloatingActionButton
    private lateinit var btnImage: FloatingActionButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var toolbar: Toolbar
    private lateinit var titleName: TextView

    private lateinit var adapter: GossipAdapter
    private val messages = mutableListOf<GossipModel>()

    private lateinit var currentUserId: String
    private lateinit var currentUserName: String

    private lateinit var otherUserId: String
    private lateinit var otherUserName: String

    private lateinit var chatId: String
    private lateinit var messagesRef: DatabaseReference
    private lateinit var summariesRef: DatabaseReference

    private val PICK_IMAGE = 101

    // ====== Cloudinary (Unsigned) ======
    // Create an unsigned upload preset in Cloudinary dashboard and put it here.
    private val CLOUD_NAME = "YOUR_CLOUD_NAME"
    private val UPLOAD_PRESET = "YOUR_UNSIGNED_PRESET"
    // ===================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Views
        inputMsg = findViewById(R.id.inputMsg)
        btnSend = findViewById(R.id.btnSend)
        btnImage = findViewById(R.id.btnImage)
        recyclerView = findViewById(R.id.recyclerView)
        toolbar = findViewById(R.id.toolbar)
        titleName = findViewById(R.id.usernameDisplay)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Current user (real IDs/names)
        currentUserId = UserSession.id(this)
        currentUserName = UserSession.name(this)

        // Matched user (passed from list)
        otherUserId = intent.getStringExtra("otherUserId") ?: ""
        otherUserName = intent.getStringExtra("otherUserName") ?: ""
        if (otherUserId.isEmpty() || otherUserName.isEmpty()) {
            snack("otherUser missing"); finish(); return
        }

        titleName.text = otherUserName

        chatId = ChatUtils.chatIdOf(currentUserId, otherUserId)
        val root = FirebaseDatabase.getInstance().reference
        messagesRef = root.child("chats").child(chatId).child("messages")
        summariesRef = root.child("chat_summaries")

        adapter = GossipAdapter(messages, currentUserName) // your existing adapter
        recyclerView.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        recyclerView.adapter = adapter

        btnSend.setOnClickListener { sendText() }
        btnImage.setOnClickListener { pickImage() }

        listenMessages()
    }

    private fun sendText() {
        val text = inputMsg.text.toString().trim()
        if (text.isEmpty()) return
        val msg = GossipModel(
            senderId = currentUserId,
            senderName = currentUserName,
            message = text
        )
        messagesRef.push().setValue(msg)
            .addOnSuccessListener {
                inputMsg.setText("")
                updateSummaries(msg)
            }
            .addOnFailureListener { snack("Send failed: ${it.message}") }
    }

    private fun listenMessages() {
        messagesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                messages.clear()
                for (s in snapshot.children) {
                    val m = s.getValue(GossipModel::class.java)
                    if (m != null) messages.add(m)
                }
                messages.sortBy { it.timestamp }
                adapter.notifyDataSetChanged()
                if (messages.isNotEmpty()) recyclerView.smoothScrollToPosition(messages.size - 1)
            }
            override fun onCancelled(error: DatabaseError) {
                snack("Load failed: ${error.message}")
            }
        })
    }

    // ====== Image upload (Cloudinary unsigned) ======
    private fun pickImage() {
        val it = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(it, PICK_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            try {
                val uri: Uri = data.data!!
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                snack("Uploading image…")
                uploadToCloudinary(bitmap)
            } catch (e: Exception) {
                snack("Image error: ${e.message}")
            }
        }
    }

    private fun uploadToCloudinary(bitmap: Bitmap) {
        val bytes = ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.toByteArray()
        }

        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                "image.jpg",
                RequestBody.create("image/*".toMediaTypeOrNull(), bytes)
            )
            .addFormDataPart("upload_preset", UPLOAD_PRESET)
            .build()

        val url = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload"
        val req = Request.Builder().url(url).post(body).build()

        OkHttpClient().newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { snack("Upload failed: ${e.message}") }
            }
            override fun onResponse(call: Call, response: Response) {
                val respStr = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    runOnUiThread { snack("Cloudinary ${response.code}") }
                    return
                }
                try {
                    val json = JSONObject(respStr)
                    val secureUrl = json.getString("secure_url")
                    val msg = GossipModel(
                        senderId = currentUserId,
                        senderName = currentUserName,
                        imageUrl = secureUrl
                    )
                    messagesRef.push().setValue(msg)
                        .addOnSuccessListener {
                            runOnUiThread {
                                updateSummaries(msg)
                                snack("Image sent")
                            }
                        }
                        .addOnFailureListener { e ->
                            runOnUiThread { snack("Share failed: ${e.message}") }
                        }
                } catch (e: Exception) {
                    runOnUiThread { snack("Parse error: ${e.message}") }
                }
            }
        })
    }
    // ====== end image upload ======

    private fun updateSummaries(lastMsg: GossipModel) {
        val preview = lastMsg.message ?: "[image]"
        val ts = lastMsg.timestamp

        val me = com.example.classcrush.data.model.ChatSummary(
            chatId = chatId,
            otherUserId = otherUserId,
            otherUserName = otherUserName,
            lastMessage = preview,
            lastTimestamp = ts
        )
        val them = com.example.classcrush.data.model.ChatSummary(
            chatId = chatId,
            otherUserId = currentUserId,
            otherUserName = currentUserName,
            lastMessage = preview,
            lastTimestamp = ts
        )

        val updates = hashMapOf<String, Any>(
            "/${currentUserId}/$chatId" to me,
            "/${otherUserId}/$chatId" to them
        )
        summariesRef.updateChildren(updates)
    }

    private fun snack(s: String) =
        Snackbar.make(findViewById(android.R.id.content), s, Snackbar.LENGTH_SHORT).show()
}

// Simple adapter for GossipModel messages
class GossipAdapter(
    private val messages: List<GossipModel>,
    private val currentUserName: String
) : RecyclerView.Adapter<GossipAdapter.ViewHolder>() {

    class ViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
        val messageText: TextView = view.findViewById(R.id.messageText)
        val senderName: TextView = view.findViewById(R.id.senderName)
        val timestamp: TextView = view.findViewById(R.id.timestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // You'll need to create a layout for message items
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = messages[position]
        holder.messageText.text = message.message ?: "[image]"
        holder.senderName.text = message.senderName
        holder.timestamp.text = android.text.format.DateFormat.format("hh:mm a", message.timestamp)
        
        // You can add logic here to show different layouts for sent vs received messages
    }

    override fun getItemCount() = messages.size
}
