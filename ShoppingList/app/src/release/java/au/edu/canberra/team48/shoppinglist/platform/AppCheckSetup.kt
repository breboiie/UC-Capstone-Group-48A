package au.edu.canberra.team48.shoppinglist.platform

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

/**
 * Release builds: Firebase App Check with Play Integrity, which proves requests come
 * from this app installed from Google Play. Needed for photo recognition (Firebase AI Logic).
 */
object AppCheckSetup {
    fun install() {
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
    }
}
