package com.rohan.chatapp

import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.rohan.chatapp.messages.ChatLogActivity
import com.rohan.chatapp.messages.LatestMessagesActivity
import com.rohan.chatapp.models.User
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_new_message.*
import kotlinx.android.synthetic.main.chat_to_row.view.*
import kotlinx.android.synthetic.main.user_row_new_message.view.*
import kotlinx.coroutines.*
import java.lang.Exception

class NewMessageActivity : AppCompatActivity() {
    lateinit var rv_new_message:RecyclerView
    val dbUrl = "https://chatapp-43ce5-default-rtdb.asia-southeast1.firebasedatabase.app/"
    var vh:View?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_message)
        supportActionBar?.title = "Select User"
        rv_new_message = findViewById(R.id.rv_new_message)
        pbNewMsg.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch{
            fetchUsers()
        }
        adapter.setOnItemLongClickListener { item, view ->
            vh = rv_new_message.layoutManager?.findViewByPosition(adapter.getAdapterPosition(item))
            showPopup(view)
            true
        }
    }
    private fun showPopup(view:View) {
        val popup = PopupMenu(this, view)
        popup.inflate(R.menu.profile_menu)
        popup.setOnMenuItemClickListener { item: MenuItem? ->
            when (item!!.itemId) {
                R.id.viewProfile -> {

                    Intent(this,ProfileImageDisplayActivity::class.java).also {
                        it.putExtra("Image",vh?.new_image?.text.toString())
                        it.putExtra("uid",vh?.newUid?.text.toString())
                        startActivity(it)
                    }
                }
            }
            true
        }
        popup.show()
    }
    companion object{
        val USER_KEY = "USER_KEY"
    }
    val adapter = GroupAdapter<ViewHolder>()
    private fun fetchUsers(){
    Log.d("test","HERE!!!!")
        val ref = FirebaseDatabase.getInstance(dbUrl).getReference("/users")
        ref.addListenerForSingleValueEvent(object:ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("test","HERE2!!!!")
               snapshot.children.forEach{
                   val user = it.getValue(User::class.java)
                   if(user!=null&&user.uid!=FirebaseAuth.getInstance().uid){
                       adapter.add(UserItem(user))
                   }
               }
                adapter.setOnItemClickListener{ item,view->
                    val userItem = item as UserItem
                    val intent = Intent(view.context,ChatLogActivity::class.java)
                    intent.putExtra(USER_KEY,userItem.user)
                   startActivity(intent)
                    finish()
                }
                rv_new_message.adapter = adapter
            }
            override fun onCancelled(error: DatabaseError) {

            }
        })
        pbNewMsg.visibility = View.GONE
        rv_new_message.visibility = View.VISIBLE
    }
}
class UserItem(val user:User): Item<ViewHolder>(){
    lateinit var rocketAnimation: AnimationDrawable
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.newUid.text = user.uid.toString()
        viewHolder.itemView.new_image.text = user.profileImageUrl.toString()
        viewHolder.itemView.tv_username_row.text = user.username
        viewHolder.itemView.img_user_row.apply {
            setBackgroundResource(R.drawable.progress_animation)
            rocketAnimation = background as AnimationDrawable
        }
        rocketAnimation.start()
        Picasso.get().load(user.profileImageUrl).fit().centerCrop().into(viewHolder.itemView.img_user_row)
    }
    override fun getLayout(): Int {
        return R.layout.user_row_new_message
    }

}