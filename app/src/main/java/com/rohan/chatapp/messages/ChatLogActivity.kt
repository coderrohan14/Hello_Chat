package com.rohan.chatapp.messages

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.rohan.chatapp.NewMessageActivity
import com.rohan.chatapp.R
import com.rohan.chatapp.models.ChatMessage
import com.rohan.chatapp.models.User
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.OnItemLongClickListener
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_chat_log.*
import kotlinx.android.synthetic.main.chat_from_row.*
import kotlinx.android.synthetic.main.chat_from_row.view.*
import kotlinx.android.synthetic.main.chat_to_row.*
import kotlinx.android.synthetic.main.chat_to_row.view.*
import kotlinx.android.synthetic.main.latest_messages_row.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.lang.StringBuilder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class ChatLogActivity : AppCompatActivity() {
    lateinit var rvChatLog:RecyclerView
    val adapter = GroupAdapter<ViewHolder>()
    var toUser:User? = null
    val dbUrl = "https://chatapp-43ce5-default-rtdb.asia-southeast1.firebasedatabase.app/"
    lateinit var auth: FirebaseAuth
    lateinit var clipboardManager: ClipboardManager
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_log)
        auth = FirebaseAuth.getInstance()
        toUser = intent.getParcelableExtra<User>(NewMessageActivity.USER_KEY)
        supportActionBar?.title = toUser?.username
        rvChatLog = findViewById(R.id.rvChatLog)
        rvChatLog.adapter = adapter
        listenForMessages()
        btnSend.setOnClickListener {
            performSendMesssage()
        }
        adapter.setOnItemClickListener{ item, view ->
            Log.d("testing3",ftId.toString())
            Log.d("testing3",tfId.toString())
            val vh = rvChatLog.layoutManager?.findViewByPosition(adapter.getAdapterPosition(item))
            vh?.tvChatTo?.setOnClickListener {
                showPopupTo(vh.tvChatTo,vh.midTo.text.toString(),adapter.getAdapterPosition(item))
            }
            vh?.tvChatFrom?.setOnClickListener {
                showPopupFrom(vh.tvChatFrom)
            }
        }
    }

    private fun copyText(view:TextView?) {
        val text = view?.text.toString()
        if (text.isNotEmpty()) {
            clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("key", text)
            clipboardManager.setPrimaryClip(clipData)
            Toast.makeText(applicationContext, "Copied", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(applicationContext, "No text to be copied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPopupFrom(view: TextView?) {
        val popup = PopupMenu(this, view)
        popup.inflate(R.menu.pop_up_from_menu)
        popup.setOnMenuItemClickListener { item: MenuItem? ->
            when (item!!.itemId) {
                R.id.copyFrom -> {
                    copyText(view)
                }
            }
            true
        }
        popup.show()
    }

    private fun showPopupTo(view: TextView?,id:String,ind:Int) {
        val popup = PopupMenu(this, view)
        popup.inflate(R.menu.pop_up_menu)
        popup.setOnMenuItemClickListener { item: MenuItem? ->
            when (item!!.itemId) {
                R.id.delete -> {
                    deleteMsg(id,ind)
                }
                R.id.copy -> {
                    copyText(view)
                }
            }
            true
        }
        popup.show()
    }

    override fun onBackPressed() {
        Intent(this,LatestMessagesActivity::class.java).also {
            it.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(it)
        }
    }

    var ftId = HashMap<String,String>()
    var tfId = HashMap<String,String>()

    private fun listenForMessages(){
        val fromId = FirebaseAuth.getInstance().uid
        val toId = toUser?.uid
        val ref = FirebaseDatabase.getInstance(dbUrl).getReference("/user-messages/$fromId/$toId")
        ref.addChildEventListener(object:ChildEventListener{
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                Log.d("testing",snapshot.toString())
               val chatMessage = snapshot.getValue(ChatMessage::class.java)
                ftId[chatMessage?.id.toString()] = snapshot.key.toString()
                if(chatMessage!=null){
                    if(chatMessage.fromId!=fromId){
                        adapter.add(ChatFromItem(chatMessage,toUser!!))
                    }else{
                        val currentUser = LatestMessagesActivity.currentUser?:return
                        adapter.add(ChatToItem(chatMessage,currentUser))
                    }
                }
                rvChatLog.scrollToPosition(adapter.itemCount-1)
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {

            }
            override fun onChildRemoved(snapshot: DataSnapshot) {
            }
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

            }
            override fun onCancelled(error: DatabaseError) {

            }
        })
        val toRef = FirebaseDatabase.getInstance(dbUrl).getReference("/user-messages/$toId/$fromId")
        toRef.addChildEventListener(object:ChildEventListener{
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val chatMessage = snapshot.getValue(ChatMessage::class.java)
                tfId[chatMessage?.id.toString()] = snapshot.key.toString()
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {

            }
            override fun onChildRemoved(snapshot: DataSnapshot) {

            }
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

            }
            override fun onCancelled(error: DatabaseError) {

            }
        })
        pbChat.visibility = View.INVISIBLE
        rvChatLog.visibility = View.VISIBLE
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun performSendMesssage(){
        val fromId = FirebaseAuth.getInstance().uid ?: return
        val user = intent.getParcelableExtra<User>(NewMessageActivity.USER_KEY)
        val toId = user.uid
        val text = etEnterMessage.text.toString()
        if(text.isEmpty()) return
       // CoroutineScope(Dispatchers.IO).launch {
            val ref = FirebaseDatabase.getInstance(dbUrl).getReference("/user-messages/$fromId/$toId").push()
            val toRef = FirebaseDatabase.getInstance(dbUrl).getReference("/user-messages/$toId/$fromId").push()
            val currentDateTime = LocalDateTime.now()
            var date = currentDateTime.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)).toString()
            val dateStr:StringBuilder = StringBuilder(date)
            dateStr.removeRange(dateStr.indexOf(","),dateStr.length)
            date = dateStr.toString()
            val time = currentDateTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)).toString()
            val chatMessage = ChatMessage(date,time,toId,ref.key!!,text,fromId)
            ref.setValue(chatMessage)
                .addOnSuccessListener {
                    etEnterMessage.text.clear()
                    rvChatLog.scrollToPosition(adapter.itemCount-1)
                }
            toRef.setValue(chatMessage)
            val latestMessageRef = FirebaseDatabase.getInstance(dbUrl).getReference("/latest-messages/$fromId/$toId")
            latestMessageRef.setValue(chatMessage)
            val latestMessageToRef = FirebaseDatabase.getInstance(dbUrl).getReference("/latest-messages/$toId/$fromId")
            latestMessageToRef.setValue(chatMessage)
      //  }
    }

    private fun deleteMsg(id:String,ind:Int){
        val fromId = FirebaseAuth.getInstance().uid
        val toId = toUser?.uid
        val ref = FirebaseDatabase.getInstance(dbUrl).getReference("/user-messages/$fromId/$toId")
        val toRef = FirebaseDatabase.getInstance(dbUrl).getReference("/user-messages/$toId/$fromId")
        ref.child(id).removeValue()
        var idStr:String? = null
        if(tfId[id]!=id){
            idStr = tfId[id]!!
        }else{
            idStr = ftId[id]!!
        }
        tfId.remove(id)
        ftId.remove(id)
        toRef.child(idStr).removeValue()
        if(ind==adapter.itemCount-1){
                    //toId->fromId
            val ref2 = FirebaseDatabase.getInstance(dbUrl).getReference("/latest-messages/$toId/$fromId")
            val ref4 = FirebaseDatabase.getInstance(dbUrl).getReference("/latest-messages/$fromId/$toId")
            if(ind-1>=0) {
                val vh = rvChatLog.layoutManager?.findViewByPosition(ind-1)
                val fromF = vh?.FromIdF
                val toF = vh?.ToIdF
                val fromT = vh?.FromIdT
                val toT = vh?.ToIdT
                ref2.removeValue()
                ref4.removeValue()
                if(fromF==null||toF==null){
                    val ref1 = FirebaseDatabase.getInstance(dbUrl).getReference("/latest-messages/${toT?.text.toString()}/${fromT?.text.toString()}")
                    val ref5 = FirebaseDatabase.getInstance(dbUrl).getReference("/latest-messages/${fromT?.text.toString()}/${toT?.text.toString()}")
                    ref1.setValue(ChatMessage(vh?.tvDateTo?.text.toString(),vh?.tvTimeTo?.text.toString(),
                    toT?.text.toString(),vh?.midTo?.text.toString(),vh?.tvChatTo?.text.toString(),fromT?.text.toString()))
                    ref5.setValue(ChatMessage(vh?.tvDateTo?.text.toString(),vh?.tvTimeTo?.text.toString(),
                        toT?.text.toString(),vh?.midTo?.text.toString(),vh?.tvChatTo?.text.toString(),fromT?.text.toString()))
                }else{
                    val ref1 = FirebaseDatabase.getInstance(dbUrl).getReference("/latest-messages/${toF.text.toString()}/${fromF.text.toString()}")
                    val ref5 = FirebaseDatabase.getInstance(dbUrl).getReference("/latest-messages/${fromF.text.toString()}/${toF.text.toString()}")
                    ref1.setValue(ChatMessage(vh.tvDateFrom?.text.toString(),vh.tvTimeFrom?.text.toString(),
                        toF.text.toString(),vh.midFrom?.text.toString(),vh.tvChatFrom?.text.toString(),fromF.text.toString()))
                    ref5.setValue(ChatMessage(vh.tvDateFrom?.text.toString(),vh.tvTimeFrom?.text.toString(),
                        toF.text.toString(),vh.midFrom?.text.toString(),vh.tvChatFrom?.text.toString(),fromF.text.toString()))
                }
            }else{
                ref2.removeValue()
                ref4.removeValue()
            }
        }
        adapter.removeGroup(ind)
    }
}
class ChatFromItem(val chatMessage:ChatMessage,val user:User): Item<ViewHolder>(){
    lateinit var rocketAnimation: AnimationDrawable
    @RequiresApi(Build.VERSION_CODES.O)
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.tvChatFrom.text = chatMessage.text
        viewHolder.itemView.tvDateFrom!!.text = chatMessage.date
        viewHolder.itemView.tvTimeFrom.text = chatMessage.time
        viewHolder.itemView.midFrom.text = chatMessage.id.toString()
        viewHolder.itemView.FromIdF.text = chatMessage.fromId.toString()
        viewHolder.itemView.ToIdF.text = chatMessage.toId.toString()
        val uri = user.profileImageUrl
        viewHolder.itemView.imgChatFrom.apply {
            setBackgroundResource(R.drawable.progress_animation)
            rocketAnimation = background as AnimationDrawable
        }
        rocketAnimation.start()
        Picasso.get().load(uri).fit().centerCrop().into(viewHolder.itemView.imgChatFrom)
    }
    override fun getLayout(): Int {
        return R.layout.chat_from_row
    }
}
class ChatToItem(val chatMessage:ChatMessage,val user:User): Item<ViewHolder>(){
    lateinit var rocketAnimation: AnimationDrawable
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.tvChatTo.text = chatMessage.text
        viewHolder.itemView.tvDateTo.text = chatMessage.date
        viewHolder.itemView.tvTimeTo.text = chatMessage.time
        viewHolder.itemView.midTo.text = chatMessage.id.toString()
        viewHolder.itemView.FromIdT.text = chatMessage.fromId.toString()
        viewHolder.itemView.ToIdT.text = chatMessage.toId.toString()
        val uri = user.profileImageUrl
        viewHolder.itemView.imgChatTo.apply {
            setBackgroundResource(R.drawable.progress_animation)
            rocketAnimation = background as AnimationDrawable
        }
        rocketAnimation.start()
        Picasso.get().load(uri).fit().centerCrop().into(viewHolder.itemView.imgChatTo)
    }
    override fun getLayout(): Int {
        return R.layout.chat_to_row
    }
}