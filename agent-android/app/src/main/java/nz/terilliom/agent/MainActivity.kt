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
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var status: TextView
    private lateinit var transcript: TextView
    private lateinit var input: EditText
    private var wakeButton: Button? = null
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
        if (intent?.getBooleanExtra("agent_wake_detected", false) == true) {
            intent.removeExtra("agent_wake_detected")
            window.decorView.postDelayed({ listen() }, 500)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("agent_wake_detected", false)) {
            intent.removeExtra("agent_wake_detected")
            window.decorView.postDelayed({ listen() }, 500)
        }
    }

    override fun onResume() {
        super.onResume()
        wakeButton?.text = if (prefs.getBoolean("wake_enabled", false)) "Stop wake listening" else "Start wake listening"
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
        val wake = Button(this).apply {
            text = if (prefs.getBoolean("wake_enabled", false)) "Stop wake listening" else "Start wake listening"
            setOnClickListener {
                if (prefs.getBoolean("wake_enabled", false)) {
                    stopService(Intent(this@MainActivity, WakeWordService::class.java))
                    prefs.edit().putBoolean("wake_enabled", false).apply()
                    text = "Start wake listening"
                    say("Wake listening stopped.")
                } else if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    try {
                        val serviceIntent = Intent(this@MainActivity, WakeWordService::class.java)
                        if (Build.VERSION.SDK_INT >= 26) startForegroundService(serviceIntent) else startService(serviceIntent)
                        prefs.edit().putBoolean("wake_enabled", true).apply()
                        text = "Stop wake listening"
                        say("Wake listening started. Say Oi Agent.")
                    } catch (_: Exception) { say("Android blocked background listening. Tap the microphone to speak instead.") }
                } else { requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 100); say("Allow microphone access, then tap again.") }
            }
        }
        wakeButton = wake
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
        val weeklyTasks = Button(this).apply {
            text = "Weekly tasks"
            setOnClickListener { showTaskList("weekly_tasks", "No weekly tasks saved.") }
        }
        val notes = Button(this).apply {
            text = "Notes"
            setOnClickListener { showNotes() }
        }
        val ai = Button(this).apply {
            text = if (prefs.getString("gemini_api_key", "").isNullOrBlank()) "Connect AI brain" else "AI brain settings"
            setOnClickListener { configureAiBrain() }
        }
        val files = Button(this).apply {
            text = "Choose files"
            setOnClickListener { chooseFiles() }
        }
        listOf<View>(title, subtitle, status, transcript, mic, wake, input, run, tasks, weeklyTasks, notes, ai, files).forEach {
            root.addView(it, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = 10
            })
        }
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun requestPermissionsIfNeeded() {
        val wanted = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33) wanted.add(Manifest.permission.POST_NOTIFICATIONS)
        val missing = wanted.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) requestPermissions(missing.toTypedArray(), 100)
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
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
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
            lower.startsWith("open chatgpt") || lower.startsWith("open chat gpt") -> {
                val wantsVoice = lower.contains("voice") || lower.contains("new chat")
                launchOrConfirm(if (wantsVoice) "Open ChatGPT and try to start voice chat?" else "Open ChatGPT?") {
                    val launch = packageManager.getLaunchIntentForPackage("com.openai.chatgpt")
                    if (launch == null) say("ChatGPT isn't installed.")
                    else {
                        if (wantsVoice) prefs.edit().putString("ui_command", "tap:Voice").apply()
                        startActivity(launch)
                        say(if (wantsVoice) "ChatGPT opened. Trying the visible Voice control." else "ChatGPT opened.")
                    }
                }
            }
            lower.startsWith("close chatgpt") || lower.startsWith("close chat gpt") || lower == "close this app" -> {
                launchOrConfirm("Leave the current app and return home?") {
                    prefs.edit().putString("ui_command", "home").apply()
                    moveTaskToBack(true)
                    say("Returning to home.")
                }
            }
            lower.startsWith("open maps") -> openApp("com.google.android.apps.maps", "Google Maps")
            lower.startsWith("open gmail") -> openApp("com.google.android.gm", "Gmail")
            lower.startsWith("open calendar") -> openApp("com.google.android.calendar", "Calendar")
            lower.startsWith("open drive") -> openApp("com.google.android.apps.docs", "Google Drive")
            lower.startsWith("open chrome") -> openApp("com.android.chrome", "Chrome")
            lower.startsWith("text ") || lower.startsWith("sms ") -> prepareSms(command)
            lower.startsWith("check replies from ") -> checkSmsReplies(command.substringAfter("from ").trim())
            lower.startsWith("email to ") -> prepareEmail(command)
            lower == "choose files" || lower == "attach file" || lower.startsWith("attach ") -> chooseFiles()
            lower.startsWith("tap ") -> {
                val target = command.substringAfter("tap ").trim()
                launchOrConfirm("Tap the visible control named " + target + "?") {
                    prefs.edit().putString("ui_command", "tap:" + target).apply()
                    say("Tap queued.")
                    moveTaskToBack(true)
                }
            }
            lower.startsWith("type ") -> {
                val value = command.substringAfter("type ").trim()
                launchOrConfirm("Type this into the focused field: " + value + "?") {
                    prefs.edit().putString("ui_command", "type:" + value).apply()
                    say("Typing queued.")
                    moveTaskToBack(true)
                }
            }
            lower == "scroll down" || lower == "scroll up" -> {
                launchOrConfirm(command + "?") {
                    prefs.edit().putString("ui_command", command).apply()
                    say("Scroll action queued.")
                    moveTaskToBack(true)
                }
            }
            lower == "go back" -> {
                launchOrConfirm("Go back?") {
                    prefs.edit().putString("ui_command", "back").apply()
                    say("Back action queued.")
                    moveTaskToBack(true)
                }
            }
            lower == "go home" -> {
                launchOrConfirm("Return to the home screen?") {
                    prefs.edit().putString("ui_command", "home").apply()
                    say("Home action queued.")
                    moveTaskToBack(true)
                }
            }
            lower == "enable accessibility" -> {
                startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
                say("Enable Agent under installed accessibility services, then return here.")
            }
            lower.startsWith("remind me in ") -> createReminderFromCommand(command)
            lower.startsWith("remind me at ") -> createReminderAt(command)
            lower.startsWith("complete weekly task ") || lower.startsWith("delete weekly task ") -> {
                val complete = lower.startsWith("complete")
                val number = command.substringAfterLast(" ").toIntOrNull()
                if (number == null) say("Use a task number, for example: complete weekly task 2.")
                else changeTask("weekly_tasks", number, complete)
            }
            lower.startsWith("complete task ") || lower.startsWith("delete task ") -> {
                val complete = lower.startsWith("complete")
                val number = command.substringAfterLast(" ").toIntOrNull()
                if (number == null) say("Use a task number, for example: complete task 2.")
                else changeTask("tasks", number, complete)
            }
            lower.startsWith("add task ") || lower.startsWith("task ") -> {
                val task = if (lower.startsWith("add task ")) command.substringAfter("add task ").trim()
                           else command.substringAfter("task ").trim()
                if (task.isBlank()) say("Tell me what task to save.")
                else { saveList("tasks", task); say("Task saved.") }
            }
            lower.startsWith("note ") || lower.startsWith("remember ") -> {
                val note = command.substringAfter(" ").trim()
                saveList("notes", note)
                say("Noted.")
            }
            lower.startsWith("add weekly task ") -> {
                saveList("weekly_tasks", command.substringAfter("add weekly task ").trim()); say("Weekly task saved.")
            }
            lower.startsWith("show weekly tasks") || lower == "weekly tasks" -> showTaskList("weekly_tasks", "No weekly tasks saved.")
            lower.startsWith("what's on today") || lower.startsWith("today's calendar") || lower.startsWith("what's next") -> showNextCalendarEvent()
            lower.startsWith("show tasks") || lower == "today's tasks" -> showTasks()
            lower.startsWith("show notes") -> showNotes()
            lower == "show completed tasks" || lower == "completed tasks" -> showCompletedTasks()
            lower.startsWith("add calendar event ") || lower.startsWith("schedule ") -> scheduleCalendarEvent(command)
            lower == "yes" || lower == "yep" || lower == "ok" || lower == "confirm" || lower == "send it" -> {
                val action = pendingAction
                if (action != null) { pendingAction = null; action.invoke() } else say("Nothing is waiting for confirmation.")
            }
            lower == "no" || lower == "nope" || lower == "cancel" -> {
                pendingAction = null; say("Cancelled.")
            }
            else -> askAiForInstruction(command)
        }
    }


    private fun configureAiBrain() {
        val field = EditText(this).apply {
            hint = "Gemini API key"
            setSingleLine(true)
            setText(prefs.getString("gemini_api_key", "").orEmpty())
        }
        val info = TextView(this).apply {
            text = "Connect Gemini so Agent can answer questions and understand unfamiliar instructions. Create a key in Google AI Studio. The key stays on this phone and is sent to Google when used."
            setPadding(24, 12, 24, 12)
        }
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(info)
            addView(field)
        }
        android.app.AlertDialog.Builder(this)
            .setTitle("Connect Agent's AI brain")
            .setView(panel)
            .setPositiveButton("Save") { _, _ ->
                prefs.edit().putString("gemini_api_key", field.text.toString().trim()).apply()
                say(if (field.text.toString().isBlank()) "AI key cleared." else "AI key saved on this phone.")
            }
            .setNeutralButton("Get API key") { _, _ ->
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey")))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun askAiForInstruction(command: String) {
        val key = prefs.getString("gemini_api_key", "").orEmpty().trim()
        if (key.isBlank()) {
            say("The AI brain is not connected. Open AI brain settings to add a Gemini API key.")
            return
        }
        say("Thinking.")
        Thread {
            var answer = ""
            var routedCommand = ""
            var error = ""
            try {
                val endpoint = java.net.URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent")
                val connection = endpoint.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.setRequestProperty("x-goog-api-key", key)
                val prompt = """You are Agent, an Android personal assistant. Classify the user's request and return ONLY one valid JSON object, no markdown.
Schema: {"type":"action","command":"one canonical command"} OR {"type":"answer","text":"short helpful response"}.
Use type action ONLY when the request maps clearly to one supported command. Canonical commands:
- "add task <task text>" for daily tasks
- "add weekly task <task text>"
- "note <note text>" to save a note
- "show tasks", "show weekly tasks", "show notes", "show completed tasks"
- "what's on today" for calendar
- "remind me in <number> minutes <reminder text>" or "remind me in <number> hours <reminder text>" for a reminder; do not use days
- "remind me at <time> <reminder text>" for a reminder at a time
- "search maps for <place/address>"
- "navigate to <place/address>"
- "open maps", "open gmail", "open calendar", "open drive", "open chrome", "open chatgpt", "open chatgpt voice"
- "text <contact> saying <message>" to prepare an SMS, never send without the app's confirmation
- "email to <recipient> subject <subject> body <body>" to prepare an email
- "add calendar event <event details>" to prepare a calendar event
- "check replies from <contact>"
- "complete task <number>", "delete task <number>", "complete weekly task <number>", "delete weekly task <number>"
Never invent details. If dates, recipients, times, or task text are unclear, ask a short clarifying question using type answer. For requests that require unsupported abilities, answer honestly. Do not claim an action has happened. Never treat a request to send a message as permission to bypass confirmation.
User request: """ + command
                val body = org.json.JSONObject().put("contents",
                    org.json.JSONArray().put(org.json.JSONObject().put("parts",
                        org.json.JSONArray().put(org.json.JSONObject().put("text", prompt)))))
                    .put("generationConfig", org.json.JSONObject().put("temperature", 0.1).put("maxOutputTokens", 300).put("responseMimeType", "application/json"))
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                connection.disconnect()
                if (code in 200..299) {
                    val text = org.json.JSONObject(response).optJSONArray("candidates")?.optJSONObject(0)
                        ?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)
                        ?.optString("text", "").orEmpty().trim()
                    val fence = 96.toChar().toString().repeat(3)
                    val jsonText = text.removePrefix(fence + "json").removePrefix(fence).removeSuffix(fence).trim()
                    val result = org.json.JSONObject(jsonText)
                    if (result.optString("type") == "action") {
                        routedCommand = result.optString("command", "").trim()
                        if (routedCommand.isBlank() || routedCommand.equals(command, true)) {
                            routedCommand = ""
                            answer = "I couldn't safely map that to an action. Please say it another way."
                        }
                    } else {
                        answer = result.optString("text", "I couldn't interpret that request.").trim()
                    }
                    if (routedCommand.isBlank() && answer.isBlank()) answer = "The AI returned no usable answer. Try again."
                } else {
                    error = when (code) {
                        400, 401, 403 -> "Gemini rejected the API key or request. Check AI brain settings."
                        429 -> "Gemini usage limit reached. Check the Google AI Studio quota."
                        else -> "Gemini connection failed with error " + code + "."
                    }
                }
            } catch (_: Exception) {
                error = "Could not get a valid response from Gemini. Check internet access, the API key, and try again."
            }
            runOnUiThread {
                when {
                    routedCommand.isNotBlank() -> handle(routedCommand)
                    answer.isNotBlank() -> say(answer)
                    else -> say(error.ifBlank { "AI request failed." })
                }
            }
        }.start()
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
        val rawBody = if (stripped.contains(" saying ", true)) stripped.substringAfter(" saying ")
                      else if (stripped.contains(" message ", true)) stripped.substringAfter(" message ") else ""
        val expiryMatch = Regex("(?i)\\b(?:expires?|deadline) in (\\d+) days?\\b").find(rawBody)
        val expiryDays = expiryMatch?.groupValues?.get(1)?.toLongOrNull()
        val body = if (expiryMatch != null) rawBody.replace(expiryMatch.value, "").trim() else rawBody.trim()
        if (recipient.isBlank() || body.isBlank()) {
            say("Use: text Alan saying Hi Alan, please advise on any changes."); return
        }
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), 103)
            say("Allow Contacts access, then repeat the instruction."); return
        }
        if (checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.SEND_SMS), 106)
            say("Allow SMS permission, then repeat the instruction."); return
        }
        val cursor = contentResolver.query(
            android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER),
            android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?",
            arrayOf("%" + recipient + "%"), null)
        var phone: String? = null
        cursor?.use { if (it.count == 1 && it.moveToFirst()) phone = it.getString(0) }
        if (phone == null) { say("I couldn't match that name to exactly one contact. Check Contacts first."); return }
        launchOrConfirm("SMS to " + recipient + ": " + body + ". Say OK to send now?") {
            try {
                val smsManager = if (Build.VERSION.SDK_INT >= 31) getSystemService(android.telephony.SmsManager::class.java) else android.telephony.SmsManager.getDefault()
                val parts = smsManager.divideMessage(body)
                if (parts.size > 1) smsManager.sendMultipartTextMessage(phone, null, parts, null, null)
                else smsManager.sendTextMessage(phone, null, body, null, null)
                prefs.edit().putLong("sms_sent_at_" + phone, System.currentTimeMillis()).apply()
                if (expiryDays != null && expiryDays > 0) {
                    scheduleReminderQuiet("Check reply from " + recipient + "; follow-up deadline reached", expiryDays * 86_400_000L)
                }
                say(if (expiryDays != null) "SMS submitted. Follow-up reminder set." else "SMS submitted to the mobile network.")
            } catch (_: Exception) {
                say("SMS could not be sent. Opening the message draft instead.")
                startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(phone))).putExtra("sms_body", body))
            }
        }
    }

    private fun checkSmsReplies(recipient: String) {
        if (checkSelfPermission(Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_SMS), 107)
            say("Allow SMS access, then repeat the instruction."); return
        }
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), 103)
            say("Allow Contacts access, then repeat the instruction."); return
        }
        val contacts = contentResolver.query(
            android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER),
            android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?",
            arrayOf("%" + recipient + "%"), null)
        var phone: String? = null
        contacts?.use { if (it.count == 1 && it.moveToFirst()) phone = it.getString(0) }
        val resolvedPhone = phone
        if (resolvedPhone == null) { say("I couldn't match that name to exactly one contact."); return }
        val sentAt = prefs.getLong("sms_sent_at_" + resolvedPhone, 0L)
        val selection = if (sentAt > 0L) "address = ? AND date > ?" else "address = ?"
        val args: Array<String> = if (sentAt > 0L) arrayOf(resolvedPhone, sentAt.toString()) else arrayOf(resolvedPhone)
        val cursor = contentResolver.query(android.provider.Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf("body", "date"), selection, args, "date DESC")
        cursor?.use {
            if (it.moveToFirst()) {
                val body = it.getString(0) ?: "Message received"
                val date = java.text.DateFormat.getDateTimeInstance().format(java.util.Date(it.getLong(1)))
                say("New reply from " + recipient + " at " + date + ": " + body)
                return
            }
        }
        say("No new SMS reply found from " + recipient + ".")
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

    private fun scheduleCalendarEvent(command: String) {
        if (checkSelfPermission(Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED ||
            checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR), 105)
            say("Allow Calendar access, then repeat the instruction."); return
        }
        val timeMatch = Regex("(?i)\\bat\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)\\b").find(command)
        if (timeMatch == null) {
            val title = command.removePrefix("schedule ").removePrefix("add calendar event ").trim()
            launchOrConfirm("Open Calendar to finish scheduling " + title + "?") {
                startActivity(Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
                    .putExtra(CalendarContract.Events.TITLE, title))
                say("Calendar opened. Add the date and time, then save.")
            }
            return
        }
        val eventTime = java.util.Calendar.getInstance()
        val dateText = command.lowercase(Locale.getDefault())
        when {
            dateText.contains("tomorrow") -> eventTime.add(java.util.Calendar.DAY_OF_YEAR, 1)
            dateText.contains("monday") -> moveToWeekday(eventTime, java.util.Calendar.MONDAY)
            dateText.contains("tuesday") -> moveToWeekday(eventTime, java.util.Calendar.TUESDAY)
            dateText.contains("wednesday") -> moveToWeekday(eventTime, java.util.Calendar.WEDNESDAY)
            dateText.contains("thursday") -> moveToWeekday(eventTime, java.util.Calendar.THURSDAY)
            dateText.contains("friday") -> moveToWeekday(eventTime, java.util.Calendar.FRIDAY)
            dateText.contains("saturday") -> moveToWeekday(eventTime, java.util.Calendar.SATURDAY)
            dateText.contains("sunday") -> moveToWeekday(eventTime, java.util.Calendar.SUNDAY)
        }
        var hour = timeMatch.groupValues[1].toInt()
        val minute = timeMatch.groupValues[2].ifBlank { "0" }.toInt()
        val meridiem = timeMatch.groupValues[3].lowercase()
        if (meridiem == "pm" && hour < 12) hour += 12
        if (meridiem == "am" && hour == 12) hour = 0
        eventTime.set(java.util.Calendar.HOUR_OF_DAY, hour)
        eventTime.set(java.util.Calendar.MINUTE, minute)
        eventTime.set(java.util.Calendar.SECOND, 0)
        eventTime.set(java.util.Calendar.MILLISECOND, 0)
        val title = command.replaceFirst(Regex("(?i)^(schedule|add calendar event)\\s+"), "")
            .replace(Regex("(?i)\\b(tomorrow|today|monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b"), "")
            .replace(Regex("(?i)\\bat\\s+\\d{1,2}(?::\\d{2})?\\s*(am|pm)\\b"), "")
            .replace(Regex("\\s+"), " ").trim()
        launchOrConfirm("Add " + title + " to Calendar for " +
            java.text.DateFormat.getDateTimeInstance().format(eventTime.time) + "?") {
            val calendars = contentResolver.query(CalendarContract.Calendars.CONTENT_URI,
                arrayOf(CalendarContract.Calendars._ID),
                CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL + ">=?",
                arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString()), null)
            var calendarId: Long? = null
            calendars?.use { if (it.moveToFirst()) calendarId = it.getLong(0) }
            if (calendarId == null) {
                startActivity(Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
                    .putExtra(CalendarContract.Events.TITLE, title)
                    .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, eventTime.timeInMillis)
                    .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, eventTime.timeInMillis + 30 * 60 * 1000L))
                say("Calendar opened with the event details. Save it to finish.")
                return@launchOrConfirm
            }
            val values = android.content.ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calendarId!!)
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.DTSTART, eventTime.timeInMillis)
                put(CalendarContract.Events.DTEND, eventTime.timeInMillis + 30 * 60 * 1000L)
                put(CalendarContract.Events.EVENT_TIMEZONE, java.util.TimeZone.getDefault().id)
            }
            val created = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            if (created != null) say("Confirmed. Calendar event added.")
            else say("Calendar did not confirm the event. Open Calendar and check.")
        }
    }

    private fun moveToWeekday(calendar: java.util.Calendar, weekday: Int) {
        var delta = (weekday - calendar.get(java.util.Calendar.DAY_OF_WEEK) + 7) % 7
        if (delta == 0) delta = 7
        calendar.add(java.util.Calendar.DAY_OF_YEAR, delta)
    }

    private fun showNextCalendarEvent() {
        if (checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_CALENDAR), 104)
            say("Allow Calendar access, then ask again."); return
        }
        val now = System.currentTimeMillis()
        val end = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 23); set(java.util.Calendar.MINUTE, 59); set(java.util.Calendar.SECOND, 59)
        }.timeInMillis
        val projection = arrayOf(CalendarContract.Events.TITLE, CalendarContract.Events.DTSTART)
        val cursor = contentResolver.query(
            CalendarContract.Events.CONTENT_URI, projection,
            CalendarContract.Events.DTSTART + ">=? AND " + CalendarContract.Events.DTSTART + "<=?",
            arrayOf(now.toString(), end.toString()), CalendarContract.Events.DTSTART + " ASC")
        cursor?.use {
            if (it.moveToFirst()) {
                val title = it.getString(0) ?: "Untitled event"
                val time = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(java.util.Date(it.getLong(1)))
                say("Next today: " + title + " at " + time + ".")
                return
            }
        }
        say("No upcoming calendar events found for today.")
    }

    private fun saveList(key: String, value: String) {
        val old = prefs.getString(key, "").orEmpty()
        val next = if (old.isBlank()) "• " + value else old.trimEnd() + "\n• " + value
        prefs.edit().putString(key, next).apply()
    }

    private fun changeTask(key: String, number: Int, complete: Boolean) {
        val lines = prefs.getString(key, "").orEmpty().lines().filter { it.isNotBlank() }.toMutableList()
        if (number < 1 || number > lines.size) {
            say("There isn't a task numbered " + number + ".")
            return
        }
        val task = lines[number - 1].removePrefix("• ").removePrefix("✓ ").trim()
        lines.removeAt(number - 1)
        if (complete) {
            val completedKey = if (key == "weekly_tasks") "completed_weekly_tasks" else "completed_tasks"
            val completed = prefs.getString(completedKey, "").orEmpty()
            val entry = "✓ " + task
            prefs.edit()
                .putString(key, lines.joinToString("\n"))
                .putString(completedKey, if (completed.isBlank()) entry else completed.trimEnd() + "\n" + entry)
                .apply()
            say("Completed and archived: " + task + ".")
        } else {
            prefs.edit().putString(key, lines.joinToString("\n")).apply()
            say("Deleted: " + task + ".")
        }
    }

    private fun showTasks() = showTaskList("tasks", "No tasks saved yet.")

    private fun showTaskList(key: String, emptyMessage: String) {
        val lines = prefs.getString(key, "").orEmpty().lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) { say(emptyMessage); return }
        val numbered = lines.mapIndexed { index, line ->
            (index + 1).toString() + ". " + line.removePrefix("• ").removePrefix("✓ ").trim()
        }
        say(numbered.joinToString(". "))
    }

    private fun showNotes() = say(prefs.getString("notes", "").takeUnless { it.isNullOrBlank() } ?: "No notes saved yet.")

    private fun showCompletedTasks() {
        val daily = prefs.getString("completed_tasks", "").orEmpty().lines().filter { it.isNotBlank() }
        val weekly = prefs.getString("completed_weekly_tasks", "").orEmpty().lines().filter { it.isNotBlank() }
        val all = daily.map { "Daily: " + it.removePrefix("✓ ").trim() } +
            weekly.map { "Weekly: " + it.removePrefix("✓ ").trim() }
        say(if (all.isEmpty()) "No completed tasks archived yet." else all.joinToString(". "))
    }

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
        scheduleReminderQuiet(task, delay)
        say("Reminder set.")
    }

    private fun scheduleReminderQuiet(task: String, delay: Long) {
        val intent = Intent(this, ReminderReceiver::class.java).putExtra("text", task)
        val pending = PendingIntent.getBroadcast(this, (System.currentTimeMillis() % Int.MAX_VALUE).toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val alarm = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delay, pending)
    }

    override fun onDestroy() {
        if (::tts.isInitialized) { tts.stop(); tts.shutdown() }
        super.onDestroy()
    }
}