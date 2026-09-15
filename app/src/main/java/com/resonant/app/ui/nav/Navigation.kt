package com.resonant.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.resonant.app.content.LessonData
import com.resonant.app.content.QuizData
import com.resonant.app.ui.chat.ChatScreen
import com.resonant.app.ResonantApp
import com.resonant.app.ui.home.HomeScreen
import com.resonant.app.ui.home.ROUTE_EXIT
import com.resonant.app.ui.onboarding.OnboardingScreen
import com.resonant.app.ui.lessons.LessonScreen
import com.resonant.app.ui.lessons.LessonsScreen
import com.resonant.app.ui.quiz.QuizResultsScreen
import com.resonant.app.ui.quiz.QuizScreen
import com.resonant.app.ui.settings.DebugScreen
import com.resonant.app.ui.settings.SettingsScreen

object Routes {
    const val HOME = "home"
    const val LESSONS = "lessons"
    const val LESSON = "lesson/{lessonId}"
    const val QUIZ = "quiz"
    const val QUIZ_RESULTS = "quiz_results/{correct}/{total}"
    const val CHAT = "chat"
    const val SETTINGS = "settings"
    const val DEBUG = "debug"
    const val ONBOARDING = "onboarding"

    fun lesson(id: String) = "lesson/$id"
    fun quizResults(correct: Int, total: Int) = "quiz_results/$correct/$total"
}

@Composable
fun ResonantNavHost(startDestination: String = Routes.HOME) {
    val navController = rememberNavController()
    val prefs = (LocalContext.current.applicationContext as ResonantApp).container.prefs

    // Single definition of "back". Pops one entry; if this is the root there is
    // nothing to pop, so we land on Home rather than dropping out of the app.
    val goBack: () -> Unit = {
        if (!navController.popBackStack()) {
            navController.navigate(Routes.HOME) {
                popUpTo(Routes.HOME) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onFinished = {
                prefs.onboardingComplete = true
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                }
            })
        }
        composable(Routes.HOME) {
            // "Exit" is a menu item, not a destination — it closes the activity.
            // The spoken confirmation is already playing when we get here, so the
            // finish is deferred by a beat to let it land.
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            HomeScreen(onNavigate = { route ->
                if (route == ROUTE_EXIT) {
                    scope.launch {
                        delay(900)
                        context.findActivity()?.finish()
                    }
                } else {
                    navController.navigate(route)
                }
            })
        }
        composable(Routes.LESSONS) {
            LessonsScreen(
                onOpenLesson = { id -> navController.navigate(Routes.lesson(id)) },
                onBack = goBack
            )
        }
        composable(
            Routes.LESSON,
            arguments = listOf(navArgument("lessonId") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("lessonId")
            val lesson = LessonData.allLessons.firstOrNull { it.id == id } ?: LessonData.binarySearchLesson
            LessonScreen(lesson = lesson, onExit = goBack)
        }
        composable(Routes.QUIZ) {
            QuizScreen(
                quiz = QuizData.sampleQuiz,
                onFinished = { correct, total ->
                    navController.navigate(Routes.quizResults(correct, total)) {
                        popUpTo(Routes.QUIZ) { inclusive = true }
                    }
                },
                // Item 6: leaving a quiz mid-way lands on the lessons list, which is
                // where a user who wants to go study instead actually wants to be.
                onBack = {
                    navController.navigate(Routes.LESSONS) {
                        popUpTo(Routes.QUIZ) { inclusive = true }
                    }
                }
            )
        }
        composable(
            Routes.QUIZ_RESULTS,
            arguments = listOf(
                navArgument("correct") { type = NavType.IntType },
                navArgument("total") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val correct = backStackEntry.arguments?.getInt("correct") ?: 0
            val total = backStackEntry.arguments?.getInt("total") ?: 0
            QuizResultsScreen(
                correct = correct,
                total = total,
                onDone = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.CHAT) {
            ChatScreen(onBack = goBack)
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onOpenDebug = { navController.navigate(Routes.DEBUG) },
                onReplayTutorial = { navController.navigate(Routes.ONBOARDING) },
                onBack = goBack
            )
        }
        composable(Routes.DEBUG) {
            DebugScreen(onBack = goBack)
        }
    }
}

/** Walks the ContextWrapper chain — Compose's LocalContext is not always the Activity. */
private fun android.content.Context.findActivity(): Activity? {
    var ctx: android.content.Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
