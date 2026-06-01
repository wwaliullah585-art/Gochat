package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.models.ChatMessage
import com.example.data.models.ChatSession
import com.example.ui.theme.*
import com.example.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun GoChatApp(viewModel: ChatViewModel) {
    val activeSessionId by viewModel.activeSessionId.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        AnimatedContent(
            targetState = activeSessionId,
            transitionSpec = {
                if (targetState != null) {
                    // Slide in details
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut()
                } else {
                    // Slide out details
                    slideInHorizontally { width -> -width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> width } + fadeOut()
                }
            },
            label = "ScreenTransition"
        ) { targetId ->
            if (targetId == null) {
                ChatDashboard(
                    viewModel = viewModel,
                    onSelectSession = { sessionId -> viewModel.selectSession(sessionId) }
                )
            } else {
                ChatRoom(
                    sessionId = targetId,
                    viewModel = viewModel,
                    onBack = { viewModel.selectSession(null) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDashboard(
    viewModel: ChatViewModel,
    onSelectSession: (String) -> Unit
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Dynamic filtering based on search query
    val filteredSessions = remember(sessions, searchQuery) {
        if (searchQuery.isBlank()) {
            sessions
        } else {
            sessions.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.lastMessage.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "GoChat",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("dashboard_title")
                    )
                },
                actions = {
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("new_chat_fab")
            ) {
                Icon(Icons.Filled.Message, contentDescription = "New Chat")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
        ) {
            // Authentic Search Banner
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Search friends, bots, or history...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = SlateGray) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("search_field"),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = SlateGray.copy(alpha = 0.4f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                singleLine = true
            )

            // Dynamic Active Contacts Status Tray (Stories)
            ActiveStatusTray(
                sessions = sessions,
                onSelectSession = onSelectSession
            )

            HorizontalDivider(color = SlateGray.copy(alpha = 0.15f), thickness = 1.dp)

            // Conversation Log/List
            if (filteredSessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Message,
                            contentDescription = null,
                            tint = SlateGray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "No chats available. Click + to begin!" else "No matching chats found.",
                            color = SlateGray,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .testTag("chat_list")
                ) {
                    items(filteredSessions, key = { it.id }) { session ->
                        ChatSessionRow(
                            session = session,
                            onClick = { onSelectSession(session.id) },
                            onDelete = { viewModel.deleteSession(session.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        CreateChatDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, isAi, topic ->
                viewModel.startNewSession(
                    name = name,
                    subtitle = if (isAi) "Custom Gemini Helper" else topic,
                    isAi = isAi,
                    colorHex = when (isAi) {
                        true -> 0xFF4285F4.toInt()
                        false -> listOf(0xFFFF6E40, 0xFF9C27B0, 0xFF00BFA5, 0xFFFFC107).random().toInt()
                    }
                )
                showAddDialog = false
            }
        )
    }

    if (showSettingsDialog) {
        AppSettingsDialog(
            isKeyAvailable = viewModel.isApiKeyConfigured,
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
fun ActiveStatusTray(
    sessions: List<ChatSession>,
    onSelectSession: (String) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(
            text = "ACTIVE CHANNELS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = SlateGray,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(sessions) { session ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onSelectSession(session.id) }
                        .padding(4.dp)
                ) {
                    Box(modifier = Modifier.size(58.dp)) {
                        // Avatar view
                        AvatarView(
                            imageUrl = session.avatarUrl,
                            colorHex = session.avatarColor,
                            name = session.name,
                            size = 56.dp
                        )
                        // Online green indicator circle
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .align(Alignment.BottomEnd)
                                .background(Color.White, CircleShape)
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(ActiveGreen, CircleShape)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = session.name.split(" ").firstOrNull() ?: session.name,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(60.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatSessionRow(
    session: ChatSession,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { showMenu = true }
                )
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarView(
                imageUrl = session.avatarUrl,
                colorHex = session.avatarColor,
                name = session.name,
                size = 50.dp
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = session.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatTimestamp(session.lastTimestamp),
                        fontSize = 12.sp,
                        color = SlateGray
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (session.lastMessage.startsWith("http")) "[Attached File/Image]" else session.lastMessage,
                        fontSize = 14.sp,
                        color = if (session.unreadCount > 0) MaterialTheme.colorScheme.onBackground else SlateGray,
                        fontWeight = if (session.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (session.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .background(ActiveGreen, CircleShape)
                                .size(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = session.unreadCount.toString(),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Delete Conversation") },
                onClick = {
                    onDelete()
                    showMenu = false
                },
                leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = Color.Red) }
            )
        }
    }
}

@Composable
fun AvatarView(
    imageUrl: String?,
    colorHex: Int,
    name: String,
    size: androidx.compose.ui.unit.Dp
) {
    if (!imageUrl.isNullOrEmpty()) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "Profile Photo of $name",
            modifier = Modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .background(Color(colorHex), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val initials = remember(name) {
                name.trim().split(" ")
                    .mapNotNull { it.firstOrNull() }
                    .take(2)
                    .joinToString("")
                    .uppercase()
            }
            Text(
                text = initials,
                color = Color.White,
                fontSize = (size.value * 0.4).sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoom(
    sessionId: String,
    viewModel: ChatViewModel,
    onBack: () -> Unit
) {
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val activeMessages by viewModel.activeMessages.collectAsStateWithLifecycle()
    val currentSession = remember(sessions, sessionId) { sessions.find { it.id == sessionId } }
    var textMessage by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var showMediaSheet by remember { mutableStateOf(false) }

    // Scroll to bottom when message list changes
    LaunchedEffect(activeMessages.size) {
        if (activeMessages.isNotEmpty()) {
            listState.animateScrollToItem(activeMessages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { /* Profile info */ }
                    ) {
                        AvatarView(
                            imageUrl = currentSession?.avatarUrl,
                            colorHex = currentSession?.avatarColor ?: 0xFF00BFA5.toInt(),
                            name = currentSession?.name ?: "Chat Entity",
                            size = 40.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = currentSession?.name ?: "Support Desk",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentSession?.subtitle ?: "Online",
                                fontSize = 12.sp,
                                color = SlateGray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Elegant Background Wallpaper
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Main messages output area
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(activeMessages, key = { it.id }) { msg ->
                        MessageBubble(message = msg)
                    }
                }

                // Interactive Media Attachment panel toggled
                if (showMediaSheet) {
                    MediaAttachmentPanel(
                        onSelectMedia = { url, text ->
                            viewModel.sendMessage(text, url, isImage = true)
                            showMediaSheet = false
                        },
                        onClose = { showMediaSheet = false }
                    )
                }

                // API Key Alert Notice for AI Chat
                if (currentSession?.isAi == true && !viewModel.isApiKeyConfigured) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = OrangeFlame.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = OrangeFlame)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Gemini API key is not configured. Configured in AI Studio's Secrets as GEMINI_API_KEY.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }

                // Message Input Row
                Surface(
                    tonalElevation = 2.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Media Add button
                        IconButton(onClick = { showMediaSheet = !showMediaSheet }) {
                            Icon(
                                imageVector = if (showMediaSheet) Icons.Filled.Close else Icons.Filled.Add,
                                contentDescription = "Add Files",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Input Field
                        OutlinedTextField(
                            value = textMessage,
                            onValueChange = { textMessage = it },
                            placeholder = { Text("Message...") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("message_input"),
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 4,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = MaterialTheme.colorScheme.background,
                                unfocusedContainerColor = MaterialTheme.colorScheme.background
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Send
                            ),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (textMessage.isNotBlank()) {
                                        viewModel.sendMessage(textMessage)
                                        textMessage = ""
                                    }
                                }
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Send Floating Action
                        IconButton(
                            onClick = {
                                if (textMessage.isNotBlank()) {
                                    viewModel.sendMessage(textMessage)
                                    textMessage = ""
                                }
                            },
                            enabled = textMessage.isNotBlank(),
                            modifier = Modifier
                                .background(
                                    if (textMessage.isNotBlank()) MaterialTheme.colorScheme.primary else SlateGray.copy(alpha = 0.2f),
                                    CircleShape
                                )
                                .testTag("send_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Send,
                                contentDescription = "Send",
                                tint = if (textMessage.isNotBlank()) Color.White else SlateGray
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val bubbleColor = if (message.isMine) {
        if (isSystemInDarkTheme()) GoChatBubbleUserDark else GoChatBubbleUserLight
    } else {
        if (isSystemInDarkTheme()) GoChatBubblePartnerDark else GoChatBubblePartnerLight
    }

    val alignment = if (message.isMine) Alignment.CenterEnd else Alignment.CenterStart
    val shape = if (message.isMine) {
        RoundedCornerShape(16.dp, 16.dp, 0.dp, 16.dp)
    } else {
        RoundedCornerShape(16.dp, 16.dp, 16.dp, 0.dp)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentAlignment = alignment
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(bubbleColor, shape)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Sender display label if not mine
            if (!message.isMine) {
                Text(
                    text = message.senderName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            // Image display
            if (message.isImage && message.mediaUrl != null) {
                AsyncImage(
                    model = message.mediaUrl,
                    contentDescription = "Image Attach",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .padding(bottom = 6.dp),
                    contentScale = ContentScale.Crop
                )
            }

            // Message Body Text
            Text(
                text = message.messageText,
                fontSize = 15.sp,
                color = if (isSystemInDarkTheme()) Color.White else Color(0xFF1E2A32)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Sub Status Details (Timestamp + Read status logs)
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = formatTimestamp(message.timestamp),
                    fontSize = 10.sp,
                    color = SlateGray
                )
                if (message.isMine) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = when (message.status) {
                            "SENDING" -> Icons.Filled.Refresh
                            "ERROR" -> Icons.Filled.Warning
                            else -> Icons.Filled.Check
                        },
                        contentDescription = message.status,
                        modifier = Modifier.size(13.dp),
                        tint = when (message.status) {
                            "ERROR" -> OrangeFlame
                            "SENDING" -> SlateGray
                            else -> ActiveGreen
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MediaAttachmentPanel(
    onSelectMedia: (String, String) -> Unit,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Simulate File Uploads",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Close panel")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                AttachmentOption(
                    icon = Icons.Filled.Image,
                    label = "Hiking Map",
                    color = ActiveGreen,
                    onClick = {
                        onSelectMedia(
                            "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?q=80&w=300",
                            "Here is the map for our high trails weekend trip! 🏕️"
                        )
                    }
                )
                AttachmentOption(
                    icon = Icons.Filled.Code,
                    label = "Code snippet",
                    color = TechPurple,
                    onClick = {
                        onSelectMedia(
                            "https://images.unsplash.com/photo-1555066931-4365d14bab8c?q=80&w=300",
                            "Look at this robust structure. Looks very neat!"
                        )
                    }
                )
                AttachmentOption(
                    icon = Icons.Filled.Star,
                    label = "Gift Card",
                    color = OrangeFlame,
                    onClick = {
                        onSelectMedia(
                            "https://images.unsplash.com/photo-1549465220-1a8b9238cd48?q=80&w=300",
                            "Sent a GoChat Gold Premium Badge! 🎁"
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun AttachmentOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(color.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = color)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, color = SlateGray, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun CreateChatDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, isAi: Boolean, topic: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var isAi by remember { mutableStateOf(false) }
    var topic by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    "Start New Channel",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Channel / Friend Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { isAi = !isAi }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(checked = isAi, onCheckedChange = { isAi = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Is AI Assistant? (Powered by Gemini)")
                }

                if (!isAi) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = topic,
                        onValueChange = { topic = it },
                        label = { Text("Dialogue Status Topic") },
                        placeholder = { Text("e.g. Online, Busy coding...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onConfirm(name, isAi, if (topic.isBlank()) "Online" else topic)
                            }
                        },
                        enabled = name.isNotBlank()
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}

@Composable
fun AppSettingsDialog(
    isKeyAvailable: Boolean,
    onDismiss: () -> Unit
) {
    var deviceOwnerName by remember { mutableStateOf("Raaza Engineer") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "GoChat Settings",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text("Profile Settings", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = deviceOwnerName,
                    onValueChange = { deviceOwnerName = it },
                    label = { Text("Display Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text("Integrations Status", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isKeyAvailable) ActiveGreen.copy(alpha = 0.12f) else OrangeFlame.copy(alpha = 0.12f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isKeyAvailable) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                            contentDescription = null,
                            tint = if (isKeyAvailable) ActiveGreen else OrangeFlame
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isKeyAvailable) "Gemini API Integrated" else "AI API Key Missing",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = if (isKeyAvailable) "Connected to gemini-3.5-flash model" else "Setup key in AI Studio Secrets",
                                fontSize = 11.sp,
                                color = SlateGray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Done")
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
