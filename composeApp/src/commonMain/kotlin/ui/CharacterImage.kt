package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.seiko.imageloader.ui.AutoSizeImage
import core.db.DB
import core.model.Character
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.count
import kotlin.random.Random

@Composable
fun CharacterImage(
    character: Character,
    modifier: Modifier = Modifier
) {
    val modCount by produceState(0) {
        DB.modDao.observeCountByCharacter(character.name).collect { value = it }
    }
    val enabledCount by produceState(0) {
        DB.modDao.observeEnabledCountByCharacter(character.name).collect { value = it }
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
                    Brush.verticalGradient(listOf(MaterialTheme.colors.surface, gradientColor))
                ),
            contentScale = ContentScale.FillWidth,
            contentDescription = null
        )
        Spacer(Modifier.height(12.dp))
        Column(Modifier) {
            Text(
                text = character.name,
                textAlign = TextAlign.Start,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFEDE0DD)
            )
            Text(
                text = remember(enabledCount) { "Mods enabled: $enabledCount" },
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = remember(modCount) { "Mods installed: $modCount" },
                textAlign = TextAlign.Start,
                style = MaterialTheme.typography.subtitle2,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}