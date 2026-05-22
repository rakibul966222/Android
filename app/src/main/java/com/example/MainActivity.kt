package com.example

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import com.example.ui.components.JarvisAppContent
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.JarvisViewModel
import java.util.Locale

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    private val viewModel: JarvisViewModel by viewModels()
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Supported edge-to-edge full screen overlays
        enableEdgeToEdge()

        // Initialize Native Text-To-Speech (TTS)
        try {
            tts = TextToSpeech(this, this)
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to construct TextToSpeech", e)
        }

        setContent {
            MyApplicationTheme {
                // Speech-to-Text standard intents launcher in Compose
                val speechLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        val spokenTextList = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                        val spokenText = spokenTextList?.firstOrNull() ?: ""
                        if (spokenText.isNotBlank()) {
                            viewModel.updatePrompt(spokenText)
                            viewModel.sendMessage()
                        }
                    } else {
                        Log.w("MainActivity", "Voice prompt recognizer returned empty / aborted.")
                    }
                }

                // Handle system trigger to speak
                LaunchedEffect(Unit) {
                    viewModel.ttsSpeakTrigger.collect { textToSpeak ->
                        speakOut(textToSpeak)
                    }
                }

                JarvisAppContent(
                    viewModel = viewModel,
                    onLaunchStt = {
                        try {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Establish Voice Link with JARVIS...")
                            }
                            speechLauncher.launch(intent)
                        } catch (e: Exception) {
                            Toast.makeText(this, "Voice recognition is not supported on this device.", Toast.LENGTH_SHORT).show()
                            Log.e("MainActivity", "Launch Speech Recognizer intent failed", e)
                        }
                    }
                )
            }
        }
    }

    // TextToSpeech Interface Initializations
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("MainActivity", "Language is not supported or missing speech datasets.")
            } else {
                isTtsInitialized = true
                tts?.setPitch(1.05f)   // Cybernetic, slightly raised pitch
                tts?.setSpeechRate(1.1f) // Slightly faster speech pace for advanced intelligent feeling
            }
        } else {
            Log.e("MainActivity", "TTS Initialization failed.")
        }
    }

    private fun speakOut(text: String) {
        if (isTtsInitialized && tts != null) {
            // Flush past speech and read the new sentence immediately
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "JARVIS_TTS_SESSION")
        } else {
            Log.w("MainActivity", "TTS is not fully initialized. Ignoring speak trigger.")
        }
    }

    override fun onDestroy() {
        // Shutdown text to speech gracefully to release resources
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
