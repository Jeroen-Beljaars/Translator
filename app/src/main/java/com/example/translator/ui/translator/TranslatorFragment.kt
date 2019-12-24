package com.example.translator.ui.translator


import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.example.translator.R
import kotlinx.android.synthetic.main.fragment_translator.*

/**
 * A simple [Fragment] subclass.
 */
class TranslatorFragment : Fragment() {
    private lateinit var viewModel: TranslatorViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        this.initViewModel()

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_translator, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ivSwitchLanguages.setOnClickListener {
            this.viewModel.switchLanguages()
        }

        if(viewModel.textToTranslate.value != "") {
            etTextToTranslate.setText(viewModel.textToTranslate.value)
        }

        etTextToTranslate.addTextChangedListener( object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.textToTranslate.apply {
                    value = s.toString()
                }
            }

        })

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
        viewModel = ViewModelProviders.of(activity as AppCompatActivity).get(TranslatorViewModel::class.java)

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

        viewModel.storedFromLanguage.observe(this, Observer { storedFromLanguage ->
            if (storedFromLanguage != null) {
                viewModel.fromLanguage.apply {
                    value = storedFromLanguage
                }
            }
        })

        viewModel.storedToLanguage.observe(this, Observer { storedToLanguage ->
            if (storedToLanguage != null) {
                viewModel.toLanguage.apply {
                    value = storedToLanguage
                }
            }
        })

        viewModel.translatedText.observe(this, Observer { translateResponse ->
            if (translateResponse.error == null && translateResponse.result == "") {
                toggleTranslationResultVisibility(false)
            }
            else if (translateResponse.error != null) {
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
    }

    fun toggleTranslationResultVisibility(visible: Boolean){
        val visibility = if(visible) View.VISIBLE else View.INVISIBLE
        translationTopBar.visibility = visibility
        tvTranslatedText.visibility = visibility
    }
}
