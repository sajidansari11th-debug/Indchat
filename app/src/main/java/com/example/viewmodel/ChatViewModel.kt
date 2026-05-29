package com.example.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val userSessionDao = db.userSessionDao()
    private val contactDao = db.contactDao()
    private val messageDao = db.messageDao()
    private val callLogDao = db.callLogDao()
    private val statusDao = db.statusDao()

    // --- UI STATES ---

    // Auth Flows
    val activeSession = userSessionDao.getActiveSessionFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    var loginPhone = mutableStateOf("")
    var loginName = mutableStateOf("")
    var smsOtpCode = mutableStateOf("")
    var generatedOtp = mutableStateOf<String?>(null)
    var isVerifyingCode = mutableStateOf(false)
    var loginError = mutableStateOf<String?>(null)
    var otpBannerMessage = mutableStateOf<String?>(null)

    // Main Sections
    val contacts = contactDao.getAllContactsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val callLogs = callLogDao.getAllCallLogsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val statuses = statusDao.getAllStatusesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Chat
    val currentChatContact = MutableStateFlow<Contact?>(null)
    val activeMessages = currentChatContact.flatMapLatest { contact ->
        if (contact != null) {
            val session = activeSession.value
            if (session != null) {
                messageDao.getMessagesForChatFlow(session.phone, contact.phone)
            } else {
                flowOf(emptyList())
            }
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    var chatInputField = mutableStateOf("")
    var isTypingBot = MutableStateFlow(false)

    // DB Inspection Engine (Unique Feature!)
    val isDbInspectionMode = MutableStateFlow(false)

    // Call Engine Simulation
    val activeCall = MutableStateFlow<CallLog?>(null)
    var callTimeSeconds = mutableStateOf(0)
    var isCallMuted = mutableStateOf(false)
    var isCallSpeakerOn = mutableStateOf(true)

    // Status System
    val currentStatusViewer = MutableStateFlow<List<StatusUpdate>?>(null)
    val statusSelectedIndex = mutableStateOf(0)
    var myStatusInputText = mutableStateOf("")
    val myStatusColorIndex = mutableStateOf(0)

    val statusGradients = listOf(
        "#0F172A", // Dark Slate Default
        "#0D9488", // Teal
        "#6366F1", // Indigo
        "#EC4899", // Pink Glow
        "#D97706", // Amber
        "#7C3AED", // Violet
        "#BE123C"  // Rose
    )

    init {
        // Start Call Timer loop in the background
        viewModelScope.launch {
            while (true) {
                delay(1000)
                if (activeCall.value != null && activeCall.value?.status == "CONNECTED") {
                    callTimeSeconds.value += 1
                }
            }
        }
    }

    // --- ACTIONS ---

    // 1. PHONE LOGIN FLOW (SIMULATED OTP)

    fun sendOtp() {
        if (loginPhone.value.trim().length < 8) {
            loginError.value = "Please enter a valid phone number (at least 8 digits)"
            return
        }
        if (loginName.value.trim().isEmpty()) {
            loginError.value = "Please enter your display name"
            return
        }
        loginError.value = null

        // Generate a 6 digit code
        val randomCode = (100000 + Random.nextInt(900000)).toString()
        generatedOtp.value = randomCode
        isVerifyingCode.value = true
        otpBannerMessage.value = "💬 CipherWire SMS: Verification code is $randomCode"
    }

    fun submitOtp() {
        val codeInput = smsOtpCode.value.trim()
        val expected = generatedOtp.value
        if (codeInput != expected) {
            loginError.value = "Incorrect code. Please type the simulated SMS code above."
            return
        }

        loginError.value = null
        isVerifyingCode.value = false
        otpBannerMessage.value = null

        viewModelScope.launch {
            // Generate E2EE private/public keys
            val keySeed = loginPhone.value + "SECURE_RESTRICTED"
            val mockPublicKey = "CIPHERWIRE_PUBLIC_KEY_" + loginPhone.value.hashCode().toString(16).uppercase()
            val mockPrivateKey = "CIPHERWIRE_PRIVATE_KEY_" + loginPhone.value.hashCode().toString(36).uppercase()

            val session = UserSession(
                phone = loginPhone.value.trim(),
                name = loginName.value.trim(),
                avatarUrl = "#10B981", // Emerald Accent
                publicKey = mockPublicKey,
                privateKey = mockPrivateKey
            )
            userSessionDao.saveSession(session)
        }
    }

    fun cancelOtpFlow() {
        isVerifyingCode.value = false
        generatedOtp.value = null
        smsOtpCode.value = ""
        loginError.value = null
        otpBannerMessage.value = null
    }

    // 2. CHAT SYSTEM & CRYPTO RESPONSE ENGINE

    fun sendMessage() {
        val plainText = chatInputField.value.trim()
        val recipient = currentChatContact.value
        val sender = activeSession.value

        if (plainText.isEmpty() || recipient == null || sender == null) return

        chatInputField.value = ""

        viewModelScope.launch {
            // E2EE Channel cryptography seed
            val channelSeed = if (sender.phone < recipient.phone) {
                sender.phone + "_" + recipient.phone
            } else {
                recipient.phone + "_" + sender.phone
            }

            // Encrypt plain message into base64 ciphertext
            val ciphertext = CryptoEngine.encrypt(plainText, channelSeed)

            val message = Message(
                senderPhone = sender.phone,
                receiverPhone = recipient.phone,
                ciphertext = ciphertext,
                isEncrypted = true,
                status = "SENT"
            )

            messageDao.insertMessage(message)

            // Trigger simulated contact behavior if bot
            if (recipient.isBot) {
                triggerBotResponse(recipient, plainText, channelSeed)
            }
        }
    }

    private suspend fun triggerBotResponse(bot: Contact, userText: String, channelSeed: String) {
        // Setup typing status
        delay(800)
        isTypingBot.value = true
        delay(1200)
        isTypingBot.value = false

        val responseText = when {
            bot.phone.contains("0143") -> { // Alice
                when {
                    userText.contains("security", ignoreCase = true) || userText.contains("key", ignoreCase = true) -> {
                        "🔒 Cryptographic keys rotated successfully. Our end-to-end channel uses AES-256 bits algorithm over a shared local secret. Tap the secure shield in headers to verify visual fingerprints!"
                    }
                    userText.contains("whatsapp", ignoreCase = true) -> {
                        "Compared to other messengers, CipherWire does not upload your telemetry metadata to servers. Everything stays encrypted locally."
                    }
                    else -> "Alice here. Aegis systems are online. Message was securely received as ciphertext: \"${CryptoEngine.encrypt(userText, channelSeed).take(12)}...\""
                }
            }
            bot.phone.contains("2671") -> { // Bob
                when {
                    userText.contains("matrix", ignoreCase = true) -> {
                        "You take the blue pill... the story ends, you wake up in your bed. You take the red pill... you stay in Wonderland, and I show you how deep the rabbit hole goes. Remember: all we're offering is the truth."
                    }
                    userText.contains("hello", ignoreCase = true) || userText.contains("hi", ignoreCase = true) -> {
                        "Welcome to our safe harbor. Ask me about the Matrix, encryption, or simply toggle 'Database Inspection' to inspect what SQL persists!"
                    }
                    else -> "Your message sent encrypted bits through SQLite database space. Bob approves the absolute privacy."
                }
            }
            bot.phone.contains("3224") -> { // DevBot
                "Hello developer companion! I can inspect cipher state easily. Toggle the DB Inspector in the top right to analyze encrypted BLOBs."
            }
            else -> {
                "Hello! This is an end-to-end encrypted auto-generated test reply from Clara. We are safe! ✨"
            }
        }

        val botCipher = CryptoEngine.encrypt(responseText, channelSeed)
        val botMessage = Message(
            senderPhone = bot.phone,
            receiverPhone = activeSession.value?.phone ?: "+1000",
            ciphertext = botCipher,
            isEncrypted = true,
            status = "READ"
        )
        messageDao.insertMessage(botMessage)
    }

    fun toggleDbInspection() {
        isDbInspectionMode.value = !isDbInspectionMode.value
    }

    // 3. SECURE CALLING SYSTEM

    fun initiateCall(contact: Contact, type: String) {
        val caller = activeSession.value ?: return
        viewModelScope.launch {
            val outgoingLog = CallLog(
                contactPhone = contact.phone,
                contactName = contact.name,
                contactAvatarColor = contact.avatarColorHex,
                callType = type,
                isIncoming = false,
                status = "CONNECTED",
                durationSeconds = 0
            )
            // Hold log temporarily in the active state
            activeCall.value = outgoingLog
            callTimeSeconds.value = 0

            // Simulate ringing response animation, then connect
            delay(2000)
            activeCall.value = outgoingLog.copy(status = "CONNECTED")
        }
    }

    fun simulateIncomingCall(contact: Contact, type: String) {
        val receiver = activeSession.value ?: return
        viewModelScope.launch {
            val incomingLog = CallLog(
                contactPhone = contact.phone,
                contactName = contact.name,
                contactAvatarColor = contact.avatarColorHex,
                callType = type,
                isIncoming = true,
                status = "RINGING",
                durationSeconds = 0
            )
            activeCall.value = incomingLog
            callTimeSeconds.value = 0
        }
    }

    fun acceptCall() {
        val current = activeCall.value ?: return
        activeCall.value = current.copy(status = "CONNECTED")
        callTimeSeconds.value = 0
    }

    fun hangupCall() {
        val current = activeCall.value ?: return
        viewModelScope.launch {
            val finalLog = current.copy(
                durationSeconds = callTimeSeconds.value,
                status = if (current.status == "RINGING") "MISSED" else "CONNECTED",
                timestamp = System.currentTimeMillis()
            )
            callLogDao.insertCallLog(finalLog)
            activeCall.value = null
            callTimeSeconds.value = 0
        }
    }

    // 4. STATUS ENCRYPTED UPDATER

    fun postMyStatus() {
        val text = myStatusInputText.value.trim()
        val session = activeSession.value
        if (text.isEmpty() || session == null) return

        viewModelScope.launch {
            val update = StatusUpdate(
                contactPhone = session.phone,
                contactName = session.name,
                contactAvatarColor = "#10B981",
                text = text,
                backgroundColorHex = statusGradients[myStatusColorIndex.value],
                timestamp = System.currentTimeMillis(),
                viewCount = 0
            )
            statusDao.insertStatus(update)
            myStatusInputText.value = ""
            myStatusColorIndex.value = 0
        }
    }

    fun viewStatuses(updates: List<StatusUpdate>) {
        currentStatusViewer.value = updates
        statusSelectedIndex.value = 0
    }

    fun nextStatusStory() {
        val current = currentStatusViewer.value ?: return
        val currentIdx = statusSelectedIndex.value
        if (currentIdx < current.size - 1) {
            statusSelectedIndex.value = currentIdx + 1
        } else {
            currentStatusViewer.value = null
        }
    }

    fun prevStatusStory() {
        val currentIdx = statusSelectedIndex.value
        if (currentIdx > 0) {
            statusSelectedIndex.value = currentIdx - 1
        } else {
            currentStatusViewer.value = null
        }
    }

    fun clearSession() {
        viewModelScope.launch {
            userSessionDao.clearSession()
            currentChatContact.value = null
        }
    }

    fun resetDatabaseToDefaults() {
        viewModelScope.launch {
            messageDao.clearAllMessages()
            callLogDao.clearCallLogs()
            // Clear current selections
            currentChatContact.value = null
            activeCall.value = null
            currentStatusViewer.value = null
        }
    }

    fun addNewContact(contact: Contact) {
        viewModelScope.launch {
            contactDao.insertContact(contact)
        }
    }
}
