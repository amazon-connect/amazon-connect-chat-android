@file:OptIn(ExperimentalMaterial3Api::class)

package com.amazon.connect.chat.androidchatexample.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.amazon.connect.chat.androidchatexample.Config
import com.amazon.connect.chat.androidchatexample.viewmodel.ChatViewModel

@Composable
fun ConfigPicker(viewModel: ChatViewModel) {
    val configOptions = listOf(
        "Micheal acc",
        "Rajat acc",
        "mobile-bug-bash-1",
        "mobile-bug-bash-2",
        "mobile-bug-bash-3",
        "mobile-bug-bash-4",
        "mobile-bug-bash-5",
        "mobile-bug-bash-6",
        "mobile-bug-bash-7",
        "mobile-bug-bash-8"
    )
    val selectedConfigIndex by viewModel.selectedConfigIndex.observeAsState(0)
    var expanded by remember { mutableStateOf(false) }
    var selectedOptionText by remember { mutableStateOf(configOptions[selectedConfigIndex]) }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Note: If you see Participant Token available, DO NOT change your account from dropdown, just start the chat to resume previous session",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 36.dp).fillMaxWidth().align(Alignment.CenterHorizontally),
            color = Color.Red,
            textAlign = TextAlign.Center
        )

        Text("Select Configuration", style = MaterialTheme.typography.bodyLarge)


        // Exposed dropdown menu for selecting configuration
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            TextField(
                value = selectedOptionText,
                onValueChange = {},
                readOnly = true,
                label = { Text("Configuration") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )

            // The actual dropdown menu items
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }  // Dismiss when clicking outside
            ) {
                configOptions.forEachIndexed { index, option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            selectedOptionText = option
                            viewModel.setSelectedConfig(index)
                            expanded = false  // Close the dropdown after selection
                        }
                    )
                }
            }
        }

        val selectedConfig = Config.configurations[selectedConfigIndex]
        Column(
            modifier = Modifier.padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {

            DetailText(label = "Connect Instance ID", value = selectedConfig.connectInstanceId)
            DetailText(label = "Contact Flow ID", value = selectedConfig.contactFlowId)
            DetailText(label = "Start Chat Endpoint", value = selectedConfig.startChatEndpoint)
            DetailText(label = "Region", value = selectedConfig.region.name)
            DetailText(label = "Agent Name", value = selectedConfig.agentName)
            DetailText(label = "Customer Name", value = selectedConfig.customerName)
        }
    }
}

@Composable
fun DetailText(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

