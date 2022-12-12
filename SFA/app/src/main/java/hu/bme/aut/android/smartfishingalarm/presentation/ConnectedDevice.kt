package hu.bme.aut.android.smartfishingalarm.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

object ConnectedDeviceCompose {
    val readData = mutableStateOf(MyApplication.receivedValue)

    @Composable
    fun InteractionPresentation(onDeviceSelected: () -> Unit) {
        val readDataShow by readData
        Column() {
            Text(text = "Data received from fishing alarm")
            Text(text = readDataShow, modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth())

            InputField()
        }
    }

    @Composable
    fun InputField() {
        var text by remember { mutableStateOf(MyApplication.receivedValue) }
        val byteArray = text.toByteArray()

        TextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Data to be sent to the fishing alarm") },
            modifier = Modifier.padding(vertical = 80.dp).fillMaxWidth()
        )

        Button(
            onClick = {
                writeCharacteristic(byteArray)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Send")
        }
    }
}