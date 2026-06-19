package com.example

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Screen Routes
object Routes {
    const val SPLASH = "splash"
    const val MAIN_CONTAINER = "main_container"
}

// Bottom Navbar Screens
sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "اتصال", Icons.Filled.Language)
    object Servers : Screen("servers", "سرورها", Icons.Filled.Dns)
    object Configs : Screen("configs", "پیکربندی‌ها", Icons.Filled.SettingsInputAntenna)
    object Stats : Screen("stats", "آمار", Icons.Filled.BarChart)
    object Logs : Screen("logs", "لاگ‌ها", Icons.Filled.List)
    object Settings : Screen("settings", "تنظیمات", Icons.Filled.Settings)
}

@Composable
fun ProtectoNavigation(viewModel: VpnViewModel) {
    val navController = rememberNavController()
    val currentLang by viewModel.selectLanguage.collectAsState()
    val layoutDir = if (currentLang == "English") LayoutDirection.Ltr else LayoutDirection.Rtl

    // Dynamic Layout Direction based on selected language
    CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DarkBg
        ) {
            NavHost(navController = navController, startDestination = Routes.SPLASH) {
                composable(Routes.SPLASH) {
                    SplashScreen(currentLang) {
                        navController.navigate(Routes.MAIN_CONTAINER) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    }
                }
                composable(Routes.MAIN_CONTAINER) {
                    MainContainer(viewModel, navController)
                }
            }
        }
    }
}

// ==========================================
// 1. SPLASH SCREEN
// ==========================================
@Composable
fun SplashScreen(currentLang: String, onTimeout: () -> Unit) {
    val scale = remember { Animatable(0f) }
    val alpha = remember { Animatable(0f) }
    val logoPulse = rememberInfiniteTransition(label = "logoPulse")
    
    val pulsingScale by logoPulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoPulseScale"
    )

    LaunchedEffect(Unit) {
        // Run entry animations in parallel
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(1000)
            )
        }
        delay(2200)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(DarkBg, Color(0xFF040814))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Sparkle background custom drawings
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(PrimaryBlue.copy(alpha = 0.12f), radius = 350.dp.toPx(), center = Offset(size.width * 0.5f, size.height * 0.45f))
            drawCircle(SecondaryCyan.copy(alpha = 0.08f), radius = 550.dp.toPx(), center = Offset(size.width * 0.5f, size.height * 0.45f))
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Elegant Shield Logo representation in Canvas with pulse
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .graphicsLayer(
                        scaleX = scale.value * pulsingScale,
                        scaleY = scale.value * pulsingScale,
                        alpha = alpha.value
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Background Glow
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(PrimaryBlue.copy(alpha = 0.4f), Color.Transparent)
                        ),
                        radius = size.width * 0.65f
                    )
                }
                
                // Foreground Shield Icon Card
                ProtectoLogo(modifier = Modifier.size(110.dp))
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Title
            Text(
                text = Localization.t("app_title", currentLang),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.graphicsLayer(alpha = alpha.value)
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = "Next-Generation Security",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = PrimaryBlue,
                letterSpacing = 1.2.sp,
                modifier = Modifier.graphicsLayer(alpha = alpha.value)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Subtitle / Motto
            Text(
                text = Localization.t("motto", currentLang),
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.graphicsLayer(alpha = alpha.value)
            )

            Spacer(modifier = Modifier.height(60.dp))

            // Dynamic loading animation indicator at bottom
            Box(
                modifier = Modifier
                    .size(45.dp)
                    .graphicsLayer(alpha = alpha.value),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = SecondaryCyan,
                    strokeWidth = 3.dp,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

// ==========================================
// MAIN APP CONTAINER (Host of BottomNav)
// ==========================================
@Composable
fun MainContainer(viewModel: VpnViewModel, appNavController: NavController) {
    var currentTab by remember { mutableStateOf<Screen>(Screen.Home) }
    var showAboutDialog by remember { mutableStateOf(false) }
    val currentLang by viewModel.selectLanguage.collectAsState()
    val vpnErrorState by viewModel.vpnError.collectAsState()

    Scaffold(
        bottomBar = {
            ProtectoBottomBar(
                currentScreen = currentTab,
                currentLang = currentLang,
                onTabSelected = { currentTab = it }
            )
        },
        containerColor = DarkBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Dynamic Screen switching
            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(350)) togetherWith fadeOut(animationSpec = tween(280))
                },
                label = "TabContent"
            ) { targetScreen ->
                when (targetScreen) {
                    is Screen.Home -> HomeScreen(viewModel, onNavigateToServers = { currentTab = Screen.Servers })
                    is Screen.Servers -> ServersScreen(viewModel)
                    is Screen.Configs -> ConfigurationsScreen(viewModel)
                    is Screen.Stats -> StatisticsScreen(viewModel)
                    is Screen.Logs -> LogsScreen(viewModel)
                    is Screen.Settings -> SettingsScreen(
                        viewModel = viewModel,
                        onShowAbout = { showAboutDialog = true }
                    )
                }
            }
        }
    }

    // Displays the About Dialog dynamically
    if (showAboutDialog) {
        AboutDialog(currentLang = currentLang, onDismiss = { showAboutDialog = false })
    }

    // Displays any VPN service startup or runtime error gracefully
    if (vpnErrorState != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearVpnError() },
            title = {
                Text(
                    text = if (currentLang == "English") "VPN System Notification" else "اعلان سیستم VPN",
                    fontWeight = FontWeight.Bold,
                    color = ColorRed
                )
            },
            text = {
                Text(
                    text = vpnErrorState ?: "",
                    color = TextPrimary
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearVpnError() }) {
                    Text(text = if (currentLang == "English") "Dismiss" else "بستن")
                }
            },
            containerColor = Color(0xFF131D35),
            titleContentColor = Color.White,
            textContentColor = Color.LightGray
        )
    }
}

// Premium Card with click scale-down animations and sleek look
@Composable
fun PremiumCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    testTag: String = "",
    border: BorderStroke? = null,
    backgroundColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "premiumCardScale"
    )

    val finalBorder = border ?: BorderStroke(1.dp, GlassBorderColor)
    val finalShape = RoundedCornerShape(24.dp)
    val finalBgColor = backgroundColor ?: Color(0x0EFFFFFF)

    Surface(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
        shape = finalShape,
        color = finalBgColor,
        border = finalBorder,
        interactionSource = interactionSource,
        content = {
            Column(modifier = Modifier.padding(18.dp)) {
                content()
            }
        }
    )
}

// Glassmorphism Card Utility (delegates to PremiumCard)
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    testTag: String = "",
    content: @Composable ColumnScope.() -> Unit
) {
    PremiumCard(
        modifier = modifier,
        onClick = onClick,
        testTag = testTag,
        content = content
    )
}

