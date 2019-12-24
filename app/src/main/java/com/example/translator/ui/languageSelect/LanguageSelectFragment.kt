package com.example.translator.ui.languageSelect

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.translator.R
import com.example.translator.model.Language
import com.example.translator.ui.translator.TranslatorViewModel
import kotlinx.android.synthetic.main.fragment_language_select.*

/**
 * A simple [Fragment] subclass.
 */
class LanguageSelectFragment : Fragment() {
    private val args: LanguageSelectFragmentArgs by navArgs()
    private lateinit var languageViewModel: LanguageSelectViewModel
    private lateinit var translatorViewModel: TranslatorViewModel

    private var filteredLanguages = arrayListOf<Language>()
    private lateinit var languageAdapter: LanguageAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        this.initViewModel()
        setHasOptionsMenu(true)

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_language_select, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this.languageAdapter = LanguageAdapter(filteredLanguages, this.args.currentSelectedLanguage) {language -> onLanguageClick(language)}

        rvLanguages.layoutManager = LinearLayoutManager(activity as AppCompatActivity, RecyclerView.VERTICAL, false)
        rvLanguages.adapter = languageAdapter

//        createItemTouchHelper().attachToRecyclerView(rvGames)
    }

    fun onLanguageClick(language: Language) {
        if (args.changingFromLanguage){
            translatorViewModel.selectFromLanguage(language)
        } else {
            translatorViewModel.selectToLanguage(language)
        }
        findNavController().navigate(R.id.action_languageSelectActivity_to_translatorFragment)
    }

    private fun initViewModel() {
        languageViewModel = ViewModelProviders.of(this).get(LanguageSelectViewModel::class.java)
        translatorViewModel = ViewModelProviders.of(activity as AppCompatActivity).get(
            TranslatorViewModel::class.java)

        // Initialize the list with all the languages
        languageViewModel.languages.observe(this, Observer { languages ->
            this@LanguageSelectFragment.languageViewModel.filteredLanguages.apply {
                value=ArrayList(languages)
            }
        })

        // If the filter changes also filter it in the ui
        languageViewModel.filteredLanguages.observe(this, Observer { languages ->
            this@LanguageSelectFragment.filteredLanguages.clear()
            this@LanguageSelectFragment.filteredLanguages.addAll(languages)

            languageAdapter.notifyDataSetChanged()
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.menu_search, menu)

        val searchItem = menu.findItem(R.id.action_search)
        if(searchItem != null){
            val searchView = searchItem.actionView as SearchView
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    languageViewModel.filterLanguages(newText)
                    return true
                }

            })
        }
    }


}
