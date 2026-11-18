package com.mind.app.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mind.app.capture.MindCaptureWidgetProvider
import com.mind.app.core.designsystem.component.LocalMindHazeState
import com.mind.app.core.designsystem.component.MindMistBackdrop
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import com.mind.app.feature.collections.CollectionDetailScreen
import com.mind.app.feature.collections.CollectionsScreen
import com.mind.app.feature.favourites.FavouritesScreen
import com.mind.app.feature.home.HomeScreen
import com.mind.app.feature.memorydetail.MemoryDetailScreen
import com.mind.app.feature.search.SearchScreen
import com.mind.app.feature.settings.SettingsScreen

internal const val ACTION_PICK_MEDIA_CAPTURE = "com.mind.app.action.PICK_MEDIA"

@Composable
fun MindAppShell(
    launchAction: String? = null,
    launchPrefillUrl: String? = null,
    launchMemoryId: String? = null,
    onConsumeLaunchAction: () -> Unit = {},
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    var pendingHomeCaptureAction by remember { mutableStateOf<String?>(null) }
    var pendingHomePrefillUrl by remember { mutableStateOf<String?>(null) }
    var isHomeOverlaySheetActive by remember { mutableStateOf(false) }

    LaunchedEffect(launchAction, launchPrefillUrl, launchMemoryId) {
        when (launchAction) {
            MindCaptureWidgetProvider.ACTION_OPEN_MEMORY -> {
                val targetId = launchMemoryId
                if (!targetId.isNullOrBlank()) {
                    navController.navigate(MindDestination.MemoryDetail(targetId)) {
                        launchSingleTop = true
                    }
                }
                onConsumeLaunchAction()
            }
            MindCaptureWidgetProvider.ACTION_OPEN_SEARCH -> {
                navController.navigate(MindDestination.Search()) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
                onConsumeLaunchAction()
            }
            MindCaptureWidgetProvider.ACTION_OPEN_SETTINGS -> {
                navController.navigate(MindDestination.Settings) {
                    launchSingleTop = true
                }
                onConsumeLaunchAction()
            }
            MindCaptureWidgetProvider.ACTION_OPEN_VOICE_CAPTURE,
            MindCaptureWidgetProvider.ACTION_OPEN_NOTE_CAPTURE,
            MindCaptureWidgetProvider.ACTION_OPEN_LINK_CAPTURE,
            ACTION_PICK_MEDIA_CAPTURE -> {
                pendingHomePrefillUrl = launchPrefillUrl
                pendingHomeCaptureAction = launchAction
                navController.navigate(MindDestination.Home) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
                onConsumeLaunchAction()
            }
            else -> Unit
        }
    }

    val showBottomBar = (currentDestination == null || TopLevelTab.entries.any { currentDestination.matchesTab(it) }) &&
        !isHomeOverlaySheetActive
    val navEaseOut = remember { CubicBezierEasing(0.20f, 1.0f, 0.32f, 1.0f) }
    val hazeState = rememberHazeState()

    MindMistBackdrop(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = MindDestination.Home,
                        enterTransition = {
                            val fromTab = initialState.destination.tabOrderIndex()
                            val toTab = targetState.destination.tabOrderIndex()
                            if (fromTab != null && toTab != null) {
                                val dir = if (toTab >= fromTab) 1 else -1
                                fadeIn(tween(260, easing = navEaseOut)) +
                                    slideInHorizontally(tween(340, easing = navEaseOut)) { dir * (it / 6) } +
                                    scaleIn(initialScale = 0.94f, animationSpec = tween(340, easing = navEaseOut))
                            } else {
                                fadeIn(tween(260, easing = navEaseOut)) +
                                    slideInVertically(tween(340, easing = navEaseOut)) { it / 10 } +
                                    scaleIn(initialScale = 0.93f, animationSpec = tween(340, easing = navEaseOut))
                            }
                        },
                        exitTransition = {
                            val fromTab = initialState.destination.tabOrderIndex()
                            val toTab = targetState.destination.tabOrderIndex()
                            if (fromTab != null && toTab != null) {
                                val dir = if (toTab >= fromTab) 1 else -1
                                fadeOut(tween(190)) +
                                    slideOutHorizontally(tween(300, easing = navEaseOut)) { -dir * (it / 8) } +
                                    scaleOut(targetScale = 0.95f, animationSpec = tween(300, easing = navEaseOut))
                            } else {
                                fadeOut(tween(200)) +
                                    scaleOut(targetScale = 0.95f, animationSpec = tween(280, easing = navEaseOut))
                            }
                        },
                        popEnterTransition = {
                            val fromTab = initialState.destination.tabOrderIndex()
                            val toTab = targetState.destination.tabOrderIndex()
                            if (fromTab != null && toTab != null) {
                                val dir = if (toTab >= fromTab) 1 else -1
                                fadeIn(tween(250, easing = navEaseOut)) +
                                    slideInHorizontally(tween(320, easing = navEaseOut)) { dir * (it / 6) } +
                                    scaleIn(initialScale = 0.94f, animationSpec = tween(320, easing = navEaseOut))
                            } else {
                                fadeIn(tween(240, easing = navEaseOut)) +
                                    scaleIn(initialScale = 0.95f, animationSpec = tween(300, easing = navEaseOut))
                            }
                        },
                        popExitTransition = {
                            val fromTab = initialState.destination.tabOrderIndex()
                            val toTab = targetState.destination.tabOrderIndex()
                            if (fromTab != null && toTab != null) {
                                val dir = if (toTab >= fromTab) 1 else -1
                                fadeOut(tween(190)) +
                                    slideOutHorizontally(tween(300, easing = navEaseOut)) { -dir * (it / 8) } +
                                    scaleOut(targetScale = 0.95f, animationSpec = tween(300, easing = navEaseOut))
                            } else {
                                fadeOut(tween(200)) +
                                    slideOutVertically(tween(300, easing = navEaseOut)) { it / 10 } +
                                    scaleOut(targetScale = 0.93f, animationSpec = tween(300, easing = navEaseOut))
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .hazeSource(state = hazeState),
                    ) {
                            composable<MindDestination.Home> {
                                HomeScreen(
                                    pendingExternalCaptureAction = pendingHomeCaptureAction,
                                    pendingExternalPrefillUrl = pendingHomePrefillUrl,
                                    onConsumeExternalCaptureAction = {
                                        pendingHomeCaptureAction = null
                                        pendingHomePrefillUrl = null
                                    },
                                    onOverlaySheetActiveChanged = { active ->
                                        isHomeOverlaySheetActive = active
                                    },
                                    onOpenSearch = {
                                        navController.navigate(MindDestination.Search()) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    onOpenSettings = { navController.navigate(MindDestination.Settings) },
                                    onOpenCollections = {
                                        navController.navigate(MindDestination.Collections) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    onOpenFavourites = {
                                        navController.navigate(MindDestination.Favourites) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    onOpenCategory = { categoryId ->
                                        navController.navigate(MindDestination.Search(categoryId = categoryId))
                                    },
                                    onOpenMemory = { memoryId ->
                                        navController.navigate(MindDestination.MemoryDetail(memoryId))
                                    },
                                )
                            }

                            composable<MindDestination.Collections> {
                                CollectionsScreen(
                                    onOpenCategorySearch = { categoryId ->
                                        navController.navigate(MindDestination.Search(categoryId = categoryId))
                                    },
                                    onOpenCollection = { collectionId ->
                                        navController.navigate(MindDestination.CollectionDetail(collectionId))
                                    },
                                )
                            }

                            composable<MindDestination.CollectionDetail> {
                                CollectionDetailScreen(
                                    onBack = { navController.popBackStack() },
                                    onOpenMemory = { memoryId ->
                                        navController.navigate(MindDestination.MemoryDetail(memoryId))
                                    },
                                )
                            }

                            composable<MindDestination.Search> {
                                SearchScreen(
                                    onOpenMemory = { memoryId ->
                                        navController.navigate(MindDestination.MemoryDetail(memoryId))
                                    },
                                )
                            }

                            composable<MindDestination.Favourites> {
                                FavouritesScreen(
                                    onOpenMemory = { memoryId ->
                                        navController.navigate(MindDestination.MemoryDetail(memoryId))
                                    },
                                )
                            }

                            composable<MindDestination.MemoryDetail> {
                                MemoryDetailScreen(
                                    onBack = { navController.popBackStack() },
                                    onOpenMemory = { relatedId ->
                                        navController.navigate(MindDestination.MemoryDetail(relatedId))
                                    },
                                )
                            }

                            composable<MindDestination.Settings> {
                                SettingsScreen(
                                    onBack = { navController.popBackStack() },
                                )
                            }
                        }

                        CompositionLocalProvider(LocalMindHazeState provides hazeState) {
                            AnimatedVisibility(
                                visible = showBottomBar,
                                modifier = Modifier.align(Alignment.BottomCenter),
                                enter = fadeIn() + slideInVertically { it / 2 },
                                exit = fadeOut() + slideOutVertically { it / 2 },
                            ) {
                                MindBottomBar(
                                    currentDestination = currentDestination,
                                    onSelectTab = { tab ->
                                        navController.navigate(tab.destination) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
}

private fun NavDestination?.tabOrderIndex(): Int? {
    if (this == null) return null
    return when {
        hasRoute<MindDestination.Home>() -> 0
        hasRoute<MindDestination.Collections>() -> 1
        hasRoute<MindDestination.Search>() -> 2
        hasRoute<MindDestination.Favourites>() -> 3
        else -> null
    }
}
