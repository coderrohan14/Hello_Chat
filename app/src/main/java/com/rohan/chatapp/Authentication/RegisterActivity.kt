package com.rohan.chatapp.Authentication

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.core.content.res.ResourcesCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.rohan.chatapp.R
import com.rohan.chatapp.messages.LatestMessagesActivity
import com.rohan.chatapp.models.User
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_register.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.util.*


class RegisterActivity : AppCompatActivity() {
    lateinit var auth : FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        val myFont: Typeface? = ResourcesCompat.getFont(this.applicationContext, R.font.myfont)
        tvWelcomRegister.typeface = myFont
        tvAddPhoto.typeface = myFont
        tvAlreadyRegistered.typeface = myFont
        btnRegister.typeface = myFont
        supportActionBar?.hide()
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        auth = FirebaseAuth.getInstance()
        val btnRegiter = findViewById<Button>(R.id.btnRegister)
        btnRegiter.setOnClickListener {
            vibratePhone()
            if(selectedPhotoUri==null){
                Toast.makeText(this,"Please select a profile photo.",Toast.LENGTH_LONG).show()
            }else {
                pbRegister.visibility = View.VISIBLE
                loginUser()
            }
        }
        tvAlreadyRegistered.setOnClickListener {
            Intent(this, LoginActivity::class.java).also {
                it.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(it)
            }
        }
        btnImageCircle.setOnClickListener {
            Intent(Intent.ACTION_GET_CONTENT).also {
                it.type = "image/*"
                startActivityForResult(it,0)
            }
        }
    }

    private fun vibratePhone() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(200)
        }
    }

    private fun loginUser() {
        val username = findViewById<EditText>(R.id.etUsername).text.toString()
        val email = etEmailId.text.toString()
        val password = etPassword.text.toString()
        if (username.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    auth.createUserWithEmailAndPassword(email, password).await()
                    auth.signInWithEmailAndPassword(email, password).await()
                    withContext(Dispatchers.Main){
                        uploadImageToFirebaseStorage()
                        Intent(this@RegisterActivity, LatestMessagesActivity::class.java).also {
                            it.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(it)
                        }
                        pbRegister.visibility = View.GONE
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main){
                        Toast.makeText(this@RegisterActivity, e.message.toString(), Toast.LENGTH_LONG).show()
                        pbRegister.visibility = View.INVISIBLE
                    }
                }
            }
        }else{
            Toast.makeText(this,"Please enter all the details.", Toast.LENGTH_LONG).show()
            pbRegister.visibility = View.INVISIBLE
        }
    }

    var selectedPhotoUri: Uri? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode== RESULT_OK&&requestCode==0){
            selectedPhotoUri = data?.data
            tvAddPhoto.visibility = View.GONE
            btnImageCircle.setImageURI(selectedPhotoUri)
        }
    }

    private fun uploadImageToFirebaseStorage(){
        if(auth.uid==null){
            Toast.makeText(this,"User not logged in..",Toast.LENGTH_SHORT).show()
            return
        }
        val filename = UUID.randomUUID().toString()
        val ref = FirebaseStorage.getInstance().getReference("/images/$filename")
        ref.putFile(selectedPhotoUri!!)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener {
                    Log.d("test","HERE AGAIN!!")
                    saveUserToFirebaseDataBase(it.toString())
                }
            }
            .addOnFailureListener {
                Toast.makeText(this,it.message.toString(),Toast.LENGTH_SHORT).show()
                pbRegister.visibility = View.INVISIBLE
                return@addOnFailureListener
            }
    }

    val dbUrl = "https://chatapp-43ce5-default-rtdb.asia-southeast1.firebasedatabase.app/"

    private fun saveUserToFirebaseDataBase(profileImageUrl:String){
        val uid = auth.uid?:""
        val ref = FirebaseDatabase.getInstance(dbUrl).getReference("users/$uid")
        val user = User(uid,etUsername.text.toString(),profileImageUrl)
        ref.setValue(user)
            .addOnSuccessListener {
                Toast.makeText(this,"Registered Successfully!!",Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }
            .addOnFailureListener {
                Toast.makeText(this,it.message.toString(),Toast.LENGTH_SHORT).show()
                pbRegister.visibility = View.INVISIBLE
                return@addOnFailureListener
            }
    }
}