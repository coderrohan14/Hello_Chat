package com.rohan.chatapp.messages

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.os.*
import android.telecom.Call
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.getColor
import androidx.core.view.isEmpty
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.google.android.gms.tasks.Tasks.await
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.collection.LLRBNode
import com.rohan.chatapp.Authentication.LoginActivity
import com.rohan.chatapp.NewMessageActivity
import com.rohan.chatapp.NewMessageActivity.Companion.USER_KEY
import com.rohan.chatapp.R
import com.rohan.chatapp.models.ChatMessage
import com.rohan.chatapp.models.User
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_chat_log.*
import kotlinx.android.synthetic.main.activity_latest_messages.*
import kotlinx.android.synthetic.main.latest_messages_row.view.*
import kotlinx.coroutines.*
import kotlinx.coroutines.NonCancellable.start
import java.lang.Exception

class LatestMessagesActivity : AppCompatActivity() {
    companion object{
        var currentUser: User? = null
    }
    lateinit var rvLatestMsg:RecyclerView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_latest_messages)
        supportActionBar?.title = "Latest Messages"
        rvLatestMsg = findViewById(R.id.rvLatestMsg)
        rvLatestMsg.adapter = adapter
        adapter.setOnItemClickListener { item, view ->
            Intent(this, ChatLogActivity::class.java).also {
                val row = item as LatestMessageRow
                it.putExtra(USER_KEY, row.chatPartnerUser)
                startActivity(it)
            }
        }

        CoroutineScope(Dispatchers.Main).launch {
            listenForLatestMessages()
            fetchCurrentUser()
        }
        Handler().postDelayed({
            if(adapter.itemCount==0) {
                pgLatest.visibility = View.INVISIBLE
                tvNoMsg.visibility = View.VISIBLE
            }
        },3900)
    }

    val latestMessagesMap = HashMap<String,ChatMessage>()

    private fun refreshRecyclerView(){
        adapter.clear()
        latestMessagesMap.values.forEach{
            adapter.add(LatestMessageRow(it))
        }
        pgLatest.visibility = View.INVISIBLE
        tvNoMsg.visibility = View.INVISIBLE
        rvLatestMsg.visibility = View.VISIBLE
    }

    val dbUrl = "https://chatapp-43ce5-default-rtdb.asia-southeast1.firebasedatabase.app/"

    private fun listenForLatestMessages(){
        val fromId = FirebaseAuth.getInstance().uid
        val ref = FirebaseDatabase.getInstance(dbUrl).getReference("/latest-messages/$fromId")
        ref.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                pgLatest.visibility = View.VISIBLE
                tvNoMsg.visibility = View.INVISIBLE
                Log.d("testing1",snapshot.toString())
                val chatMessage = snapshot.getValue(ChatMessage::class.java)?:return
                latestMessagesMap[snapshot.key!!] = chatMessage
                refreshRecyclerView()
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                pgLatest.visibility = View.VISIBLE
                tvNoMsg.visibility = View.INVISIBLE
                Log.d("testing1",snapshot.toString())
                val chatMessage =
                    snapshot.getValue(ChatMessage::class.java) ?: return
                latestMessagesMap[snapshot.key!!] = chatMessage
                refreshRecyclerView()
            }
            override fun onChildRemoved(snapshot: DataSnapshot) {
                latestMessagesMap.remove(snapshot.key!!)
                refreshRecyclerView()
            }
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

            }
            override fun onCancelled(error: DatabaseError) {

            }
        })
    }
    val adapter = GroupAdapter<ViewHolder>()
    private fun fetchCurrentUser(){
        val uid = FirebaseAuth.getInstance().uid
        val ref = FirebaseDatabase.getInstance(dbUrl).getReference("/users/$uid")
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                currentUser = snapshot.getValue(User::class.java)
            }
            override fun onCancelled(error: DatabaseError) {

            }
        })
    }
    class LatestMessageRow(val chatmessage: ChatMessage): Item<ViewHolder>(){
        val dbUrl = "https://chatapp-43ce5-default-rtdb.asia-southeast1.firebasedatabase.app/"
        var chatPartnerUser:User?=null
        lateinit var rocketAnimation: AnimationDrawable
        override fun bind(viewHolder: ViewHolder, position: Int) {
            viewHolder.itemView.tvLatestMsg.text = chatmessage.text
            val chatPartenerId:String
            if(chatmessage.fromId==FirebaseAuth.getInstance().uid){
                chatPartenerId = chatmessage.toId
            }else{
                chatPartenerId = chatmessage.fromId
            }
            val ref = FirebaseDatabase.getInstance(dbUrl).getReference("/users/$chatPartenerId")
            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    chatPartnerUser = snapshot.getValue(User::class.java)
                    viewHolder.itemView.tvLatestUsername.text = chatPartnerUser?.username
                    viewHolder.itemView.imgLatestMessage.apply {
                        setBackgroundResource(R.drawable.progress_animation)
                        rocketAnimation = background as AnimationDrawable
                    }
                    rocketAnimation.start()
                    Picasso.get().load(chatPartnerUser?.profileImageUrl).fit().centerCrop().into(viewHolder.itemView.imgLatestMessage)
                }
                override fun onCancelled(error: DatabaseError) {

                }
            })
        }
        override fun getLayout(): Int {
            return R.layout.latest_messages_row
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.menu_new_message -> {
                Intent(this, NewMessageActivity::class.java).also {
                    startActivity(it)
                }
            }
            R.id.menu_sign_out -> {
                val alert = AlertDialog.Builder(this@LatestMessagesActivity, R.style.MyDialogTheme)
                alert.setTitle("Log Out?")
                alert.setMessage("Are you sure you want to Log Out?")
                alert.setPositiveButton("Yes") { text, listener ->
                    FirebaseAuth.getInstance().signOut()
                    Intent(this, LoginActivity::class.java).also {
                        it.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(it)
                    }
                }
                alert.setNegativeButton("No") { text, listener ->
                    Intent(this, LatestMessagesActivity::class.java).also {
                        it.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(it)
                    }
                }
                alert.setCancelable(false)
                alert.create()
                alert.show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.nav_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }
}