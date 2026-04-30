package com.example.nam.utils

import androidx.navigation.NavController

object Navigation {
    fun navigateToUserProfile(userId: String, navController: NavController) {
        navController.navigate("profile/$userId")
    }

    fun navigateToPostSearch(navController: NavController) {
        navController.navigate("post_search")
    }
}