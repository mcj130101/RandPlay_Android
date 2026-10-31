package com.mcj130101.randplayer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mcj130101.randplayer.domain.model.AppSettings
import com.mcj130101.randplayer.domain.model.AppTheme
import com.mcj130101.randplayer.domain.model.PlayerSelectionMode
import com.mcj130101.randplayer.playback.PlaybackManager
import com.mcj130101.randplayer.playback.PlaybackSource
import com.mcj130101.randplayer.ui.explorer.ExplorerScreen
import com.mcj130101.randplayer.ui.explorer.ExplorerViewModel
import com.mcj130101.randplayer.ui.history.HistoryScreen
import com.mcj130101.randplayer.ui.history.HistoryViewModel
import com.mcj130101.randplayer.ui.home.HomeScreen
import com.mcj130101.randplayer.ui.home.HomeViewModel
import com.mcj130101.randplayer.ui.liked.LikedScreen
import com.mcj130101.randplayer.ui.liked.LikedViewModel
import com.mcj130101.randplayer.ui.settings.SettingsScreen
import com.mcj130101.randplayer.ui.settings.SettingsViewModel
import com.mcj130101.randplayer.ui.sources.SourcesScreen
import com.mcj130101.randplayer.ui.sources.SourcesViewModel
import com.mcj130101.randplayer.ui.theme.RandPlayerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var playbackManager: PlaybackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
            val sourcesViewModel: SourcesViewModel = hiltViewModel()
            
            // Ping sources on app start
            LaunchedEffect(Unit) {
                sourcesViewModel.pingSources()
            }
            
            val isDarkTheme = when (settings.theme) {
                AppTheme.SYSTEM -> isSystemInDarkTheme()
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
            }

            RandPlayerTheme(
                accentColorHex = settings.accentColorHex,
                darkTheme = isDarkTheme
            ) {
                MainScreen(playbackManager, settings)
            }
        }
    }
}

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Rounded.Casino)
    object History : Screen("history", "History", Icons.Default.History)
    object Liked : Screen("liked", "Liked", Icons.Default.Favorite)
    object Sources : Screen("sources", "Sources", Icons.AutoMirrored.Filled.List)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object Explorer : Screen("explorer/{sourceId}", "Explorer", Icons.AutoMirrored.Filled.ListAlt)
}

