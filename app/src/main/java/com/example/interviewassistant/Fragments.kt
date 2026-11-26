package com.example.interviewassistant

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController

/**
 * Base class: every screen is a Fragment that hosts the existing Compose UI.
 * All Fragments share one [InterviewState] (a ViewModel owned by the Activity).
 */
abstract class ComposeFragment : Fragment() {
    protected val state: InterviewState by activityViewModels()

    @Composable
    protected abstract fun Content()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppTheme {
                    AppBackground {
                        this@ComposeFragment.Content()
                        StatusBanner(state, Modifier.align(Alignment.TopCenter))
                    }
                }
            }
        }
}

// ---- account -----------------------------------------------------------------------------
class SignInFragment : ComposeFragment() {
    @Composable override fun Content() = SignInScreen(state)
}

class SignUpFragment : ComposeFragment() {
    @Composable override fun Content() = SignUpScreen(state)
}

// ---- interview flow ----------------------------------------------------------------------
class WelcomeFragment : ComposeFragment() {
    @Composable override fun Content() {
        var showServer by remember { mutableStateOf(false) }
        WelcomeScreen(state, onServerClick = { showServer = true })
        if (showServer) ServerDialog(state) { showServer = false }
    }
}

class CareerFragment : ComposeFragment() {
    @Composable override fun Content() = CareerScreen(state)
}

class CvFragment : ComposeFragment() {
    @Composable override fun Content() = CvScreen(state)
}

class ReadyFragment : ComposeFragment() {
    @Composable override fun Content() = ReadyScreen(state)
}

class LiveFragment : ComposeFragment() {
    @Composable override fun Content() {
        val context = LocalContext.current
        val speaker = remember { Speaker(context) }
        DisposableEffect(Unit) { onDispose { speaker.shutdown() } }
        LiveScreen(state, speaker)
    }
}

class ReviewFragment : ComposeFragment() {
    @Composable override fun Content() = ReviewScreen(state)
}

// ---- menu pages --------------------------------------------------------------------------
class ProfileFragment : ComposeFragment() {
    @Composable override fun Content() = ProfileScreen()
}

class SettingsFragment : ComposeFragment() {
    @Composable override fun Content() = SettingsScreen(state)
}

class HistoryFragment : ComposeFragment() {
    @Composable override fun Content() = HistoryScreen { id ->
        findNavController().navigate(R.id.historyDetailFragment, Bundle().apply { putString("id", id) })
    }
}

/** A past interview: loads the saved report from Firestore and shows it with the normal Review screen. */
class HistoryDetailFragment : ComposeFragment() {
    @Composable override fun Content() {
        val id = requireArguments().getString("id") ?: ""
        var loaded by remember { mutableStateOf(false) }
        LaunchedEffect(id) {
            Repo.loadReport(id) { r ->
                if (r != null) {
                    state.report = r
                    state.career = r.career
                } else {
                    state.report = null
                    state.error = "Could not load this interview."
                }
                loaded = true
            }
        }
        if (loaded && state.report != null) {
            ReviewScreen(state)
        } else {
            Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                if (!loaded) Txt("Loading…", 14, color = C.Muted)
            }
        }
    }
}