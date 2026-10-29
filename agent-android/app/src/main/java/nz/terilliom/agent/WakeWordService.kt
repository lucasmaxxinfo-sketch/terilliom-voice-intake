package nz.terilliom.agent

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.content.Context
import android.os.Bundle
import java.util.Locale

class WakeWordService : Service() {
    private var recognizer: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var running = false

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(
            NotificationChannel("agent_listener", "Agent listening", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, "agent_listener") else Notification.Builder(this)
        startForeground(7, notification.setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Agent is listening").setContentText("Say “Oi Agent” to give a command.")
            .setContentIntent(open).setOngoing(true).build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        running = true
        startRecognition()
        return START_STICKY
    }

    private fun startRecognition() {
        if (!running) return
        if (!SpeechRecognizer.isRecognitionAvailable(this)) { stopSelf(); return }
        try {
            recognizer?.destroy()
            recognizer = SpeechRecognizer.createSpeechRecognizer(this)
            recognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) { retry() }
                override fun onResults(results: Bundle?) {
                    val words = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                    val wake = words.firstOrNull { it.lowercase(Locale.getDefault()).contains("oi agent") || it.lowercase(Locale.getDefault()).contains("hey agent") }
                    if (wake != null) {
                        getSharedPreferences("agent", Context.MODE_PRIVATE).edit().putBoolean("wake_enabled", false).apply()
                        running = false
                        recognizer?.destroy(); recognizer = null
                        val launch = Intent(this@WakeWordService, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            .putExtra("agent_wake_detected", true)
                        startActivity(launch)
                        stopSelf()
                    } else retry()
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            recognizer?.startListening(intent)
        } catch (_: Exception) { retry() }
    }

    private fun retry() {
        recognizer?.cancel()
        handler.postDelayed({ startRecognition() }, 700)
    }

    override fun onDestroy() {
        running = false
        handler.removeCallbacksAndMessages(null)
        recognizer?.destroy(); recognizer = null
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}