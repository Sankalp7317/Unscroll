package com.example.unscroll

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.airbnb.lottie.compose.*
import com.example.unscroll.data.repository.UserPreferencesRepository
import com.example.unscroll.service.UsageTrackingService
import com.example.unscroll.ui.MainViewModel
import com.example.unscroll.ui.MainViewModelFactory
import com.example.unscroll.ui.navigation.Screen
import com.example.unscroll.ui.theme.UnscrollTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val repository = UserPreferencesRepository(context)
            val viewModel: MainViewModel = viewModel(factory = MainViewModelFactory(repository))
            val navController = rememberNavController()
            
            UnscrollTheme {
                AppNavigation(navController, viewModel)
            }
        }
    }
}

@Composable
fun AppNavigation(navController: NavHostController, viewModel: MainViewModel) {
    NavHost(navController = navController, startDestination = Screen.Dashboard.route) {
        composable(Screen.Dashboard.route) {
            MainScreen(viewModel, onNavigateToSettings = { navController.navigate(Screen.Settings.route) })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(viewModel, onBack = { navController.popBackStack() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel, onNavigateToSettings: () -> Unit) {
    val context = LocalContext.current
    val isUsageStatsGranted by viewModel.isUsageStatsPermissionGranted.collectAsState()
    val isOverlayGranted by viewModel.isOverlayPermissionGranted.collectAsState()

    val youtubeUsage by viewModel.youtubeUsage.collectAsState(initial = 0L)
    val instagramUsage by viewModel.instagramUsage.collectAsState(initial = 0L)
    val tiktokUsage by viewModel.tiktokUsage.collectAsState(initial = 0L)

    val youtubeLimit by viewModel.youtubeLimit.collectAsState(initial = 1800000L)
    val instagramLimit by viewModel.instagramLimit.collectAsState(initial = 1800000L)
    val tiktokLimit by viewModel.tiktokLimit.collectAsState(initial = 1800000L)

    val youtubeEnabled by viewModel.youtubeEnabled.collectAsState(initial = true)
    val instagramEnabled by viewModel.instagramEnabled.collectAsState(initial = true)
    val tiktokEnabled by viewModel.tiktokEnabled.collectAsState(initial = true)

    LaunchedEffect(Unit) {
        viewModel.checkPermissions(context)
    }

    LaunchedEffect(isUsageStatsGranted, isOverlayGranted) {
        if (isUsageStatsGranted && isOverlayGranted) {
            val intent = Intent(context, UsageTrackingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(
        Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                MaterialTheme.colorScheme.background
            )
        )
    )) {
        Scaffold(
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "UNSCROLL",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 4.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    actions = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (!isUsageStatsGranted || !isOverlayGranted) {
                    PermissionSection(
                        isUsageStatsGranted = isUsageStatsGranted,
                        isOverlayGranted = isOverlayGranted,
                        onOpenUsageStats = { viewModel.openUsageStatsSettings(context) },
                        onOpenOverlay = { viewModel.openOverlaySettings(context) }
                    )
                } else {
                    StreakSection()

                    val appList = mutableListOf<AppData>()
                    if (youtubeEnabled) appList.add(AppData("YouTube", youtubeUsage, youtubeLimit, Color(0xFFFF0000)))
                    if (instagramEnabled) appList.add(AppData("Instagram", instagramUsage, instagramLimit, Color(0xFFE4405F)))
                    if (tiktokEnabled) appList.add(AppData("TikTok", tiktokUsage, tiktokLimit, Color(0xFF000000)))

                    if (appList.isEmpty()) {
                        EmptyState()
                    } else {
                        Text(
                            text = "Daily Focus",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            itemsIndexed(appList) { index, app ->
                                var visible by remember { mutableStateOf(false) }
                                LaunchedEffect(Unit) {
                                    delay(index * 100L)
                                    visible = true
                                }
                                AnimatedVisibility(
                                    visible = visible,
                                    enter = fadeIn() + slideInVertically(initialOffsetY = { 50 })
                                ) {
                                    PremiumUsageCard(app)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StreakSection() {
    val composition by rememberLottieComposition(LottieCompositionSpec.Url("https://assets9.lottiefiles.com/packages/lf20_m6cuL6.json"))
    val progress by animateLottieCompositionAsState(composition, iterations = LottieConstants.IterateForever)

    GlassCard(
        modifier = Modifier.fillMaxWidth().height(160.dp),
        gradientColors = listOf(
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.8f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Focus Streak",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "3 Days",
                    color = Color.White,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "You're killing it!",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier.size(100.dp)
            )
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    gradientColors: List<Color> = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    ),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(gradientColors))
            .border(
                BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                RoundedCornerShape(28.dp)
            )
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(28.dp))
    ) {
        content()
    }
}

@Composable
fun PremiumUsageCard(app: AppData) {
    val progress = if (app.limit > 0) (app.usage.toFloat() / app.limit.toFloat()).coerceIn(0f, 1f) else 0f
    val isOverLimit = app.usage >= app.limit
    
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )

    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(app.color)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                if (isOverLimit) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = formatMillis(app.usage),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "used today",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    text = "${(progress * 100).toInt()}% of limit",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape),
                color = if (isOverLimit) MaterialTheme.colorScheme.error else app.color,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )
        }
    }
}

@Composable
fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val composition by rememberLottieComposition(LottieCompositionSpec.Url("https://assets10.lottiefiles.com/packages/lf20_glp9alhi.json"))
        LottieAnimation(composition, iterations = LottieConstants.IterateForever, modifier = Modifier.size(200.dp))
        
        Spacer(Modifier.height(24.dp))
        Text(
            "Clean Slate!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "No apps are being tracked. Go to settings to start your journey.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val youtubeEnabled by viewModel.youtubeEnabled.collectAsState(initial = true)
    val instagramEnabled by viewModel.instagramEnabled.collectAsState(initial = true)
    val tiktokEnabled by viewModel.tiktokEnabled.collectAsState(initial = true)

    val youtubeLimit by viewModel.youtubeLimit.collectAsState(initial = 1800000L)
    val instagramLimit by viewModel.instagramLimit.collectAsState(initial = 1800000L)
    val tiktokLimit by viewModel.tiktokLimit.collectAsState(initial = 1800000L)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Text(
                    "Focus Preferences",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }

            item {
                AppSettingItem(
                    name = "YouTube",
                    enabled = youtubeEnabled,
                    limitMs = youtubeLimit,
                    icon = Icons.Rounded.PlayCircle,
                    onEnabledChange = viewModel::updateYoutubeEnabled,
                    onLimitChange = viewModel::updateYoutubeLimit
                )
            }

            item {
                AppSettingItem(
                    name = "Instagram",
                    enabled = instagramEnabled,
                    limitMs = instagramLimit,
                    icon = Icons.Rounded.CameraAlt,
                    onEnabledChange = viewModel::updateInstagramEnabled,
                    onLimitChange = viewModel::updateInstagramLimit
                )
            }

            item {
                AppSettingItem(
                    name = "TikTok",
                    enabled = tiktokEnabled,
                    limitMs = tiktokLimit,
                    icon = Icons.Rounded.MusicNote,
                    onEnabledChange = viewModel::updateTiktokEnabled,
                    onLimitChange = viewModel::updateTiktokLimit
                )
            }
        }
    }
}

@Composable
fun AppSettingItem(
    name: String,
    enabled: Boolean,
    limitMs: Long,
    icon: ImageVector,
    onEnabledChange: (Boolean) -> Unit,
    onLimitChange: (Long) -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Switch(checked = enabled, onCheckedChange = onEnabledChange)
            }

            if (enabled) {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Daily Limit", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatMillis(limitMs), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = (limitMs / 60000).toFloat(),
                    onValueChange = { onLimitChange((it.toLong() * 60000)) },
                    valueRange = 1f..120f,
                    steps = 23
                )
                Text(
                    "Slide to adjust (1 - 120 min)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

data class AppData(val name: String, val usage: Long, val limit: Long, val color: Color = Color.Gray)

fun formatMillis(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val hours = minutes / 60
    return if (hours > 0) {
        "${hours}h ${minutes % 60}m"
    } else {
        "${minutes}m"
    }
}

@Composable
fun PermissionSection(
    isUsageStatsGranted: Boolean,
    isOverlayGranted: Boolean,
    onOpenUsageStats: () -> Unit,
    onOpenOverlay: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Permissions Required",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = "Unscroll needs these permissions to monitor app usage and block addictive content.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            if (!isUsageStatsGranted) {
                PermissionItem(
                    title = "Usage Access",
                    icon = Icons.Rounded.History,
                    onClick = onOpenUsageStats
                )
            }

            if (!isOverlayGranted) {
                PermissionItem(
                    title = "Overlay Permission",
                    icon = Icons.Rounded.Block,
                    onClick = onOpenOverlay
                )
            }
        }
    }
}

@Composable
fun PermissionItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.onErrorContainer,
            contentColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Spacer(Modifier.padding(horizontal = 4.dp))
        Text(text = "Grant $title", fontWeight = FontWeight.Bold)
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MainScreenPreview() {
    UnscrollTheme {
        MainScreen(viewModel = viewModel(factory = MainViewModelFactory(UserPreferencesRepository(LocalContext.current))), onNavigateToSettings = {})
    }
}
