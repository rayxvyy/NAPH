package com.naph.app.ui.screens.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naph.app.ui.state.NaphAppState
import com.naph.app.ui.theme.NaphColors
import com.naph.app.ui.theme.naphTextFieldColors

@Composable
fun ProfilePickerOverlay(
    appState: NaphAppState,
    modifier: Modifier = Modifier
) {
    val profiles by appState.profiles.collectAsState()
    val activeProfile by appState.activeProfile.collectAsState()

    var showCreateForm by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newEmoji by remember { mutableStateOf("🍿") }
    var isKids by remember { mutableStateOf(false) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xF5070709))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "Who's Watching NAPH?",
                style = MaterialTheme.typography.displayLarge,
                color = Color.White
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Each profile keeps its own separate watch progress, watchlist, and preferences across Windows, Linux, iOS & Android",
                style = MaterialTheme.typography.bodyLarge,
                color = NaphColors.TextSecondary
            )

            Spacer(Modifier.height(38.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                profiles.forEach { profile ->
                    val isSelected = profile.id == activeProfile.id
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { appState.switchProfile(profile) }
                            .padding(12.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(112.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(profile.avatarColorHex).copy(alpha = 0.25f))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color(profile.avatarColorHex),
                                    shape = RoundedCornerShape(24.dp)
                                )
                        ) {
                            Text(text = profile.avatarEmoji, fontSize = 46.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = profile.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isSelected) Color.White else NaphColors.TextSecondary,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { showCreateForm = !showCreateForm }
                        .padding(12.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(112.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Profile",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Add Profile", style = MaterialTheme.typography.titleMedium, color = NaphColors.TextSecondary)
                }
            }

            if (showCreateForm) {
                Spacer(Modifier.height(28.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .width(420.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                        .padding(20.dp)
                ) {
                    Text("Create Household Profile", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Profile Name") },
                            singleLine = true,
                            colors = naphTextFieldColors(),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newEmoji,
                            onValueChange = { newEmoji = it },
                            label = { Text("Emoji") },
                            singleLine = true,
                            colors = naphTextFieldColors(),
                            modifier = Modifier.width(90.dp)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Kids Safe Catalog Mode", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        Spacer(Modifier.width(12.dp))
                        Switch(checked = isKids, onCheckedChange = { isKids = it })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                appState.addProfile(newName, newEmoji, isKids)
                                newName = ""
                                showCreateForm = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Create & Switch", color = Color.White)
                        }
                        OutlinedButton(onClick = { showCreateForm = false }) {
                            Text("Cancel", color = Color.White)
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
            OutlinedButton(
                onClick = { appState.toggleProfilePicker(false) },
                shape = CircleShape
            ) {
                Text("Continue as ${activeProfile.name}", color = Color.White)
            }
        }
    }
}
