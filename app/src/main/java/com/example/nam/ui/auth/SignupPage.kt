package com.example.nam.ui.auth

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.nam.R
import com.example.nam.ui.theme.LocalCustomColors

@Composable
fun SignupPage(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel
) {

    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var displayName by remember {
        mutableStateOf("")
    }

    val authState = authViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(authState.value.authState) {
        when(authState.value.authState){
            is AuthState.Authenticated -> navController.navigate("home")
            is AuthState.Error -> {
                val errorState = authState.value.authState as AuthState.Error
                Toast.makeText(context, errorState.message, Toast.LENGTH_SHORT).show()
            }
            else -> Unit
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = context.getString(R.string.signup_page),
            fontSize = 32.sp,
            color = LocalCustomColors.current.textPrimary
        )

        Spacer(modifier = Modifier.size(16.dp))

        // Display Name
        OutlinedTextField(
            value = displayName,
            onValueChange = {
                displayName = it
            },
            label = {
                Text(
                    text = context.getString(R.string.display_name),
                    color = LocalCustomColors.current.textPrimary
                )
            }
        )

        Spacer(modifier = Modifier.size(8.dp))

        // Email
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
            },
            label = {
                Text(
                    text = context.getString(R.string.email),
                    color = LocalCustomColors.current.textPrimary
                )
            }
        )

        Spacer(modifier = Modifier.size(8.dp))

        // Password
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
            },
            label = {
                Text(
                    text = context.getString(R.string.password),
                    color = LocalCustomColors.current.textPrimary
                )
            },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        Spacer(modifier = Modifier.size(16.dp))

        // Signup button
        Button(
            onClick = {
                authViewModel.signup(email, password, displayName)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = LocalCustomColors.current.secondaryBackground,
            ),
        ) {
            Text(
                text = context.getString(R.string.create_account),
                color = LocalCustomColors.current.textPrimary
            )
        }

        Spacer(modifier = Modifier.size(8.dp))

        // Navigate to sign up
        TextButton(
            onClick = {
                navController.navigate("login")
            }
        ) {
            Text(
                text = context.getString(R.string.already_have_account),
                color = LocalCustomColors.current.textPrimary
            )
        }
    }
}