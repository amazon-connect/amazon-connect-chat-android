package com.amazon.connect.chat.androidchatexample.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.amazon.connect.chat.androidchatexample.Config
import com.amazon.connect.chat.androidchatexample.viewmodel.ChatViewModel

@Composable
fun ConfigPicker(viewModel: ChatViewModel) {
    val configOptions = listOf("Rajat acc","Pentest account 1", "Pentest account 2")
    val selectedConfigIndex by viewModel.selectedConfigIndex.observeAsState(0)

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Select Configuration", style = MaterialTheme.typography.bodyLarge)

        // Display radio buttons for selecting configuration
        configOptions.forEachIndexed { index, option ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                RadioButton(
                    selected = (selectedConfigIndex == index),
                    onClick = {
                        viewModel.setSelectedConfig(index)
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(option)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("${Config.configurations[index]}")
        }
    }
}
