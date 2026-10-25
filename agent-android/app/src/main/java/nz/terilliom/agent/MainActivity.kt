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
    private var pendingFiles = mutableListOf<Uri>()
    private var pendingEmailTo = ""
    private var pendingEmailSubject = ""
    private var pendingEmailBody = ""

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
        val files = Button(this).apply {
            text = "Choose files"
            setOnClickListener { chooseFiles() }
        }
        listOf<View>(title, subtitle, status, transcript, mic, input, run, tasks, notes, files).forEach {
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
        } else if (requestCode == 102 && resultCode == RESULT_OK && data != null) {
            val clip = data.clipData
            if (clip != null) {
                for (i in 0 until clip.itemCount) pendingFiles.add(clip.getItemAt(i).uri)
            } else data.data?.let { pendingFiles.add(it) }
            say("Added " + pendingFiles.size + " file(s).")
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
            lower.startsWith("open chatgpt") || lower.startsWith("open chat gpt") -> openApp("com.openai.chatgpt", "ChatGPT")
            lower.startsWith("open maps") -> openApp("com.google.android.apps.maps", "Google Maps")
            lower.startsWith("open gmail") -> openApp("com.google.android.gm", "Gmail")
            lower.startsWith("open calendar") -> openApp("com.google.android.calendar", "Calendar")
            lower.startsWith("open drive") -> openApp("com.google.android.apps.docs", "Google Drive")
            lower.startsWith("open chrome") -> openApp("com.android.chrome", "Chrome")
            lower.startsWith("text ") || lower.startsWith("sms ") -> prepareSms(command)
            lower.startsWith("email to ") -> prepareEmail(command)
            lower == "choose files" || lower == "attach file" || lower.startsWith("attach ") -> chooseFiles()
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

    private fun openApp(packageName: String, label: String) {
        launchOrConfirm("Open " + label + "?") {
            val launch = packageManager.getLaunchIntentForPackage(packageName)
            if (launch != null) { startActivity(launch); say(label + " opened.") }
            else say(label + " isn't installed.")
        }
    }

    private fun prepareSms(command: String) {
        val stripped = command.replaceFirst(Regex("(?i)^(text|sms)\\s+"), "")
        val recipient = stripped.substringBefore(" saying ").substringBefore(" message ").trim()
        val body = if (stripped.contains(" saying ", true)) stripped.substringAfter(" saying ")
                   else if (stripped.contains(" message ", true)) stripped.substringAfter(" message ") else ""
        if (recipient.isBlank() || body.isBlank()) {
            say("Use: text Alan saying Hi Alan, please advise on any changes."); return
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS), 103)
            say("Allow Contacts access, then repeat the instruction."); return
        }
        val cursor = contentResolver.query(
            android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER),
            android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?",
            arrayOf("%" + recipient + "%"), null)
        var phone: String? = null
        cursor?.use { if (it.count == 1 && it.moveToFirst()) phone = it.getString(0) }
        if (phone == null) { say("I couldn't match that name to exactly one contact. Check Contacts first."); return }
        launchOrConfirm("SMS to " + recipient + ": " + body + ". Open it for final review?") {
            startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(phone)))
                .putExtra("sms_body", body))
            say("Message opened. Check it and tap Send in your messaging app.")
        }
    }

    private fun prepareEmail(command: String) {
        val rest = command.substringAfter("email to ", "")
        pendingEmailTo = rest.substringBefore(" subject ").trim()
        pendingEmailSubject = if (rest.contains(" subject ", true)) rest.substringAfter(" subject ").substringBefore(" body ").trim() else ""
        pendingEmailBody = if (rest.contains(" body ", true)) rest.substringAfter(" body ").trim() else ""
        if (!pendingEmailTo.contains("@")) { say("Use: email to address subject topic body message."); return }
        launchOrConfirm("Email to " + pendingEmailTo + " with " + pendingFiles.size + " attachment(s). Open for review?") {
            val intent = Intent(if (pendingFiles.size > 1) Intent.ACTION_SEND_MULTIPLE else Intent.ACTION_SEND)
            intent.type = "*/*"
            intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(pendingEmailTo))
            intent.putExtra(Intent.EXTRA_SUBJECT, pendingEmailSubject)
            intent.putExtra(Intent.EXTRA_TEXT, pendingEmailBody)
            if (pendingFiles.size == 1) intent.putExtra(Intent.EXTRA_STREAM, pendingFiles.first())
            if (pendingFiles.size > 1) intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(pendingFiles))
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(Intent.createChooser(intent, "Review email before sending"))
            say("Email opened for review. Verify the details and send in your email app.")
        }
    }

    private fun chooseFiles() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE); type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        startActivityForResult(intent, 102)
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
