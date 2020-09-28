package com.example.simplechatapp

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_main.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class MainActivity : AppCompatActivity() {
    companion object {
        const val MESSAGES_LENGTH = 200
        const val ANONYMOUS = "Anonymous"
        const val RC_SIGN_IN = 101
        const val RC_PHOTO_PICKER = 102
        const val TAG = "Inspection"
        const val CHAT_MSG_LENGTH_KEY = "chat_length"

    }

    lateinit var userName: String
    lateinit var messageAdapter: MessageAdapter
    lateinit var firebasAuth: FirebaseAuth
    lateinit var authStateListener: FirebaseAuth.AuthStateListener
    lateinit var firebaseStorage: FirebaseStorage
    lateinit var storageReference: StorageReference
    lateinit var firebaseDatabase: FirebaseDatabase
    lateinit var firebaseRemoteConfig: FirebaseRemoteConfig
    private lateinit var messagesDatabaseReference: DatabaseReference
    private var childEventListener: ChildEventListener? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        userName = ANONYMOUS


        //Initialize Firebase components
        firebaseDatabase = FirebaseDatabase.getInstance()
        firebasAuth = FirebaseAuth.getInstance()
        firebaseStorage = FirebaseStorage.getInstance()
        firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

        messagesDatabaseReference = firebaseDatabase.reference.child("messages")
        storageReference = firebaseStorage.reference.child("chat_photos")


        messageEditText.filters = arrayOf(InputFilter.LengthFilter(MESSAGES_LENGTH))


        val chatMessages = ArrayList<ChatMessage>()
        messageAdapter = MessageAdapter(this, R.layout.item_message, chatMessages)
        messageListView.adapter = messageAdapter

        photoPickerButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/jpeg"
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            startActivityForResult(
                Intent.createChooser(intent, "Complete action using"),
                RC_PHOTO_PICKER
            )
        }
        //Send button sends message and clears editText
        sendButton.setOnClickListener {

            val date = getDateString()

            val chatMessage = ChatMessage(messageEditText.text.toString(), userName, null, date)
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

        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(0).build()
        firebaseRemoteConfig.setConfigSettingsAsync(configSettings)

        val defaultConfigMap = HashMap<String, Any>()
        defaultConfigMap[CHAT_MSG_LENGTH_KEY] = MESSAGES_LENGTH
        firebaseRemoteConfig.setDefaultsAsync(defaultConfigMap)
        fetchConfig()

    }

    private fun fetchConfig() {
        var cacheExpiration: Long = 3600
        if (firebaseRemoteConfig.info.configSettings.minimumFetchIntervalInSeconds == 0L) {
            cacheExpiration = 0
        }
        firebaseRemoteConfig.fetch(cacheExpiration).addOnSuccessListener {
            firebaseRemoteConfig.activate(); applyRetrievedLengthLimit()
        }.addOnFailureListener {
            Log.d(TAG, "Remote Config fetch failed")
            applyRetrievedLengthLimit()
        }

    }

    private fun applyRetrievedLengthLimit() {
        val length = firebaseRemoteConfig.getLong(CHAT_MSG_LENGTH_KEY)
        messageEditText.filters = arrayOf(InputFilter.LengthFilter(length.toInt()))

    }

    private fun onSignedOutCleanUp() {

        userName = ANONYMOUS
        messageAdapter.clear()
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

                    if (snapshot.exists()) {
                        val chatMessage = snapshot.getValue(ChatMessage::class.java)

                        messageAdapter.add(chatMessage)
                    }
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
        messageAdapter.clear()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            when (resultCode) {
                RESULT_OK -> {
                    Toast.makeText(this, "You are signed in!", Toast.LENGTH_SHORT).show()

                }
                RESULT_CANCELED -> {
                    //                Toast.makeText(this, "Sign in canceled", Toast.LENGTH_LONG).show()
                    finish()

                }
            }

        } else if (resultCode == RESULT_OK && requestCode == RC_PHOTO_PICKER) {

            val selectedImageUri = data!!.data
            val photoRef = storageReference.child(selectedImageUri!!.lastPathSegment!!)
            photoRef.putFile(selectedImageUri).addOnSuccessListener {
                photoRef.downloadUrl.addOnSuccessListener {
                    val url = it.toString()
                    val date = getDateString()
                    val chatMessage = ChatMessage(null, userName, url, date)
                    messagesDatabaseReference.push().setValue(chatMessage)

                }
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getDateString(): String {
        val current = LocalDateTime.now()

        val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
        return current.format(formatter)

    }
}
