package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.data.*
import com.example.viewmodel.ChatViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CipherWireApp(viewModel: ChatViewModel) {
    val activeSession by viewModel.activeSession.collectAsState()
    val activeCall by viewModel.activeCall.collectAsState()
    val currentStatusViewer by viewModel.currentStatusViewer.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                // 1. Current Active Call screen takes absolute precedence
                activeCall != null -> {
                    CallScreen(viewModel = viewModel, activeCallLog = activeCall!!)
                }
                // 2. Full-screen Status Story viewer
                currentStatusViewer != null -> {
                    StatusStoriesPlayer(viewModel = viewModel, stories = currentStatusViewer!!)
                }
                // 3. User Login Required
                activeSession == null -> {
                    LoginScreen(viewModel = viewModel)
                }
                // 4. Main App Hub
                else -> {
                    DashboardScreen(viewModel = viewModel, session = activeSession!!)
                }
            }

            // Simulated OTP Alert Dialog / Top Banner Notification
            val bannerMessage = viewModel.otpBannerMessage.value
            if (bannerMessage != null) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 48.dp, start = 16.dp, end = 16.dp)
                        .fillMaxWidth()
                        .testTag("otp_notification"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Sms, contentDescription = "SMS OTP Notification")
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Simulated Carrier SMS Incoming",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Text(bannerMessage, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 1. PHONE INITIAL LOGIN WITH OTP SIMULATOR
// ==========================================

