package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.db.ChatDatabase
import com.example.data.repository.ChatRepository
import com.example.ui.screens.GoChatApp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.ChatViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize local SQLite Room Database
        val database = ChatDatabase.getDatabase(applicationContext)
        val chatDao = database.chatDao()
        val repository = ChatRepository(chatDao)
        
        // Initialize state view-model with constructor dependency injection
        val viewModelFactory = ChatViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, viewModelFactory)[ChatViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                GoChatApp(viewModel)
            }
        }
    }
}
