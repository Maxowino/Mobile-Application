package com.example.interviewassistant

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import com.google.firebase.auth.FirebaseAuth

/**
 * The only Activity. It owns the splash screen, the toolbar with its menu, and the NavHostFragment
 * that swaps the Fragments (screens) listed in res/navigation/nav_graph.xml.
 */
class MainActivity : AppCompatActivity() {

    private val state: InterviewState by viewModels()
    private lateinit var navController: NavController
    private lateinit var toolbar: Toolbar

    /** No toolbar (and no menu) on these screens: they draw their own header. */
    private val noToolbar = setOf(R.id.signInFragment, R.id.signUpFragment, R.id.liveFragment)

    /** Pages opened from the menu; they get a back arrow. */
    private val menuPages = setOf(
        R.id.profileFragment, R.id.historyFragment, R.id.historyDetailFragment, R.id.settingsFragment
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash screen: must be installed before super.onCreate. Held for a moment so the logo is seen.
        val splash = installSplashScreen()
        var hold = true
        splash.setKeepOnScreenCondition { hold }
        Handler(Looper.getMainLooper()).postDelayed({ hold = false }, 1200)

        super.onCreate(savedInstanceState)
        // dark app: always use light status-bar icons
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContentView(R.layout.activity_main)

        // keep the content clear of the status bar, navigation bar and keyboard
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Navigation: start at Welcome when someone is signed in, otherwise at Sign in
        val host = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        navController = host.navController
        val graph = navController.navInflater.inflate(R.navigation.nav_graph)
        graph.setStartDestination(
            if (FirebaseAuth.getInstance().currentUser != null) R.id.welcomeFragment else R.id.signInFragment
        )
        navController.graph = graph

        navController.addOnDestinationChangedListener { _, destination, _ ->
            toolbar.visibility = if (destination.id in noToolbar) View.GONE else View.VISIBLE
            supportActionBar?.title = destination.label
            supportActionBar?.setDisplayHomeAsUpEnabled(destination.id in menuPages)
            invalidateOptionsMenu()
        }

        // The screens ask the shared state to navigate; we do it here.
        state.onNavigate = { target -> navigateTo(target) }
        state.onBack = { navController.popBackStack() }
    }

    override fun onDestroy() {
        state.onNavigate = null
        state.onBack = null
        super.onDestroy()
    }

    // ---- navigation ------------------------------------------------------------------------
    private fun destinationOf(target: Screen): Int = when (target) {
        Screen.SIGN_IN -> R.id.signInFragment
        Screen.SIGN_UP -> R.id.signUpFragment
        Screen.WELCOME -> R.id.welcomeFragment
        Screen.CAREER -> R.id.careerFragment
        Screen.CV -> R.id.cvFragment
        Screen.READY -> R.id.readyFragment
        Screen.LIVE -> R.id.liveFragment
        Screen.REVIEW -> R.id.reviewFragment
    }

    private fun navigateTo(target: Screen) {
        val id = destinationOf(target)
        if (navController.currentDestination?.id == id) return
        val clearAll = navOptions { popUpTo(R.id.nav_graph) { inclusive = true } }
        when (target) {
            Screen.SIGN_IN -> navController.navigate(id, null, clearAll)
            // go "home": reuse Welcome if it is already in the back stack, otherwise start fresh from it
            Screen.WELCOME -> if (!navController.popBackStack(id, false)) navController.navigate(id, null, clearAll)
            // the review replaces the live interview; Back from it returns to Welcome
            Screen.REVIEW -> navController.navigate(id, null, navOptions { popUpTo(R.id.welcomeFragment) })
            else -> navController.navigate(id)
        }
    }

    private fun openMenuPage(id: Int) {
        val current = navController.currentDestination?.id ?: return
        if (current == id) return
        navController.navigate(id, null, navOptions {
            launchSingleTop = true
            // switching between menu pages should not pile them up in the back stack
            if (current in menuPages) popUpTo(current) { inclusive = true }
        })
    }

    // ---- menu --------------------------------------------------------------------------------
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val show = navController.currentDestination?.id !in noToolbar
        menu.setGroupVisible(R.id.menu_group, show)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.menu_profile -> { openMenuPage(R.id.profileFragment); true }
        R.id.menu_history -> { openMenuPage(R.id.historyFragment); true }
        R.id.menu_settings -> { openMenuPage(R.id.settingsFragment); true }
        R.id.menu_sign_out -> { state.signOut(); true }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean = navController.navigateUp() || super.onSupportNavigateUp()
}