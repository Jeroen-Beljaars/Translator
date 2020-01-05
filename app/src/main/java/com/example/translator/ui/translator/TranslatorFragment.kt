package com.example.translator.ui.translator


import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.*
import androidx.fragment.app.Fragment
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.translator.R
import com.example.translator.model.TranslateResponse
import com.example.translator.model.Translation
import com.example.translator.ui.MainActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_translator.*
import java.util.*
import kotlin.concurrent.schedule

/**
 * In this [Fragment] the user can see his Translation History
 * and donew translations
 */
class TranslatorFragment : Fragment() {
    private lateinit var viewModel: TranslatorViewModel

    private var translationHistory = arrayListOf<Translation>()
    private lateinit var translationAdapter: TranslationAdapter

    // Setup the listener which we will use to keep track of what the user enters in the
    // Text to translate box
    private var textWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            val textToTranslate = s.toString()

            // Check if live translation has been disabled. If it has then enable it again
            // since the user is trying to perform a new translation or edit an existing one
            if (!viewModel.liveTranslation) {
                ivSave.visibility = View.VISIBLE
                viewModel.toggleLiveTranslation(true)
            }

            // If the user removes all the text from the textToTranslate box disable edit mode
            if (textToTranslate == "" && start != before) {
                this@TranslatorFragment.toggleEditMode(null)
            }

            // Update the value of textToTranslate so the ViewModel can do live translation for us
            viewModel.textToTranslate.apply {
                value = textToTranslate
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Notify the fragment that we have a custom menu
        // This makes it call onCreateOptionsMenu
        setHasOptionsMenu(true)

        // Initialize the view model
        this.initViewModel()

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_translator, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(viewModel.textToTranslate.value != "") {
            etTextToTranslate.setText(viewModel.textToTranslate.value)
        } else {
            viewModel.translatedText.apply {
                value = TranslateResponse("", null)
            }
        }

        this.translationAdapter = TranslationAdapter(
            translationHistory,
            { translation ->
                scrollView.smoothScrollTo(0, 0)
                viewModel.selectTranslation(translation)
                (activity as MainActivity).menuCross.isVisible = true
            },
            { translation -> viewModel.favoriteTranslation(translation) }
        )

        btnSaveChanges.setOnClickListener {
            this.saveTranslation()
        }

        // Initialize the translation history
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

        this.toggleTextWatcher(true)

        etTextToTranslate.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_DONE) {
                saveTranslation()
            }
            false
        }


        btnFromLanguage.setOnClickListener {
            val action = TranslatorFragmentDirections
                .actionTranslatorFragmentToLanguageSelectActivity(
                    viewModel.fromLanguage.value,
                    true
                )
            findNavController().navigate(action)
        }

        btnToLanguage.setOnClickListener {
            val action = TranslatorFragmentDirections
                .actionTranslatorFragmentToLanguageSelectActivity(
                    viewModel.toLanguage.value,
                    false
                )
            findNavController().navigate(action)
        }

        ivSave.setOnClickListener {
            this.saveTranslation()
        }
    }

    fun initViewModel() {
        viewModel = ViewModelProviders.of(activity as AppCompatActivity)
            .get(TranslatorViewModel::class.java)

        viewModel.loading.observe(this, Observer { loading ->
            if (loading) {
                Handler().postDelayed({
                    if (this@TranslatorFragment.viewModel.loading.value == true) {
                        pbLoading.visibility = View.VISIBLE
                    }
                }, 1000)
            }
            else {
                pbLoading.visibility = View.GONE
            }
        })

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

    /**
     * Enable or disable edit mode for a specific translation
     *
     * @param translation The translation which we want to edit
     *                    if it's null then edit mode will be disabled
     */
    private fun toggleEditMode(translation: Translation?){
        // First check if the edit mode has already been enabled for this translation
        // If it has then we know that the user likely tried to change the language if we
        // Wouldn't check this then the user would not be able to change the languages
        if (viewModel.editModeEnabledForTranslation != translation) {
            // Enable edit mode for the specified translation
            val a = activity as AppCompatActivity
            viewModel.editModeEnabledForTranslation = translation
            if (translation != null) {
                a.supportActionBar?.setTitle(R.string.edit_translation)
                (activity as MainActivity).menuCross.isVisible = true
                toggleTextWatcher(false)
                etTextToTranslate.setText(translation.originalText)
                toggleTextWatcher(true)
                btnSaveChanges.visibility = View.VISIBLE
            } else {
                // Disable edit mode
                a.supportActionBar?.setTitle(R.string.app_name)
                etTextToTranslate.hint = getString(R.string.press_to_enter)
                toggleTextWatcher(false)
                etTextToTranslate.text.clear()
                viewModel.textToTranslate.apply {
                    value = ""
                }
                toggleTranslationResultVisibility(false)
                toggleTextWatcher(true)
                (activity as MainActivity).menuCross.isVisible = false
                btnSaveChanges.visibility = View.GONE
            }
        } else if(translation != null) {
            // Edit mode has already been enabled and the user changed the language
            // Show the save changes button again
            btnSaveChanges.visibility = View.VISIBLE
        }
    }

    private fun saveTranslation(){
        this.toggleTextWatcher(false)

        // Disable live translation so we can keep showing the result
        // until the user translates something else. ( will be enabled again once text is being
        // entered in etTextToTranslate)
        this@TranslatorFragment.viewModel.toggleLiveTranslation(false)
        ivSave.visibility = View.GONE
        this@TranslatorFragment.viewModel.createOrUpdateTranslation()
        etTextToTranslate.hint = etTextToTranslate.text
        etTextToTranslate.text.clear()
        viewModel.textToTranslate.apply {
            value = ""
        }
        this.toggleTextWatcher(true)
    }

    private fun toggleTranslationResultVisibility(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        translationTopBar.visibility = visibility
        tvTranslatedText.visibility = visibility
    }

    fun toggleTextWatcher(enabled: Boolean = true) {
        if (enabled){
            etTextToTranslate.addTextChangedListener(this.textWatcher)
        } else {
            etTextToTranslate.removeTextChangedListener(this.textWatcher)
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

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        menu.clear()

        inflater.inflate(R.menu.menu_close, menu)
        val cross = menu.findItem(R.id.action_deselect) ?: (activity as MainActivity).menuCross
        (activity as MainActivity).menuCross = cross
        cross.isVisible = viewModel.selectedTranslation.value != null

        cross?.setOnMenuItemClickListener {
            this@TranslatorFragment.viewModel.selectedTranslation.apply { value = null }
            return@setOnMenuItemClickListener true
        }
    }
}
