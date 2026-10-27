package au.edu.canberra.team48.shoppinglist.platform

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/**
 * Debug builds: Firebase App Check with the debug provider. Photo recognition (Firebase
 * AI Logic) is refused without App Check. On first run Logcat prints a line
 * "Enter this debug secret into the allow list ...": add that code in the Firebase
 * console under App Check, Apps, Manage debug tokens. See README.
 */
object AppCheckSetup {
    fun install() {
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
    }
}
