package com.ultramusic.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultramusic.player.ai.GrokAIService

/**
 * AI SETTINGS SCREEN
 * 
 * Configure free AI services for battle intelligence:
 * - Groq (FREE, fast, recommended!)
 * - Grok (xAI, free tier)
 * - OpenRouter (many free models)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AISettingsScreen(
    grokAIService: GrokAIService,
    onNavigateBack: () -> Unit
) {
    val isConfigured by grokAIService.isConfigured.collectAsState()
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current
    
    var apiKey by remember { mutableStateOf("") }
    var selectedProvider by remember { mutableStateOf(GrokAIService.AIProvider.GROQ) }
    var showApiKey by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🤖 AI Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A2E)
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Card
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isConfigured) Color(0xFF1B5E20) else Color(0xFF4A4A4A)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isConfigured) Icons.Default.Check else Icons.Default.Psychology,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    if (isConfigured) Color(0xFF4CAF50) else Color(0xFF666666),
                                    CircleShape
                                )
                                .padding(12.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                if (isConfigured) "AI ENABLED ✓" else "AI NOT CONFIGURED",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                if (isConfigured) 
                                    "Battle AI is ready to help you win!" 
                                else 
                                    "Set up a FREE API key below",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
            
            // Provider Selection
            item {
                Text(
                    "Select AI Provider (All FREE!):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Groq (Recommended)
            item {
                ProviderCard(
                    name = "Groq",
                    description = "⚡ FASTEST! Free tier with 14,400 requests/day",
                    url = "https://console.groq.com/keys",
                    isSelected = selectedProvider == GrokAIService.AIProvider.GROQ,
                    isRecommended = true,
                    onSelect = { selectedProvider = GrokAIService.AIProvider.GROQ },
                    onOpenUrl = { uriHandler.openUri("https://console.groq.com/keys") }
                )
            }
            
            // Grok
            item {
                ProviderCard(
                    name = "Grok (xAI)",
                    description = "🧠 xAI's Grok - Free beta access",
                    url = "https://console.x.ai/",
                    isSelected = selectedProvider == GrokAIService.AIProvider.GROK,
                    isRecommended = false,
                    onSelect = { selectedProvider = GrokAIService.AIProvider.GROK },
                    onOpenUrl = { uriHandler.openUri("https://console.x.ai/") }
                )
            }
            
            // OpenRouter
            item {
                ProviderCard(
                    name = "OpenRouter",
                    description = "🌐 Many free models available",
                    url = "https://openrouter.ai/keys",
                    isSelected = selectedProvider == GrokAIService.AIProvider.OPENROUTER,
                    isRecommended = false,
                    onSelect = { selectedProvider = GrokAIService.AIProvider.OPENROUTER },
                    onOpenUrl = { uriHandler.openUri("https://openrouter.ai/keys") }
                )
            }
            
            // API Key Input
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Enter your API Key:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    placeholder = { Text("sk-... or gsk_...") },
                    visualTransformation = if (showApiKey) 
                        VisualTransformation.None 
                    else 
                        PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                if (showApiKey) Icons.Default.VisibilityOff 
                                else Icons.Default.Visibility,
                                "Toggle visibility"
                            )
                        }
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Key, "API Key")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
            
            // Save Button
            item {
                Button(
                    onClick = {
                        if (apiKey.isNotBlank()) {
                            grokAIService.configure(apiKey, selectedProvider)
                            testResult = "✅ API Key saved! AI is now enabled."
                        } else {
                            testResult = "❌ Please enter an API key"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    enabled = apiKey.isNotBlank()
                ) {
                    Text("💾 SAVE & ENABLE AI", fontWeight = FontWeight.Bold)
                }
                
                testResult?.let { result ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        result,
                        color = if (result.startsWith("✅")) Color(0xFF4CAF50) else Color(0xFFFF5252)
                    )
                }
            }
            
            // Instructions
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "📋 How to get FREE API Key:",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        InstructionStep(1, "Click the 🔗 link on your chosen provider")
                        InstructionStep(2, "Sign up / Log in (free account)")
                        InstructionStep(3, "Create new API key")
                        InstructionStep(4, "Copy and paste it here")
                        InstructionStep(5, "Click SAVE - You're done! 🎉")
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "💡 Groq is recommended - it's the fastest and has generous free limits!",
                            color = Color(0xFF4CAF50),
                            fontSize = 13.sp
                        )
                    }
                }
            }
            
            // What AI Does
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A2E)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "🎯 What AI Powers:",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        AIFeatureItem("🎵 Smart Counter Song Selection")
                        AIFeatureItem("📊 Battle Strategy Recommendations")
                        AIFeatureItem("🏷️ Auto Clip Purpose Detection")
                        AIFeatureItem("🎤 Live Battle Commentary")
                        AIFeatureItem("👥 Crowd Reaction Predictions")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderCard(
    name: String,
    description: String,
    url: String,
    isSelected: Boolean,
    isRecommended: Boolean,
    onSelect: () -> Unit,
    onOpenUrl: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1E3A5F) else Color(0xFF2A2A2A)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        name,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (isRecommended) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "RECOMMENDED",
                            fontSize = 10.sp,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(Color(0xFF4CAF50), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    description,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
            
            IconButton(onClick = onOpenUrl) {
                Icon(
                    Icons.Default.OpenInNew,
                    "Open URL",
                    tint = Color(0xFF2196F3)
                )
            }
        }
    }
}

@Composable
private fun InstructionStep(number: Int, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(Color(0xFF4CAF50), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                number.toString(),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text,
            color = Color.White,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun AIFeatureItem(text: String) {
    Text(
        text,
        color = Color.White.copy(alpha = 0.9f),
        fontSize = 14.sp,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}
