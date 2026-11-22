package com.tixi.app.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tixi.app.diagnostics.CrashRecorder
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tixi.app.domain.model.ThemeMode
import com.tixi.app.ui.RootViewModel
import com.tixi.app.ui.categories.CategoryScreen
import com.tixi.app.ui.categorymanage.CategoryManageScreen
import com.tixi.app.ui.home.HomeScreen
import com.tixi.app.ui.importflow.ImportHistoryScreen
import com.tixi.app.ui.importflow.ImportReviewScreen
import com.tixi.app.ui.importflow.ImportSourceScreen
import com.tixi.app.ui.importflow.ImportTaskScreen
import com.tixi.app.ui.library.LibraryScreen
import com.tixi.app.ui.onboarding.OnboardingScreen
import com.tixi.app.ui.practice.PracticeScreen
import com.tixi.app.ui.questionedit.QuestionEditScreen
import com.tixi.app.ui.settings.SettingsScreen
import com.tixi.app.ui.theme.TixiMotion
import com.tixi.app.ui.theme.TixiTheme
import com.tixi.app.ui.common.TixiBottomNav
import androidx.compose.foundation.isSystemInDarkTheme

@Composable
fun TixiAppRoot() {
    val context = LocalContext.current
    // 启动期崩溃取证：区分「Hilt 建 ViewModel 失败」与「UI 渲染失败」
    remember { CrashRecorder.stage(context, "TixiAppRoot:before-viewmodel") }
    val viewModel: RootViewModel = hiltViewModel()
    remember { CrashRecorder.stage(context, "TixiAppRoot:viewmodel-ready") }

    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val onboardingComplete by viewModel.onboardingComplete.collectAsStateWithLifecycle()
    remember(onboardingComplete) {
        if (onboardingComplete != null) {
            CrashRecorder.stage(context, "TixiAppRoot:settings-loaded")
        }
    }

    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    TixiTheme(darkTheme = darkTheme) {
        val colors = TixiTheme.colors
        val done = onboardingComplete
        if (done == null) {
            // 读到引导状态前不渲染路由，避免闪一下引导页
            Box(Modifier.fillMaxSize().background(colors.canvas))
            return@TixiTheme
        }
        TixiNavHost(startWithOnboarding = !done, onboardingPending = !done)
    }
}

