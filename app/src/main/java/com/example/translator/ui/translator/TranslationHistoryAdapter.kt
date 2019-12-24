package com.example.translator.ui.translator

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*
import androidx.appcompat.app.AppCompatActivity
import com.example.translator.R
import com.example.translator.model.Language
import kotlinx.android.synthetic.main.item_language.view.*

class TranslationHistoryAdapter(private val languages: List<Language>, private val currentSelectedLanguage: Language, private val onClick: (Language) -> Unit) :
    RecyclerView.Adapter<TranslationHistoryAdapter.ViewHolder>() {

    /**
     * Creates and returns a ViewHolder object, inflating a standard layout called simple_list_item_1.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_language, parent, false)
        )
    }

    /**
     * Returns the size of the list
     */
    override fun getItemCount(): Int {
        return languages.size
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(languages[position])
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.setOnClickListener { onClick(languages[adapterPosition]) }
        }

        fun bind(language: Language) {
            // set the picture
            itemView
            itemView.tvLanguage.text = language.name

            if (language == this@TranslationHistoryAdapter.currentSelectedLanguage) {
                itemView.ivIsSelected.visibility = View.VISIBLE
            } else {
                itemView.ivIsSelected.visibility = View.INVISIBLE
            }
        }
    }
}