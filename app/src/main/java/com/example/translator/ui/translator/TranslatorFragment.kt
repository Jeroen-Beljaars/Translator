package com.example.translator.ui.translator


import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.translator.R
import com.example.translator.model.Translation
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_translator.*

/**
 * A simple [Fragment] subclass.
 */
class TranslatorFragment : Fragment() {
    private lateinit var viewModel: TranslatorViewModel

    private var translationHistory = arrayListOf<Translation>()
    private lateinit var translationAdapter: TranslationAdapter
    private var textWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            val textToTranslate = s.toString()
            if (textToTranslate == "") {
                etTextToTranslate.hint = getString(R.string.press_to_enter)
                viewModel.selectedTranslation.apply {
                    value = null
                }
            }
            viewModel.textToTranslate.apply {
                value = textToTranslate
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        this.initViewModel()

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_translator, container, false)

    }

    private fun toggleEditMode(translation: Translation?){
        val a = activity as AppCompatActivity

        if (translation!= null) {
            a.supportActionBar?.setTitle(R.string.edit_translation)
            toggleTextWatcher(true)
            etTextToTranslate.setText(translation.originalText)
            toggleTextWatcher(false)
            btnSaveChanges.visibility = View.VISIBLE
        } else {
            a.supportActionBar?.setTitle(R.string.app_name)
            btnSaveChanges.visibility = View.GONE
        }

    }

    fun saveTranslation(){
        this.toggleTextWatcher(true)
        this@TranslatorFragment.viewModel.createOrUpdateTranslation()
        etTextToTranslate.hint = etTextToTranslate.text
        etTextToTranslate.text.clear()
        this.toggleTextWatcher()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        this.translationAdapter = TranslationAdapter(
            translationHistory,
            { translation ->
                scrollView.smoothScrollTo(0, 0)
                viewModel.selectTranslation(translation)
            },
            { translation -> viewModel.favoriteTranslation(translation) }
        )

        btnSaveChanges.setOnClickListener {
            this.saveTranslation()
        }

        rvTranslationHistory.layoutManager = LinearLayoutManager(activity as AppCompatActivity, RecyclerView.VERTICAL, false)
        rvTranslationHistory.adapter = translationAdapter
        createItemTouchHelper().attachToRecyclerView(rvTranslationHistory)
        rvTranslationHistory.addItemDecoration(
            DividerItemDecoration(
                activity as AppCompatActivity,
                DividerItemDecoration.VERTICAL
            )
        )

        ivSwitchLanguages.setOnClickListener {
            this.viewModel.switchLanguages()
        }

        if (viewModel.textToTranslate.value != "") {
            etTextToTranslate.setText(viewModel.textToTranslate.value)
        }

        etTextToTranslate.imeOptions = EditorInfo.IME_ACTION_DONE
        etTextToTranslate.setRawInputType(InputType.TYPE_CLASS_TEXT)

        this.toggleTextWatcher()

        etTextToTranslate.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_DONE) {
                saveTranslation()
            }
            false
        }


        btnFromLanguage.setOnClickListener {
            val action = TranslatorFragmentDirections
                .actionTranslatorFragmentToLanguageSelectActivity(
                    viewModel.fromLanguage.value!!,
                    true
                )
            findNavController().navigate(action)
        }

        btnToLanguage.setOnClickListener {
            val action = TranslatorFragmentDirections
                .actionTranslatorFragmentToLanguageSelectActivity(
                    viewModel.toLanguage.value!!,
                    false
                )
            findNavController().navigate(action)
        }
    }

    fun initViewModel() {
        viewModel = ViewModelProviders.of(activity as AppCompatActivity)
            .get(TranslatorViewModel::class.java)

        viewModel.fromLanguage.observe(this, Observer { fromLanguage ->
            if (fromLanguage != null) {
                btnFromLanguage.text = fromLanguage.name
            }
        })

        viewModel.toLanguage.observe(this, Observer { toLanguage ->
            if (toLanguage != null) {
                btnToLanguage.text = toLanguage.name
            }
        })

        viewModel.translatedText.observe(this, Observer { translateResponse ->
            if (translateResponse.error == null && translateResponse.result == "") {
                toggleTranslationResultVisibility(false)
            } else if (translateResponse.error != null) {
                Toast.makeText(
                    this.context,
                    translateResponse.error?.localizedMessage.toString(),
                    Toast.LENGTH_LONG
                ).show()
            } else {
                tvTranslatedText.text = translateResponse.result
                tvToLanguage.text = viewModel.toLanguage.value?.name
                toggleTranslationResultVisibility(true)
            }
        })

        viewModel.translationHistory.observe(this, Observer { history ->
            if (history != null) {
                this.translationHistory.clear()
                this.translationHistory.addAll(history)
                this.translationAdapter.notifyDataSetChanged()
            }
        })

        viewModel.selectedTranslation.observe(this, Observer { selectedTranslation ->
            toggleEditMode(selectedTranslation)
        })


    }

    private fun toggleTranslationResultVisibility(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        translationTopBar.visibility = visibility
        tvTranslatedText.visibility = visibility
    }

    fun toggleTextWatcher(remove: Boolean = false) {
        if (remove){
            etTextToTranslate.removeTextChangedListener(this.textWatcher)
        } else {
            etTextToTranslate.addTextChangedListener(this.textWatcher)
        }
    }

    /**
     * Create a touch helper to recognize when a user swipes an item from a recycler view.
     * An ItemTouchHelper enables touch behavior (like swipe and move) on each ViewHolder,
     * and uses callbacks to signal when a user is performing these actions.
     */
    private fun createItemTouchHelper(): ItemTouchHelper {

        // Callback which is used to create the ItemTouch helper. Only enables left swipe.
        // Use ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) to also enable right swipe.
        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

            // Enables or Disables the ability to move items up and down.
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            // Callback triggered when a user swiped an item.
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if (direction == ItemTouchHelper.LEFT) {
                    val position = viewHolder.adapterPosition
                    val translationToDelete = translationHistory[position]

                    viewModel.deleteTranslation(translationToDelete)

                    val mySnackbar = Snackbar.make(
                        activity!!.findViewById(android.R.id.content),
                        R.string.removed_from_history,
                        Snackbar.LENGTH_LONG
                    )

                    mySnackbar.setAction(R.string.undo) {
                        viewModel.createOrUpdateTranslation(translationToDelete)
                    }

                    mySnackbar.show()
                } else if (direction == ItemTouchHelper.RIGHT) {
                    val position = viewHolder.adapterPosition
                    val translationToFavorite = translationHistory[position]

                    viewModel.favoriteTranslation(translationToFavorite)

                    val mySnackbar = Snackbar.make(
                        activity!!.findViewById(android.R.id.content),
                        if(translationToFavorite.isFavorite) R.string.removed_from_favorites else R.string.added_to_favorite,
                        Snackbar.LENGTH_LONG
                    )

                    mySnackbar.setAction(R.string.undo) {
                        viewModel.favoriteTranslation(translationToFavorite)
                    }

                    mySnackbar.show()

                }
            }
        }
        return ItemTouchHelper(callback)
    }
}
