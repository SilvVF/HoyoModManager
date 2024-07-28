import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
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
import core.model.Character
import kotlin.random.Random

@Composable
fun CharacterCard(
    character: Character,
    modifier: Modifier = Modifier
) {

    val backgroundColor = MaterialTheme.colors.background
    val typeColor = remember(character) {
        val random = Random(character.hashCode())
        Color(
            random.nextInt(255),
            random.nextInt(255),
            random.nextInt(255),
        )
    }

    Box(
        modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(12.dp))
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(typeColor, backgroundColor)
                    )
                )
            }
    ) {
        AutoSizeImage(
            url = character.avatarUrl ?: "",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
            contentDescription = null
        )
        Text(
            text = character.name,
            maxLines = 2,
            textAlign = TextAlign.Start,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFEDE0DD),
            modifier =
            Modifier
                .fillMaxWidth()
                .drawWithCache {
                    onDrawBehind {
                        drawRect(
                            brush =
                            Brush.verticalGradient(
                                colors =
                                listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.7f),
                                    Color.Black.copy(alpha = 0.9f),
                                ),
                            ),
                        )
                    }
                }
                .padding(top = 62.dp, bottom = 16.dp)
                .padding(horizontal = 6.dp)
                .align(Alignment.BottomCenter),
        )
    }
}