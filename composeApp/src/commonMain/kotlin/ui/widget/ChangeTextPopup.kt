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
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
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
            style = MaterialTheme.typography.subtitle2
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
    surfaceColor: Color = MaterialTheme.colors.surface,
    modifier: Modifier = Modifier
) {
    Card(
        backgroundColor = surfaceColor,
        modifier = Modifier
            .wrapContentSize()
            .clip(MaterialTheme.shapes.medium)
    ) {
        Column(
            modifier.padding(12.dp),
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
                        contentColor = MaterialTheme.colors.error
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
