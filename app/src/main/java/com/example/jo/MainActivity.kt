package com.example.jo

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import java.util.*
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.media.AudioManager
import android.hardware.camera2.CameraManager
import android.net.Uri
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.*
import androidx.lifecycle.lifecycleScope
import android.net.ConnectivityManager
import android.net.NetworkInfo





class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var wakeWordRecognizer: SpeechRecognizer


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Request microphone permission
        requestMicrophonePermission()

        setContent {
            JoApp { startListening() }
        }

        // Initialize Speech Recognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        setupSpeechRecognition()

        // Initialize Text-To-Speech (TTS)
        textToSpeech = TextToSpeech(this, this)
    
    }

    // Request microphone permission
    private fun requestMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }
    }
    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.CAMERA), 1)
        }
    }

    // Setup speech recognition listener
    private fun setupSpeechRecognition() {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech input recognized"
                    else -> "Speech recognition error"
                }
                Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_SHORT).show()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.firstOrNull()?.let { recognizedText ->
                    Toast.makeText(applicationContext, "You said: $recognizedText", Toast.LENGTH_LONG).show()
                    respondToSpeech(recognizedText)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }


    // Start listening for speech
    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
        }
        speechRecognizer.startListening(intent)
    }
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private suspend fun askQuestionToGPT(question: String) {
        // Create a ChatRequest object
        val request = ChatRequest(
            model = "gpt-3.5-turbo",  // Model name
            messages = listOf(
                Message(role = "system", content = "You are a helpful assistant."),
                Message(role = "user", content = question)  // The user's question
            ),
            max_tokens = 150
        )

        try {
            // Call the Retrofit API with the correct argument (ChatRequest object)
            val response = RetrofitInstance.api.getChatCompletion(request)

            // Access the 'choices' field and extract the assistant's reply
            val answer = response.choices.firstOrNull()?.message?.content?.trim()

            if (answer != null) {
                speak(answer)  // Assuming you have a speak() function to speak the answer
            } else {
                speak("Sorry, I couldn't get an answer.")
            }
        } catch (e: Exception) {
            Log.e("JoApp", "Error while requesting GPT: ${e.message}")
            speak("Error: ${e.message}")
        }
    }





    // Respond to recognized speech using TTS
    private fun respondToSpeech(text: String) {
        Log.d("JoApp", "Received command: $text") // Log recognized speech

        when {
            text.contains("how are you", ignoreCase = true) -> speak("I'm doing great, thank you for asking!")
            text.contains("what is your name", ignoreCase = true) -> speak("I'm Jo, your assistant.")
            text.contains("hello", ignoreCase = true) -> speak("Hello! How can I assist you today?")
            text.contains("tell me a joke", ignoreCase = true) -> tellJoke()
            text.contains("i am sad", ignoreCase = true) -> speak("I'm sorry to hear that. Do you want to talk about it?")
            text.contains("i am happy", ignoreCase = true) -> speak("That's great! I'm glad you're feeling good.")
            listOf("hi", "hi jo").any { text.contains(it, ignoreCase = true) }  -> speak("Hi Boss")


            text.contains("open YouTube", ignoreCase = true) -> openYouTube()
            text.contains("open WhatsApp", ignoreCase = true) -> openWhatsAppUsingUri()
            text.contains("open camera", ignoreCase = true) -> openCamera()
            text.contains("turn on Bluetooth", ignoreCase = true) -> toggleBluetooth(true)
            text.contains("turn off Bluetooth", ignoreCase = true) -> toggleBluetooth(false)
            text.contains("increase volume", ignoreCase = true) -> adjustVolume(true)
            text.contains("decrease volume", ignoreCase = true) -> adjustVolume(false)
            text.contains("turn on flashlight", ignoreCase = true) -> toggleFlashlight(true)
            text.contains("turn off flashlight", ignoreCase = true) -> toggleFlashlight(false)
            text.contains("hey", ignoreCase = true) -> {
                Log.d("JoApp", "Command starts with 'hey'... proceeding with GPT request.")
                // Use lifecycle-aware coroutine scope tied to MainActivity
                lifecycleScope.launch {
                    askQuestionToGPT(text)  // Calls GPT when asking a question
                }
            }
            else -> speak("Sorry, No matter with me")
        }
    }
    private fun openYouTube() {
        // Try opening YouTube directly through the URL scheme
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com"))
        val youtubeIntent = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)

        if (youtubeIntent != null) {
            // If an app that can handle the intent (YouTube or a browser) is found
            startActivity(intent)
            speak("Opening YouTube...")
        } else {
            // If no app is found to handle the URL (shouldn't normally happen)
            speak("YouTube app not found. Opening in browser instead.")
            startActivity(intent)
        }
    }

    private fun openWhatsAppUsingUri() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://wa.me/")  // Opens WhatsApp main screen
        }

        try {
            startActivity(intent)
            speak("Opening WhatsApp...")
        } catch (e: Exception) {
            speak("WhatsApp is not installed on your device.")
        }
    }
    private fun tellJoke() {
        val jokes = listOf(
            "Why don’t skeletons fight each other? They don’t have the guts.",
            "I told my computer I needed a break, now it won’t stop sending me Kit-Kats.",
            "Why don’t programmers like nature? It has too many bugs.",
            "Why was the math book sad? Because it had too many problems."
        )
        val randomJoke = jokes.random()
        speak(randomJoke)
    }



    private fun openApp(packageName: String) {
        try {
            // Attempt to get the package info for WhatsApp
            val intent = packageManager.getLaunchIntentForPackage(packageName)

            if (intent != null) {
                // If intent is not null, open the app
                startActivity(intent)
                speak("Opening WhatsApp...")
            } else {
                // If WhatsApp is not found, speak the message
                speak("WhatsApp not found.")
            }
        } catch (e: PackageManager.NameNotFoundException) {
            // Catch exception if package is not found
            speak("WhatsApp name is not found.")
        }
    }
    private fun openCamera() {
        // Check if the camera permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission()  // You might need to request the permission here
            return
        }

        val intent = Intent("android.media.action.IMAGE_CAPTURE")
        startActivity(intent)
        speak("Opening camera...")
    }

    private fun toggleBluetooth(enable: Boolean) {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter != null) {
            if (enable) {
                bluetoothAdapter.enable()
                speak("Bluetooth is now ON")
            } else {
                bluetoothAdapter.disable()
                speak("Bluetooth is now OFF")
            }
        } else {
            speak("Bluetooth is not supported on this device.")
        }
    }
    private fun adjustVolume(increase: Boolean) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val direction = if (increase) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        audioManager.adjustVolume(direction, AudioManager.FLAG_SHOW_UI)
        speak("Volume adjusted")
    }
    private fun toggleFlashlight(enable: Boolean) {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList[0]
        cameraManager.setTorchMode(cameraId, enable)
        speak(if (enable) "Flashlight is ON" else "Flashlight is OFF")
    }
    // Function to make Jo speak
    private fun speak(text: String) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }


    // Initialize TTS
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.language = Locale.US
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
        textToSpeech.stop()
        textToSpeech.shutdown()
    }
}


@Composable
fun JoApp(onButtonClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "JO",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onButtonClick) {
            Text(text = "Speak my dear..", fontSize = 18.sp)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewJoApp() {
    JoApp {}
}