@Composable
fun LoginScreen(viewModel: ChatViewModel) {
    var phone by viewModel.loginPhone
    var name by viewModel.loginName
    var code by viewModel.smsOtpCode
    val isVerifying = viewModel.isVerifyingCode.value
    val error = viewModel.loginError.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .navigationBarsPadding()
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Aesthetic Cryptographic branding header
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = "🔒 Key Logo",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "INDChat",
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.SansSerif,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "END-TO-END ENCRYPTED SECURE NETWORKS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(36.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                if (!isVerifying) {
                    Text(
                        text = "Establish Security Profile",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Enter your mobile phone number and a display identity card to generate E2EE security profiles inside SQLite.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Display Name") },
                        placeholder = { Text("e.g. Neo") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_name_input"),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Filled.Person, "Name") }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        placeholder = { Text("+1 (555) 019-9234") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_phone_input"),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Filled.Phone, "Phone") }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { viewModel.sendOtp() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_submit_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Connect Securely", fontWeight = FontWeight.Bold)
                    }

                } else {
                    Text(
                        text = "Verify Cryptographic Handshake",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Confirm matching SMS digits to establish AES keys on-device storage databases safely.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("6-Digit OTP Code") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_otp_input"),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Filled.Sms, "SMS OTP") }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { viewModel.submitOtp() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("login_verify_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Verify OTP Code", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(
                        onClick = { viewModel.cancelOtpFlow() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Go Back & Change Number", color = MaterialTheme.colorScheme.error)
                    }
                }

                if (error != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Security Footnote
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Shield,
                contentDescription = "Encrypted",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                "E2EE SQLite Storage Handshake Verified",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ==========================================
// 2. DASHBOARD HUB (CHATS, STATUS, CALLS, KEYS)
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: ChatViewModel, session: UserSession) {
    var activeTab by remember { mutableStateOf(0) } // 0: Chats, 1: Status, 2: Calls, 3: E2EE Keys
    val currentChatContact by viewModel.currentChatContact.collectAsState()
    val isDbInspection by viewModel.isDbInspectionMode.collectAsState()

    if (currentChatContact != null) {
        ChatDetailScreen(viewModel = viewModel, session = session, contact = currentChatContact!!)
    } else {
        Scaffold(
            topBar = {
                Column {
                    // Elevated cryptographic topbar
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Lock,
                                    "INDChat padlock logo",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "INDChat",
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Badge(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                                    Text("E2EE", fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        },
                        actions = {
                            // Unique Database cipher inspection toggle
                            IconButton(onClick = { viewModel.toggleDbInspection() }) {
                                Icon(
                                    imageVector = if (isDbInspection) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = "Database raw inspection toggle",
                                    tint = if (isDbInspection) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }
                            IconButton(onClick = { viewModel.clearSession() }) {
                                Icon(
                                    imageVector = Icons.Filled.Logout,
                                    contentDescription = "Log out local secure session",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )

                    // DB Inspector Indicator banner
                    AnimatedVisibility(visible = isDbInspection) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.DeveloperMode,
                                    "Database inspect active logo",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "DB Inspector Active: Message views are displaying raw encrypted SQLite data.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Luxury Sliding Custom Tabs
                    TabRow(
                        selectedTabIndex = activeTab,
                        containerColor = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.primary,
                        indicator = { tabPositions ->
                            TabRowDefaults.Indicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                                height = 3.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    ) {
                        Tab(
                            selected = activeTab == 0,
                            onClick = { activeTab = 0 },
                            text = { Text("CHATS", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        )
                        Tab(
                            selected = activeTab == 1,
                            onClick = { activeTab = 1 },
                            text = { Text("STATUS", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        )
                        Tab(
                            selected = activeTab == 2,
                            onClick = { activeTab = 2 },
                            text = { Text("CALLS", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        )
                        Tab(
                            selected = activeTab == 3,
                            onClick = { activeTab = 3 },
                            text = { Text("E2EE KEY", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (activeTab) {
                    0 -> ChatsTab(viewModel = viewModel)
                    1 -> StatusTab(viewModel = viewModel)
                    2 -> CallsTab(viewModel = viewModel)
                    3 -> E2EEMetricsScreen(viewModel = viewModel, session = session)
                }
            }
        }
    }
}

// ==========================================
// 3. CHATS TAB (CONTACT LIST, ACTIONS)
// ==========================================

@Composable
fun ChatsTab(viewModel: ChatViewModel) {
    val contactList by viewModel.contacts.collectAsState()
    var searchField by remember { mutableStateOf("") }
    var showDialogAddContact by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Aesthetic modern styling search bar
        OutlinedTextField(
            value = searchField,
            onValueChange = { searchField = it },
            placeholder = { Text("Search encrypted channel contacts...", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Filled.Search, "Search logo", tint = MaterialTheme.colorScheme.outline) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        val filteredContacts = contactList.filter {
            it.name.contains(searchField, ignoreCase = true) || it.phone.contains(searchField)
        }

        if (filteredContacts.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = "Empty",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No Encrypted Contacts Found",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "Press the button below to establish a new AES/RSA validated contact profile.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(filteredContacts) { contact ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.currentChatContact.value = contact }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .testTag("contact_item_${contact.phone}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Colored Circle Initial Icon
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(
                                    color = Color(android.graphics.Color.parseColor(contact.avatarColorHex)),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = contact.name.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )

                            // Bot marker indicator dot
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(MaterialTheme.colorScheme.secondary, CircleShape)
                                    .align(Alignment.BottomEnd)
                                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = contact.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.VerifiedUser,
                                        "End to end encrypted secure link verified",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        "E2EE",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = contact.statusMessage,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 0.5.dp)
                }
            }
        }

        // Add Contact FAB Trigger
        Box(
            modifier = Modifier
                .align(Alignment.End)
                .padding(24.dp)
        ) {
            FloatingActionButton(
                onClick = { showDialogAddContact = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_contact_fab")
            ) {
                Icon(Icons.Filled.PersonAdd, "Add encrypted contact")
            }
        }
    }

    // Add Encrypted Contact Dialog
    if (showDialogAddContact) {
        var inputName by remember { mutableStateOf("") }
        var inputPhone by remember { mutableStateOf("") }
        var inputStatusMessage by remember { mutableStateOf("Encrypted and validated channel.") }

        AlertDialog(
            onDismissRequest = { showDialogAddContact = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Shield, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Secure Contact")
                }
            },
            text = {
                Column {
                    Text(
                        "Input the phone and metadata to negotiate device asymmetric public keys.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = inputPhone,
                        onValueChange = { inputPhone = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = inputStatusMessage,
                        onValueChange = { inputStatusMessage = it },
                        label = { Text("Security Status Message") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputName.isNotBlank() && inputPhone.isNotBlank()) {
                            // Let's create secure contact details
                            val randomHexColors = listOf("#0D9488", "#EC4899", "#6366F1", "#10B981", "#7C3AED")
                            val avatarColor = randomHexColors.random()
                            val mockContact = Contact(
                                phone = inputPhone.trim(),
                                name = inputName.trim(),
                                avatarColorHex = avatarColor,
                                statusMessage = inputStatusMessage.trim(),
                                publicKey = "CIPHER_WIRE_GENERATED_" + inputPhone.hashCode().toString(16).uppercase(),
                                isBot = true // Interactive simulated bots
                            )

                            // Insert contact safely via VM
                            viewModel.addNewContact(mockContact)
                            showDialogAddContact = false
                        }
                    }
                ) {
                    Text("Negotiate & Save Keys")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialogAddContact = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ==========================================
// 4. CHAT DETAIL PAGE WITH RAW PERSISTENCE INSPECTOR
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(viewModel: ChatViewModel, session: UserSession, contact: Contact) {
    val messages by viewModel.activeMessages.collectAsState()
    val isDbInspection by viewModel.isDbInspectionMode.collectAsState()
    val isTypingBot by viewModel.isTypingBot.collectAsState()
    var showSafetyShieldDialog by remember { mutableStateOf(false) }

    // Channel Seed is compiled to resolve decrypt coordinates
    val channelSeed = if (session.phone < contact.phone) {
        session.phone + "_" + contact.phone
    } else {
        contact.phone + "_" + session.phone
    }

    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredMessages = remember(messages, searchQuery, channelSeed) {
        if (searchQuery.isBlank()) {
            messages
        } else {
            messages.filter { msg ->
                val decrypted = CryptoEngine.decrypt(msg.ciphertext, channelSeed)
                decrypted.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (isSearching) {
                        IconButton(onClick = {
                            isSearching = false
                            searchQuery = ""
                        }) {
                            Icon(Icons.Filled.ArrowBack, "Exit message search", tint = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        IconButton(onClick = { viewModel.currentChatContact.value = null }) {
                            Icon(Icons.Filled.ArrowBack, "Go back to dashboard hub", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                title = {
                    if (isSearching) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search decrypted messages...", fontSize = 14.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("chat_search_input"),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            ),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Filled.Clear, "Clear search text", tint = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { showSafetyShieldDialog = true }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = Color(android.graphics.Color.parseColor(contact.avatarColorHex)),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = contact.name.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = contact.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isTypingBot) {
                                    Text(
                                        "typing...",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Filled.EnhancedEncryption,
                                            "Padlock status",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            "Tap for encryption credentials",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                actions = {
                    if (isSearching) {
                        // Empty when searching
                    } else {
                        IconButton(onClick = { isSearching = true }) {
                            Icon(Icons.Filled.Search, "Search messages within chat", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { viewModel.initiateCall(contact, "AUDIO") }) {
                            Icon(Icons.Filled.Phone, "Secure phone voice call")
                        }
                        IconButton(onClick = { viewModel.initiateCall(contact, "VIDEO") }) {
                            Icon(Icons.Filled.VideoCall, "Secure video stream call")
                        }
                        IconButton(onClick = { showSafetyShieldDialog = true }) {
                            Icon(Icons.Filled.Shield, "Verify asymmetric key pairs details", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // E2EE Disclaimer Header bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Lock,
                        "E2EE status verified informational logo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Messages and calls are fully end-to-end encrypted under AES-256 standard and stored on-device database.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 13.sp
                    )
                }
            }

            // Search result indicator banner
            if (isSearching && searchQuery.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Active search",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Found ${filteredMessages.size} matching message${if (filteredMessages.size == 1) "" else "s"}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        TextButton(
                            onClick = { searchQuery = "" },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text("Clear", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // Message list panel
            if (filteredMessages.isEmpty() && isSearching && searchQuery.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "No search results icon",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No Matches Found",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "Could not find any decrypted messages matching \"$searchQuery\".",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                ) {
                    items(filteredMessages) { msg ->
                    val isMine = msg.senderPhone == session.phone

                    // Decrypt the raw db ciphertext based on inspection mode state
                    val displayText = if (isDbInspection) {
                        msg.ciphertext // raw base64 displayed!
                    } else {
                        CryptoEngine.decrypt(msg.ciphertext, channelSeed) // decrypted inline!
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                contentColor = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isMine) 16.dp else 4.dp,
                                bottomEnd = if (isMine) 4.dp else 16.dp
                            ),
                            modifier = Modifier
                                .widthIn(max = 290.dp)
                                .testTag("chat_bubble_${msg.id}")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                if (isDbInspection) {
                                    // Cool hacker styling indicator for ciphertext
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Code,
                                            "Hacker DB raw code indicator",
                                            tint = if (isMine) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "SQLite Ciphertext (Base64)",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isMine) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }

                                Text(
                                    text = displayText,
                                    fontSize = 14.sp,
                                    lineHeight = 18.sp,
                                    fontFamily = if (isDbInspection) FontFamily.Monospace else FontFamily.Default
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.align(Alignment.End),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.Lock,
                                        "End to End lock confirmed",
                                        tint = if (isMine) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                        modifier = Modifier.size(9.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = DateUtils.formatDateTime(
                                            LocalContext.current,
                                            msg.timestamp,
                                            DateUtils.FORMAT_SHOW_TIME
                                        ),
                                        fontSize = 9.sp,
                                        color = if (isMine) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

            // Input Chat Panel Box
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = viewModel.chatInputField.value,
                    onValueChange = { viewModel.chatInputField.value = it },
                    placeholder = { Text("Secure encrypted message...", fontSize = 14.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_text_field"),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable { viewModel.sendMessage() }
                        .testTag("chat_send_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Send,
                        contentDescription = "Send message securely",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    // Interactive security credentials shield bottom card / info dialog
    if (showSafetyShieldDialog) {
        AlertDialog(
            onDismissRequest = { showSafetyShieldDialog = false },
            icon = { Icon(Icons.Filled.VerifiedUser, "Shield", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp)) },
            title = { Text("E2EE Verification Card") },
            text = {
                Column {
                    Text(
                        "You are communicating on a secure end-to-end symmetric channel utilizing cryptographic AES certificates on disk.",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Fingerprint mapping
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Channel Visual Fingerprint", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = CryptoEngine.getVisualFingerprint(session.phone, contact.phone),
                                fontSize = 15.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    // Keys listing
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Target Recipient Public ID", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                contact.publicKey,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(top = 4.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showSafetyShieldDialog = false }) {
                    Text("Trust & Dismiss")
                }
            }
        )
    }
}

// ==========================================
// 5. STATUS ENCRYPTED TAB & CREATOR LAYOUT
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusTab(viewModel: ChatViewModel) {
    val statusUpdatesList by viewModel.statuses.collectAsState()
    var displayCreator by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (!displayCreator) {
            // Status Feed Row layout
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                // Post status trigger item
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { displayCreator = true }
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Add,
                                "Add status card icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                "My Secure Status",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                "Share a message encrypted with your safety circle",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 0.5.dp)
                }

                item {
                    Text(
                        text = "Encrypted Status Feed Updates",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        letterSpacing = 1.sp
                    )
                }

                if (statusUpdatesList.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Outlined.PhotoCamera,
                                "No status camera logo",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No Dynamic Status Logs Available",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                } else {
                    // Group status updates by contactPhone
                    val grouped = statusUpdatesList.groupBy { it.contactPhone }
                    items(grouped.keys.toList()) { contactPhone ->
                        val groupList = grouped[contactPhone] ?: emptyList()
                        val latest = groupList.first()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.viewStatuses(groupList) }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .testTag("status_feed_item_${contactPhone}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Circular Indicator around the Avatar
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .border(
                                        width = 3.dp,
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.secondary
                                            )
                                        ),
                                        shape = CircleShape
                                    )
                                    .padding(4.dp)
                                    .background(
                                        color = Color(android.graphics.Color.parseColor(latest.contactAvatarColor)),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    latest.contactName.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    latest.contactName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    DateUtils.getRelativeTimeSpanString(
                                        latest.timestamp,
                                        System.currentTimeMillis(),
                                        DateUtils.MINUTE_IN_MILLIS
                                    ).toString(),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), thickness = 0.5.dp)
                    }
                }
            }
        } else {
            // Creators Interface layout
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { displayCreator = false }) {
                        Icon(Icons.Filled.Close, "Exit creator screen")
                    }
                    Text(
                        "Compose Encrypted Status Update",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Button(
                        onClick = {
                            viewModel.postMyStatus()
                            displayCreator = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Post Status", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Themed gradient card selector
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(android.graphics.Color.parseColor(viewModel.statusGradients[viewModel.myStatusColorIndex.value]))
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TextField(
                            value = viewModel.myStatusInputText.value,
                            onValueChange = { viewModel.myStatusInputText.value = it },
                            placeholder = {
                                Text(
                                    "What's on your mind? Everything you post remains locally securely on-device logs.",
                                    color = Color.White.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center,
                                    fontSize = 20.sp
                                )
                            },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                fontSize = 22.sp,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("status_composition_input")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Color picker bubble nodes row
                Text(
                    "Select Encrypted Card Color Theme Theme:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    viewModel.statusGradients.forEachIndexed { idx, colHex ->
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(colHex)))
                                .border(
                                    width = if (viewModel.myStatusColorIndex.value == idx) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                )
                                .clickable { viewModel.myStatusColorIndex.value = idx }
                        )
                    }
                }
            }
        }
    }
}

// Full screen Immersive status slider view
@Composable
fun StatusStoriesPlayer(viewModel: ChatViewModel, stories: List<StatusUpdate>) {
    val activeIdx = viewModel.statusSelectedIndex.value
    val currentStory = stories.getOrNull(activeIdx)

    // Launch automated timer to transition status stories
    LaunchedEffect(activeIdx) {
        delay(6000) // auto rotate story after 6 seconds
        viewModel.nextStatusStory()
    }

    if (currentStory != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(android.graphics.Color.parseColor(currentStory.backgroundColorHex)))
                .testTag("status_immersive_player")
        ) {
            // Immersive background text / decoration space
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = currentStory.text,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 36.sp,
                    fontFamily = FontFamily.SansSerif
                )
            }

            // Top Status navigation indicators bars
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Progress segments representation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    stories.forEachIndexed { idx, _ ->
                        val barColor = if (idx <= activeIdx) Color.White else Color.White.copy(alpha = 0.3f)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .background(barColor, RoundedCornerShape(2.dp))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Profile card header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = { viewModel.currentStatusViewer.value = null }) {
                        Icon(Icons.Filled.ArrowBack, "Close story logs preview", tint = Color.White)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                color = Color(android.graphics.Color.parseColor(currentStory.contactAvatarColor)),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            currentStory.contactName.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            currentStory.contactName,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            "Secure Status Story",
                            color = Color.White.copy(alpha = 0.60f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Click trackers overlays (Left takes back, Right goes forward)
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { viewModel.prevStatusStory() }
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { viewModel.nextStatusStory() }
                )
            }
        }
    }
}

// ==========================================
// 6. CALLS HISTORY DATABASE RECORDS VIEW
// ==========================================

@Composable
fun CallsTab(viewModel: ChatViewModel) {
    val logs by viewModel.callLogs.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Encrypted Audio & Video Session Logs",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )

            // Dynamic simulator trigger of incoming call
            TextButton(
                onClick = {
                    // Trigger dynamic morpheus/bob video call simulation!
                    val contactSeed = Contact(
                        phone = "+14155552671",
                        name = "Bob [Morpheus]",
                        avatarColorHex = "#6366F1",
                        statusMessage = "Incoming encrypted channel verification.",
                        publicKey = "CIPHER_WIRE_BOB_RSA2048_PUB_KEY"
                    )
                    viewModel.simulateIncomingCall(contactSeed, "VIDEO")
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.PhoneInTalk, null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Simulate Inbound", fontSize = 12.sp)
                }
            }
        }

        if (logs.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Outlined.RingVolume,
                    "Empty lists logo",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "No Audio/Video History Found",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(logs) { log ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 13.dp)
                            .testTag("call_log_item_${log.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    color = Color(android.graphics.Color.parseColor(log.contactAvatarColor)),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                log.contactName.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                log.contactName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val callDirectionIcon = if (log.isIncoming) {
                                    if (log.durationSeconds == 0) Icons.Filled.CallMissed else Icons.Filled.CallReceived
                                } else {
                                    Icons.Filled.CallMade
                                }
                                val activeColor = if (log.isIncoming && log.durationSeconds == 0) {
                                    MaterialTheme.colorScheme.error // Red for missed
                                } else {
                                    MaterialTheme.colorScheme.primary // Green/Teal for completed
                                }

                                Icon(
                                    imageVector = callDirectionIcon,
                                    contentDescription = "Direction status Icon",
                                    tint = activeColor,
                                    modifier = Modifier.size(14.dp)
                                )

                                Spacer(modifier = Modifier.width(6.dp))

                                val statusInfo = if (log.durationSeconds == 0) {
                                    "Missed Call"
                                } else {
                                    "Connected duration (${log.durationSeconds}s)"
                                }

                                Text(
                                    text = "$statusInfo — " + DateUtils.getRelativeTimeSpanString(
                                        log.timestamp,
                                        System.currentTimeMillis(),
                                        DateUtils.MINUTE_IN_MILLIS
                                    ),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Call indicator icon (audio vs video)
                        Icon(
                            imageVector = if (log.callType == "VIDEO") Icons.Filled.VideoCall else Icons.Filled.Phone,
                            contentDescription = "Call specification type icon",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), thickness = 0.5.dp)
                }
            }
        }
    }
}

// ==========================================
// 7. E2EE CRYPTOGRAPHIC ENGINE WALKTHROUGH SCREEN
// ==========================================

@Composable
fun E2EEMetricsScreen(viewModel: ChatViewModel, session: UserSession) {
    var showExplanationDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Aesthetic holographic shield icon
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Shield,
                "Shield security E2EE",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            "Device Cryptographic Node",
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            "Keys generated locally on device via AES and Base64 schema mapping protocols.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Decoupled keys listings
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Primary Device Profile Metadata",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Validated Phone ID:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(session.phone, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Display Card Name:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(session.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                // Public Key details
                Text(
                    "Asymmetric Handshake Public Key (Shared):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = session.publicKey,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Private Key lock
                Text(
                    "Asymmetric Handshake Private Key (Secret - Locked on-disk SQLite):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Key, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "•••••••••••••••••••••••••••••••••",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Info Button triggers
        Button(
            onClick = { showExplanationDialog = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Understand INDChat Cryptography", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { viewModel.resetDatabaseToDefaults() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Reset Local Simulation Log Databases", fontWeight = FontWeight.Bold)
        }
    }

    if (showExplanationDialog) {
        AlertDialog(
            onDismissRequest = { showExplanationDialog = false },
            title = { Text("Asymmetric E2EE Architecture") },
            text = {
                Text(
                    "Normally messaging apps read your communications on servers. INDChat implements client-side encryption.\n\n" +
                            "1. AES: Outgoing messages are compiled into solid base64 ciphertext blocks before insertion in databases.\n\n" +
                            "2. Asymmetric RSA: Every recipient has a public-key pairing. Only their private local credential can restore plaintexts in device memory. It is complete, leakproof isolation.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(onClick = { showExplanationDialog = false }) {
                    Text("Dismiss Info")
                }
            }
        )
    }
}

// ==========================================
// 8. FULL-SCREEN AUDIO & VIDEO CALLING ENGINE (CAMERAX OPTIONAL)
// ==========================================

@Composable
fun CallScreen(viewModel: ChatViewModel, activeCallLog: CallLog) {
    val durationSeconds = viewModel.callTimeSeconds.value
    val isMuted = viewModel.isCallMuted.value
    val isSpeakerOn = viewModel.isCallSpeakerOn.value
    var cameraPermissionGranted by remember { mutableStateOf(false) }

    // Launcher request for Camera permissions if VIDEO call is triggered
    val context = LocalContext.current
    val systemCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        cameraPermissionGranted = isGranted
    }

    LaunchedEffect(activeCallLog.callType) {
        if (activeCallLog.callType == "VIDEO") {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                cameraPermissionGranted = true
            } else {
                systemCameraLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // Slate Dark solid Background
    ) {
        if (activeCallLog.callType == "VIDEO") {
            // Render Live Camera Feed layer if permission is granted
            if (cameraPermissionGranted) {
                CameraPreviewLayout()
            } else {
                // Interactive cryptographic motion wireframe aesthetic if camera is deactivated/empty
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val pulseInfiniteState = rememberInfiniteTransition()
                    val pulseScale by pulseInfiniteState.animateFloat(
                        initialValue = 0.85f,
                        targetValue = 1.15f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1500, easing = LinearOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        )
                    )

                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.40f), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .graphicsLayer(scaleX = pulseScale * 0.8f, scaleY = pulseScale * 0.8f)
                            .border(1.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.50f), CircleShape)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Lock,
                            "E2EE Secure video logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Encrypted Stream",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        }

        // Overlay Call Card Details
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                // Circle profile image area
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            color = Color(android.graphics.Color.parseColor(activeCallLog.contactAvatarColor)),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        activeCallLog.contactName.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 36.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = activeCallLog.contactName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.EnhancedEncryption,
                        "Lock verification icon secure call",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (activeCallLog.status == "RINGING") {
                            "SECURE RINGING..."
                        } else {
                            "END-TO-END ENCRYPTED (${formatDuration(durationSeconds)})"
                        },
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Waveform simulation
            if (activeCallLog.status == "CONNECTED" && activeCallLog.callType == "AUDIO") {
                AudioWaveformPulseIndicator()
            }

            // Floating Controls bar Row representation
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.navigationBarsPadding()) {
                if (activeCallLog.status == "RINGING" && activeCallLog.isIncoming) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        FloatingActionButton(
                            onClick = { viewModel.acceptCall() },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White,
                            shape = CircleShape
                        ) {
                            Icon(Icons.Filled.Call, "Accept call stream")
                        }

                        FloatingActionButton(
                            onClick = { viewModel.hangupCall() },
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = Color.White,
                            shape = CircleShape
                        ) {
                            Icon(Icons.Filled.CallEnd, "Reject call stream")
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.isCallMuted.value = !isMuted },
                            modifier = Modifier
                                .size(50.dp)
                                .background(if (isMuted) Color.White.copy(alpha = 0.2f) else Color.Transparent, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                                contentDescription = "Mute audio feed toggle",
                                tint = Color.White
                            )
                        }

                        FloatingActionButton(
                            onClick = { viewModel.hangupCall() },
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.testTag("end_call_fab")
                        ) {
                            Icon(Icons.Filled.CallEnd, "End encrypted session call")
                        }

                        IconButton(
                            onClick = { viewModel.isCallSpeakerOn.value = !isSpeakerOn },
                            modifier = Modifier
                                .size(50.dp)
                                .background(if (!isSpeakerOn) Color.White.copy(alpha = 0.2f) else Color.Transparent, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isSpeakerOn) Icons.Filled.VolumeUp else Icons.Filled.VolumeMute,
                                contentDescription = "Speaker audio feed toggle",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

// Simple layout mapping actual CameraX provider stream helper
@Composable
fun CameraPreviewLayout() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val streamPreviewView = remember { PreviewView(context) }

    LaunchedEffect(Unit) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        try {
            val cameraProvider = providerFuture.get()
            val selector = CameraSelector.DEFAULT_FRONT_CAMERA
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(streamPreviewView.surfaceProvider)
            
            // Unbind all prior stream mappings and register active ones
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview)
        } catch (_: Exception) {
            // gracefully catch if provider initialization behaves differently
        }
    }

    AndroidView(
        factory = { streamPreviewView },
        modifier = Modifier
            .fillMaxSize()
            .testTag("camerax_view_finder")
    )
}

// Simulated active audio waveform bars pulsing
@Composable
fun AudioWaveformPulseIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(horizontal = 48.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val pulseCount = 12
        val infiniteTransition = rememberInfiniteTransition()

        for (i in 0 until pulseCount) {
            val duration = (400 + i * 85)
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.15f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(duration, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight(scale)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
            )
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}
