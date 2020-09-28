package com.example.simplechatapp

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide

class MessageAdapter(context: Context, resource: Int, objects: List<ChatMessage>) :
    ArrayAdapter<ChatMessage>(context, resource, objects) {


    @SuppressLint("InflateParams")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.item_message, null)
        }
        val photoImageView = view!!.findViewById<ImageView>(R.id.photoImageView)
        val messageTextView = view.findViewById<TextView>(R.id.messageTextView)
        val authorTextView = view.findViewById<TextView>(R.id.nameTextView)
        val dateTextView = view.findViewById<TextView>(R.id.dateTextView)
        val message = getItem(position)

        val isPhoto = message!!.photoUrl != null
        if (isPhoto) {
            messageTextView.visibility = View.GONE
            photoImageView.visibility = View.VISIBLE
            Glide.with(photoImageView.context).load(message.photoUrl).into(photoImageView)
        } else {
            messageTextView.visibility = View.VISIBLE
            photoImageView.visibility = View.GONE
            messageTextView.text = message.text
        }

        authorTextView.text = message.name
        dateTextView.text = message.date

        return view
    }
}
