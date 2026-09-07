package com.example.service

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class IntentResult(
    val replyText: String,
    val spokenText: String,
    val actionType: String? = null,
    val actionData: String? = null,
    val isActionExecuted: Boolean = false,
    val noteContent: String? = null
)

class OfflineIntentEngine(private val deviceController: DeviceActionController) {

    fun processCommand(rawInput: String): IntentResult {
        val query = rawInput.trim().lowercase()

        // 1. Flashlight / Torch
        if (isTorchCommand(query)) {
            val isOff = query.contains("off") || query.contains("bandh") || query.contains("band") ||
                        query.contains("બંધ") || query.contains("nox")
            val newState = !isOff
            val success = deviceController.toggleTorch(newState)
            return if (newState) {
                IntentResult(
                    replyText = "🔦 ટોર્ચ ચાલુ કરી દીધી છે! (Flashlight is turned ON)",
                    spokenText = "Torch chalu kari didhi chhe",
                    actionType = "TORCH",
                    actionData = "ON",
                    isActionExecuted = success
                )
            } else {
                IntentResult(
                    replyText = "🔦 ટોર્ચ બંધ કરી દીધી છે. (Flashlight is turned OFF)",
                    spokenText = "Torch bandh kari didhi chhe",
                    actionType = "TORCH",
                    actionData = "OFF",
                    isActionExecuted = success
                )
            }
        }

        // 2. Battery Check
        if (isBatteryCommand(query)) {
            val info = deviceController.getBatteryInfo()
            val chargeStatus = if (info.isCharging) "ચાર્જિંગ થઈ રહ્યું છે" else "ચાર્જિંગ નથી"
            val text = "🔋 Vivo T3 બેટરી: ${info.level}%\nસ્થિતિ: ${info.statusText}\n(Battery is at ${info.level}%, $chargeStatus)"
            return IntentResult(
                replyText = text,
                spokenText = "Tamara Vivo T3 phone ni battery ${info.level} taka chhe",
                actionType = "BATTERY",
                actionData = "${info.level}%"
            )
        }

        // 3. Current Time
        if (isTimeCommand(query)) {
            val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val currentTime = sdf.format(Date())
            return IntentResult(
                replyText = "⏰ અત્યારે સમય થયો છે: $currentTime",
                spokenText = "Atyare samay thayo chhe $currentTime",
                actionType = "TIME",
                actionData = currentTime
            )
        }

        // 4. Current Date & Day
        if (isDateCommand(query)) {
            val dateFmt = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
            val currentDate = dateFmt.format(Date())
            return IntentResult(
                replyText = "📅 આજે તારીખ છે:\n$currentDate",
                spokenText = "Aaje tarikh chhe $currentDate",
                actionType = "DATE",
                actionData = currentDate
            )
        }

        // 5. Vivo T3 Phone Specifications & Diagnostics
        if (isVivoSpecsCommand(query)) {
            val specs = deviceController.getVivoPhoneSpecs()
            val text = """
                📱 ${specs.model} વિગત:
                • પ્રોસેસર: ${specs.processor}
                • ડિસ્પ્લે: ${specs.displaySpec}
                • કેમેરો: ${specs.cameraSpec}
                • બેટરી: ${specs.batterySpec}
                • RAM: ${specs.availableRamMb} MB ઉપલબ્ધ / ${specs.totalRamMb} MB
                • સ્ટોરેજ: ${specs.freeStorageGb} GB મુક્ત / ${specs.totalStorageGb} GB
                • ઓપરેટિંગ સિસ્ટમ: ${specs.androidVersion}
            """.trimIndent()
            return IntentResult(
                replyText = text,
                spokenText = "Tamaro Vivo T3 Dimensity 7200 processor ane 5000 mAh battery sathe smoothly kaam kare chhe",
                actionType = "VIVO_INFO",
                actionData = specs.model
            )
        }

        // 6. Ringer Mode: Silent / Vibrate / Normal
        if (query.contains("silent") || query.contains("સાયલન્ટ")) {
            val msg = deviceController.setRingerMode("silent")
            return IntentResult(
                replyText = "🔕 $msg",
                spokenText = "Phone silent mode par muki didho chhe",
                actionType = "RINGER",
                actionData = "SILENT"
            )
        }
        if (query.contains("vibrate") || query.contains("વાઇબ્રેટ") || query.contains("vibration")) {
            val msg = deviceController.setRingerMode("vibrate")
            return IntentResult(
                replyText = "📳 $msg",
                spokenText = "Vibrate mode active karyo chhe",
                actionType = "RINGER",
                actionData = "VIBRATE"
            )
        }
        if (query.contains("normal sound") || query.contains("ringtone") || query.contains("નોર્મલ અવાજ")) {
            val msg = deviceController.setRingerMode("normal")
            return IntentResult(
                replyText = "🔔 $msg",
                spokenText = "Normal sound mode active karyo chhe",
                actionType = "RINGER",
                actionData = "NORMAL"
            )
        }

        // 7. Open Applications
        val appResult = checkAppOpenCommands(query)
        if (appResult != null) return appResult

        // 8. Set Timer
        if (isTimerCommand(query)) {
            val seconds = extractTimerSeconds(query)
            val success = deviceController.setTimer(seconds, "Siri Vivo T3 Timer")
            val mins = seconds / 60
            return IntentResult(
                replyText = "⏱️ $mins મિનિટ માટે ટાઈમર શરૂ કર્યો છે! (Timer set for $mins minutes)",
                spokenText = "$mins minute no timer chalu karyo chhe",
                actionType = "TIMER",
                actionData = "$mins min",
                isActionExecuted = success
            )
        }

        // 9. Set Alarm
        if (isAlarmCommand(query)) {
            val (hour, minute) = extractAlarmTime(query)
            val success = deviceController.setAlarm(hour, minute, "Siri Vivo Alarm")
            val amPm = if (hour >= 12) "PM" else "AM"
            val displayHour = if (hour > 12) hour - 12 else if (hour == 0) 12 else hour
            val minStr = if (minute < 10) "0$minute" else "$minute"
            return IntentResult(
                replyText = "⏰ સવારે/સાંજે $displayHour:$minStr $amPm નું એલાર્મ સેટ કર્યું છે! (Alarm set for $displayHour:$minStr $amPm)",
                spokenText = "$displayHour vagya nu alarm set karyo chhe",
                actionType = "ALARM",
                actionData = "$displayHour:$minStr $amPm",
                isActionExecuted = success
            )
        }

        // 10. Math Calculations (Offline Math Evaluator)
        val mathAnswer = evaluateMath(query)
        if (mathAnswer != null) {
            return IntentResult(
                replyText = "🔢 ગણતરી પરિણામ:\n$mathAnswer",
                spokenText = "Javab chhe $mathAnswer",
                actionType = "CALCULATOR",
                actionData = mathAnswer
            )
        }

        // 11. Take Offline Voice Note / Reminder
        if (isNoteCommand(query)) {
            val content = extractNoteContent(rawInput)
            return IntentResult(
                replyText = "📝 નોંધ સાચવી લીધી છે:\n\"$content\"",
                spokenText = "Note save kari didhi chhe",
                actionType = "NOTE",
                actionData = content,
                noteContent = content
            )
        }

        // 12. Siri Persona, Greetings & Humor
        val siriChat = getSiriPersonalityReply(query)
        if (siriChat != null) {
            return siriChat
        }

        // 13. Smart Offline Fallback
        return IntentResult(
            replyText = "🤖 હું ઑફલાઇન મોડમાં આ આદેશો કરી શકું છું:\n" +
                    "• 🔦 ટોર્ચ ચાલુ/બંધ કરો (Flashlight)\n" +
                    "• 🔋 બેટરી કેટલી છે? (Battery check)\n" +
                    "• ⏰ સમય / તારીખ (Time & Date)\n" +
                    "• 📱 Vivo T3 વિગત (Phone Specs & RAM)\n" +
                    "• 🚀 WhatsApp, YouTube, Camera ખોલો\n" +
                    "• ⏱️ ટાઈમર અને એલાર્મ મુકો\n" +
                    "• 🔢 ગણતરી (દા.ત. 450 * 12)\n" +
                    "• 📝 ઓફલાઇન નોટ સાચવો\n\n" +
                    "વધુ વિગતવાર AI પ્રશ્નો માટે Hybrid Mode માં ઇન્ટરનેટ કનેક્ટ કરો.",
            spokenText = "Hu offline chhu pan flashlight, battery, samay, apps ane notes mate taiyar chhu."
        )
    }

