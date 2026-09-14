package com.resonant.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.resonant.app.content.LessonData
import com.resonant.app.content.QuizData
import com.resonant.app.ui.chat.ChatScreen
import com.resonant.app.ui.home.HomeScreen
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

    fun lesson(id: String) = "lesson/$id"
    fun quizResults(correct: Int, total: Int) = "quiz_results/$correct/$total"
}

@Composable
fun ResonantNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(onNavigate = { route -> navController.navigate(route) })
        }
        composable(Routes.LESSONS) {
            LessonsScreen(onOpenLesson = { id -> navController.navigate(Routes.lesson(id)) })
        }
        composable(
            Routes.LESSON,
            arguments = listOf(navArgument("lessonId") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("lessonId")
            val lesson = LessonData.allLessons.firstOrNull { it.id == id } ?: LessonData.binarySearchLesson
            LessonScreen(lesson = lesson, onExit = { navController.popBackStack() })
        }
        composable(Routes.QUIZ) {
            QuizScreen(
                quiz = QuizData.sampleQuiz,
                onFinished = { correct, total ->
                    navController.navigate(Routes.quizResults(correct, total)) {
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
            ChatScreen()
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onOpenDebug = { navController.navigate(Routes.DEBUG) })
        }
        composable(Routes.DEBUG) {
            DebugScreen()
        }
    }
}
