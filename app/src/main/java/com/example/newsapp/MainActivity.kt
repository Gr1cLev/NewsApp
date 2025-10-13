package com.example.newsapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.example.newsapp.data.ProfileRepository
import com.example.newsapp.data.UserPreferences
import com.example.newsapp.databinding.ActivityMainBinding
import com.example.newsapp.navigation.ArticleNavigator
import com.example.newsapp.navigation.AuthNavigator
import com.example.newsapp.navigation.ProfileNavigator
import com.example.newsapp.ui.auth.LoginFragment
import com.example.newsapp.ui.auth.RegisterFragment
import com.example.newsapp.ui.bookmarks.BookmarksFragment
import com.example.newsapp.ui.detail.ArticleDetailFragment
import com.example.newsapp.ui.news.NewsFragment
import com.example.newsapp.ui.profile.ProfileFragment
import com.example.newsapp.ui.search.SearchFragment
import com.example.newsapp.ui.settings.EditProfileFragment
import com.example.newsapp.ui.settings.SettingsFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity(), ArticleNavigator, ProfileNavigator, AuthNavigator {

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
            lifecycleScope.launch {
                val hasActiveProfile = withContext(Dispatchers.IO) {
                    ProfileRepository.hasActiveProfile(this@MainActivity)
                }
                if (hasActiveProfile) {
                    showFragment(R.id.navigation_news)
                    binding.bottomNavigation.selectedItemId = R.id.navigation_news
                } else {
                    showAuthScreen(LoginFragment(), LOGIN_FRAGMENT_TAG)
                }
                updateBottomNavigationVisibility()
            }
        } else {
            currentItemId = savedInstanceState.getInt(KEY_SELECTED_ITEM, R.id.navigation_news)
            binding.bottomNavigation.selectedItemId = currentItemId
            val authFragmentPresent = supportFragmentManager.findFragmentById(R.id.authContainer) != null
            binding.authContainer.isVisible = authFragmentPresent
            if (authFragmentPresent) {
                binding.bottomNavigation.isVisible = false
            } else {
                updateBottomNavigationVisibility()
            }
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
        binding.bottomNavigation.isVisible =
            !binding.authContainer.isVisible && supportFragmentManager.backStackEntryCount == 0
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
        if (supportFragmentManager.findFragmentByTag(tag) != null) {
            return
        }
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

    override fun openLogin() {
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                currentItemId = R.id.navigation_news
                showAuthScreen(LoginFragment(), LOGIN_FRAGMENT_TAG)
            }
        }
    }

    override fun openRegister() {
        showAuthScreen(RegisterFragment(), REGISTER_FRAGMENT_TAG)
    }

    override fun onAuthenticationSuccess() {
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                supportFragmentManager.findFragmentById(R.id.authContainer)?.let {
                    supportFragmentManager.beginTransaction()
                        .remove(it)
                        .commitAllowingStateLoss()
                }
                binding.authContainer.isVisible = false
                supportFragmentManager.setFragmentResult(ProfileFragment.PROFILE_UPDATED_RESULT, android.os.Bundle())
                showFragment(R.id.navigation_news)
                binding.bottomNavigation.selectedItemId = R.id.navigation_news
                updateBottomNavigationVisibility()
            }
        }
    }

    private fun showAuthScreen(fragment: Fragment, tag: String) {
        binding.authContainer.isVisible = true
        binding.bottomNavigation.isVisible = false
        supportFragmentManager.beginTransaction()
            .replace(R.id.authContainer, fragment, tag)
            .commitAllowingStateLoss()
    }

    companion object {
        private const val KEY_SELECTED_ITEM = "selected_bottom_nav_item"
        private const val ARTICLE_BACKSTACK_NAME = "article_detail_backstack"
        private const val ARTICLE_FRAGMENT_TAG = "article_detail_fragment"
        private const val SETTINGS_BACKSTACK_NAME = "settings_backstack"
        private const val SETTINGS_FRAGMENT_TAG = "settings_fragment"
        private const val EDIT_PROFILE_BACKSTACK = "edit_profile_backstack"
        private const val EDIT_PROFILE_FRAGMENT_TAG = "edit_profile_fragment"
        private const val LOGIN_FRAGMENT_TAG = "login_fragment"
        private const val REGISTER_FRAGMENT_TAG = "register_fragment"
    }
}