    private fun isTorchCommand(q: String): Boolean {
        return (q.contains("torch") || q.contains("flashlight") || q.contains("ટોર્ચ") || q.contains("બત્તી") || q.contains("lumos") || q.contains("nox")) &&
                (q.contains("on") || q.contains("off") || q.contains("chalu") || q.contains("bandh") || q.contains("band") ||
                 q.contains("ચાલુ") || q.contains("બંધ") || q.contains("કરો") || q.contains("toggle") || q.contains("lumos") || q.contains("nox"))
    }

    private fun isBatteryCommand(q: String): Boolean {
        return q.contains("battery") || q.contains("બેટરી") || q.contains("charge") || q.contains("ચાર્જ")
    }

    private fun isTimeCommand(q: String): Boolean {
        return q.contains("time") || q.contains("સમય") || q.contains("વાગ્યા") || q.contains("vagya") || q.contains("kitne baje")
    }

    private fun isDateCommand(q: String): Boolean {
        return q.contains("date") || q.contains("તારીખ") || q.contains("tarikh") || q.contains("વાર") || q.contains("day")
    }

    private fun isVivoSpecsCommand(q: String): Boolean {
        return q.contains("vivo") || q.contains("t3") || q.contains("specs") || q.contains("phone info") ||
                q.contains("ફોન વિગત") || q.contains("ram") || q.contains("storage") || q.contains("પ્રોસેસર")
    }

