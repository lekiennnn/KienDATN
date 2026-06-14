package com.example.nam.utils

import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth

object Navigation {
    fun navigateToUserProfile(userId: String, navController: NavController?) {
        val endAccount = FirebaseAuth.getInstance().currentUser?.uid.toString()
        if (userId != endAccount) {
            navController?.navigate("profile/$userId")
        } else {
            navController?.navigate("profile")
        }
    }

    fun navigateToPostSearch(navController: NavController) {
        navController.navigate("post_search")
    }
}