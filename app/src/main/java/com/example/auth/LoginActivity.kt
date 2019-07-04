package com.example.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_main.*

class LoginActivity: AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        login_button_login.setOnClickListener {
            performSignIn()

        }
        back_to_register_textview.setOnClickListener {
            finish()
        }

    }
    private fun performSignIn(){
        val email = email_edittext_login.text.toString()
        val password = password_edittext_login.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this,"Please enter text in email/pw", Toast.LENGTH_SHORT).show()
            return
        }

        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnCompleteListener{
                if (!it.isSuccessful) return@addOnCompleteListener
                Log.d("Login", "User singed in with email/pw: $email/****")
                val user = FirebaseAuth.getInstance().currentUser
                val name = user!!.displayName
                val intent = Intent(this, UserAccount::class.java)
                intent.putExtra( "email", email)
                intent.putExtra( "name", name)
                Log.d("Main","starting UserAccount")
                startActivity(intent)
            }
            .addOnFailureListener {
                Toast.makeText(this,"Failed to sign in: ${it.message}", Toast.LENGTH_SHORT).show()
                Log.d("Login", "Failed to sign in: ${it.message}")
            }
    }
}