    private fun isTimerCommand(q: String): Boolean {
        return q.contains("timer") || q.contains("ટાઈમર") || q.contains("second") || (q.contains("મિનિટ") && q.contains("મુકો"))
    }

    private fun isAlarmCommand(q: String): Boolean {
        return q.contains("alarm") || q.contains("એલાર્મ")
    }

    private fun isNoteCommand(q: String): Boolean {
        return q.startsWith("note") || q.startsWith("નોટ") || q.startsWith("નોંધ") ||
                q.contains("yaad rakh") || q.contains("યાદ રાખો") || q.contains("save note") || q.contains("take a note")
    }

    private fun extractNoteContent(raw: String): String {
        var clean = raw
        val prefixes = listOf("note", "take a note", "save note", "નોટ લખો", "નોંધ લખો", "નોટ કરો", "યાદ રાખો", "yaad rakhjo", "note that")
        for (prefix in prefixes) {
            if (clean.lowercase().startsWith(prefix)) {
                clean = clean.substring(prefix.length).trim()
                if (clean.startsWith("that") || clean.startsWith("ke") || clean.startsWith("કે") || clean.startsWith(":")) {
                    clean = clean.substring(2).trim()
                }
                break
            }
        }
        return if (clean.isNotBlank()) clean else "નવી અવાજ નોંધ (New voice note)"
    }