@Composable
fun MainScreen(playbackManager: PlaybackManager, appSettings: AppSettings) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    val mainScreens = listOf(Screen.Home, Screen.History, Screen.Liked, Screen.Sources)
    val isDashboard = (currentRoute == "dashboard") || (currentRoute == null)

    val pagerState = rememberPagerState { mainScreens.size }
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val playbackEvent by playbackManager.playbackEvent.collectAsStateWithLifecycle()

    LaunchedEffect(playbackEvent, appSettings) {
        playbackEvent?.let { source ->
            val intent = when (source) {
                is PlaybackSource.Local -> {
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(source.uri, "video/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                }
                is PlaybackSource.Stream -> {
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(source.url.toUri(), "video/*")
                    }
                }
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            try {
                when (appSettings.playerSelectionMode) {
                    PlayerSelectionMode.SPECIFIC_APP -> {
                        val preferredPlayer = appSettings.preferredPlayerPackage
                        if (!preferredPlayer.isNullOrEmpty()) {
                            intent.setPackage(preferredPlayer)
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                intent.setPackage(null)
                                context.startActivity(intent)
                            }
                        } else {
                            context.startActivity(intent)
                        }
                    }
                    PlayerSelectionMode.ASK_EVERY_TIME -> {
                        val chooser = Intent.createChooser(intent, "Open video with...")
                        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(chooser)
                    }
                    PlayerSelectionMode.SYSTEM_DEFAULT -> {
                        context.startActivity(intent)
                    }
                }
                playbackManager.onPlaybackLaunched()
            } catch (_: Exception) { }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (isDashboard) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 40.dp, vertical = 12.dp)
                        .height(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer Floating Capsule Bar
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = CircleShape,
                        shadowElevation = 8.dp,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 6.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            mainScreens.forEachIndexed { index, screen ->
                                val isSelected = pagerState.currentPage == index

                                val targetWidth = if (isSelected) {
                                    when (screen) {
                                        Screen.Home -> 110.dp
                                        Screen.Liked -> 104.dp
                                        Screen.History -> 116.dp
                                        Screen.Sources -> 118.dp
                                        else -> 114.dp
                                    }
                                } else {
                                    48.dp
                                }

                                val itemWidth by animateDpAsState(
                                    targetValue = targetWidth,
                                    animationSpec = tween(300),
                                    label = "itemWidth"
                                )

                                Surface(
                                    modifier = Modifier
                                        .height(46.dp)
                                        .width(itemWidth)
                                        .selectable(
                                            selected = isSelected,
                                            onClick = {
                                                if (pagerState.currentPage != index) {
                                                    scope.launch {
                                                        pagerState.animateScrollToPage(index)
                                                    }
                                                }
                                            },
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() }
                                        ),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                                ) {
                                    if (isSelected) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(start = 5.dp, end = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = screen.icon,
                                                        contentDescription = screen.label,
                                                        modifier = Modifier.size(20.dp),
                                                        tint = MaterialTheme.colorScheme.onPrimary
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Text(
                                                text = screen.label,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = screen.icon,
                                                contentDescription = screen.label,
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        val targetBottomPadding = if (isDashboard) innerPadding.calculateBottomPadding() else 0.dp
        val animatedBottomPadding by animateDpAsState(
            targetValue = targetBottomPadding,
            animationSpec = tween(180, easing = FastOutLinearInEasing),
            label = "bottomPadding"
        )

        NavHost(
            navController = navController, 
            startDestination = "dashboard",
            modifier = Modifier.padding(bottom = animatedBottomPadding),
            enterTransition = {
                fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                slideInHorizontally(animationSpec = tween(180, easing = FastOutSlowInEasing), initialOffsetX = { it / 6 })
            },
            exitTransition = {
                fadeOut(animationSpec = tween(150, easing = FastOutLinearInEasing)) +
                slideOutHorizontally(animationSpec = tween(150, easing = FastOutLinearInEasing), targetOffsetX = { -it / 6 })
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing)) +
                slideInHorizontally(animationSpec = tween(180, easing = FastOutSlowInEasing), initialOffsetX = { -it / 6 })
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(150, easing = FastOutLinearInEasing)) +
                slideOutHorizontally(animationSpec = tween(150, easing = FastOutLinearInEasing), targetOffsetX = { it / 6 })
            }
        ) {
            composable("dashboard") {
                DashboardPager(pagerState, mainScreens, navController)
            }

            composable("settings") {
                val settingsViewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Explorer.route) { backStackEntry ->
                val sourceId = backStackEntry.arguments?.getString("sourceId") ?: return@composable
                val viewModel: ExplorerViewModel = hiltViewModel()
                LaunchedEffect(sourceId) {
                    viewModel.initialize(sourceId)
                }
                ExplorerScreen(viewModel) { navController.popBackStack() }
            }
        }
    }
}

@Composable
fun DashboardPager(
    pagerState: PagerState,
    mainScreens: List<Screen>,
    navController: androidx.navigation.NavHostController,
    homeViewModel: HomeViewModel = hiltViewModel(),
    historyViewModel: HistoryViewModel = hiltViewModel(),
    likedViewModel: LikedViewModel = hiltViewModel(),
    sourcesViewModel: SourcesViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 3
    ) { page ->
        when (mainScreens[page]) {
            Screen.Home -> {
                HomeScreen(
                    viewModel = homeViewModel,
                    settingsViewModel = settingsViewModel,
                    onNavigateToSettings = { navController.navigate("settings") }
                )
            }
            Screen.History -> {
                HistoryScreen(historyViewModel)
            }
            Screen.Liked -> {
                LikedScreen(likedViewModel)
            }
            Screen.Sources -> {
                SourcesScreen(
                    viewModel = sourcesViewModel,
                    onBrowse = { sourceId -> navController.navigate("explorer/$sourceId") }
                )
            }
            else -> Box(Modifier.fillMaxSize())
        }
    }
}
