package com.example.translator.ui.translator

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.translator.R
import com.example.translator.model.Translation
import kotlinx.android.synthetic.main.item_language.view.*
import kotlinx.android.synthetic.main.item_translation.view.*

class TranslationAdapter(
    private val translations: List<Translation>,
    private val onTranslationClick: (Translation) -> Unit,
    private val onFavoriteClick: (Translation) -> Unit
) :
    RecyclerView.Adapter<TranslationAdapter.ViewHolder>() {

    /**
     * Creates and returns a ViewHolder object, inflating the item_translation layout
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_translation, parent, false)
        )
    }

    /**
     * Returns the size of the list
     */
    override fun getItemCount(): Int {
        return translations.size
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(translations[position])
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.translationHolder.setOnClickListener {
                onTranslationClick(translations[adapterPosition])
            }

            itemView.ivFavorite.setOnClickListener {
                onFavoriteClick(translations[adapterPosition])
            }
        }

        fun bind(translation: Translation) {
            // set the picture
            itemView.tvOriginalText.text = translation.originalText
            itemView.tvTranslatedText.text = translation.translatedText
            itemView.ivFavorite.setImageResource(
                if (translation.isFavorite) R.drawable.ic_star_filled_24dp
                else R.drawable.ic_star_border_24dp
            )

        }
    }
}