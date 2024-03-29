package com.example.auth

import android.app.Activity
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.account_user.*
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.URL
import java.util.*

class MainActivity : AppCompatActivity() {
    val gso: GoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestIdToken("7626130504-ag9cj7fqjs385jpf4les7m68obbhcih2.apps.googleusercontent.com")
        .requestEmail()
        .build()
    val RC_SIGN_IN: Int = 1
    var selectedPhotoUri: Uri? = null
    lateinit var mGoogleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
        register_button_register.setOnClickListener {

            performRegister()
        }
        already_have_account_text_view.setOnClickListener {
            Log.d("MainActivity", "Have Account")

            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
        photo_button_register.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)
        }
        singin_with_google.setOnClickListener { view: View? ->
            signInGoogle()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null) {
            selectedPhotoUri = data.data
            val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, selectedPhotoUri)
            photo_imageview_register.setImageBitmap(bitmap)
            photo_button_register.visibility = View.GONE

        }
        if (requestCode == RC_SIGN_IN) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            Log.d("Main", "onActivityResult")
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account!!)
            } catch (e: ApiException) {
                Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun performRegister() {
        val name = username_editText_register.text.toString()
        val email = email_editText_register.text.toString()
        val password = password_editText_register.text.toString()

        if (email.isEmpty() || password.isEmpty() || selectedPhotoUri == null) {
            Toast.makeText(this, "Please enter text in email/pw an choose a photo", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("MainActivity", "email is: $email")
        Log.d("MainActivity", "password is: $password")

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener {
                if (!it.isSuccessful) return@addOnCompleteListener


                Log.d("Main", "Successfully created user with uid: ${it.result?.user?.uid}")
                val uri:Uri = saveImageToInternalStorage()
                uploadImageToFirebase()
                Log.d("Main", uri.toString())

            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to create user: ${it.message}", Toast.LENGTH_SHORT).show()
                Log.d("Main", "Failed to create user: ${it.message}")
            }
    }
    private fun saveImageToInternalStorage():Uri{
        val filename = "avatar_$username_editText_register"
        val wrapper = ContextWrapper(applicationContext)
        var file = File(wrapper.filesDir, filename)
        val bitmap = (photo_imageview_register.drawable as BitmapDrawable).bitmap
        try {
            val stream: OutputStream = FileOutputStream(file)

            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)

            stream.flush()

            stream.close()
        } catch (e: IOException){ // Catch the exception
            e.printStackTrace()
        }
        Log.d("Main", "Saved to internal storage ${Uri.parse(file.absolutePath)}")
        return Uri.parse(file.absolutePath)
    }

    private fun uploadImageToFirebase() {
        if (selectedPhotoUri == null) return
        val filename = UUID.randomUUID().toString()
        val ref = FirebaseStorage.getInstance().getReference("/images/$filename")
        ref.putFile(selectedPhotoUri!!)
            .addOnSuccessListener { it ->
                Log.d("Main", "Successfully uploaded image: ${it.metadata?.path}")
                saveUserToFirebaseDatabase(it.toString(), email_editText_register.text.toString(), username_editText_register.text.toString())

                ref.downloadUrl.addOnSuccessListener {
                    val name = username_editText_register.text.toString()
                    val email = email_editText_register.text.toString()
                    saveUserToFirebaseDatabase(it.toString(), email, name)
                    Log.d("Main", "File location: $it")
                    val intent = Intent(this, UserAccount::class.java)
                    intent.putExtra("name", name)
                    intent.putExtra("email", email)
                    intent.putExtra("photoUrl", it.toString())
                    Log.d("Main", "starting UserAccount")
                    startActivity(intent)
                }
            }
            .addOnFailureListener {
                Log.d("Main", "Failed to upload image")
            }
    }

    private fun signInGoogle() {
        Log.d("Main", "Intent to sing in with Google")

        val signIntent: Intent = mGoogleSignInClient.signInIntent
        startActivityForResult(signIntent, RC_SIGN_IN)
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        Log.d("Main", "firebaseAuthWithGoogle:" + acct.id!!)

        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        FirebaseAuth.getInstance().signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("Main", "signInWithCredential:success")
                    val user = FirebaseAuth.getInstance().currentUser
                    Log.d("Main", (user!!.photoUrl).toString())
                    saveUserToFirebaseDatabase((user!!.photoUrl).toString(), user!!.email.toString(),user!!.displayName.toString() )

                    openAcc(user)
                } else {
                    Log.w("Main", "signInWithCredential:failure", task.exception)
                    Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show()
                    openAcc(null)
                }
            }
    }


    private fun saveUserToFirebaseDatabase(profileImageUrl: String, email: String, username: String) {
        val uid = FirebaseAuth.getInstance().uid ?: ""
        val db = FirebaseFirestore.getInstance()
        val user = hashMapOf(
            "email" to email,
            "uid" to uid,
            "username" to username,
            "profileImageUrl" to profileImageUrl
        )

        db.collection("users").document(uid).set(user)
            .addOnSuccessListener {
                Log.d("Main", "User saved to Firebase database")
            }
            .addOnFailureListener {
                Log.d("Main", "Failed to save user to database ${it.message}")
            }
    }

    //class User(val uid: String,val email: String, val username: String, val profileImageUrl: String)
    private fun openAcc(user: FirebaseUser?) {
        if (user != null) {
            val name: String? = user.displayName
            val email: String? = user.email
            val photoUrl = (user!!.photoUrl).toString()

            val intent = Intent(this, UserAccount::class.java)
            intent.putExtra("name", name)
            intent.putExtra("email", email)
            intent.putExtra("photoUrl", photoUrl)
            Log.d("Main", "starting UserAccount")
            startActivity(intent)
        }


    }
}
