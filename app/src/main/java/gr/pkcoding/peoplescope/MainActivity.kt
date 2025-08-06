package gr.pkcoding.peoplescope

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import gr.pkcoding.peoplescope.presentation.navigation.PeopleScopeNavGraph
import gr.pkcoding.peoplescope.ui.theme.PeopleScopeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PeopleScopeTheme {
                PeopleScopeNavGraph()
            }
        }
    }
}