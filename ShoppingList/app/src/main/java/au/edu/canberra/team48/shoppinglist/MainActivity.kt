package au.edu.canberra.team48.shoppinglist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import au.edu.canberra.team48.shoppinglist.platform.AppCheckSetup
import au.edu.canberra.team48.shoppinglist.ui.AppRoot
import com.google.firebase.FirebaseApp
import au.edu.canberra.team48.shoppinglist.ui.ShoppingTheme

/** The only activity. Every screen is drawn by Compose inside it. */
class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // App Check: Firebase AI Logic (photo recognition) only answers apps that prove who they are.
        if (FirebaseApp.getApps(this).isNotEmpty()) runCatching { AppCheckSetup.install() }
        setContent {
            ShoppingTheme {
                AppRoot(vm)
            }
        }
    }

    /** Back on screen: catch up with the other phone at once. */
    override fun onStart() {
        super.onStart()
        vm.onAppShown()
    }

    override fun onStop() {
        vm.onAppHidden()
        super.onStop()
    }
}
