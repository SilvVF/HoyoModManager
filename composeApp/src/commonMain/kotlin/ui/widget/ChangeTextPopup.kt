package ui.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ui.AppTheme


object ChangeTextPopupDefaults {

    @Composable
    fun Message(
        text: String,
        modifier: Modifier = Modifier
    ) {
        Text(
            modifier = modifier,
            text = text,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun ChangeTextPopup(
    value: String,
    onValueChange: (String) -> Unit,
    message: @Composable ChangeTextPopupDefaults.() -> Unit = {},
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    surfaceColor: Color = MaterialTheme.colorScheme.surface,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = surfaceColor
        ),
        modifier = modifier
            .wrapContentSize()
            .clip(MaterialTheme.shapes.medium)
    ) {
        Column(
            Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.align(Alignment.Start)) {
                ChangeTextPopupDefaults.message()
            }
            OutlinedTextField(
                onValueChange = onValueChange,
                value = value,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Cancel")
                }
                Spacer(Modifier.width(12.dp))
                Button(
                    shape = CircleShape,
                    onClick = onConfirm,
                ) {
                    Text("Confirm")
                }
            }
        }
    }
}

@Composable
@androidx.compose.desktop.ui.tooling.preview.Preview
private fun PreviewChangeTextPopup() {
    AppTheme {

        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

            var value by remember { mutableStateOf("") }

            ChangeTextPopup(
                value = value,
                onValueChange = { value = it },
                onConfirm = {},
                onCancel = {},
                message = { Message("Change text popup title") }
            )
        }
    }
}
