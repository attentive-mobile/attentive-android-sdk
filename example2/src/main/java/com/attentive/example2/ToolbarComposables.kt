package com.attentive.example2

import android.app.Activity
import android.widget.Toolbar
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat.getColor
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.attentive.example2.ui.theme.AttentiveDarkYellow

@OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SimpleToolbar(title: String, actions: @Composable RowScope.() -> Unit = {}, navController: NavController) {
        TopAppBar(
            title = { Text(title) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = AttentiveDarkYellow,
                titleContentColor = Color.White
            ),
            actions = actions,
            navigationIcon = {
                IconButton(
                        onClick = {
                        navController.navigateUp()
                    }
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )
    }

//    @Preview
//    @Composable
//    fun SimpleToolbarPreview() {
//        SimpleToolbar("My Toolbar", {}, navController by rememberNavController())
//    }