    private fun checkAppOpenCommands(q: String): IntentResult? {
        val isOpen = q.contains("open") || q.contains("kholo") || q.contains("ખોલો") || q.contains("chalu") || q.contains("ચાલુ") || q.contains("start")

        if (q.contains("whatsapp") || q.contains("વોટ્સએપ")) {
            val opened = deviceController.openApp("com.whatsapp")
            return IntentResult(
                replyText = if (opened) "💬 WhatsApp ખોલી રહ્યું છે..." else "WhatsApp ઇન્સ્ટોલ થયેલ નથી.",
                spokenText = "WhatsApp kholi rahyu chhe",
                actionType = "APP",
                actionData = "WhatsApp",
                isActionExecuted = opened
            )
        }

        if (q.contains("youtube") || q.contains("યુટ્યુબ")) {
            val opened = deviceController.openApp("com.google.android.youtube")
            return IntentResult(
                replyText = if (opened) "▶️ YouTube ખોલી રહ્યું છે..." else "YouTube ખોલવામાં સમસ્યા આવી.",
                spokenText = "YouTube kholi rahyu chhe",
                actionType = "APP",
                actionData = "YouTube",
                isActionExecuted = opened
            )
        }

        if (q.contains("camera") || q.contains("કેમેરો") || q.contains("photo") || q.contains("ફોટો")) {
            val opened = deviceController.openCamera()
            return IntentResult(
                replyText = "📷 કેમેરો શરૂ થયો છે (Camera launched)...",
                spokenText = "Camera kholi didho chhe",
                actionType = "APP",
                actionData = "Camera",
                isActionExecuted = opened
            )
        }

        if (q.contains("calculator") || q.contains("કેલ્ક્યુલેટર") || (isOpen && q.contains("calc"))) {
            val opened = deviceController.openApp("com.google.android.calculator") ||
                    deviceController.openApp("com.android.calculator2") ||
                    deviceController.openApp("com.vivo.calculator")
            return IntentResult(
                replyText = "🧮 કેલ્ક્યુલેટર ખુલી ગયું છે.",
                spokenText = "Calculator kholi didhu chhe",
                actionType = "APP",
                actionData = "Calculator",
                isActionExecuted = opened
            )
        }

        if (q.contains("call") || q.contains("dialer") || q.contains("ડાયલર") || q.contains("ફોન કરો")) {
            val digits = q.filter { it.isDigit() }
            val opened = deviceController.openDialer(digits)
            return IntentResult(
                replyText = if (digits.isNotEmpty()) "📞 ડાયલિંગ: $digits" else "📞 ફોન ડાયલર ખોલી રહ્યું છે...",
                spokenText = "Phone dialer open kari rahyu chhe",
                actionType = "CALL",
                actionData = digits,
                isActionExecuted = opened
            )
        }

        if (q.contains("sms") || q.contains("message") || q.contains("મેસેજ")) {
            val opened = deviceController.openSms()
            return IntentResult(
                replyText = "💬 મેસેજ એપ ખોલી રહ્યું છે...",
                spokenText = "Messages open kari rahyu chhe",
                actionType = "APP",
                actionData = "Messages",
                isActionExecuted = opened
            )
        }

        if (q.contains("setting") || q.contains("સેટિંગ")) {
            val type = when {
                q.contains("wifi") -> "wifi"
                q.contains("bluetooth") -> "bluetooth"
                q.contains("display") -> "display"
                q.contains("sound") -> "sound"
                else -> "general"
            }
            val opened = deviceController.openSettings(type)
            return IntentResult(
                replyText = "⚙️ $type સેટિંગ્સ ખુલી રહી છે...",
                spokenText = "Settings open kari rahyu chhe",
                actionType = "SETTINGS",
                actionData = type,
                isActionExecuted = opened
            )
        }

        return null
    }

    private fun extractTimerSeconds(q: String): Int {
        val numberRegex = Regex("(\\d+)")
        val match = numberRegex.find(q)
        val num = match?.value?.toIntOrNull() ?: 5
        return if (q.contains("second") || q.contains("સેકન્ડ")) {
            num
        } else {
            num * 60
        }
    }

