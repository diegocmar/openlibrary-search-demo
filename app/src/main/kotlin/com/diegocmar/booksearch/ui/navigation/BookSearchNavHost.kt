package com.diegocmar.booksearch.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.diegocmar.booksearch.ui.about.AboutRoute
import com.diegocmar.booksearch.ui.detail.DetailRoute
import com.diegocmar.booksearch.ui.search.SearchRoute
import kotlinx.serialization.Serializable

@Serializable
data object SearchDestination

@Serializable
data object AboutDestination

@Serializable
data class DetailDestination(val bookId: String, val title: String)

@Composable
fun BookSearchNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = SearchDestination) {
        composable<SearchDestination> {
            SearchRoute(
                onBookClick = { book ->
                    navController.navigate(DetailDestination(book.id, book.title))
                },
                onInfoClick = { navController.navigate(AboutDestination) }
            )
        }
        composable<AboutDestination> {
            AboutRoute(onBack = { navController.popBackStack() })
        }
        composable<DetailDestination> { entry ->
            val destination = entry.toRoute<DetailDestination>()
            DetailRoute(
                bookId = destination.bookId,
                title = destination.title,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