@Composable
private fun TixiNavHost(startWithOnboarding: Boolean, onboardingPending: Boolean) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val colors = TixiTheme.colors
    val context = LocalContext.current

    LaunchedEffect(currentRoute) {
        currentRoute?.let { CrashRecorder.stage(context, "route:$it") }
    }

    val showTabs = currentRoute == TixiRoutes.HOME || currentRoute == TixiRoutes.SETTINGS

    Column(Modifier.fillMaxSize().background(colors.canvas)) {
        Box(Modifier.weight(1f).statusBarsPadding()) {
            NavHost(
                navController = navController,
                startDestination = if (startWithOnboarding) TixiRoutes.ONBOARDING else TixiRoutes.HOME,
                enterTransition = {
                    slideInHorizontally(tween(TixiMotion.Normal)) { it / 36 } + fadeIn(tween(TixiMotion.Normal))
                },
                exitTransition = { fadeOut(tween(TixiMotion.Fast)) },
                popEnterTransition = { fadeIn(tween(TixiMotion.Fast)) },
                popExitTransition = {
                    slideOutHorizontally(tween(TixiMotion.Normal)) { it / 36 } + fadeOut(tween(TixiMotion.Normal))
                },
            ) {
                composable(
                    route = TixiRoutes.ONBOARDING,
                    arguments = listOf(
                        navArgument("step") { type = NavType.StringType; defaultValue = "1" },
                    ),
                ) {
                    OnboardingScreen(
                        onOpenImport = { navController.navigate(TixiRoutes.IMPORT_SOURCE) },
                        onOpenLibrary = { navController.navigate(TixiRoutes.LIBRARY) },
                        onStartPractice = { navController.navigate(TixiRoutes.CATEGORIES) },
                        onFinished = {
                            navController.navigate(TixiRoutes.HOME) {
                                popUpTo(navController.graph.id) { inclusive = true }
                            }
                        },
                    )
                }

                composable(TixiRoutes.HOME) {
                    HomeScreen(
                        onOpenCategoryList = { navController.navigate(TixiRoutes.CATEGORIES) },
                        onOpenLibrary = { navController.navigate(TixiRoutes.LIBRARY) },
                        onOpenImport = { navController.navigate(TixiRoutes.IMPORT_SOURCE) },
                        onOpenHistory = { navController.navigate(TixiRoutes.IMPORT_HISTORY) },
                        onOpenCategory = { id -> navController.navigate(TixiRoutes.practice(id)) },
                    )
                }

                composable(TixiRoutes.SETTINGS) {
                    SettingsScreen()
                }

                composable(TixiRoutes.CATEGORIES) {
                    CategoryScreen(
                        onBack = { navController.popBackStack() },
                        onStartPractice = { id -> navController.navigate(TixiRoutes.practice(id)) },
                        onOpenImport = { navController.navigate(TixiRoutes.IMPORT_SOURCE) },
                    )
                }

                composable(
                    route = TixiRoutes.PRACTICE,
                    arguments = listOf(navArgument("categoryId") { type = NavType.LongType }),
                ) {
                    PracticeScreen(
                        onBack = { navController.popBackStack() },
                        onFinished = {
                            navController.navigate(TixiRoutes.HOME) {
                                popUpTo(TixiRoutes.HOME) { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                    )
                }

                composable(TixiRoutes.LIBRARY) {
                    LibraryScreen(
                        onOpenImport = { navController.navigate(TixiRoutes.IMPORT_SOURCE) },
                        onOpenCategoryManage = { navController.navigate(TixiRoutes.CATEGORY_MANAGE) },
                        onEditQuestion = { id -> navController.navigate(TixiRoutes.questionEdit(id)) },
                        onCreateQuestion = {
                            navController.navigate(
                                TixiRoutes.questionEdit(com.tixi.app.ui.questionedit.QuestionEditViewModel.NEW_QUESTION_ID),
                            )
                        },
                    )
                }

                composable(
                    route = TixiRoutes.QUESTION_EDIT,
                    arguments = listOf(navArgument("questionId") { type = NavType.LongType }),
                ) {
                    QuestionEditScreen(onBack = { navController.popBackStack() })
                }

                composable(TixiRoutes.CATEGORY_MANAGE) {
                    CategoryManageScreen(onBack = { navController.popBackStack() })
                }

                composable(TixiRoutes.IMPORT_SOURCE) {
                    ImportSourceScreen(
                        onBack = { navController.popBackStack() },
                        onOpenTask = { navController.navigate(TixiRoutes.IMPORT_TASK) },
                        onOpenSettings = { navController.navigate(TixiRoutes.SETTINGS) },
                    )
                }

                composable(TixiRoutes.IMPORT_TASK) {
                    ImportTaskScreen(
                        onBack = { navController.popBackStack() },
                        onOpenReview = { draftId ->
                            navController.navigate(TixiRoutes.importReview(draftId)) {
                                popUpTo(TixiRoutes.IMPORT_SOURCE) { inclusive = true }
                            }
                        },
                        onOpenSettings = { navController.navigate(TixiRoutes.SETTINGS) },
                    )
                }

                composable(
                    route = TixiRoutes.IMPORT_REVIEW,
                    arguments = listOf(navArgument("draftId") { type = NavType.LongType }),
                ) {
                    ImportReviewScreen(
                        onBack = { navController.popBackStack() },
                        onImported = {
                            if (onboardingPending) {
                                navController.navigate(TixiRoutes.onboarding(4)) {
                                    popUpTo(navController.graph.id) { inclusive = true }
                                }
                            } else {
                                navController.navigate(TixiRoutes.HOME) {
                                    popUpTo(navController.graph.id) { inclusive = true }
                                }
                            }
                        },
                    )
                }

                composable(TixiRoutes.IMPORT_HISTORY) {
                    ImportHistoryScreen(onBack = { navController.popBackStack() })
                }
            }
        }

        if (showTabs) {
            TixiBottomNav(
                selectedIndex = if (currentRoute == TixiRoutes.SETTINGS) 1 else 0,
                onSelect = { index ->
                    val target = if (index == 0) TixiRoutes.HOME else TixiRoutes.SETTINGS
                    navController.navigate(target) {
                        popUpTo(TixiRoutes.HOME) { saveState = true; inclusive = false }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }
    }
}
