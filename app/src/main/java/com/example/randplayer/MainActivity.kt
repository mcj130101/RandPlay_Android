package com.example.randplayer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.randplayer.domain.model.AppSettings
import com.example.randplayer.domain.model.AppTheme
import com.example.randplayer.domain.model.PlayerSelectionMode
import com.example.randplayer.playback.PlaybackManager
import com.example.randplayer.playback.PlaybackSource
import com.example.randplayer.ui.history.HistoryScreen
import com.example.randplayer.ui.history.HistoryViewModel
import com.example.randplayer.ui.home.HomeScreen
import com.example.randplayer.ui.home.HomeViewModel
import com.example.randplayer.ui.liked.LikedScreen
import com.example.randplayer.ui.liked.LikedViewModel
import com.example.randplayer.ui.logs.LogsScreen
import com.example.randplayer.ui.logs.LogsViewModel
import com.example.randplayer.ui.settings.SettingsScreen
import com.example.randplayer.ui.settings.SettingsViewModel
import com.example.randplayer.ui.sources.AddSourceScreen
import com.example.randplayer.ui.sources.SourceEditorViewModel
import com.example.randplayer.ui.sources.SourcesScreen
import com.example.randplayer.ui.sources.SourcesViewModel
import com.example.randplayer.ui.theme.RandPlayerTheme
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
        
        // Handle widget intent if launched with action
        if (intent?.action == "com.example.randplayer.ACTION_PLAY_RANDOM") {
            // In a real app we'd want a cleaner way to get the ViewModel outside of Compose
            // or use a dedicated BroadcastReceiver/Service to handle the play action.
            // For now, setting a flag that will be read when the HomeViewModel is initialized.
            intent?.action = null
            // We will let the Home screen trigger it when it boots
        }
        
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
            
            val isDarkTheme = when (settings.theme) {
                AppTheme.SYSTEM -> isSystemInDarkTheme()
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
            }

            RandPlayerTheme(darkTheme = isDarkTheme) {
                MainScreen(playbackManager, settings)
            }
        }
    }
}

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Rounded.Casino)
    object Sources : Screen("sources", "Sources", Icons.AutoMirrored.Filled.List)
    object History : Screen("history", "History", Icons.Default.History)
    object Liked : Screen("liked", label = "Liked", Icons.Default.Favorite)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object AddSource : Screen("add_source", "Add Source", Icons.AutoMirrored.Filled.ListAlt)
    object Logs : Screen("logs/{sourceId}", "Logs", Icons.AutoMirrored.Filled.ListAlt)
}

