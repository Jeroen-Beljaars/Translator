package com.example.translator.ui.favoriteTranslations


import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.NavHostFragment.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.translator.R
import com.example.translator.model.Translation
import com.example.translator.ui.translator.TranslationAdapter
import com.example.translator.ui.translator.TranslatorViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_favorite_translations.*
import kotlinx.android.synthetic.main.fragment_translator.*

/**
 * A simple [Fragment] subclass.
 */
class FavoriteTranslationsFragment : Fragment() {

    private lateinit var translatorViewModel: TranslatorViewModel
    private lateinit var favoriteTranslationsViewModel: FavoriteTranslationsViewModel

    private var filteredFavoriteTranslations = arrayListOf<Translation>()
    private lateinit var translationAdapter: TranslationAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        this.initViewModel()
        setHasOptionsMenu(true)

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_favorite_translations, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this.translationAdapter = TranslationAdapter(
            filteredFavoriteTranslations,
            { translation -> onTranslationClick(translation) },
            { translation -> translatorViewModel.favoriteTranslation(translation)}
        )

        rvFavorites.layoutManager = LinearLayoutManager(activity as AppCompatActivity, RecyclerView.VERTICAL, false)
        rvFavorites.adapter = translationAdapter

        createItemTouchHelper().attachToRecyclerView(rvFavorites)
        rvFavorites.addItemDecoration(
            DividerItemDecoration(
                activity as AppCompatActivity,
                DividerItemDecoration.VERTICAL
            )
        )
    }

    /**
     * Select a translation to edit it
     *
     * @param translation The translation which we want to edit
     */
    private fun onTranslationClick(translation: Translation) {
        translatorViewModel.selectTranslation(translation)
        findNavController().navigate(R.id.action_favoriteTranslationsFragment_to_translatorFragment)
    }

    private fun initViewModel() {
        favoriteTranslationsViewModel = ViewModelProviders.of(this).get(FavoriteTranslationsViewModel::class.java)
        translatorViewModel = ViewModelProviders.of(activity as AppCompatActivity).get(
            TranslatorViewModel::class.java)

        // Initialize the list with all the languages
        favoriteTranslationsViewModel.favoriteTranslations.observe(this, Observer { translations ->
            this@FavoriteTranslationsFragment.favoriteTranslationsViewModel.filteredFavoriteTranslations.apply {
                value=ArrayList(translations)
            }
        })

        // If the filter changes also filter it in the ui
        favoriteTranslationsViewModel.filteredFavoriteTranslations.observe(this, Observer { translations ->
            this@FavoriteTranslationsFragment.filteredFavoriteTranslations.clear()
            this@FavoriteTranslationsFragment.filteredFavoriteTranslations.addAll(translations)

            translationAdapter.notifyDataSetChanged()
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
                    favoriteTranslationsViewModel.filterTranslations(newText)
                    return true
                }

            })
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
                    val translationToDelete = filteredFavoriteTranslations[position]

                    translatorViewModel.deleteTranslation(translationToDelete)

                    val mySnackbar = Snackbar.make(
                        activity!!.findViewById(android.R.id.content),
                        R.string.removed_from_history,
                        Snackbar.LENGTH_LONG
                    )

                    mySnackbar.setAction(R.string.undo) {
                        translatorViewModel.createOrUpdateTranslation(translationToDelete)
                    }

                    mySnackbar.show()
                } else if (direction == ItemTouchHelper.RIGHT) {
                    val position = viewHolder.adapterPosition
                    val translationToFavorite = filteredFavoriteTranslations[position]

                    translatorViewModel.favoriteTranslation(translationToFavorite)

                    val mySnackbar = Snackbar.make(
                        activity!!.findViewById(android.R.id.content),
                        if(translationToFavorite.isFavorite) R.string.removed_from_favorites else R.string.added_to_favorite,
                        Snackbar.LENGTH_LONG
                    )

                    mySnackbar.setAction(R.string.undo) {
                        translatorViewModel.favoriteTranslation(translationToFavorite)
                    }

                    mySnackbar.show()

                }
            }
        }
        return ItemTouchHelper(callback)
    }
}
