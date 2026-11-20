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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.naph.app.ui.components.ModernGlassButton
import com.naph.app.ui.components.ModernIconPill
import com.naph.app.ui.components.ModernPrimaryButton
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
        // Top-right close icon
        ModernIconPill(
            icon = Icons.Default.Close,
            contentDescription = "Close",
            onClick = { appState.toggleProfilePicker(false) },
            size = 38.dp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(28.dp)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "Who's Watching?",
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 28.sp),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Select a profile, create a new one, or delete profiles anytime",
                style = MaterialTheme.typography.bodyMedium,
                color = NaphColors.TextSecondary
            )

            Spacer(Modifier.height(34.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                profiles.forEach { profile ->
                    val isSelected = profile.id == activeProfile.id
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF10131C))
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(18.dp)
                            )
                            .clickable { appState.switchProfile(profile) }
                            .padding(16.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(104.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(Color(profile.avatarColorHex).copy(alpha = 0.22f))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) Color.White else Color(profile.avatarColorHex).copy(alpha = 0.7f),
                                        shape = RoundedCornerShape(22.dp)
                                    )
                            ) {
                                Text(text = profile.avatarEmoji, fontSize = 42.sp)
                            }

                            // Delete Profile Button directly on the top-right corner of every profile card
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xE6141722))
                                    .border(1.dp, NaphColors.CrimsonPlay.copy(alpha = 0.6f), CircleShape)
                                    .clickable { appState.deleteProfile(profile.id) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete ${profile.name}",
                                    tint = NaphColors.CrimsonPlay,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = profile.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                            color = if (isSelected) Color.White else NaphColors.TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Delete",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = NaphColors.CrimsonPlay,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NaphColors.CrimsonPlay.copy(alpha = 0.12f))
                                .clickable { appState.deleteProfile(profile.id) }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                // Add Profile Card
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF10131C))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                        .clickable { showCreateForm = !showCreateForm }
                        .padding(16.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(104.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(22.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Profile",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "New Profile",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                        color = NaphColors.TextSecondary
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "+ Create",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 3.dp)
                    )
                }
            }

            if (showCreateForm) {
                Spacer(Modifier.height(24.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .width(400.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF12151E))
                        .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
                        .padding(18.dp)
                ) {
                    Text("Create Profile", style = MaterialTheme.typography.titleMedium, color = Color.White)
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
                            label = { Text("Icon") },
                            singleLine = true,
                            colors = naphTextFieldColors(),
                            modifier = Modifier.width(84.dp)
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
                        ModernPrimaryButton(
                            text = "Create Profile",
                            icon = Icons.Default.Add,
                            onClick = {
                                appState.addProfile(newName, newEmoji, isKids)
                                newName = ""
                                showCreateForm = false
                            }
                        )
                        ModernGlassButton(
                            text = "Cancel",
                            onClick = { showCreateForm = false }
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            ModernGlassButton(
                text = "Done",
                onClick = { appState.toggleProfilePicker(false) }
            )
        }
    }
}
