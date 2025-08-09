package gr.pkcoding.peoplescope

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import gr.pkcoding.peoplescope.presentation.navigation.PeopleScopeNavGraph
import gr.pkcoding.peoplescope.ui.theme.PeopleScopeTheme
import gr.pkcoding.peoplescope.utils.enableImmersiveMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableImmersiveMode()
        setContent {
            PeopleScopeTheme {
                PeopleScopeNavGraph()
            }
        }
    }
}