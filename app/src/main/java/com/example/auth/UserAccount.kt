package com.example.auth

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.account_user.*
import kotlinx.android.synthetic.main.account_user.photo_imageview_account
import kotlinx.android.synthetic.main.activity_main.*
import android.graphics.BitmapFactory
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso
import java.net.URL


class UserAccount: AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.account_user)

        val email = intent.getStringExtra("email")
        val name = intent.getStringExtra("name")
        val photoUrl = intent.getStringExtra("photoUrl")
//        val user = FirebaseAuth.getInstance().currentUser
//        val photo_url = (user!!.photoUrl).toString()
        email_textView.text = email
        name_textView.text = name
        Picasso
            .with(this) // give it the context
            .load(photoUrl) // load the image
            .into(photo_imageview_account)

//        val newurl = URL(photoUrl)
//        var mIcon: Bitmap?  = null
//        mIcon = BitmapFactory.decodeStream(newurl.openConnection().getInputStream())
//        photo_imageview_account.setImageBitmap(mIcon)
//        selectedPhotoUri = data.data
//        val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver,selectedPhotoUri)
//        photo_imageview_account.setImageBitmap(bitmap)

        sign_out_button.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}