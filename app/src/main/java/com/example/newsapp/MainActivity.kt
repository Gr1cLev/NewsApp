package com.example.newsapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.newsapp.data.UserPreferences
import com.example.newsapp.databinding.ActivityMainBinding
import com.example.newsapp.navigation.ArticleNavigator
import com.example.newsapp.navigation.ProfileNavigator
import com.example.newsapp.ui.bookmarks.BookmarksFragment
import com.example.newsapp.ui.detail.ArticleDetailFragment
import com.example.newsapp.ui.news.NewsFragment
import com.example.newsapp.ui.profile.ProfileFragment
import com.example.newsapp.ui.search.SearchFragment
import com.example.newsapp.ui.settings.EditProfileFragment
import com.example.newsapp.ui.settings.SettingsFragment

class MainActivity : AppCompatActivity(), ArticleNavigator, ProfileNavigator {

    private lateinit var binding: ActivityMainBinding
    private var currentItemId: Int = R.id.navigation_news

    override fun onCreate(savedInstanceState: Bundle?) {
        val nightMode = if (UserPreferences.isNightModeEnabled(this)) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            showFragment(item.itemId)
            true
        }

        supportFragmentManager.addOnBackStackChangedListener {
            updateBottomNavigationVisibility()
        }

        if (savedInstanceState == null) {
            showFragment(R.id.navigation_news)
            binding.bottomNavigation.selectedItemId = R.id.navigation_news
        } else {
            currentItemId = savedInstanceState.getInt(KEY_SELECTED_ITEM, R.id.navigation_news)
            if (supportFragmentManager.backStackEntryCount == 0) {
                showFragment(currentItemId)
            }
            binding.bottomNavigation.selectedItemId = currentItemId
        }

        updateBottomNavigationVisibility()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_SELECTED_ITEM, currentItemId)
    }

    private fun showFragment(itemId: Int) {
        if (currentItemId == itemId && supportFragmentManager.findFragmentByTag(itemId.toString()) != null) {
            return
        }

        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
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
        updateBottomNavigationVisibility()
    }

    private fun createFragment(itemId: Int): Fragment {
        return when (itemId) {
            R.id.navigation_search -> SearchFragment()
            R.id.navigation_bookmarks -> BookmarksFragment()
            R.id.navigation_profile -> ProfileFragment()
            else -> NewsFragment()
        }
    }

    private fun updateBottomNavigationVisibility() {
        binding.bottomNavigation.isVisible = supportFragmentManager.backStackEntryCount == 0
    }

    override fun openArticleDetail(articleId: Int) {
        val detailFragment = ArticleDetailFragment.newInstance(articleId)
        val detailTag = "${ARTICLE_FRAGMENT_TAG}_$articleId"
        pushContentFragment(detailFragment, detailTag, ARTICLE_BACKSTACK_NAME)
    }

    override fun openSettings() {
        pushContentFragment(SettingsFragment(), SETTINGS_FRAGMENT_TAG, SETTINGS_BACKSTACK_NAME)
    }

    override fun openEditProfile() {
        pushContentFragment(EditProfileFragment(), EDIT_PROFILE_FRAGMENT_TAG, EDIT_PROFILE_BACKSTACK)
    }

    private fun pushContentFragment(fragment: Fragment, tag: String, backstackName: String) {
        binding.bottomNavigation.isVisible = false
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .add(R.id.fragmentContainerView, fragment, tag)
            .addToBackStack(backstackName)
            .commit()
    }

    companion object {
        private const val KEY_SELECTED_ITEM = "selected_bottom_nav_item"
        private const val ARTICLE_BACKSTACK_NAME = "article_detail_backstack"
        private const val ARTICLE_FRAGMENT_TAG = "article_detail_fragment"
        private const val SETTINGS_BACKSTACK_NAME = "settings_backstack"
        private const val SETTINGS_FRAGMENT_TAG = "settings_fragment"
        private const val EDIT_PROFILE_BACKSTACK = "edit_profile_backstack"
        private const val EDIT_PROFILE_FRAGMENT_TAG = "edit_profile_fragment"
    }
}
