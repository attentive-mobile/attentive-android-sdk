package com.attentive.bonni

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
import androidx.compose.ui.res.colorResource
import androidx.navigation.NavController
import com.attentive.bonni.ui.theme.BonniPink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleToolbar(
    title: String,
    actions: @Composable RowScope.() -> Unit = {},
    navController: NavController,
) {
    TopAppBar(
        title = {
            Text(text = title)
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = BonniPink,
                titleContentColor = Color.Black,
            ),
        navigationIcon = {
            IconButton(
                onClick = {
                    navController.navigateUp()
                },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    tint = colorResource(id = R.color.attentive_black),
                    contentDescription = "Back",
                )
            }
        },
        actions = actions, // Ensure the actions are properly displayed
    )
}
//    @Preview
//    @Composable
//    fun SimpleToolbarPreview() {
//        SimpleToolbar("My Toolbar", {}, navController by rememberNavController())
//    }
