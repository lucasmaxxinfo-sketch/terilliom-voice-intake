package nz.terilliom.agent

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.app.ActivityCompat
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var status: TextView
    private lateinit var transcript: TextView
    private lateinit var input: EditText
    private lateinit var tts: TextToSpeech
    private val prefs by lazy { getSharedPreferences("agent", Context.MODE_PRIVATE) }
    private var pendingAction: (() -> Unit)? = null
    private var pendingDescription: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        buildUi()
        requestPermissionsIfNeeded()
        show("Ready. Say “Oi Agent” to begin, or tap the microphone.")
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(24, 32, 24, 24)
            setBackgroundColor(0xFF101426.toInt())
        }
        val title = TextView(this).apply {
            text = "AGENT"
            textSize = 30f
            setTextColor(0xFF8BE9FD.toInt())
            gravity = Gravity.CENTER
        }
        val subtitle = TextView(this).apply {
            text = "Your personal action assistant"
            textSize = 14f
            setTextColor(0xFFD7DCEF.toInt())
            gravity = Gravity.CENTER
        }
        status = TextView(this).apply {
            textSize = 17f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 28, 0, 12)
        }
        transcript = TextView(this).apply {
            textSize = 14f
            setTextColor(0xFFB9C5DE.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 4, 0, 12)
        }
        val mic = Button(this).apply {
            text = "🎙  OI, AGENT"
            textSize = 18f
            setOnClickListener { listen() }
        }
        input = EditText(this).apply {
            hint = "Or type an instruction"
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF9AA6C2.toInt())
            setSingleLine(false)
        }
        val run = Button(this).apply {
            text = "Run instruction"
            setOnClickListener { handle(input.text.toString().trim()) }
        }
        val tasks = Button(this).apply {
            text = "Today's tasks"
            setOnClickListener { showTasks() }
        }
        val notes = Button(this).apply {
            text = "Notes"
            setOnClickListener { showNotes() }
        }
        listOf<View>(title, subtitle, status, transcript, mic, input, run, tasks, notes).forEach {
            root.addView(it, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = 10
            })
        }
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun requestPermissionsIfNeeded() {
        val wanted = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33) wanted.add(Manifest.permission.POST_NOTIFICATIONS)
        val missing = wanted.filter { ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) ActivityCompat.requestPermissions(this, missing.toTypedArray(), 100)
    }

    override fun onInit(result: Int) {
        if (result == TextToSpeech.SUCCESS) tts.language = Locale.getDefault()
    }

    private fun say(text: String) {
        show(text)
        if (::tts.isInitialized) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "agent-utterance")
    }

    private fun show(text: String) { if (::status.isInitialized) status.text = text }

    private fun listen() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionsIfNeeded(); show("Microphone permission is needed."); return
        }
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Say: Oi Agent, then your instruction")
            }
            startActivityForResult(intent, 101)
        } catch (_: Exception) { say("Voice recognition is not available on this device.") }
    }

    @Deprecated("Android speech result callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101 && resultCode == RESULT_OK) {
            val spoken = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            transcript.text = spoken
            val command = spoken.replaceFirst(Regex("(?i)^\\s*(oi\\s+)?agent[,: ]*"), "").trim()
            if (command.isNotBlank()) handle(command) else say("Ready.")
        }
    }

    private fun handle(raw: String) {
        val command = raw.trim()
        if (command.isBlank()) return
        val lower = command.lowercase(Locale.getDefault())
        transcript.text = command
        when {
            lower.startsWith("search maps for ") || lower.startsWith("open google maps and search for ") -> {
                val address = command.substringAfter("for ", "").trim()
                val uri = Uri.parse("geo:0,0?q=" + Uri.encode(address))
                launchOrConfirm("Open Maps and search for $address?") {
                    startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage("com.google.android.apps.maps"))
                    say("Opening Maps.")
                }
            }
            lower.startsWith("navigate to ") || lower.startsWith("start navigation to ") -> {
                val address = command.substringAfter("to ", "").trim()
                val uri = Uri.parse("google.navigation:q=" + Uri.encode(address))
                launchOrConfirm("Start navigation to $address?") {
                    startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage("com.google.android.apps.maps"))
                    say("Starting navigation.")
                }
            }
            lower.startsWith("open chatgpt") || lower.startsWith("open chat gpt") -> {
                launchOrConfirm("Open ChatGPT?") {
                    val launch = packageManager.getLaunchIntentForPackage("com.openai.chatgpt")
                    if (launch != null) { startActivity(launch); say("ChatGPT opened. Start voice chat in ChatGPT; I can't reliably press its in-app voice controls yet.") }
                    else say("ChatGPT isn't installed.")
                }
            }
            lower.startsWith("remind me in ") -> createReminderFromCommand(command)
            lower.startsWith("remind me at ") -> createReminderAt(command)
            lower.startsWith("add task ") || lower.startsWith("task ") -> {
                val task = command.substringAfter(" ").removePrefix(" ").trim()
                saveList("tasks", task)
                say("Task saved.")
            }
            lower.startsWith("note ") || lower.startsWith("remember ") -> {
                val note = command.substringAfter(" ").trim()
                saveList("notes", note)
                say("Noted.")
            }
            lower.startsWith("show tasks") || lower == "today's tasks" -> showTasks()
            lower.startsWith("show notes") -> showNotes()
            lower.startsWith("add calendar event ") || lower.startsWith("schedule ") -> {
                val details = command.substringAfter("event ").takeIf { lower.startsWith("add calendar event ") }
                    ?: command.removePrefix("schedule ").trim()
                launchOrConfirm("Open Calendar to add: $details?") {
                    val intent = Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
                        .putExtra(CalendarContract.Events.TITLE, details)
                    startActivity(intent)
                    say("Calendar opened. Check the event details and save it.")
                }
            }
            lower == "yes" || lower == "yep" || lower == "ok" || lower == "confirm" || lower == "send it" -> {
                val action = pendingAction
                if (action != null) { pendingAction = null; action.invoke() } else say("Nothing is waiting for confirmation.")
            }
            lower == "no" || lower == "nope" || lower == "cancel" -> {
                pendingAction = null; say("Cancelled.")
            }
            else -> say("I can take notes, add tasks, set reminders, open ChatGPT, search Maps and start navigation. That action isn't connected yet.")
        }
    }

    private fun launchOrConfirm(description: String, action: () -> Unit) {
        pendingDescription = description
        pendingAction = action
        say("$description Say OK to continue, or cancel.")
    }

    private fun saveList(key: String, value: String) {
        val old = prefs.getString(key, "").orEmpty()
        prefs.edit().putString(key, (old + if (old.isBlank()) "" else "\n" + "• " + value).trim()).apply()
    }

    private fun showTasks() = say(prefs.getString("tasks", "").takeUnless { it.isNullOrBlank() } ?: "No tasks saved yet.")
    private fun showNotes() = say(prefs.getString("notes", "").takeUnless { it.isNullOrBlank() } ?: "No notes saved yet.")

    private fun createReminderFromCommand(command: String) {
        val match = Regex("(?i)remind me in (\\d+)\\s*(minute|minutes|min|hour|hours)\\s*(?:to)?\\s*(.*)").find(command)
        if (match == null) { say("Say, remind me in 10 minutes to check the document."); return }
        val amount = match.groupValues[1].toLongOrNull() ?: return
        val unit = match.groupValues[2].lowercase()
        val task = match.groupValues[3].ifBlank { "the task" }
        val millis = amount * if (unit.startsWith("hour")) 3_600_000L else 60_000L
        scheduleReminder(task, millis)
    }

    private fun createReminderAt(command: String) {
        val match = Regex("(?i)remind me at (\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\\s*(?:to)?\\s*(.*)").find(command)
        if (match == null) { say("I couldn't determine that time. Try, remind me in 30 minutes."); return }
        val now = java.util.Calendar.getInstance()
        var hour = match.groupValues[1].toInt()
        val minute = match.groupValues[2].ifBlank { "0" }.toInt()
        val meridiem = match.groupValues[3].lowercase()
        if (meridiem == "pm" && hour < 12) hour += 12
        if (meridiem == "am" && hour == 12) hour = 0
        now.set(java.util.Calendar.HOUR_OF_DAY, hour); now.set(java.util.Calendar.MINUTE, minute)
        now.set(java.util.Calendar.SECOND, 0)
        if (now.timeInMillis <= System.currentTimeMillis()) now.add(java.util.Calendar.DAY_OF_YEAR, 1)
        scheduleReminder(match.groupValues[4].ifBlank { "your task" }, now.timeInMillis - System.currentTimeMillis())
    }

    private fun scheduleReminder(task: String, delay: Long) {
        val intent = Intent(this, ReminderReceiver::class.java).putExtra("text", task)
        val pending = PendingIntent.getBroadcast(this, (System.currentTimeMillis() % Int.MAX_VALUE).toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val alarm = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delay, pending)
        say("Reminder set.")
    }

    override fun onDestroy() {
        if (::tts.isInitialized) { tts.stop(); tts.shutdown() }
        super.onDestroy()
    }
}
