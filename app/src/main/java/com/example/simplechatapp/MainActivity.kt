package com.example.simplechatapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    companion object {
        const val MESSAGES_LENGTH = 1000
        const val ANONYMOUS = "Anonymous"
        const val RC_SIGN_IN = 101
    }

    lateinit var userName: String
    lateinit var messageAdapter: MessageAdapter
    lateinit var firebasAuth: FirebaseAuth
    lateinit var authStateListener: FirebaseAuth.AuthStateListener

    lateinit var firebaseDatabase: FirebaseDatabase
    lateinit var messagesDatabaseReference: DatabaseReference
    var childEventListener: ChildEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        userName = ANONYMOUS





        firebaseDatabase = FirebaseDatabase.getInstance()
        firebasAuth = FirebaseAuth.getInstance()

        messagesDatabaseReference = firebaseDatabase.reference.child("messages")



        messageEditText.filters = arrayOf(InputFilter.LengthFilter(MESSAGES_LENGTH))


//        val chatMessages=ArrayList<ChatMessage>()
//        messageAdapter= MessageAdapter(this,R.layout.item_message,chatMessages)
//        messageListView.adapter=messageAdapter

        photoPickerButton.setOnClickListener {


        }
        //Send button sends message and clears editText
        sendButton.setOnClickListener {

            val chatMessage = ChatMessage(messageEditText.text.toString(), userName, null)
            messagesDatabaseReference.push().setValue(chatMessage)
            messageEditText.setText("")
        }
        messageEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                sendButton.isEnabled = p0.toString().trim().isNotEmpty()
            }

            override fun afterTextChanged(p0: Editable?) {

            }

        })
        authStateListener = FirebaseAuth.AuthStateListener {
            val user = it.currentUser
            if (user != null) {

                //Signed in
                onSignedInInitialize(user.displayName)

            } else {
                //Signed out
                onSignedOutCleanUp()

                val providers = arrayListOf(
                    AuthUI.IdpConfig.EmailBuilder().build(),
                    AuthUI.IdpConfig.PhoneBuilder().build(),
                    AuthUI.IdpConfig.GoogleBuilder().build()
                )


// Create and launch sign-in intent
                startActivityForResult(
                    AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setIsSmartLockEnabled(false)
                        .setAvailableProviders(providers)
                        .build(),
                    RC_SIGN_IN
                )
            }
        }
    }

    private fun onSignedOutCleanUp() {

        userName = ANONYMOUS
//        messageAdapter.clear()
        detachChildEventListener()

    }

    private fun detachChildEventListener() {
        if (childEventListener != null)
            messagesDatabaseReference.removeEventListener(childEventListener!!)
        childEventListener = null

    }

    private fun onSignedInInitialize(displayName: String?) {
        userName = displayName!!
        attachChildEventListener()


    }

    private fun attachChildEventListener() {
        if (childEventListener == null) {
            childEventListener = object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
//                    val chatMessage = snapshot.getValue(ChatMessage::class.java)
//                messageAdapter.add(chatMessage)
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {

                }

                override fun onChildRemoved(snapshot: DataSnapshot) {

                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

                }

                override fun onCancelled(error: DatabaseError) {

                }
            }
            messagesDatabaseReference.addChildEventListener(childEventListener!!)
        }
    }


    override fun onResume() {
        super.onResume()
        firebasAuth.addAuthStateListener(authStateListener)


    }

    override fun onPause() {
        super.onPause()

        firebasAuth.removeAuthStateListener(authStateListener)
        detachChildEventListener()
        // messageAdapter.clear()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "You are signed in!", Toast.LENGTH_LONG).show()

            } else if (resultCode == RESULT_CANCELED) {
//                Toast.makeText(this, "Sign in canceled", Toast.LENGTH_LONG).show()
                finish()

            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        menuInflater.inflate(R.menu.sign_out, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {

            R.id.signOut -> AuthUI.getInstance().signOut(this)


        }
        return super.onOptionsItemSelected(item)

    }
}