    private fun extractAlarmTime(q: String): Pair<Int, Int> {
        val cal = Calendar.getInstance()
        var hour = cal.get(Calendar.HOUR_OF_DAY) + 1
        var minute = 0

        val timeMatch = Regex("(\\d{1,2}):(\\d{2})").find(q)
        if (timeMatch != null) {
            hour = timeMatch.groupValues[1].toIntOrNull() ?: hour
            minute = timeMatch.groupValues[2].toIntOrNull() ?: 0
        } else {
            val numMatch = Regex("(\\d{1,2})").find(q)
            if (numMatch != null) {
                hour = numMatch.groupValues[1].toIntOrNull() ?: hour
            }
        }

        if ((q.contains("pm") || q.contains("સાંજે") || q.contains("બપોરે") || q.contains("raate")) && hour < 12) {
            hour += 12
        } else if ((q.contains("am") || q.contains("સવારે") || q.contains("savare")) && hour == 12) {
            hour = 0
        }

        return Pair(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
    }

    private fun evaluateMath(q: String): String? {
        val mathPattern = Regex("([0-9.]+)\\s*([+\\-*/xX×÷]|plus|minus|into|divided by|ભાગ્યા|ગુણ્યા|વત્તા|ઓછા)\\s*([0-9.]+)")
        val match = mathPattern.find(q)
        if (match != null) {
            val num1 = match.groupValues[1].toDoubleOrNull() ?: return null
            val op = match.groupValues[2].lowercase()
            val num2 = match.groupValues[3].toDoubleOrNull() ?: return null

            val result = when {
                op == "+" || op == "plus" || op == "વત્તા" -> num1 + num2
                op == "-" || op == "minus" || op == "ઓછા" -> num1 - num2
                op == "*" || op == "x" || op == "×" || op == "into" || op == "ગુણ્યા" -> num1 * num2
                op == "/" || op == "÷" || op == "divided by" || op == "ભાગ્યા" -> if (num2 != 0.0) num1 / num2 else return "શૂન્ય વડે ભાગી ન શકાય (Cannot divide by 0)"
                else -> return null
            }
            return if (result % 1.0 == 0.0) {
                "$num1 $op $num2 = ${result.toLong()}"
            } else {
                String.format(Locale.US, "$num1 $op $num2 = %.2f", result)
            }
        }

        // Percentage check (e.g. 20 percent of 500)
        val percentPattern = Regex("([0-9.]+)\\s*(%|percent|ટકા)\\s*(of|ના)\\s*([0-9.]+)")
        val pMatch = percentPattern.find(q)
        if (pMatch != null) {
            val p = pMatch.groupValues[1].toDoubleOrNull() ?: return null
            val total = pMatch.groupValues[4].toDoubleOrNull() ?: return null
            val ans = (p / 100.0) * total
            return "$p% of $total = $ans"
        }

        return null
    }

    private fun getSiriPersonalityReply(q: String): IntentResult? {
        // Greetings
        if (q.contains("hello") || q.contains("hi") || q.contains("hey siri") || q.contains("હેલો") || q.contains("હાય") || q.contains("નમસ્તે")) {
            return IntentResult(
                replyText = "👋 નમસ્તે! હું તમારી Siri જેવી AI આસિસ્ટન્ટ છું. Vivo T3 પર હું તમને કઈ રીતે મદદ કરી શકું?",
                spokenText = "Namaste! Hu tamari AI assistant chhu. Vivo T3 par kaho shu madad karu?"
            )
        }

        // Kem chho / How are you
        if (q.contains("kem chho") || q.contains("કેમ છો") || q.contains("how are you") || q.contains("kaise ho")) {
            return IntentResult(
                replyText = "😊 હું એકદમ મજામાં છું! તમારા Vivo T3 સાથે ઝડપથી કામ કરવા તૈયાર છું. તમે કેમ છો?",
                spokenText = "Hu ekdam majama chhu! Vivo T3 sathe fast kaam karva ready chhu."
            )
        }

        // Who are you / Tu kon chhe
        if (q.contains("who are you") || q.contains("તમે કોણ છો") || q.contains("tu kon") || q.contains("kaun ho")) {
            return IntentResult(
                replyText = "✨ હું Vivo T3 માટે વિશેષ તૈયાર કરેલી iPhone Siri જેવી સ્માર્ટ AI આસિસ્ટન્ટ છું! હું સંપૂર્ણપણે ઑફલાઇન પણ તમારા ફોનનું નિયંત્રણ કરી શકું છું.",
                spokenText = "Hu Vivo T3 mate Siri jevi smart AI assistant chhu je offline pan kaam kare chhe."
            )
        }

        // Jokes
        if (q.contains("joke") || q.contains("જોક") || q.contains("હસાવો") || q.contains("chutkula")) {
            val jokes = listOf(
                "😄 શિક્ષક: 'પરીક્ષામાં ચોરી કેમ કરતો હતો?'\nવિદ્યાર્થી: 'સાહેબ, ગાંધીજીએ કહ્યું છે કે જ્ઞાન જ્યાંથી મળે ત્યાંથી લઈ લેવું!'",
                "😂 પપ્પો: 'ડોક્ટર સાહેબ, હું ઊંઘમાં બોલવા લાગ્યો છું!'\nડોક્ટર: 'કંઈ વાંધો નહીં, દિવસમાં બોલવાનો મોકો નથી મળતો એટલે!'",
                "📱 Why did the iPhone look at the Vivo T3 with jealousy?\nBecause the Vivo T3 has a 5000 mAh battery that never dies!",
                "🤖 Siri: 'મારી પાસે કોઈ લાગણી નથી, પણ જો તમારો Vivo T3 100% ચાર્જ હોય તો મને આનંદ થાય છે!'"
            )
            val selectedJoke = jokes.random()
            return IntentResult(
                replyText = selectedJoke,
                spokenText = selectedJoke.lines().first()
            )
        }

        // Vivo T3 praise
        if (q.contains("vivo t3 kaiso") || q.contains("vivo t3 કેવો છે") || q.contains("best feature")) {
            return IntentResult(
                replyText = "🚀 Vivo T3 5G એક ઉત્તમ સ્માર્ટફોન છે!\nતેમાં MediaTek Dimensity 7200 પ્રોસેસર, 120Hz AMOLED ડિસ્પ્લે, 50MP Sony IMX882 OIS કેમેરો અને 5000mAh મોટી બેટરી છે.",
                spokenText = "Vivo T3 5G Dimensity 7200 ane Sony OIS camera sathe best performance aape chhe."
            )
        }

        return null
    }
}
