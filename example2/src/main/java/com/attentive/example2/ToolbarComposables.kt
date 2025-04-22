package com.attentive.example2

import android.app.Activity
import android.widget.Toolbar
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.getColor
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.attentive.example2.ui.theme.AttentiveDarkYellow
import com.attentive.example2.ui.theme.BonniPink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleToolbar(
    title: String,
    actions: @Composable RowScope.() -> Unit = {},
    navController: NavController
) {
    TopAppBar(
        title = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 56.dp, end = 56.dp), // Add padding to account for navigationIcon and actions
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.bonni_logo),
                    contentDescription = "Toolbar Image",
                    modifier = Modifier.size(40.dp) // Adjust size as needed
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = BonniPink,
            titleContentColor = Color.Black
        ),
        navigationIcon = {
            IconButton(
                onClick = {
                    navController.navigateUp()
                }
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = actions // Ensure the actions are properly displayed
    )
}
//    @Preview
//    @Composable
//    fun SimpleToolbarPreview() {
//        SimpleToolbar("My Toolbar", {}, navController by rememberNavController())
//    }
