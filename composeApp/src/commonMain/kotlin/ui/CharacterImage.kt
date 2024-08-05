package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.seiko.imageloader.ui.AutoSizeImage
import core.db.LocalDatabase
import core.model.Character
import kotlin.random.Random

@Composable
fun CharacterImage(
    character: Character,
    onIconClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val database = LocalDatabase.current
    val modCount by produceState(0) {
        database.subscribeToModCount(character.name).collect { value = it }
    }
    val enabledCount by produceState(0) {
        database.subscribeToEnabledModCount(character.name).collect { value = it }
    }

    val gradientColor = remember(character) {
        val random = Random(character.hashCode())
        Color(
            random.nextInt(255),
            random.nextInt(255),
            random.nextInt(255),
        )
    }

    Column(modifier.padding(6.dp), horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.Center) {
        AutoSizeImage(
            url = character.avatarUrl,
            modifier = Modifier
                .weight(1f)
                .aspectRatio(1f)
                .align(Alignment.CenterHorizontally)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surface, gradientColor))
                )
                .clickable {
                    onIconClick()
                },
            contentScale = ContentScale.FillWidth,
            contentDescription = null
        )
        Spacer(Modifier.height(12.dp))
        Column(Modifier) {
            Text(
                text = character.name,
                textAlign = TextAlign.Start,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFEDE0DD)
            )
            Text(
                text = remember(enabledCount) { "Mods enabled: $enabledCount" },
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = remember(modCount) { "Mods installed: $modCount" },
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}