@Composable
fun MainScreen(playbackManager: PlaybackManager, appSettings: AppSettings) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    val mainScreens = listOf(Screen.Home, Screen.Sources, Screen.History,Screen.Liked, Screen.Settings)
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
                                // Fallback to system default if package not found
                                intent.setPackage(null)
                                context.startActivity(intent)
                            }
                        } else {
                            context.startActivity(intent)
                        }
                    }
                    PlayerSelectionMode.ASK_EVERY_TIME -> {
                        // Remove FLAG_ACTIVITY_NEW_TASK from the inner intent when using chooser
                        // or make sure the chooser itself has it if needed.
                        // Chooser usually doesn't need it if started from Activity context.
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
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 20.dp)
                        .height(96.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    // Navbar Background
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(36.dp),
                        tonalElevation = 12.dp,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp),
                    ) {}

                    // Navbar Content
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp)
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.BottomStart,
                    ) {
                        val dockWidth = maxWidth
                        val itemWidth = dockWidth / mainScreens.size
                        
                        // Real-time Sliding Pill Indicator
                        val pillX by remember {
                            derivedStateOf {
                                itemWidth * (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .width(itemWidth - 12.dp)
                                .height(56.dp)
                                .offset(x = pillX + 6.dp, y = (-8).dp),
                        ) {}

                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            mainScreens.forEachIndexed { index, screen ->
                                val isSelected = pagerState.currentPage == index
                                
                                val contentColor by animateColorAsState(
                                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    label = "content",
                                )
                                val circleColor by animateColorAsState(
                                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    label = "circleColor",
                                )
                                val iconColor by animateColorAsState(
                                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    label = "iconColor",
                                )
                                val circleElevation by animateDpAsState(
                                    targetValue = if (isSelected) 6.dp else 0.dp,
                                    label = "elevation",
                                )
                                val iconOffset by animateDpAsState(
                                    targetValue = if (isSelected) (-38).dp else (-24).dp,
                                    animationSpec = tween(300),
                                    label = "iconOffset"
                                )
                                val textOffset by animateDpAsState(
                                    targetValue = if (isSelected) (-10).dp else (-8).dp,
                                    animationSpec = tween(300),
                                    label = "textOffset"
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .selectable(
                                            selected = isSelected,
                                            onClick = {
                                                scope.launch {
                                                    pagerState.scrollToPage(index)
                                                }
                                            },
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() }
                                        ),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    // Icon container
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .offset(y = iconOffset),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Surface(
                                            color = circleColor,
                                            shape = CircleShape,
                                            modifier = Modifier.fillMaxSize(),
                                            shadowElevation = circleElevation,
                                        ) {}
                                        Icon(
                                            imageVector = screen.icon,
                                            contentDescription = screen.label,
                                            modifier = Modifier.size(24.dp),
                                            tint = iconColor
                                        )
                                    }
                                    
                                    // Text
                                    Text(
                                        text = screen.label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        ),
                                        color = contentColor,
                                        maxLines = 1,
                                        modifier = Modifier.offset(y = textOffset),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController, 
            startDestination = "dashboard", 
            modifier = Modifier.padding(bottom = if (isDashboard) innerPadding.calculateBottomPadding() else 0.dp),
        ) {
            composable("dashboard") {
                val activity = context as? MainActivity
                val shouldPlayFromWidget = activity?.intent?.action == "com.example.randplayer.ACTION_PLAY_RANDOM"
                if (shouldPlayFromWidget) {
                    activity?.intent?.action = ""
                }
                
                DashboardPager(pagerState, mainScreens, navController, shouldPlayFromWidget)
            }
            
            composable(Screen.AddSource.route) {
                val viewModel: SourceEditorViewModel = hiltViewModel()
                AddSourceScreen(viewModel) { navController.popBackStack() }
            }
            composable(Screen.Logs.route) { backStackEntry ->
                val sourceId = backStackEntry.arguments?.getString("sourceId")
                val viewModel: LogsViewModel = hiltViewModel()
                LaunchedEffect(sourceId) {
                    viewModel.selectSource(sourceId)
                }
                LogsScreen(viewModel)
            }
        }
    }
}

@Composable
fun DashboardPager(
    pagerState: PagerState,
    mainScreens: List<Screen>,
    navController: androidx.navigation.NavHostController,
    shouldPlayFromWidget: Boolean = false
) {
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 4
    ) { page ->
        when (mainScreens[page]) {
            Screen.Home -> {
                val homeViewModel: HomeViewModel = hiltViewModel()
                LaunchedEffect(shouldPlayFromWidget) {
                    if (shouldPlayFromWidget) {
                        homeViewModel.rollDice()
                    }
                }
                HomeScreen(homeViewModel)
            }
            Screen.Sources -> {
                val viewModel: SourcesViewModel = hiltViewModel()
                SourcesScreen(
                    viewModel, 
                    onAddSource = { navController.navigate(Screen.AddSource.route) },
                ) { sourceId -> navController.navigate("logs/$sourceId") }
            }
            Screen.History -> {
                val viewModel: HistoryViewModel = hiltViewModel()
                HistoryScreen(viewModel)
            }
            Screen.Liked -> {
                val viewModel: LikedViewModel = hiltViewModel()
                LikedScreen(viewModel)
            }
            Screen.Settings -> {
                val viewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen(viewModel)
            }
            else -> Box(Modifier.fillMaxSize())
        }
    }
}