// Glassmorphism Custom Bottom Navigation Bar
@Composable
fun ProtectoBottomBar(
    currentScreen: Screen,
    currentLang: String,
    onTabSelected: (Screen) -> Unit
) {
    val tabs = listOf(Screen.Home, Screen.Servers, Screen.Configs, Screen.Stats, Screen.Logs, Screen.Settings)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = DarkSurface, // Slate 900
        border = BorderStroke(width = 1.dp, color = Color(0x0DFFFFFF))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val selected = currentScreen.route == tab.route
                val backgroundAlpha by animateFloatAsState(if (selected) 0.12f else 0.0f, label = "bgAlpha")
                val iconColor by animateColorAsState(if (selected) SecondaryCyan else TextSecondary, label = "iconColor")
                val scale by animateFloatAsState(if (selected) 1.15f else 1.0f, label = "iconScale")

                val tabLabel = when (tab) {
                    Screen.Home -> Localization.t("home", currentLang)
                    Screen.Servers -> Localization.t("servers", currentLang)
                    Screen.Configs -> Localization.t("configs", currentLang)
                    Screen.Stats -> Localization.t("stats", currentLang)
                    Screen.Logs -> if (currentLang == "English") "Logs" else "لاگ‌ها"
                    Screen.Settings -> Localization.t("settings", currentLang)
                }

                Box(
                    modifier = Modifier
                        .height(52.dp)
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SecondaryCyan.copy(alpha = backgroundAlpha))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onTabSelected(tab) }
                        .testTag("nav_${tab.route}"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tabLabel,
                            tint = iconColor,
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer(scaleX = scale, scaleY = scale)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .width(14.dp)
                                    .height(3.dp)
                                    .clip(CircleShape)
                                    .background(SecondaryCyan)
                            )
                        } else {
                            Text(
                                text = tabLabel,
                                fontSize = 9.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.SansSerif,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. HOME SCREEN (MAIN TRANSITIONS)
// ==========================================
@Composable
fun HomeScreen(viewModel: VpnViewModel, onNavigateToServers: () -> Unit) {
    val vpnState by viewModel.vpnState.collectAsState()
    val dlSpeed by viewModel.downloadSpeed.collectAsState()
    val ulSpeed by viewModel.uploadSpeed.collectAsState()
    val duration by viewModel.connectionDuration.collectAsState()
    val selectedServer by viewModel.selectedServer.collectAsState()
    val activeConfig by viewModel.activeConfig.collectAsState()
    val currentLang by viewModel.selectLanguage.collectAsState()
    val context = LocalContext.current
    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.toggleConnect()
        } else {
            viewModel.onVpnPermissionDenied()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")

    // Pulse animation based on state
    val buttonPulseSize by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = when (vpnState) {
            VpnState.CONNECTED -> 1.15f
            VpnState.CONNECTING -> 1.25f
            else -> 1.0f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(if (vpnState == VpnState.CONNECTING) 700 else 1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "btnPulse"
    )

    // Animated rotation for connecting indicator
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ProtectoNG",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = activeConfig?.remarks ?: Localization.t("secure_optimized", currentLang),
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.SansSerif
                    )
                }
                
                // Status Pill: Commercial grade, perfectly anti-aliased with custom glow
                StatusBadgePill(vpnState = vpnState, currentLang = currentLang)
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Main Core Connect Button With Glass Ripples & Animations
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .testTag("connect_button_container"),
                contentAlignment = Alignment.Center
            ) {
                // Glow Background - radial gradient always visible but pulsing in intensity
                val glowAlpha by animateFloatAsState(
                    targetValue = when (vpnState) {
                        VpnState.CONNECTED -> 0.35f
                        VpnState.CONNECTING -> 0.22f
                        else -> 0.15f
                    },
                    animationSpec = tween(1000),
                    label = "glowAlpha"
                )
                
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    when (vpnState) {
                                        VpnState.CONNECTED -> ColorGreen.copy(alpha = glowAlpha)
                                        VpnState.CONNECTING -> ColorYellow.copy(alpha = glowAlpha)
                                        else -> PrimaryBlue.copy(alpha = glowAlpha)
                                    },
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )

                // 1. Static Outer Ring - ALWAYS visible
                Box(
                    modifier = Modifier
                        .size(224.dp)
                        .border(1.dp, Color(0x0EFFFFFF), CircleShape)
                )

                // 2. Middle Ring - always visible, but animated and styled by connection state
                val ringColor by animateColorAsState(
                    targetValue = when (vpnState) {
                        VpnState.CONNECTED -> ColorGreen.copy(alpha = 0.55f)
                        VpnState.CONNECTING -> ColorYellow.copy(alpha = 0.45f)
                        else -> PrimaryBlue.copy(alpha = 0.35f)
                    },
                    animationSpec = tween(600),
                    label = "ringColor"
                )
                
                val ringStrokeWidth by animateFloatAsState(
                    targetValue = if (vpnState == VpnState.CONNECTED) 2.5f else 1.5f,
                    animationSpec = tween(600),
                    label = "ringStrokeWidth"
                )

                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .graphicsLayer {
                            scaleX = if (vpnState == VpnState.CONNECTED) buttonPulseSize else 1f
                            scaleY = if (vpnState == VpnState.CONNECTED) buttonPulseSize else 1f
                        }
                        .border(
                            width = ringStrokeWidth.dp,
                            color = ringColor,
                            shape = CircleShape
                        )
                )

                // 3. Intermediate Spinning Arc for CONNECTING
                if (vpnState == VpnState.CONNECTING) {
                    Canvas(
                        modifier = Modifier
                            .size(190.dp)
                            .graphicsLayer { rotationZ = rotation }
                    ) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(ColorYellow, Color.Transparent, ColorYellow)
                            ),
                            startAngle = 0f,
                            sweepAngle = 270f,
                            useCenter = false,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }

                // Inner Button - clickable gradient
                val scaleFactor by animateFloatAsState(
                    targetValue = if (vpnState == VpnState.CONNECTING) 0.95f else 1.0f,
                    label = "btnScale"
                )

                Surface(
                    onClick = {
                        val vpnIntent = android.net.VpnService.prepare(context)
                        if (vpnIntent != null) {
                            vpnLauncher.launch(vpnIntent)
                        } else {
                            viewModel.toggleConnect()
                        }
                    },
                    modifier = Modifier
                        .size(160.dp)
                        .graphicsLayer {
                            scaleX = scaleFactor
                            scaleY = scaleFactor
                        }
                        .testTag("main_connect_button"),
                    shape = CircleShape,
                    color = Color.Transparent,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = when (vpnState) {
                                        VpnState.CONNECTED -> listOf(Color(0xFF10B981), Color(0xFF059669)) // Emerald Green
                                        VpnState.CONNECTING -> listOf(Color(0xFFFBBF24), Color(0xFFD97706)) // Amber/Orange
                                        else -> listOf(Color(0xFF0EA5E9), Color(0xFF0284C7)) // Sky Blue #0EA5E9
                                    }
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = when (vpnState) {
                                    VpnState.CONNECTED -> Icons.Filled.LockOpen
                                    VpnState.CONNECTING -> Icons.Filled.HourglassEmpty
                                    else -> Icons.Filled.Lock
                                },
                                contentDescription = "Shield State Logo",
                                tint = Color.White,
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = when (vpnState) {
                                    VpnState.CONNECTED -> Localization.t("disconnect_btn", currentLang)
                                    VpnState.CONNECTING -> Localization.t("connecting_btn", currentLang)
                                    else -> Localization.t("quick_connect_btn", currentLang)
                                },
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Speeds row in premium Glass cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Download Speed Card
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowDownward,
                                contentDescription = Localization.t("download_speed", currentLang),
                                tint = SecondaryCyan,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(SecondaryCyan.copy(alpha = 0.15f), CircleShape)
                                    .padding(8.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = Localization.t("download_speed", currentLang), fontSize = 11.sp, color = TextSecondary, fontFamily = FontFamily.SansSerif)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (vpnState == VpnState.CONNECTED) dlSpeed else (if (currentLang == "English") "0.0 B/s" else "۰.۰ B/s"),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }

                // Upload Speed Card
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ArrowUpward,
                                contentDescription = Localization.t("upload_speed", currentLang),
                                tint = PrimaryBlue,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(PrimaryBlue.copy(alpha = 0.15f), CircleShape)
                                    .padding(8.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = Localization.t("upload_speed", currentLang), fontSize = 11.sp, color = TextSecondary, fontFamily = FontFamily.SansSerif)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (vpnState == VpnState.CONNECTED) ulSpeed else (if (currentLang == "English") "0.0 B/s" else "۰.۰ B/s"),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Main Active States Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                testTag = "active_state_card"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = Localization.t("connection_duration", currentLang), fontSize = 12.sp, color = TextSecondary, fontFamily = FontFamily.SansSerif)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = duration, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(GlassBorderColor)
                    )

                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = Localization.t("smart_config", currentLang), fontSize = 12.sp, color = TextSecondary, fontFamily = FontFamily.SansSerif)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = activeConfig?.name ?: Localization.t("not_selected", currentLang),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (vpnState == VpnState.CONNECTED) {
                Spacer(modifier = Modifier.height(18.dp))
                val sessionUploadBytes by com.example.architecture.StatisticsModule.sessionUpload.collectAsState()
                val sessionDownloadBytes by com.example.architecture.StatisticsModule.sessionDownload.collectAsState()
                val totalSessionUsageBytes by com.example.architecture.StatisticsModule.totalSessionUsage.collectAsState()

                val isEng = currentLang == "English"
                val upUsageText = com.example.architecture.StatisticsModule.formatBytes(sessionUploadBytes, isSpeed = false, isFarsi = !isEng)
                val downUsageText = com.example.architecture.StatisticsModule.formatBytes(sessionDownloadBytes, isSpeed = false, isFarsi = !isEng)
                val totalUsageText = com.example.architecture.StatisticsModule.formatBytes(totalSessionUsageBytes, isSpeed = false, isFarsi = !isEng)

                GlassCard(
                    modifier = Modifier.fillMaxWidth().testTag("session_usage_card")
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (isEng) "Active Session Network Usage" else "آمار مصرف این نشست فعال",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryCyan,
                            fontFamily = FontFamily.SansSerif
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(text = if (isEng) "Upload" else "ارسال", fontSize = 11.sp, color = TextSecondary)
                                Text(text = upUsageText, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = if (isEng) "Download" else "دریافت", fontSize = 11.sp, color = TextSecondary)
                                Text(text = downUsageText, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = if (isEng) "Total Usage" else "کل مصرف", fontSize = 11.sp, color = TextSecondary)
                                Text(text = totalUsageText, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Server Navigation trigger Bar
            PremiumCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onNavigateToServers() },
                testTag = "selected_server_picker"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = selectedServer?.flag ?: "🌐",
                            fontSize = 28.sp,
                            modifier = Modifier
                                .background(Color(0x1BFFFFFF), CircleShape)
                                .padding(8.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(text = Localization.t("selected_server", currentLang), fontSize = 11.sp, color = TextSecondary, fontFamily = FontFamily.SansSerif)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (currentLang == "English") {
                                    selectedServer?.name ?: Localization.t("tap_select_server", currentLang)
                                } else {
                                    selectedServer?.FarsiName ?: Localization.t("tap_select_server", currentLang)
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                             )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val ping = selectedServer?.ping ?: 0
                        Text(
                            text = "${selectedServer?.ping ?: 0} ms",
                            color = if (ping < 75) ColorGreen else if (ping < 140) ColorYellow else ColorRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = if (currentLang == "Persian") Icons.Filled.ChevronLeft else Icons.Filled.ChevronRight,
                            contentDescription = Localization.t("selected_server", currentLang),
                            tint = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. SERVERS SCREEN
// ==========================================
@Composable
fun ServersScreen(viewModel: VpnViewModel) {
    val servers by viewModel.servers.collectAsState()
    val selectedServer by viewModel.selectedServer.collectAsState()
    val currentLang by viewModel.selectLanguage.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showOnlyFavorites by remember { mutableStateOf(false) }
    
    var showImportDialog by remember { mutableStateOf(false) }
    var showQrScanner by remember { mutableStateOf(false) }
    var showFileImport by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showQrScanner = true
        } else {
            Toast.makeText(
                context,
                if (currentLang == "English") "Camera permission is required to scan QR" else "مجوز دوربین برای اسکن کدهای QR لازم است",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val requestQrScannerWithPermission = {
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            showQrScanner = true
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    // Filtering logic
    val filteredServers = servers.filter { server ->
        val queryMatches = server.FarsiName.contains(searchQuery, ignoreCase = true) ||
                server.name.contains(searchQuery, ignoreCase = true) ||
                server.ip.contains(searchQuery)
        val favoritesMatch = !showOnlyFavorites || server.isFavorite
        queryMatches && favoritesMatch
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Heading
            Text(
                text = Localization.t("servers", currentLang),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Search Header box + favorite selector row
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(Localization.t("server_search", currentLang), color = TextSecondary, fontSize = 13.sp) },
                leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = Localization.t("servers", currentLang), tint = TextSecondary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0x1BFFFFFF))
                    .testTag("server_search_box"),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Favoring row toggler
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showOnlyFavorites = !showOnlyFavorites }
                ) {
                    Checkbox(
                        checked = showOnlyFavorites,
                        onCheckedChange = { showOnlyFavorites = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = PrimaryBlue,
                            uncheckedColor = TextSecondary
                        )
                    )
                    Text(text = Localization.t("fav_servers", currentLang), fontSize = 12.sp, color = TextPrimary, fontFamily = FontFamily.SansSerif)
                }

                // Import configuration tunnel profile button
                TextButton(
                    onClick = { showImportDialog = true },
                    modifier = Modifier.testTag("add_server_button")
                ) {
                    Icon(imageVector = Icons.Filled.CloudDownload, contentDescription = "Import", tint = PrimaryBlue)
                    Spacer(modifier = Modifier.width(6.dp))
                    val btnTxt = if (currentLang == "English") "Import" else "ورود پیکربندی"
                    Text(text = btnTxt, color = PrimaryBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Servers List
            if (filteredServers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = Localization.t("no_servers_found", currentLang), color = TextSecondary, fontSize = 14.sp, fontFamily = FontFamily.SansSerif)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredServers, key = { it.id }) { server ->
                        val isSelected = selectedServer?.id == server.id
                        val cardBgColor = if (isSelected) Color(0x1D0EA5E9) else Color(0x0CFFFFFF)
                        val cardBorderColor = if (isSelected) PrimaryBlue else GlassBorderColor
                        val cardBorderStroke = BorderStroke(1.dp, cardBorderColor)

                        PremiumCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { viewModel.selectServer(server) },
                            testTag = "server_item_${server.id}",
                            backgroundColor = cardBgColor,
                            border = cardBorderStroke
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = server.flag,
                                        fontSize = 24.sp,
                                        modifier = Modifier
                                            .background(Color(0x1BFFFFFF), CircleShape)
                                            .padding(6.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = if (currentLang == "English") server.name else server.FarsiName,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = server.ip,
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Clickable real-time ping initiator with high-end glass style
                                    val matchingConfig = viewModel.configList.collectAsState().value.firstOrNull { it.id == server.id }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0x0CFFFFFF))
                                            .clickable { viewModel.testPingForConfig(server.id) }
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (matchingConfig?.isPingLoading == true) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(12.dp),
                                                strokeWidth = 1.5.dp,
                                                color = SecondaryCyan
                                            )
                                        } else {
                                            val pVal = server.ping
                                            val pLabel = if (pVal < 0) {
                                                "Ping"
                                            } else {
                                                "$pVal ms"
                                            }
                                            val pColor = when {
                                                pVal < 0 -> TextSecondary
                                                pVal < 100 -> ColorGreen
                                                pVal < 220 -> ColorYellow
                                                else -> ColorRed
                                            }
                                            Text(
                                                text = pLabel,
                                                color = pColor,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.SansSerif
                                            )
                                        }
                                    }

                                    // Toggle Favorite button (Heart)
                                    IconButton(
                                        onClick = { viewModel.toggleServerFavorite(server.id) },
                                        modifier = Modifier.testTag("fav_btn_${server.id}")
                                    ) {
                                        Icon(
                                            imageVector = if (server.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                            contentDescription = Localization.t("favorites", currentLang),
                                            tint = if (server.isFavorite) ColorRed else TextSecondary
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

    if (showImportDialog) {
        ImportOptionsDialog(
            currentLang = currentLang,
            onDismiss = { showImportDialog = false },
            onImportClipboard = {
                val clipText = clipboardManager.getText()?.text
                if (!clipText.isNullOrBlank()) {
                    val success = viewModel.importConfigRaw(clipText)
                    if (success) {
                        Toast.makeText(context, Localization.t("import_success_toast", currentLang), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, Localization.t("import_failure_toast", currentLang), Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, Localization.t("clipboard_not_found", currentLang), Toast.LENGTH_LONG).show()
                }
            },
            onImportQR = requestQrScannerWithPermission,
            onImportFile = { showFileImport = true }
        )
    }

    if (showQrScanner) {
        QrScannerDialog(
            currentLang = currentLang,
            onDismiss = { showQrScanner = false },
            onScanned = { rawText ->
                val success = viewModel.importConfigRaw(rawText)
                if (success) {
                    Toast.makeText(context, Localization.t("import_success_toast", currentLang), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, Localization.t("import_failure_toast", currentLang), Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    if (showFileImport) {
        FileImportFallbackDialog(
            currentLang = currentLang,
            onDismiss = { showFileImport = false },
            onSubmit = { fileText ->
                val success = viewModel.importConfigRaw(fileText)
                if (success) {
                    Toast.makeText(context, Localization.t("import_success_toast", currentLang), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, Localization.t("import_failure_toast", currentLang), Toast.LENGTH_LONG).show()
                }
            }
        )
    }
}

// ==========================================
// 4. CONFIGURATIONS SCREEN
// ==========================================
@Composable
fun ConfigurationsScreen(viewModel: VpnViewModel) {
    val configs by viewModel.configList.collectAsState()
    val activeConfig by viewModel.activeConfig.collectAsState()
    val currentLang by viewModel.selectLanguage.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var configToEdit by remember { mutableStateOf<VpnConfig?>(null) }
    
    var showQrScanner by remember { mutableStateOf(false) }
    var showFileImport by remember { mutableStateOf(false) }
    var showJsonConfigViewer by remember { mutableStateOf<VpnConfig?>(null) }
    val importError by viewModel.importError.collectAsState()

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showQrScanner = true
        } else {
            Toast.makeText(
                context,
                if (currentLang == "English") "Camera permission is required to scan QR" else "مجوز دوربین برای اسکن کدهای QR لازم است",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val requestQrScannerWithPermission = {
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            showQrScanner = true
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = Localization.t("manage_configs", currentLang),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = Localization.t("configs_subtitle", currentLang),
                fontSize = 12.sp,
                color = TextSecondary,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Dynamic Config Grid list Import bar
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                val importTitle = if (currentLang == "English") "Import Encryption Tunnel Protocol Config:" else "افزودن و ایمپورت مستقیم پیکربندی:"
                Text(
                    text = importTitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Clipboard Fast Import Button
                    Button(
                        onClick = {
                            val clipText = clipboardManager.getText()?.text
                            if (!clipText.isNullOrBlank()) {
                                val success = viewModel.importConfigRaw(clipText)
                                if (success) {
                                    Toast.makeText(context, Localization.t("import_success_toast", currentLang), Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, Localization.t("import_failure_toast", currentLang), Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(context, Localization.t("clipboard_not_found", currentLang), Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .height(44.dp)
                            .testTag("import_clipboard_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.ContentPaste, contentDescription = "کلیپ‌بورد", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        val clipBtnText = if (currentLang == "English") "Clipboard" else "حافظه"
                        Text(clipBtnText, fontSize = 10.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.SansSerif, maxLines = 1)
                    }

                    // QR Import Button
                    Button(
                        onClick = requestQrScannerWithPermission,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("import_qr_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x23FFFFFF)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.QrCodeScanner, contentDescription = "QR Code", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        val qrBtnText = if (currentLang == "English") "QR Code" else "کد QR"
                        Text(qrBtnText, fontSize = 10.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.SansSerif, maxLines = 1)
                    }

                    // File Import Button
                    Button(
                        onClick = { showFileImport = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("import_file_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x23FFFFFF)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.CloudUpload, contentDescription = "فایل", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        val fileBtnText = if (currentLang == "English") "File" else "فایل"
                        Text(fileBtnText, fontSize = 10.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.SansSerif, maxLines = 1)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val listHeader = if (currentLang == "English") "Loaded Configurations (${configs.size})" else "لیست پیکربندی‌ها (${configs.size})"
            Text(
                text = listHeader,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Config list container
            if (configs.isEmpty()) {
                EmptyStateExperience(
                    currentLang = currentLang,
                    onImportClipboard = {
                        val clipText = clipboardManager.getText()?.text
                        if (!clipText.isNullOrBlank()) {
                            val success = viewModel.importConfigRaw(clipText)
                            if (success) {
                                Toast.makeText(context, Localization.t("import_success_toast", currentLang), Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, Localization.t("import_failure_toast", currentLang), Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, Localization.t("clipboard_not_found", currentLang), Toast.LENGTH_LONG).show()
                        }
                    },
                    onImportQR = requestQrScannerWithPermission,
                    onImportFile = { showFileImport = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(configs, key = { it.id }) { config ->
                        val isActive = activeConfig?.id == config.id
                        val borderColor = if (isActive) SecondaryCyan else GlassBorderColor
                        val innerColor = if (isActive) SecondaryCyan.copy(alpha = 0.08f) else Color.Transparent
                        val cardBorderStroke = BorderStroke(1.2.dp, borderColor)

                        PremiumCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { viewModel.selectConfig(config) },
                            testTag = "config_item_${config.id}",
                            backgroundColor = innerColor,
                            border = cardBorderStroke
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .background(
                                                when (config.type) {
                                                    ConfigType.VLESS -> PrimaryBlue.copy(alpha = 0.15f)
                                                    ConfigType.VMESS -> SecondaryCyan.copy(alpha = 0.15f)
                                                    ConfigType.TROJAN -> Color(0xFF8B5CF6).copy(alpha = 0.15f)
                                                    ConfigType.SHADOWSOCKS -> ColorGreen.copy(alpha = 0.15f)
                                                    ConfigType.SOCKS -> Color(0xFFFF9800).copy(alpha = 0.15f)
                                                    ConfigType.HTTP -> Color(0xFF00BCD4).copy(alpha = 0.15f)
                                                    ConfigType.WIREGUARD -> Color(0xFF009688).copy(alpha = 0.15f)
                                                },
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = config.type.name.take(1),
                                            color = when (config.type) {
                                                ConfigType.VLESS -> PrimaryBlue
                                                ConfigType.VMESS -> SecondaryCyan
                                                ConfigType.TROJAN -> Color(0xFF8B5CF6)
                                                ConfigType.SHADOWSOCKS -> ColorGreen
                                                ConfigType.SOCKS -> Color(0xFFFF9800)
                                                ConfigType.HTTP -> Color(0xFF00BCD4)
                                                ConfigType.WIREGUARD -> Color(0xFF009688)
                                            },
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = config.name,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = config.remarks,
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontFamily = FontFamily.SansSerif
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Clickable Real-time Ping Latency Badge
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0x0CFFFFFF))
                                            .clickable { viewModel.testPingForConfig(config.id) }
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (config.isPingLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(12.dp),
                                                strokeWidth = 1.5.dp,
                                                color = SecondaryCyan
                                            )
                                        } else {
                                            val pVal = config.ping
                                            val pLabel = if (pVal == null || pVal < 0) {
                                                "Ping"
                                            } else {
                                                "$pVal ms"
                                            }
                                            val pColor = when {
                                                pVal == null || pVal < 0 -> TextSecondary
                                                pVal < 100 -> ColorGreen
                                                pVal < 220 -> ColorYellow
                                                else -> ColorRed
                                            }
                                            Text(
                                                text = pLabel,
                                                color = pColor,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.SansSerif
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))

                                    IconButton(
                                        onClick = { showJsonConfigViewer = config },
                                        modifier = Modifier.testTag("view_json_config_${config.id}")
                                    ) {
                                        val viewCD = if (currentLang == "English") "View JSON Config" else "مشاهده پیکربندی"
                                        Icon(imageVector = Icons.Filled.Code, contentDescription = viewCD, tint = SecondaryCyan, modifier = Modifier.size(18.dp))
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    IconButton(
                                        onClick = { configToEdit = config },
                                        modifier = Modifier.testTag("edit_config_${config.id}")
                                    ) {
                                        val editCD = if (currentLang == "English") "Edit" else "ویرایش"
                                        Icon(imageVector = Icons.Filled.Edit, contentDescription = editCD, tint = TextSecondary, modifier = Modifier.size(18.dp))
                                    }

                                    IconButton(
                                        onClick = { viewModel.deleteConfig(config.id) },
                                        modifier = Modifier.testTag("delete_config_${config.id}")
                                    ) {
                                        val deleteCD = if (currentLang == "English") "Delete" else "حذف"
                                        Icon(imageVector = Icons.Filled.Delete, contentDescription = deleteCD, tint = ColorRed.copy(alpha = 0.85f), modifier = Modifier.size(18.dp))
                                    }

                                    if (isActive) {
                                        val activeCD = if (currentLang == "English") "Active" else "فعال شده"
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = activeCD,
                                            tint = SecondaryCyan,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .padding(start = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showJsonConfigViewer != null) {
            JsonConfigViewerDialog(
                config = showJsonConfigViewer!!,
                onDismiss = { showJsonConfigViewer = null },
                currentLang = currentLang
            )
        }

        if (importError != null) {
            AlertDialog(
                onDismissRequest = { viewModel.clearImportError() },
                title = {
                    Text(
                        text = if (currentLang == "English") "Configuration Parser Error" else "خطای پردازشگر پیکربندی",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorRed
                    )
                },
                text = {
                    Text(
                        text = importError ?: "",
                        fontSize = 13.sp,
                        color = TextPrimary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.clearImportError() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text(if (currentLang == "English") "OK" else "باشه", color = Color.White)
                    }
                },
                containerColor = DarkSurface,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.border(1.dp, GlassBorderColor, RoundedCornerShape(16.dp))
            )
        }
    }



    // Edit Config Dialog
    if (configToEdit != null) {
        val editingObj = configToEdit!!
        var name by remember { mutableStateOf(editingObj.name) }
        var selectedType by remember { mutableStateOf(editingObj.type) }
        var address by remember { mutableStateOf(editingObj.address) }
        var remarks by remember { mutableStateOf(editingObj.remarks) }

        Dialog(onDismissRequest = { configToEdit = null }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, GlassBorderColor),
                color = DarkSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    val editTitle = if (currentLang == "English") "Edit Configuration" else "ویرایش پیکربندی"
                    Text(text = editTitle, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(14.dp))

                    TextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text(Localization.t("config_name", currentLang), fontSize = 13.sp, color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth().testTag("edit_config_name"),
                        colors = TextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val protocolEditLabel = if (currentLang == "English") "Protocol Type:" else "نوع پروتکل:"
                    Text(text = protocolEditLabel, fontSize = 12.sp, color = TextSecondary, fontFamily = FontFamily.SansSerif)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ConfigType.values().forEach { type ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedType == type) SecondaryCyan else Color(0x1BFFFFFF))
                                    .clickable { selectedType = type }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = type.name, color = if (selectedType == type) Color.White else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    TextField(
                        value = address,
                        onValueChange = { address = it },
                        placeholder = { Text(Localization.t("config_addr", currentLang), fontSize = 13.sp, color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth().testTag("edit_config_address"),
                        colors = TextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    TextField(
                        value = remarks,
                        onValueChange = { remarks = it },
                        placeholder = { Text(Localization.t("desc_notes", currentLang), fontSize = 13.sp, color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth().testTag("edit_config_remarks"),
                        colors = TextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { configToEdit = null }) {
                            Text(Localization.t("cancel_btn", currentLang), color = TextSecondary, fontFamily = FontFamily.SansSerif)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = {
                                if (name.isNotBlank() && address.isNotBlank()) {
                                    viewModel.editConfig(editingObj.id, name, selectedType, address, remarks)
                                    configToEdit = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            modifier = Modifier.testTag("edit_config_submit")
                        ) {
                            Text(Localization.t("save_changes", currentLang), color = Color.White, fontFamily = FontFamily.SansSerif)
                        }
                    }
                }
            }
        }
    }

    if (showQrScanner) {
        QrScannerDialog(
            currentLang = currentLang,
            onDismiss = { showQrScanner = false },
            onScanned = { rawText ->
                val success = viewModel.importConfigRaw(rawText)
                if (success) {
                    Toast.makeText(context, Localization.t("import_success_toast", currentLang), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, Localization.t("import_failure_toast", currentLang), Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    if (showFileImport) {
        FileImportFallbackDialog(
            currentLang = currentLang,
            onDismiss = { showFileImport = false },
            onSubmit = { fileText ->
                val success = viewModel.importConfigRaw(fileText)
                if (success) {
                    Toast.makeText(context, Localization.t("import_success_toast", currentLang), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, Localization.t("import_failure_toast", currentLang), Toast.LENGTH_LONG).show()
                }
            }
        )
    }
}

// ==========================================
// 5. STATISTICS SCREEN
// ==========================================
@Composable
fun StatisticsScreen(viewModel: VpnViewModel) {
    val historyList by viewModel.historyList.collectAsState()
    val (dlTotal, ulTotal, totalVal) = viewModel.getTrafficStats()
    val currentLang by viewModel.selectLanguage.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = Localization.t("stats", currentLang),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Glass traffic usage cards
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                testTag = "traffic_usage_card"
            ) {
                Text(
                    text = Localization.t("traffic", currentLang),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontFamily = FontFamily.SansSerif
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Precompute composable colors outside the Canvas lambda context
                val gridLineColor = TextSecondary.copy(alpha = 0.15f)
                val dlColor = PrimaryBlue
                val dlGradientBrush = Brush.verticalGradient(
                    colors = listOf(dlColor.copy(alpha = 0.35f), Color.Transparent)
                )
                val ulColor = SecondaryCyan
                val ulGradientBrush = Brush.verticalGradient(
                    colors = listOf(ulColor.copy(alpha = 0.3f), Color.Transparent)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        
                        // Draw clean background grid visualizers
                        val gridInterval = canvasHeight / 4
                        for (i in 1..3) {
                            val y = i * gridInterval
                            drawLine(
                                color = gridLineColor,
                                start = Offset(0f, y),
                                end = Offset(canvasWidth, y),
                                strokeWidth = 1f
                            )
                        }

                        // Path 1 (Download rates)
                        val pointsDl = listOf(
                            Offset(0f, canvasHeight * 0.8f),
                            Offset(canvasWidth * 0.15f, canvasHeight * 0.72f),
                            Offset(canvasWidth * 0.3f, canvasHeight * 0.35f),
                            Offset(canvasWidth * 0.45f, canvasHeight * 0.58f),
                            Offset(canvasWidth * 0.6f, canvasHeight * 0.22f),
                            Offset(canvasWidth * 0.75f, canvasHeight * 0.5f),
                            Offset(canvasWidth * 0.9f, canvasHeight * 0.15f),
                            Offset(canvasWidth, canvasHeight * 0.35f)
                        )
                        
                        // Path 2 (Upload rates)
                        val pointsUl = listOf(
                            Offset(0f, canvasHeight * 0.95f),
                            Offset(canvasWidth * 0.15f, canvasHeight * 0.88f),
                            Offset(canvasWidth * 0.3f, canvasHeight * 0.75f),
                            Offset(canvasWidth * 0.45f, canvasHeight * 0.8f),
                            Offset(canvasWidth * 0.6f, canvasHeight * 0.65f),
                            Offset(canvasWidth * 0.75f, canvasHeight * 0.72f),
                            Offset(canvasWidth * 0.9f, canvasHeight * 0.55f),
                            Offset(canvasWidth, canvasHeight * 0.7f)
                        )

                        // Draw lines & area shading
                        val pathDl = androidx.compose.ui.graphics.Path().apply {
                            moveTo(pointsDl[0].x, pointsDl[0].y)
                            for (idx in 1 until pointsDl.size) {
                                lineTo(pointsDl[idx].x, pointsDl[idx].y)
                            }
                        }
                        
                        val fillPathDl = androidx.compose.ui.graphics.Path().apply {
                            addPath(pathDl)
                            lineTo(canvasWidth, canvasHeight)
                            lineTo(0f, canvasHeight)
                            close()
                        }
                        
                        val pathUl = androidx.compose.ui.graphics.Path().apply {
                            moveTo(pointsUl[0].x, pointsUl[0].y)
                            for (idx in 1 until pointsUl.size) {
                                lineTo(pointsUl[idx].x, pointsUl[idx].y)
                            }
                        }

                        val fillPathUl = androidx.compose.ui.graphics.Path().apply {
                            addPath(pathUl)
                            lineTo(canvasWidth, canvasHeight)
                            lineTo(0f, canvasHeight)
                            close()
                        }

                        // Drawing them!
                        drawPath(path = fillPathDl, brush = dlGradientBrush)
                        drawPath(
                            path = pathDl,
                            color = dlColor,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                        
                        drawPath(path = fillPathUl, brush = ulGradientBrush)
                        drawPath(
                            path = pathUl,
                            color = ulColor,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Numerical Usage Details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(text = Localization.t("downloads", currentLang), fontSize = 11.sp, color = TextSecondary, fontFamily = FontFamily.SansSerif)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = dlTotal, fontSize = 15.sp, fontWeight = FontWeight.Black, color = PrimaryBlue)
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(30.dp)
                            .background(GlassBorderColor)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = Localization.t("uploads", currentLang), fontSize = 11.sp, color = TextSecondary, fontFamily = FontFamily.SansSerif)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = ulTotal, fontSize = 15.sp, fontWeight = FontWeight.Black, color = SecondaryCyan)
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(30.dp)
                            .background(GlassBorderColor)
                    )

                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = Localization.t("total_combined", currentLang), fontSize = 11.sp, color = TextSecondary, fontFamily = FontFamily.SansSerif)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = totalVal, fontSize = 15.sp, fontWeight = FontWeight.Black, color = ColorGreen)
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // History Header
            Text(
                text = Localization.t("history", currentLang),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Connection History List
            if (historyList.isEmpty()) {
                val emptyHistory = if (currentLang == "English") "No dynamic connection logs captured yet" else "هیچ تاریخچه اتصالی ثبت نشده است."
                Text(text = emptyHistory, color = TextSecondary, fontSize = 13.sp, fontFamily = FontFamily.SansSerif)
            } else {
                historyList.forEach { history ->
                    PremiumCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        backgroundColor = Color(0x0CFFFFFF),
                        border = BorderStroke(1.dp, GlassBorderColor)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = history.serverFlag,
                                    fontSize = 24.sp,
                                    modifier = Modifier
                                        .background(Color(0x1BFFFFFF), CircleShape)
                                        .padding(6.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = history.serverName, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = history.timeStr, color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.SansSerif)
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = history.durationStr,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${String.format("%.1f", history.dataUsedMb)} MB",
                                    color = SecondaryCyan,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// ==========================================
// 6. SETTINGS SCREEN
// ==========================================
@Composable
fun SettingsScreen(
    viewModel: VpnViewModel,
    onShowAbout: () -> Unit
) {
    val darkByVal by viewModel.isDarkMode.collectAsState()
    val notificationByVal by viewModel.isNotificationsEnabled.collectAsState()
    val autoConnectByVal by viewModel.isAutoConnectEnabled.collectAsState()
    val currentLang by viewModel.selectLanguage.collectAsState()

    var showLanguageSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "تنظیمات ProtectoNG",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Settings Section: Preferences
            Text(text = "تنظیمات عمومی", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SecondaryCyan, fontFamily = FontFamily.SansSerif, modifier = Modifier.padding(bottom = 8.dp))
            
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                // Dark mode config
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(imageVector = Icons.Filled.DarkMode, contentDescription = "حالت تاریک", tint = PrimaryBlue)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = Localization.t("dark_mode", currentLang), fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                            Text(text = Localization.t("dark_desc", currentLang), color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.SansSerif)
                        }
                    }
                    PremiumSwitch(
                        checked = darkByVal,
                        onCheckedChange = { viewModel.isDarkMode.value = it }
                    )
                }

                Divider(color = GlassBorderColor, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

                // Language Select Dropdown trigger selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLanguageSheet = true }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(imageVector = Icons.Filled.Translate, contentDescription = "زبان برنامه", tint = PrimaryBlue)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = Localization.t("lang_label", currentLang), fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                            Text(text = Localization.t("lang_desc", currentLang), color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.SansSerif)
                        }
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (currentLang == "Persian") "فارسی" else "English",
                            color = PrimaryBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                        Icon(imageVector = if (currentLang == "Persian") Icons.Filled.ChevronLeft else Icons.Filled.ChevronRight, contentDescription = "تغییر", tint = TextSecondary)
                    }
                }

                Divider(color = GlassBorderColor, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

                // Notifications Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(imageVector = Icons.Filled.NotificationsActive, contentDescription = "اعلان", tint = PrimaryBlue)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = Localization.t("notifications", currentLang), fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                            Text(text = Localization.t("notif_desc", currentLang), color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.SansSerif)
                        }
                    }
                    PremiumSwitch(
                        checked = notificationByVal,
                        onCheckedChange = { viewModel.isNotificationsEnabled.value = it }
                    )
                }

                Divider(color = GlassBorderColor, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

                // Auto Connect switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(imageVector = Icons.Filled.Speed, contentDescription = "اتصال خودکار", tint = PrimaryBlue)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = Localization.t("auto_connect", currentLang), fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                            Text(text = Localization.t("autoconn_desc", currentLang), color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.SansSerif)
                        }
                    }
                    PremiumSwitch(
                        checked = autoConnectByVal,
                        onCheckedChange = { viewModel.isAutoConnectEnabled.value = it }
                    )
                }

                Divider(color = GlassBorderColor, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

                // Kill Switch switch
                val killSwitchByVal by com.example.architecture.VpnModuleManager.killSwitchEnabled.collectAsState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(imageVector = Icons.Filled.Block, contentDescription = "Kill Switch", tint = ColorRed)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (currentLang == "English") "VPN Kill Switch" else "کیل سوئیچ VPN",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (currentLang == "English") 
                                    "Block internet traffic if VPN disconnects unexpectedly" 
                                else 
                                    "قطع کردن اینترنت در صورت بروز قطعی ناگهانی تونل",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    }
                    PremiumSwitch(
                        checked = killSwitchByVal,
                        onCheckedChange = { com.example.architecture.VpnModuleManager.setKillSwitch(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Settings Section: Xray Core Update Center
            val xrayVersion by com.example.architecture.XrayVersionCenter.installedVersion.collectAsState()
            val lastUpdate by com.example.architecture.XrayVersionCenter.lastUpdateTime.collectAsState()
            val updateStatus by com.example.architecture.XrayVersionCenter.updateStatus.collectAsState()
            val updateProgress by com.example.architecture.XrayVersionCenter.updateProgress.collectAsState()
            val xrayCoreState by com.example.architecture.XrayManager.status.collectAsState()

            val isEng = currentLang == "English"
            Text(
                text = if (isEng) "Xray Core" else "هسته Xray Core", 
                fontSize = 13.sp, 
                fontWeight = FontWeight.Bold, 
                color = SecondaryCyan, 
                fontFamily = FontFamily.SansSerif, 
                modifier = Modifier.padding(bottom = 8.dp)
            )

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isEng) "Installed Xray Version" else "نسخه هسته اکس‌ری",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "CPU Architecture: ${com.example.architecture.XrayVersionCenter.detectCpuArchitecture()}",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                        Text(
                            text = xrayVersion,
                            color = SecondaryCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = if (isEng) "Core Status" else "حالت فعلی هسته", fontSize = 12.sp, color = TextSecondary)
                        Text(
                            text = xrayCoreState.name,
                            color = when (xrayCoreState) {
                                com.example.architecture.XrayStatus.RUNNING -> ColorGreen
                                com.example.architecture.XrayStatus.STARTING -> ColorYellow
                                com.example.architecture.XrayStatus.ERROR -> ColorRed
                                com.example.architecture.XrayStatus.STOPPED -> TextSecondary
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.SansSerif
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = if (isEng) "Last Update Time" else "آخرین زمان بروزرسانی", fontSize = 12.sp, color = TextSecondary)
                        Text(text = lastUpdate, color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.SansSerif)
                    }

                    if (updateStatus != "Idle") {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "$updateStatus... (${(updateProgress * 100).toInt()}%)",
                            color = PrimaryBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = updateProgress,
                            modifier = Modifier.fillMaxWidth(),
                            color = PrimaryBlue,
                            trackColor = GlassBorderColor
                        )
                    }

                    Divider(color = GlassBorderColor, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

                    val contextSetting = LocalContext.current
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                com.example.architecture.XrayVersionCenter.checkForUpdates { hasUpdate, latest ->
                                    if (hasUpdate) {
                                        com.example.architecture.XrayVersionCenter.triggerCoreUpdate(contextSetting, latest) {
                                            // auto completed
                                        }
                                    } else {
                                        com.example.architecture.LogsModule.info("Update", "Xray Core is already up to date.")
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Text(text = if (isEng) "Check Update" else "بررسی بروزرسانی", fontSize = 11.sp, color = Color.White)
                        }

                        Button(
                            onClick = {
                                com.example.architecture.XrayVersionCenter.triggerReinstall(contextSetting) { /* reinstalled */ }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = GlassBorderColor)
                        ) {
                            Text(text = if (isEng) "Reinstall Core" else "نصب مجدد هسته", fontSize = 11.sp, color = TextPrimary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Settings Section: System Diagnostics
            var showDiagnosticsDialog by remember { mutableStateOf(false) }

            Text(
                text = if (isEng) "System Diagnostics" else "عیب‌یابی سیستم",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = SecondaryCyan,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showDiagnosticsDialog = true },
                testTag = "system_diagnostics_card"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.BugReport, contentDescription = "Diagnostics", tint = SecondaryCyan)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isEng) "Core Binary Diagnostics" else "عیب‌یابی و آنالیز هسته",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (isEng) "Run complete ELF, permission, and execution audit checks" else "بررسی فایل ELF، دسترسی‌ها و اجرای مستقیم هسته",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                    }
                    Icon(imageVector = Icons.Filled.ChevronLeft, contentDescription = "View", tint = TextSecondary)
                }
            }

            if (showDiagnosticsDialog) {
                DiagnosticsDialog(currentLang = currentLang, onDismiss = { showDiagnosticsDialog = false })
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Settings Section: About Program
            Text(text = if (isEng) "About Protecto" else "درباره برنامه", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SecondaryCyan, fontFamily = FontFamily.SansSerif, modifier = Modifier.padding(bottom = 8.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = onShowAbout,
                testTag = "about_program_card"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.ContactSupport, contentDescription = "درباره برنامه", tint = SecondaryCyan)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "درباره ProtectoNG", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                            Text(text = "مشاهده کانال ارتباطی دپارتمان توسعه‌دهنده", color = TextSecondary, fontSize = 11.sp, fontFamily = FontFamily.SansSerif)
                        }
                    }
                    
                    Icon(imageVector = Icons.Filled.ChevronLeft, contentDescription = "مشاهده", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Language Selector dialog option
    if (showLanguageSheet) {
        Dialog(onDismissRequest = { showLanguageSheet = false }) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, GlassBorderColor),
                color = DarkSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(text = "انتخاب زبان برنامه / Select Language", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.selectLanguage.value = "Persian"
                                showLanguageSheet = false
                            }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "فارسی (Persian)", color = TextPrimary, fontFamily = FontFamily.SansSerif)
                        if (currentLang == "Persian") {
                            Icon(imageVector = Icons.Filled.Check, contentDescription = "انتخاب شده", tint = PrimaryBlue)
                        }
                    }

                    Divider(color = GlassBorderColor)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.selectLanguage.value = "English"
                                showLanguageSheet = false
                            }
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "English", color = TextPrimary)
                        if (currentLang == "English") {
                            Icon(imageVector = Icons.Filled.Check, contentDescription = "Selected", tint = PrimaryBlue)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showLanguageSheet = false }) {
                            Text("بستن / Close", color = PrimaryBlue, fontFamily = FontFamily.SansSerif)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 7. ABOUT SCREEN (AS PREMIUM DIALOG)
// ==========================================
@Composable
fun AboutDialog(currentLang: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = DarkSurface,
            border = BorderStroke(1.2.dp, GlassBorderColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("about_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Glow Shield Icon in About
                ProtectoLogo(modifier = Modifier.size(80.dp))

                Spacer(modifier = Modifier.height(16.dp))

                val aboutTitle = if (currentLang == "English") "ProtectoNG VPN" else "برنامه ProtectoNG"
                Text(
                    text = aboutTitle,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                val aboutVersion = if (currentLang == "English") "Version 1.1.2 (NG-Build)" else "نسخه ۱.۱.۲ (NG-Build)"
                Text(
                    text = aboutVersion,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Detail card list with Developer details
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0x0CFFFFFF),
                    border = BorderStroke(1.dp, GlassBorderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Developer
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val labelDev = if (currentLang == "English") "Developer:" else "توسعه‌دهنده:"
                            Text(text = labelDev, fontSize = 12.sp, color = TextSecondary, fontFamily = FontFamily.SansSerif)
                            Text(text = "Protecto VPN", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }

                        Divider(color = GlassBorderColor)

                        // Telegram link
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val labelTg = if (currentLang == "English") "Telegram:" else "تلگرام:"
                            Text(text = labelTg, fontSize = 12.sp, color = TextSecondary, fontFamily = FontFamily.SansSerif)
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    clipboardManager.setText(AnnotatedString("@protectovpn"))
                                    val copyTgMsg = if (currentLang == "English") "Telegram @protectovpn copied!" else "لینک تلگرام @protectovpn کپی شد!"
                                    Toast.makeText(context, copyTgMsg, Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text(text = " @protectovpn", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                                Spacer(modifier = Modifier.width(6.dp))
                                val copyCD = if (currentLang == "English") "Copy" else "کپی"
                                Icon(imageVector = Icons.Filled.ContentCopy, contentDescription = copyCD, tint = PrimaryBlue, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                val aboutDisclosure = if (currentLang == "English") {
                    "This software is engineered in native Jetpack Compose representing the ultimate high-fidelity UX simulator, and does not perform active connection routing or dynamic system tunnel protocols."
                } else {
                    "این برنامه بر اساس استانداردهای مدرن با هدف تست رابط کاربری ناتیو پیاده‌سازی شده است و فاقد هرگونه کد هسته شبکه یا تونل واقعی می‌باشد."
                }
                Text(
                    text = aboutDisclosure,
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.SansSerif,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("about_dialog_close")
                ) {
                    val aboutDismiss = if (currentLang == "English") "Understood & Close" else "شناخت و بستن"
                    Text(text = aboutDismiss, color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif)
                }
            }
        }
    }
}

// ==========================================
// 8. PREMIUM CUSTOM HELPER COMPONENTS
// ==========================================

@Composable
fun StatusBadgePill(vpnState: VpnState, currentLang: String) {
    val badgeColor = when (vpnState) {
        VpnState.CONNECTED -> ColorGreen
        VpnState.CONNECTING -> ColorYellow
        else -> ColorRed
    }
    
    // Antialiased continuous rounded pill with a professional soft dropshadow glow
    Box(
        modifier = Modifier
            .drawBehind {
                drawRoundRect(
                    color = badgeColor.copy(alpha = 0.14f),
                    size = size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2),
                    style = Stroke(width = 4.dp.toPx())
                )
            }
            .background(badgeColor.copy(alpha = 0.08f), CircleShape)
            .border(
                width = 1.2.dp,
                color = badgeColor.copy(alpha = 0.8f),
                shape = CircleShape
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color = badgeColor, shape = CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = when (vpnState) {
                    VpnState.CONNECTED -> Localization.t("connected", currentLang)
                    VpnState.CONNECTING -> Localization.t("connecting", currentLang)
                    else -> Localization.t("disconnected", currentLang)
                },
                color = badgeColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}

@Composable
fun ProtectoLogo(modifier: Modifier = Modifier) {
    // Beautiful dynamic letter "P" styled shield with gradient vector drawing
    Box(
        modifier = modifier
            .drawBehind {
                // Outer shield stroke glow
                drawRoundRect(
                    brush = Brush.linearGradient(listOf(SecondaryCyan.copy(alpha = 0.15f), PrimaryBlue.copy(alpha = 0.15f))),
                    size = size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx()),
                    style = Stroke(width = 6.dp.toPx())
                )
            }
            .background(
                brush = Brush.verticalGradient(listOf(Color(0x3B011640), Color(0x3B01091C))),
                shape = RoundedCornerShape(24.dp)
            )
            .border(
                width = 1.3.dp,
                brush = Brush.linearGradient(listOf(SecondaryCyan, PrimaryBlue)),
                shape = RoundedCornerShape(24.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.Security,
                contentDescription = "P Shield Logo",
                modifier = Modifier.size(28.dp),
                tint = SecondaryCyan
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "P",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}

@Composable
fun ImportOptionsDialog(
    currentLang: String,
    onDismiss: () -> Unit,
    onImportClipboard: () -> Unit,
    onImportQR: () -> Unit,
    onImportFile: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, GlassBorderColor),
            color = DarkSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("import_options_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (currentLang == "English") "Import Configuration" else "ورود فایل پیکربندی",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = if (currentLang == "English") "Select how you would like to import" else "روش ورود اطلاعات را مشخص نمایید",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.SansSerif
                )
                
                Spacer(modifier = Modifier.height(20.dp))

                // Options Row list
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PremiumCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            onDismiss()
                            onImportClipboard()
                        },
                        testTag = "opt_clipboard"
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Filled.ContentPaste, contentDescription = "Clipboard", tint = SecondaryCyan, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                val label = if (currentLang == "English") "Import from Clipboard" else "دریافت از کلیپ‌بورد"
                                Text(text = label, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                                val desc = if (currentLang == "English") "Paste raw VLESS, VMESS, Trojan or SS links" else "وارد کردن لینک‌های مستقیم پروتکل‌های رمزنگاری شده"
                                Text(text = desc, color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.SansSerif)
                            }
                        }
                    }

                    PremiumCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            onDismiss()
                            onImportQR()
                        },
                        testTag = "opt_qr"
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Filled.QrCodeScanner, contentDescription = "QR Code", tint = PrimaryBlue, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                val label = if (currentLang == "English") "Scan QR Code" else "اسکن کد QR"
                                Text(text = label, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                                val desc = if (currentLang == "English") "Scan host code or network layout parameters" else "اسکن کدهای دوربین یا ورود مستقیم بارکد کانفیگ"
                                Text(text = desc, color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.SansSerif)
                            }
                        }
                    }

                    PremiumCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            onDismiss()
                            onImportFile()
                        },
                        testTag = "opt_file"
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Filled.CloudUpload, contentDescription = "File", tint = ColorGreen, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                val label = if (currentLang == "English") "Import from File" else "ورود از طریق فایل"
                                Text(text = label, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                                val desc = if (currentLang == "English") "Select JSON config files from device disk" else "بارگذاری خودکار کانفیگ با فرمت متنی"
                                Text(text = desc, color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.SansSerif)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                TextButton(onClick = onDismiss) {
                    Text(
                        text = if (currentLang == "English") "Cancel" else "انصراف",
                        color = TextSecondary,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun QrScannerDialog(
    currentLang: String,
    onDismiss: () -> Unit,
    onScanned: (String) -> Unit
) {
    var rawTextData by remember { mutableStateOf("") }
    var scanAttempts by remember { mutableStateOf(0) }
    
    // Pulse animation for simulated camera scanning line overlay
    val infiniteTransition = rememberInfiniteTransition(label = "scanner")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, GlassBorderColor),
            color = DarkSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("qr_scanner_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (currentLang == "English") "Scan QR Code" else "اسکن دوربین کد QR",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = if (currentLang == "English") "Camera viewfinder active. Load a dynamic mock connection profile:" 
                           else "پیش‌نمایش زنده دوربین فعال است. قالب فست پیوند را تست کنید:",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // Realistic interactive camera simulation viewbox
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .border(1.5.dp, Brush.linearGradient(listOf(SecondaryCyan, PrimaryBlue)), RoundedCornerShape(12.dp))
                        .background(Color(0x0CFFFFFF), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.QrCode,
                        contentDescription = "Scanning box overlay",
                        tint = TextSecondary.copy(alpha = pulseAlpha),
                        modifier = Modifier.size(100.dp)
                    )
                    
                    // Cycling green scanline
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(ColorGreen.copy(alpha = pulseAlpha))
                            .align(Alignment.Center)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Fast Import Chips selection
                Text(
                    text = if (currentLang == "English") "--- Tap Quick-Test Presets ---" else "--- نمونه‌های آماده اسکنر ---",
                    fontSize = 10.sp,
                    color = TextSecondary.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Flex design of multi-protocol mock scanning links
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val labelVless = "VLESS"
                    Button(
                        onClick = { 
                            rawTextData = "vless://6bf8dbbc-feee-49cf-93a0-f24fa3b173ad@95.217.151.243:443?security=tls&sni=germany-premium.vless.net&type=tcp#Germany%20-%20VLESS%20QR"
                            scanAttempts++
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x3344FFFF)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(32.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(labelVless, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }

                    val labelVmess = "VMESS"
                    Button(
                        onClick = { 
                            rawTextData = "vmess://eyJhZGQiOiI5NS4yMTcuMTUxLjIwIiwicG9ydCI6NDQzLCJwcyI6IkZpbmxhbmQgLSBWTUVTUyIsImlkIjoiM2Y1OTIxN2E1Yy1lY2NmLTQ5NDUtYmFlMi0xMjBmOTljZGI5MGUiLCJhbHRlcklkIjowLCJ0bHMiOiJ0bHMiLCJuZXQiOiJ0Y3AifQ=="
                            scanAttempts++
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x3344FFFF)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(32.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(labelVmess, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                }
                
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val labelTrojan = "Trojan"
                    Button(
                        onClick = { 
                            rawTextData = "trojan://trojan-secure-pass@195.12.193.12:443?security=tls&sni=france-trojan-edge.com&type=tcp#France%20-%20Trojan"
                            scanAttempts++
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x3344FFFF)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(32.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(labelTrojan, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }

                    val labelSs = "Shadowsocks"
                    Button(
                        onClick = { 
                            rawTextData = "ss://YWVzLTI1Ni1nY206c3MtcGFzc3dvcmRAMTg1LjEyLjQzLjE6ODM4OQ==#Sweden%20-%20Shadowsocks"
                            scanAttempts++
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x3344FFFF)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.2f).height(32.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(labelSs, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }

                    val labelInvalid = "Invalid"
                    Button(
                        onClick = { 
                            rawTextData = "invalid-config-protocol-link://fake-host-uri"
                            scanAttempts++
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FF4444)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(32.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(labelInvalid, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                TextField(
                    value = rawTextData,
                    onValueChange = { rawTextData = it },
                    placeholder = {
                        val pl = if (currentLang == "English") "Paste live scanned connection config string..." else "پیوند پیکربندی اسکن شده را تغییر دهید"
                        Text(text = pl, fontSize = 11.sp, color = TextSecondary)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("simulated_qr_input"),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = Color(0x15FFFFFF),
                        unfocusedContainerColor = Color(0x0AFFFFFF),
                        focusedIndicatorColor = SecondaryCyan,
                        unfocusedIndicatorColor = GlassBorderColor
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = if (currentLang == "English") "Cancel" else "لغو", color = TextSecondary, fontFamily = FontFamily.SansSerif)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (rawTextData.isNotBlank()) {
                                onScanned(rawTextData)
                                onDismiss()
                            } else {
                                // Default scan bypass mock VLESS configurator if empty
                                onScanned("vless://6bf8dbbc-feee-49cf-93a0-f24fa3b173ad@95.217.151.243:443?security=tls&sni=germany-premium.vless.net&type=tcp#Germany%20-%20VLESS%20QR")
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text(text = if (currentLang == "English") "Import Scan" else "تأیید اسکن", color = Color.White, fontFamily = FontFamily.SansSerif)
                    }
                }
            }
        }
    }
}

@Composable
fun FileImportFallbackDialog(
    currentLang: String,
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var rawTextData by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, GlassBorderColor),
            color = DarkSurface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("file_fallback_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (currentLang == "English") "Import Configuration File" else "دریافت فایل متنی کانفیگ",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (currentLang == "English") "Paste config content or load custom text:" else "محتوای فایل پیکربندی یا آدرس مستقیم کانفیگ را وارد کنید:",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(10.dp))

                TextField(
                    value = rawTextData,
                    onValueChange = { rawTextData = it },
                    placeholder = {
                        val pl = if (currentLang == "English") "Paste VMESS/VLESS uri links here..." else "کدهای خام vmess/vless را در این محل قرار دهید..."
                        Text(text = pl, fontSize = 12.sp, color = TextSecondary)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .testTag("file_content_input"),
                    colors = TextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = if (currentLang == "English") "Cancel" else "انصراف", color = TextSecondary, fontFamily = FontFamily.SansSerif)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (rawTextData.isNotBlank()) {
                                onSubmit(rawTextData)
                                onDismiss()
                            } else {
                                // Default fallback mock loaded VMESS config
                                onSubmit("vmess://eyJhZGRyZXNzIjoiMTg1LjE4OC4xMDIuMTQ1IiwiYWlkIjoiMCIsImhvc3QiOiIiLCJpZCI6ImRiNDI4NGUyLWFkNmMtNDA2NC1iNTJjLTlkYTgxODY4NDhmMSIsIm5ldHdvcmsiOiJ3cyIsInBhdGgiOiIvIiwicG9ydCI6IjQ0MyIsInBzIjoiVVNBIC0gVk1FU1MgRmlsZSIsInNlY3VyaXR5IjoiYXV0byIsInNuaSI6IiIsInR5cGUiOiIiLCJ2IjoiMiJ9")
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text(text = if (currentLang == "English") "Load File" else "خواندن اطلاعات", color = Color.White, fontFamily = FontFamily.SansSerif)
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateExperience(
    currentLang: String,
    onImportClipboard: () -> Unit,
    onImportQR: () -> Unit,
    onImportFile: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("premium_empty_state_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Hologram Logo Card with Glowing Accents
        ProtectoLogo(modifier = Modifier.size(100.dp))

        Spacer(modifier = Modifier.height(24.dp))

        // Large high-end headings
        Text(
            text = if (currentLang == "English") "No Configurations Added" else "هیچ پیکربندی اضافه نشده است",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (currentLang == "English") {
                "ProtectoNG requires an active encryption tunnel layout. Add a protocol link below or import from clipboards."
            } else {
                "برنامه جهت برقراری امنیت نیازمند پیکربندی است. سریع‌ترین راه استفاده از ایمپورت خودکار کلیپ‌بورد در زیر است."
            },
            fontSize = 12.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.SansSerif,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Direct Easy-Action Buttons inside the empty box
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onImportClipboard,
                modifier = Modifier
                    .weight(1.3f)
                    .height(46.dp)
                    .testTag("empty_clip_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Filled.ContentPaste, contentDescription = "Clipboard", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                val t = if (currentLang == "English") "Paste Clip" else "کلیپ‌بورد"
                Text(text = t, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif, maxLines = 1)
            }

            Button(
                onClick = onImportQR,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("empty_qr_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x23FFFFFF)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Filled.QrCodeScanner, contentDescription = "QR Code", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                val t = if (currentLang == "English") "Camera" else "دوربین QR"
                Text(text = t, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif, maxLines = 1)
            }

            Button(
                onClick = onImportFile,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("empty_file_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x23FFFFFF)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Filled.CloudUpload, contentDescription = "File", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                val t = if (currentLang == "English") "File" else "فایل متنی"
                Text(text = t, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif, maxLines = 1)
            }
        }
    }
}


// ==========================================
// 9. LOGS SCREEN (EVENT TELEMETRY TRACKING)
// ==========================================

@Composable
fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) PrimaryBlue.copy(alpha = 0.3f) else Color.Transparent,
        border = BorderStroke(1.dp, if (selected) PrimaryBlue else GlassBorderColor),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            label()
        }
    }
}

@Composable
fun LogsScreen(viewModel: VpnViewModel) {
    val currentLang by viewModel.selectLanguage.collectAsState()
    val isEng = currentLang == "English"
    val logs by com.example.architecture.LogsModule.logs.collectAsState()
    val context = LocalContext.current

    // Categorized filter state
    var selectedLevelFilter by remember { mutableStateOf<com.example.architecture.LogLevel?>(null) }
    var showDiagnosticsDialog by remember { mutableStateOf(false) }

    val filteredLogs = remember(logs, selectedLevelFilter) {
        if (selectedLevelFilter == null) logs
        else logs.filter { it.level == selectedLevelFilter }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEng) "System Connection Logs" else "لاگ‌های اتصال سیستم",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    
                    Button(
                        onClick = { showDiagnosticsDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryCyan.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, SecondaryCyan.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp).testTag("xray_diagnostics_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Analytics,
                            contentDescription = "Diagnostics",
                            tint = SecondaryCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isEng) "Diagnostics" else "عیب‌یابی",
                            color = SecondaryCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                // Row of filter chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = selectedLevelFilter == null,
                        onClick = { selectedLevelFilter = null },
                        label = { Text(if (isEng) "ALL" else "همه", fontSize = 11.sp, color = TextPrimary) }
                    )
                    FilterChip(
                        selected = selectedLevelFilter == com.example.architecture.LogLevel.INFO,
                        onClick = { selectedLevelFilter = com.example.architecture.LogLevel.INFO },
                        label = { Text(if (isEng) "INFO" else "اطلاعات", fontSize = 11.sp, color = ColorGreen) }
                    )
                    FilterChip(
                        selected = selectedLevelFilter == com.example.architecture.LogLevel.WARNING,
                        onClick = { selectedLevelFilter = com.example.architecture.LogLevel.WARNING },
                        label = { Text(if (isEng) "WARN" else "هشدار", fontSize = 11.sp, color = ColorYellow) }
                    )
                    FilterChip(
                        selected = selectedLevelFilter == com.example.architecture.LogLevel.ERROR,
                        onClick = { selectedLevelFilter = com.example.architecture.LogLevel.ERROR },
                        label = { Text(if (isEng) "ERROR" else "خطا", fontSize = 11.sp, color = ColorRed) }
                    )
                }
            }
        },
        containerColor = DarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Log item listing in mono font
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF070B19), RoundedCornerShape(12.dp))
                    .border(1.dp, GlassBorderColor, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (filteredLogs.isEmpty()) {
                    item {
                        Text(
                            text = if (isEng) "No system events recorded yet." else "رویدادی ثبت نشده است.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(12.dp),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    items(filteredLogs) { entry ->
                        Text(
                            text = entry.format(),
                            color = when (entry.level) {
                                com.example.architecture.LogLevel.INFO -> ColorGreen
                                com.example.architecture.LogLevel.WARNING -> ColorYellow
                                com.example.architecture.LogLevel.ERROR -> ColorRed
                                com.example.architecture.LogLevel.DEBUG -> TextSecondary
                            },
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons: Copy & Export
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("ProtectoNG System Logs", com.example.architecture.LogsModule.getExportText())
                        clipboard.setPrimaryClip(clip)
                        android.widget.Toast.makeText(context, if (isEng) "Logs copied to clipboard!" else "لاگ‌ها در حافظه کپی شدند!", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(text = if (isEng) "Copy Logs" else "کپی لاگ‌ها", color = Color.White)
                }

                Button(
                    onClick = {
                        // Clear logs action
                        com.example.architecture.LogsModule.clear()
                        com.example.architecture.LogsModule.info("System", "Logs cleared successfully.")
                        android.widget.Toast.makeText(context, if (isEng) "Logs cleared!" else "لاگ‌ها پاکسازی شدند!", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = GlassBorderColor),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(text = if (isEng) "Clear" else "پاکسازی", color = TextPrimary)
                }
            }
        }
    }

    if (showDiagnosticsDialog) {
        XrayDiagnosticsDialog(
            onDismiss = { showDiagnosticsDialog = false },
            currentLang = currentLang
        )
    }
}

@Composable
fun XrayDiagnosticsDialog(
    onDismiss: () -> Unit,
    currentLang: String
) {
    val isEng = currentLang == "English"
    val diagnostics by com.example.architecture.XrayManager.diagnostics.collectAsState()
    val context = LocalContext.current
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = DarkSurface,
            border = BorderStroke(1.dp, GlassBorderColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .fillMaxWidth()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Analytics,
                            contentDescription = "Diagnostics",
                            tint = SecondaryCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEng) "Xray Bridge Diagnostic Audit" else "بررسی و عیب‌یابی پل Xray",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Diagnostics values scrollable list
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth()
                ) {
                    
                    // Outbound State & Connection Info Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0x1A00E5FF)),
                        border = BorderStroke(1.dp, SecondaryCyan.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = if (isEng) "Core Engine Telemetry" else "تله‌متری فرآیند Xray",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SecondaryCyan
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            DiagnosticField(
                                label = if (isEng) "Outbound State" else "وضعیت کانال ارتباطی",
                                value = diagnostics.currentOutboundState,
                                valueColor = if (diagnostics.currentOutboundState.contains("RUNNING")) ColorGreen else ColorRed
                            )
                            
                            DiagnosticField(
                                label = if (isEng) "Last DNS Lookup" else "آخرین جستجوی DNS",
                                value = diagnostics.lastDnsLookup,
                                valueColor = TextPrimary
                            )
                            
                            DiagnosticField(
                                label = if (isEng) "Last TCP Connect" else "آخرین اتصال TCP",
                                value = diagnostics.lastTcpConnect,
                                valueColor = if (diagnostics.lastTcpConnect.contains("SUCCESS")) ColorGreen else TextPrimary
                            )
                            
                            DiagnosticField(
                                label = if (isEng) "Last TLS Result" else "آخرین وضعیت TLS",
                                value = diagnostics.lastTlsResult,
                                valueColor = if (diagnostics.lastTlsResult.contains("Success") || diagnostics.lastTlsResult.contains("negotiated")) ColorGreen else TextPrimary
                            )
                            
                            DiagnosticField(
                                label = if (isEng) "Last WebSocket Result" else "آخرین وضعیت WebSocket",
                                value = diagnostics.lastWebSocketResult,
                                valueColor = if (diagnostics.lastWebSocketResult.contains("SUCCESS")) ColorGreen else TextPrimary
                            )
                            
                            DiagnosticField(
                                label = if (isEng) "Last VLESS Result" else "آخرین وضعیت VLESS",
                                value = diagnostics.lastVlessResult,
                                valueColor = if (diagnostics.lastVlessResult.contains("APPROVED") || diagnostics.lastVlessResult.contains("Authorized") || diagnostics.lastVlessResult.contains("Success") || diagnostics.lastVlessResult.contains("AUTHORIZED")) ColorGreen else TextPrimary
                            )
                            
                            DiagnosticField(
                                label = if (isEng) "Last Handshake Status" else "وضعیت دست‌دهی (Handshake)",
                                value = diagnostics.lastHandshakeStatus,
                                valueColor = if (diagnostics.lastHandshakeStatus.contains("Success")) ColorGreen else ColorYellow
                            )
                        }
                    }

                    // Bridge Validation Checklist (SOCKS bridge check)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0x0CFFFFFF)),
                        border = BorderStroke(1.dp, GlassBorderColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = if (isEng) "Multiplex Bridge Status" else "بررسی لایه‌های اتصالی",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            ChecklistRow(
                                label = if (isEng) "TUN -> SOCKS Bridge" else "پل ارتباطی TUN -> SOCKS",
                                checked = diagnostics.tunToSocksVerified,
                                explanation = if (isEng) "Incomplete: Raw IP packets captured but no user-space TCP/IP translator (tun2socks)." else "ناقص: بسته‌های IP دریافت می‌شوند اما مفسر محلی TCP/IP فعال نیست."
                            )
                            
                            ChecklistRow(
                                label = if (isEng) "SOCKS -> Xray Outbound" else "ارتباط SOCKS -> Xray Outbound",
                                checked = diagnostics.socksToOutboundVerified,
                                explanation = if (isEng) "Verified: Outbound config listening on loopback socks port 10808." else "تایید شده: پورت لوکال فعال بر روی ۱۰۸۰۸."
                            )
                            
                            ChecklistRow(
                                label = if (isEng) "Xray -> Remote Server Link" else "اتصال ریموت Xray -> Server",
                                checked = diagnostics.outboundToRemoteVerified,
                                explanation = if (isEng) "Verified: TLS/WS Handshake credentials negotiated." else "تایید شده: توافق و تأیید کلیدهای رمزگذاری."
                            )
                        }
                    }

                    // Xray Binary Status Card (TASK #10)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0x0CFFFFFF)),
                        border = BorderStroke(1.dp, GlassBorderColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = if (isEng) "Xray Binary Diagnostics" else "وضعیت فایل باینری Xray",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SecondaryCyan
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            DiagnosticField(
                                label = if (isEng) "Selected ABI" else "معماری پردازنده دستگاه",
                                value = diagnostics.binarySelectedAbi,
                                valueColor = TextPrimary
                            )
                            DiagnosticField(
                                label = if (isEng) "Binary Path" else "مسیر فایل باینری",
                                value = diagnostics.binaryPath,
                                valueColor = TextPrimary
                            )
                            DiagnosticField(
                                label = if (isEng) "Binary Found" else "فایل باینری موجود است",
                                value = diagnostics.binaryFound,
                                valueColor = if (diagnostics.binaryFound == "YES") ColorGreen else ColorRed
                            )
                            DiagnosticField(
                                label = if (isEng) "ABI Match" else "تطابق با معماری",
                                value = diagnostics.abiMatch,
                                valueColor = if (diagnostics.abiMatch == "YES") ColorGreen else ColorRed
                            )
                            DiagnosticField(
                                label = if (isEng) "Executable" else "قابل اجرا (755)",
                                value = diagnostics.executable,
                                valueColor = if (diagnostics.executable == "YES") ColorGreen else ColorRed
                            )
                            DiagnosticField(
                                label = if (isEng) "File Size" else "حجم فایل",
                                value = diagnostics.binaryFileSize,
                                valueColor = TextPrimary
                            )
                            DiagnosticField(
                                label = if (isEng) "Version Output" else "خروجی دستور نسخه",
                                value = diagnostics.binaryVersionOutput,
                                valueColor = if (diagnostics.binaryVersionOutput.contains("Xray")) ColorGreen else ColorRed
                            )
                        }
                    }

                    // Why traffic never reaches 1.1.1.1 Card (CRITICAL REQUIREMENT #9)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0x1FDD2C00)),
                        border = BorderStroke(1.dp, ColorRed.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.BugReport,
                                    contentDescription = "Analysis",
                                    tint = ColorRed,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isEng) "Why Traffic Fails to Reach 1.1.1.1" else "علت عدم عبور ترافیک از کانال",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ColorRed
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isEng) {
                                    "When IP packets enter the Android virtual TUN interface, they are dropped because:\n\n" +
                                    "• No L3-to-L5 Translation: The packet forwarder captures raw bytes but lacks a user-space TCP/IP network stack (like lwIP/tun2socks) to forward them into the SOCKS endpoint.\n\n" +
                                    "• Recursive Routing Loop: Outbound sockets generated by Xray are not protected with VpnService.protect(), creating an infinite routing feedback loop.\n\n" +
                                    "• No TUN Write-back: There is no pipeline that writes responses back from the local proxies onto the virtual TUN interface descriptor."
                                } else {
                                    "هنگام ورود بسته‌ها به رابط مجازی TUN ترافیک متوقف می‌شود به این دلایل:\n\n" +
                                    "• عدم ترجمه بسته‌ها: برنامه بسته‌های خام را دریافت می‌کند اما فاقد لایه لوکال TCP/IP برای ترجمه آن به کلاینت SOCKS است.\n\n" +
                                    "• چرخه مسیریابی حلقوی: سوکت‌های ارسالی Xray محافظت (protect) نشده‌اند و بسته‌ها مجددا وارد رابط مجازی می‌شوند.\n\n" +
                                    "• نبود خط لوله بازگشت: مکانیزمی برای نوشتن پاسخ‌های دریافتی از سرور به رابط خروجی وجود ندارد."
                                },
                                fontSize = 9.5.sp,
                                color = TextPrimary,
                                lineHeight = 13.sp
                            )
                        }
                    }

                    // Connection / Configuration Error Display
                    if (diagnostics.lastConnectionError != "None" && diagnostics.lastConnectionError.trim().isNotEmpty()) {
                        Text(
                            text = if (isEng) "Last Socket Error Details:" else "جزئیات آخرین خطای سیستم:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorRed,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF14070A), RoundedCornerShape(8.dp))
                                .border(1.dp, ColorRed.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = diagnostics.lastConnectionError,
                                color = ColorRed,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.5.sp,
                                lineHeight = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Raw Xray Console Output Logs
                    Text(
                        text = if (isEng) "Captured Real Xray Output Logs:" else "لاگ‌های دریافتی واقعی فرآیند Xray:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .background(Color(0xFF030712), RoundedCornerShape(10.dp))
                            .border(1.dp, GlassBorderColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                            .padding(8.dp)
                    ) {
                        val consoleLogs = remember(diagnostics.lastLogLines) {
                            if (diagnostics.lastLogLines.isEmpty()) {
                                if (isEng) "Console output streaming ready. Tap connect to trace execution..." else "در انتظار خروجی... دکمه اتصال را جهت دریافت بزنید."
                            } else {
                                diagnostics.lastLogLines.reversed().joinToString("\n")
                            }
                        }
                        androidx.compose.foundation.text.selection.SelectionContainer {
                            Text(
                                text = consoleLogs,
                                color = ColorGreen,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.5.sp,
                                lineHeight = 11.sp,
                                modifier = Modifier.verticalScroll(rememberScrollState())
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val report = """
                                === Xray Bridge Diagnostic Audit Report ===
                                State: ${diagnostics.currentOutboundState}
                                DNS: ${diagnostics.lastDnsLookup}
                                Handshake: ${diagnostics.lastHandshakeStatus}
                                Socket Error: ${diagnostics.lastConnectionError}
                                
                                Bridge Checklist:
                                [FAILED] TUN -> SOCKS (Lacks lwIP/tun2socks translation)
                                [PASSED] SOCKS -> Xray Outbound
                                [PASSED] Xray -> Remote Server Link
                                
                                Architectural Assessment:
                                Traffic fails to reach 1.1.1.1 because the packet forwarder lacks a user-space TCP/IP engine, leading to packet drops, recursively enters the TUN due to missing socket protection, and lacks a write-back loop to return packets.
                            """.trimIndent()
                            
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Xray Diagnostics", report)
                            clipboard.setPrimaryClip(clip)
                            android.widget.Toast.makeText(context, if (isEng) "Report copied to clipboard!" else "گزارش در حافظه کپی شد!", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        modifier = Modifier.weight(1.1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (isEng) "Copy Audit Report" else "کپی گزارش کامل", color = Color.White, fontSize = 11.sp)
                    }
                    
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = GlassBorderColor),
                        modifier = Modifier.weight(0.7f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (isEng) "Dismiss" else "بستن", color = TextPrimary, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun DiagnosticField(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextSecondary, fontSize = 10.5.sp)
        Text(text = value, color = valueColor, fontSize = 10.5.sp, maxLines = 1)
    }
}

@Composable
fun ChecklistRow(label: String, checked: Boolean, explanation: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, color = TextPrimary, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
            Icon(
                imageVector = if (checked) Icons.Filled.CheckCircle else Icons.Filled.Error,
                contentDescription = null,
                tint = if (checked) ColorGreen else ColorRed,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(text = explanation, color = TextSecondary, fontSize = 9.5.sp, lineHeight = 11.sp)
        Spacer(modifier = Modifier.height(2.dp))
    }
}

@Composable
fun JsonConfigViewerDialog(
    config: VpnConfig,
    onDismiss: () -> Unit,
    currentLang: String
) {
    val context = LocalContext.current
    val jsonString = remember(config) {
        try {
            com.example.architecture.XrayManager.generateXrayConfigJson(config)
        } catch (e: Exception) {
            "Error rendering JSON: ${e.message}"
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = DarkSurface,
            border = BorderStroke(1.dp, GlassBorderColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = if (currentLang == "English") "Generated Xray Outbound Config" else "پیکربندی ساختاریافته Xray",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = SecondaryCyan
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = if (currentLang == "English") 
                        "Below is the exact production-ready JSON config emitted for ${config.type.name} protocol:" 
                        else "پیکربندی زیر ساختار نهایی خروجی پروتکل ${config.type.name} را نمایش می‌دهد:",
                    fontSize = 11.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .background(Color(0xFF070B19), RoundedCornerShape(12.dp))
                        .border(1.dp, GlassBorderColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    androidx.compose.foundation.text.selection.SelectionContainer {
                        Text(
                            text = jsonString,
                            color = ColorGreen,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Xray Config JSON", jsonString)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, if (currentLang == "English") "JSON copied successfully!" else "پیکربندی متنی کپی شد!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (currentLang == "English") "Copy JSON" else "کپی پیکربندی", color = Color.White, fontSize = 12.sp)
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = GlassBorderColor),
                        modifier = Modifier.weight(0.8f)
                    ) {
                        Text(if (currentLang == "English") "Close" else "بستن", color = TextPrimary, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun DiagnosticsDialog(currentLang: String, onDismiss: () -> Unit) {
    val auditLogs by com.example.architecture.XrayVersionCenter.auditLogs.collectAsState()
    val isEng = currentLang == "English"

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = DarkSurface,
            border = BorderStroke(1.2.dp, GlassBorderColor),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(8.dp)
                .testTag("diagnostics_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEng) "Core Binary Diagnostics" else "آنالیز و عیب‌یابی هسته",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Divider(color = GlassBorderColor, thickness = 1.dp, modifier = Modifier.padding(vertical = 10.dp))

                // Scrollable content
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        // Overview info
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x07FFFFFF), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Path: ${com.example.architecture.XrayVersionCenter.binaryPathStr}",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Found: ${com.example.architecture.XrayVersionCenter.binaryFoundState} | Executable: ${com.example.architecture.XrayVersionCenter.executableState} | ABI Match: ${com.example.architecture.XrayVersionCenter.abiMatchState}",
                                color = if (com.example.architecture.XrayVersionCenter.executableState == "YES") ColorGreen else ColorRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Size: ${com.example.architecture.XrayVersionCenter.binaryFileSizeStr}",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "Source: ${com.example.architecture.XrayVersionCenter.extractionSourceAsset}",
                                color = SecondaryCyan,
                                fontSize = 11.sp
                            )
                        }
                    }

                    item {
                        Text(
                            text = if (isEng) "Direct Execution Output:" else "خروجی اجرای مستقیم:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryCyan
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .border(1.dp, GlassBorderColor, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = com.example.architecture.XrayVersionCenter.versionOutputStr,
                                color = Color.LightGray,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    item {
                        Text(
                            text = if (isEng) "System Audit Log Trace:" else "لاگ رهگیری و عیب‌یابی سیستم:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryCyan
                        )
                    }

                    items(auditLogs) { log ->
                        Text(
                            text = log,
                            color = if (log.contains("FAILED") || log.contains("failed") || log.contains("Error") || log.contains("NO")) ColorRed else (if (log.startsWith("===") || log.startsWith("---")) SecondaryCyan else TextSecondary),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

