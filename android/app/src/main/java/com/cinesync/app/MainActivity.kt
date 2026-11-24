package com.cinesync.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cinesync.app.data.Session
import com.cinesync.app.ui.*

private const val HOME = "home?tab={tab}"
private fun home(tab: Int) = "home?tab=$tab"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CineTheme { Surface(Modifier.fillMaxSize()) { AppNav() } } }
    }
}

@Composable
private fun AppNav() {
    val nav = rememberNavController()
    fun toHome(tab: Int) = nav.navigate(home(tab)) { popUpTo(nav.graph.id) { inclusive = true } }
    fun toLogin() = nav.navigate("login") { popUpTo(nav.graph.id) { inclusive = true } }
    val longArg = { name: String -> navArgument(name) { type = NavType.LongType } }

    NavHost(nav, startDestination = "splash") {
        composable("splash") {
            SplashScreen(
                onFinish = {
                    if (Session.loggedIn) {
                        toHome(0)
                    } else {
                        toLogin()
                    }
                }
            )
        }
        composable("login") { LoginScreen(onLoggedIn = { toHome(0) }, onRegister = { nav.navigate("register") }, onForgot = { nav.navigate("forgot") }) }
        composable("register") { RegisterScreen(onDone = { toHome(0) }, onBack = { nav.popBackStack() }) }
        composable("forgot") { ForgotScreen(onBack = { nav.popBackStack() }) }
        composable(HOME, arguments = listOf(navArgument("tab") { type = NavType.IntType; defaultValue = 0 })) {
            HomeScreen(nav, it.arguments?.getInt("tab") ?: 0, onLogout = { Session.clear(); toLogin() })
        }
        composable("movie/{id}", arguments = listOf(longArg("id"))) {
            MovieDetailScreen(it.arguments!!.getLong("id"), nav)
        }
        composable("seats/{showId}", arguments = listOf(longArg("showId"))) {
            SeatScreen(it.arguments!!.getLong("showId"), onBack = { nav.popBackStack() }, onBooked = { toHome(1) })
        }
        composable("outing/{id}", arguments = listOf(longArg("id"))) {
            OutingDetailScreen(it.arguments!!.getLong("id"), onBack = { nav.popBackStack() })
        }
    }
}
