package tab.mod.components

import OS
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.seiko.imageloader.ui.AutoSizeImage
import fromHex
import net.model.gamebanana.CategoryContentResponse
import plus
import tab.mod.LocalSortMode
import tab.mod.state.BrowseState.Failure
import tab.mod.state.BrowseState.Loading
import tab.mod.state.BrowseState.Success
import tab.mod.state.BrowseState.Success.PageLoadState
import tab.mod.state.ModBrowseStateHolder
import ui.LocalDataApi


@Composable
fun ModBrowseContent(
    categoryId: Int,
    onModClick: (id: Int) -> Unit,
    onCategoryClick: (name: String, id: Int) -> Unit,
) {

    val scope = rememberCoroutineScope()
    val dataApi = LocalDataApi.current
    val sort by LocalSortMode.current

    val stateHolder = remember(dataApi, sort) {
        ModBrowseStateHolder(dataApi, categoryId, sort, scope)
    }

    Scaffold { paddingValues ->

        val modState by stateHolder.state.collectAsState()

        when (val state = modState) {
            is Failure -> {
                Box(Modifier.fillMaxSize()) {
                    TextButton(
                        onClick = { stateHolder.retry() },
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Text("Retry")
                    }
                }
            }
            is Loading -> {
                Box(Modifier.fillMaxSize()) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
            }
            is Success -> {
                Row {
                    PageContent(
                        state = state,
                        loadPage = { stateHolder.loadPage(it) },
                        onModClick = onModClick,
                        paddingValues = paddingValues,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )

                    if (state.subCategories.isNotEmpty()) {
                        Column(Modifier.wrapContentWidth().verticalScroll(rememberScrollState())) {
                            state.subCategories.fastForEach {
                                TextButton(
                                    onClick = { onCategoryClick(it.sName, it.idRow) }
                                ) {
                                    Text(it.sName)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun rememberAnnotatedCSS(name: String, css: String?): AnnotatedString {
    return remember(name, css) {
        val split =  css?.split(";")
        val colorHex =split
            ?.firstOrNull { it.startsWith("color:") }
            ?.removePrefix("color:")

        val shadows = split
            ?.firstOrNull { it.startsWith("text-shadow:") }
            ?.split(":", ",")
            ?.drop(1)
            ?.map {
                val txtShadow = it.split(" ")
                val offsets = txtShadow
                    .mapNotNull { it.removeSuffix("px").toFloatOrNull() }

                val color = txtShadow.firstNotNullOfOrNull {
                    it.takeIf { it.startsWith("#") }?.let {
                        runCatching { Color.fromHex(it) }.getOrNull()
                    }
                }
                color?.let {
                    Shadow(
                        color = color,
                        offset = Offset(
                            offsets.take(2).firstOrNull() ?: 0f,
                            offsets.take(2).lastOrNull() ?: 0f
                        ),
                        blurRadius = offsets.getOrNull(2) ?: 0f
                    )
                }
            }


        buildAnnotatedString {
            shadows?.filterNotNull()?.fastForEach { shadow ->
                addStyle(
                    style = SpanStyle(
                        color = Color.Transparent,
                        shadow = shadow,
                    ),
                    start = 0,
                    end = name.length
                )
            }
            withStyle(
                SpanStyle(
                    color = colorHex?.let { Color.fromHex(colorHex) } ?: Color.Unspecified,
                ),
            ) {
                append(name)
            }
        }
    }
}

@Composable
private fun PageContent(
    state: Success,
    loadPage: (page: Int) -> Unit,
    onModClick: (id: Int) -> Unit,
    paddingValues: PaddingValues,
    modifier: Modifier
) {
    when (val data = state.mods[state.page] ?: return) {
        PageLoadState.Failure -> Box(modifier.fillMaxSize()) {
            TextButton(
                onClick = { loadPage(state.page) },
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text("Retry")
            }
        }
        PageLoadState.Loading ->   Box(modifier.fillMaxSize()) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
        is PageLoadState.Success -> {
            val gridState = rememberLazyGridState()
            Box(modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    state = gridState,
                    contentPadding = paddingValues + PaddingValues(horizontal = 82.dp),
                    columns = GridCells.Adaptive(280.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.Center)
                ) {
                    items(data.data) { submission ->
                        Column(Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .clickable {
                                println(submission.idRow.toString() + "Clicked")
                                onModClick(submission.idRow)
                            }
                        ) {
                            ModImagePreview(
                                submission.aPreviewMedia,
                                Modifier.fillMaxWidth()
                            )

                            val updatedAt = remember {
                                submission.tsDateUpdated?.let {
                                    OS.getRelativeTimeSpanString(it.toLong())
                                }
                                    .orEmpty()
                            }

                            val uploadedAt = remember {
                                submission.tsDateAdded?.let {
                                    OS.getRelativeTimeSpanString(it.toLong())
                                }
                                    .orEmpty()
                            }

                            val annotatedString = rememberAnnotatedCSS(
                                submission.sName.orEmpty(),
                                submission.aSubmitter?.sSubjectShaperCssCode
                            )

                            Text(annotatedString)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(uploadedAt)
                                Spacer(Modifier.width(8.dp))
                                Text(updatedAt)
                            }
                        }
                    }
                }
                VerticalScrollbar(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    adapter = rememberScrollbarAdapter(gridState),
                    style = LocalScrollbarStyle.current.copy(
                        thickness = 8.dp,
                        hoverColor = MaterialTheme.colorScheme.primary,
                        unhoverColor = MaterialTheme.colorScheme.primary
                    )
                )
                PageNumbersList(
                    state.page,
                    state.pageCount,
                    loadPage,
                    Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Composable
private fun ModImagePreview(
    media: CategoryContentResponse.ARecord.APreviewMedia?,
    modifier: Modifier = Modifier
) {
    val imgUrl = remember(media) {
        runCatching {
            val img = media!!.aImages!!.first()

            val baseUrl = img.sBaseUrl!!.replace("\\", "")
            val file = (img.sFile220 ?: img.sFile!!).replace("\\", "")
            "$baseUrl/$file"
        }
            .getOrNull()
    }

    Box(modifier.aspectRatio(16f / 10f)) {
        if (imgUrl != null) {
            AutoSizeImage(
                url = imgUrl,
                alignment = Alignment.TopCenter,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(Modifier.fillMaxSize().background(Color.Gray))
        }
    }
}

@Composable
private fun PageNumbersList(
    page: Int,
    lastPage: Int,
    goToPage: (page: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .background(
                brush = Brush.radialGradient
                    (colors = listOf(
                    Color.Black.copy(alpha = 0.3f),
                    Color.Black.copy(alpha = 0.7f)))
            )
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val list = remember(page, lastPage) {
            val minPage = (page - 4).coerceAtLeast(1)
            val maxPage = (page + 9 - (page - minPage)).coerceIn(1..lastPage.coerceAtLeast(1))

            (minPage..maxPage).toList()
        }

        list.fastForEach { num ->
            ElevatedFilterChip(
                selected = page == num,
                onClick = { goToPage(num) },
                modifier = Modifier.wrapContentSize().padding(2.dp),
                label =  {
                    Text(
                        num.toString(),
                        Modifier.align(Alignment.CenterVertically)
                    )
                }
            )
        }
    }
}