package au.edu.canberra.team48.shoppinglist.platform

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content

/**
 * Says what a photo shows, using Google's Gemini model through Firebase AI Logic.
 * "Mandarins", "Milk", "Dish soap": the everyday name, ready to match to the catalogue.
 * Needs the internet and Firebase AI Logic switched on (see README). Returns null when
 * it cannot answer, and the photo screen then uses the words read off the packet.
 */
object ItemRecognizer {
    /** Tried in order: if a model is retired, the next one is used. */
    private val MODELS = listOf("gemini-3.5-flash", "gemini-3.8-flash", "gemini-3.1-flash-lite")

    private const val PROMPT =
        "This photo was taken by someone adding things to a shopping list. " +
            "What grocery or household item is shown? Reply with only its everyday name in " +
            "Australian English as it would be written on a shopping list, one to three words " +
            "(for example: Mandarins, Milk, Bread, Dish soap). " +
            "If no shopping item is shown, reply: unknown"

    fun isAvailable(context: Context): Boolean = FirebaseApp.getApps(context).isNotEmpty()

    suspend fun nameOf(context: Context, uri: Uri): String? {
        if (!isAvailable(context)) return null
        // small is enough to recognise a product, and quick to send
        val bitmap = PhotoStore.decodeUpright(context, uri, 768) ?: return null
        val prompt = content {
            image(bitmap)
            text(PROMPT)
        }
        for (name in MODELS) {
            try {
                val answer = Firebase.ai(backend = GenerativeBackend.googleAI())
                    .generativeModel(name)
                    .generateContent(prompt)
                    .text
                if (!answer.isNullOrBlank()) return answer
            } catch (e: Exception) {
                // no internet, AI Logic not switched on, App Check token not registered, or the model retired
                Log.w("ShoppingList", "Photo recognition with $name did not work", e)
            }
        }
        return null
    }
}
