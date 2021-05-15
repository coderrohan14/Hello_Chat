package com.rohan.chatapp.Authentication

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.rohan.chatapp.R
import com.rohan.chatapp.messages.LatestMessagesActivity
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        supportActionBar?.hide()
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        btnLogin.setOnClickListener {
            vibratePhone()
            pbLogin.visibility = View.VISIBLE
            val emailLogin = findViewById<EditText>(R.id.etEmailLogin).text.toString()
            val passwordLogin = findViewById<EditText>(R.id.etPasswordLogin).text.toString()
            if(emailLogin.isEmpty()||passwordLogin.isEmpty()){
                Toast.makeText(this,"Please fill all the fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            FirebaseAuth.getInstance().signInWithEmailAndPassword(emailLogin,passwordLogin)
                .addOnCompleteListener {
                    if(!it.isSuccessful) return@addOnCompleteListener
                    Toast.makeText(this,"Logged In Successfully!",Toast.LENGTH_SHORT).show()
                    Intent(this, LatestMessagesActivity::class.java).also {
                        it.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(it)
                        pbLogin.visibility = View.GONE
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this,it.message.toString(),Toast.LENGTH_SHORT).show()
                    pbLogin.visibility = View.GONE
                }
        }
        val tvNewUser = findViewById<TextView>(R.id.tvNewUser)
        tvNewUser.setOnClickListener {
            Intent(this, RegisterActivity::class.java).also {
                it.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(it)
            }
            pbLogin.visibility = View.GONE
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
}