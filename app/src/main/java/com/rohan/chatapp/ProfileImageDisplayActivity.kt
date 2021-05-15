package com.rohan.chatapp

import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.core.content.res.ResourcesCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.rohan.chatapp.messages.LatestMessagesActivity
import com.rohan.chatapp.models.User
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_profile_image_display.*
import kotlinx.android.synthetic.main.user_row_new_message.view.*

class ProfileImageDisplayActivity : AppCompatActivity() {
    val dbUrl = "https://chatapp-43ce5-default-rtdb.asia-southeast1.firebasedatabase.app/"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_image_display)
        val myFont: Typeface? = ResourcesCompat.getFont(this.applicationContext, R.font.myfont)
        tvNameDisplay.typeface = myFont
        supportActionBar?.title = "User Profile"
        val image = intent.getStringExtra("Image")
        val uid = intent.getStringExtra("uid")
        Picasso.get().load(image).fit().centerCrop().into(imgDisplay)
        val ref = FirebaseDatabase.getInstance(dbUrl).getReference("/users/$uid")
        var name:String? = null
        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = snapshot.getValue(User::class.java)
                Log.d("testing4",user?.uid.toString())
                name = user?.username.toString()
                tvNameDisplay.text = name
            }
            override fun onCancelled(error: DatabaseError) {

            }
        })
        Handler().postDelayed({
            pbDisplay.visibility = View.GONE
            tvNameDisplay.visibility = View.VISIBLE
            imgDisplay.visibility = View.VISIBLE
        },2200)
        Log.d("testing4",name.toString())
    }
}