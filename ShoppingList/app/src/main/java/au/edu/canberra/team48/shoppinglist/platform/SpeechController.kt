package au.edu.canberra.team48.shoppinglist.platform

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/** Why listening ended without words, so the screen can say something useful. */
enum class SpeechProblem {
    /** Nothing was heard, or nothing that sounded like words. */
    NOT_HEARD,
    /** This phone has no speech service (common on emulators without Google Play). */
    UNAVAILABLE,
    /** The speech service needs the internet and there is none. */
    NO_INTERNET,
}

/**
 * Android's built-in speech recognition, shown inside our own screen rather than
 * Google's pop-up. Listening stops by itself when the person goes quiet, so nobody
 * has to press Stop. Returns every guess the engine makes; the matching code picks
 * the likely items and the person confirms by tapping a picture.
 */
class SpeechController(private val context: Context) {
    private var recognizer: SpeechRecognizer? = null
    private var finished = true
    private val main = Handler(Looper.getMainLooper())
    private var session = 0

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun start(
        onPartial: (String) -> Unit,
        onResults: (List<String>) -> Unit,
        onFailed: (SpeechProblem) -> Unit,
    ) {
        cancel()
        if (!isAvailable()) {
            onFailed(SpeechProblem.UNAVAILABLE); return
        }
        val r = runCatching { SpeechRecognizer.createSpeechRecognizer(context) }.getOrNull()
        if (r == null) {
            onFailed(SpeechProblem.UNAVAILABLE); return
        }
        finished = false
        val mine = ++session
        recognizer = r

        fun fail(p: SpeechProblem) {
            if (finished || mine != session) return
            finished = true
            onFailed(p)
        }

        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}

            override fun onPartialResults(partialResults: Bundle?) {
                if (finished || mine != session) return
                partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()?.takeIf { it.isNotBlank() }?.let(onPartial)
            }

            override fun onResults(results: Bundle?) {
                if (finished || mine != session) return
                val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                    .filter { it.isNotBlank() }
                if (list.isEmpty()) {
                    fail(SpeechProblem.NOT_HEARD)
                } else {
                    finished = true
                    onResults(list)
                }
            }

            override fun onError(error: Int) {
                fail(
                    when (error) {
                        SpeechRecognizer.ERROR_NETWORK -> SpeechProblem.NO_INTERNET
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SpeechProblem.UNAVAILABLE
                        else -> SpeechProblem.NOT_HEARD
                    }
                )
            }
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
        }
        runCatching { r.startListening(intent) }.onFailure { fail(SpeechProblem.UNAVAILABLE) }

        // Safety net: a few speech services never report back. After 15 seconds ask it to
        // finish with what it has; if there is still nothing 5 seconds later, give up
        // so the screen never sits on "Listening" for ever.
        main.postDelayed({ if (!finished && mine == session) recognizer?.stopListening() }, 15_000)
        main.postDelayed({
            if (!finished && mine == session) {
                recognizer?.cancel()
                fail(SpeechProblem.NOT_HEARD)
            }
        }, 20_000)
    }

    /** The person pressed Stop early: finish listening and deliver what was heard. */
    fun finish() {
        recognizer?.stopListening()
    }

    /** Leaving the screen: throw everything away. */
    fun cancel() {
        finished = true
        session++
        recognizer?.let {
            runCatching { it.cancel() }
            runCatching { it.destroy() }
        }
        recognizer = null
    }
}
