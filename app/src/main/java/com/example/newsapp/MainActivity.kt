package com.example.newsapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.newsapp.databinding.ActivityMainBinding
import com.example.newsapp.ui.bookmarks.BookmarksFragment
import com.example.newsapp.ui.news.NewsFragment
import com.example.newsapp.ui.profile.ProfileFragment
import com.example.newsapp.ui.search.SearchFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentItemId: Int = R.id.navigation_news

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            showFragment(item.itemId)
            true
        }

        if (savedInstanceState == null) {
            showFragment(R.id.navigation_news)
            binding.bottomNavigation.selectedItemId = R.id.navigation_news
        } else {
            currentItemId = savedInstanceState.getInt(KEY_SELECTED_ITEM, R.id.navigation_news)
            showFragment(currentItemId)
            binding.bottomNavigation.selectedItemId = currentItemId
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_SELECTED_ITEM, currentItemId)
    }

    private fun showFragment(itemId: Int) {
        if (currentItemId == itemId && supportFragmentManager.findFragmentByTag(itemId.toString()) != null) {
            return
        }

        val transaction = supportFragmentManager.beginTransaction()
        supportFragmentManager.fragments.forEach { fragment ->
            transaction.hide(fragment)
        }

        var fragment = supportFragmentManager.findFragmentByTag(itemId.toString())
        if (fragment == null) {
            fragment = createFragment(itemId)
            transaction.add(R.id.fragmentContainerView, fragment, itemId.toString())
        } else {
            transaction.show(fragment)
        }

        transaction.commit()
        currentItemId = itemId
    }

    private fun createFragment(itemId: Int): Fragment {
        return when (itemId) {
            R.id.navigation_search -> SearchFragment()
            R.id.navigation_bookmarks -> BookmarksFragment()
            R.id.navigation_profile -> ProfileFragment()
            else -> NewsFragment()
        }
    }

    companion object {
        private const val KEY_SELECTED_ITEM = "selected_bottom_nav_item"
    }
}
