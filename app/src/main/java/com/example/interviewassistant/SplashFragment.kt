package com.example.interviewassistant

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

class SplashFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            AppTheme {
                AppBackground {
                    SplashContent(onFinished = { goNext() })
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Back is disabled on the splash so it can't be skipped into a half-started app
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { }
    }

    private fun goNext() {
        val nav = findNavController()
        if (nav.currentDestination?.id != R.id.splashFragment) return
        val target =
            if (FirebaseAuth.getInstance().currentUser != null) R.id.welcomeFragment
            else R.id.signInFragment
        nav.navigate(target, null, navOptions { popUpTo(R.id.splashFragment) { inclusive = true } })
    }
}

private const val TAGLINE = "Practise. Speak. Improve."

@Composable
private fun SplashContent(onFinished: () -> Unit) {
    val logo = remember { Animatable(0f) }
    val title = remember { Animatable(0f) }
    val progress = remember { Animatable(0f) }
    var typed by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { logo.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
            launch { delay(1200); title.animateTo(1f, tween(700)) }
            launch { delay(1500); for (i in 1..TAGLINE.length) { typed = i; delay(60) } }
            launch { delay(300); progress.animateTo(1f, tween(3900, easing = LinearEasing)) }
        }
        delay(300)
        onFinished()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 1. logo scales and fades in
        Box(
            Modifier.graphicsLayer {
                val s = 0.6f + 0.4f * logo.value
                scaleX = s; scaleY = s; alpha = logo.value
            }
        ) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .shadow(24.dp, RoundedCornerShape(26.dp), ambientColor = C.Primary, spotColor = C.Primary)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xFFF4F6FF)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.app_logo),
                    contentDescription = "Interview Assistant logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(72.dp)
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        // 2. brand name fades in
        Box(Modifier.graphicsLayer { alpha = title.value }) {
            BrandName(32)
        }
        Spacer(Modifier.height(8.dp))

        // 3. tagline types out
        Txt(TAGLINE.take(typed), 13, color = C.Muted)
        Spacer(Modifier.height(28.dp))

        // 4. moving bars
        SplashWave(visible = logo.value)
        Spacer(Modifier.height(32.dp))

        // 5. progress bar (your existing one)
        ProgressBar(progress.value)
    }
}

@Composable
private fun SplashWave(visible: Float) {
    val transition = rememberInfiniteTransition(label = "splashWave")
    val bars = List(13) { i ->
        transition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 380 + i * 53, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "splashBar$i"
        )
    }
    Row(
        modifier = Modifier.height(40.dp).graphicsLayer { alpha = visible },
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        bars.forEach { h ->
            Box(
                Modifier.width(4.dp).height((40f * h.value).dp)
                    .clip(RoundedCornerShape(2.dp)).background(C.AccentText)
            )
        }
    }
}