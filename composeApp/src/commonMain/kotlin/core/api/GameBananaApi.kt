package core.api

import net.GET
import net.model.gamebanana.CategoryContentResponse
import net.model.gamebanana.CategoryListResponseItem
import net.model.gamebanana.ModPageResponse
import net.model.gamebanana.UIConfig

object GameBananaApi {

    enum class Sort(val qp: String) {
        MostLiked("MostLiked"),
        MostDownloaded("MostDownloaded"),
        MostViewed("MostViewed"),
    }

    private const val BASE_URL = "https://gamebanana.com/apiv11"

    suspend fun categories(id: Int): List<CategoryListResponseItem> {
        val url = "$BASE_URL/Mod/Categories?_idCategoryRow=$id&_sSort=a_to_z&_bShowEmpty=true"
        return GET(url)
    }

    suspend fun categoryContent(
        id: Int,
        perPage: Int = 15,
        page: Int = 1,
        sort: Sort? = null,
    ): CategoryContentResponse {
        val sortQuery = if (sort == null) "" else "&_sSort=${sort.name}"
        val url = "$BASE_URL/Mod/Index?_nPerpage=$perPage&_aFilters[Generic_Category]=$id&_nPage=$page$sortQuery"
        return GET(url)
    }

    suspend fun uiConfig(id: Int): UIConfig {
        val url = "https://gamebanana.com/apiv11/Member/UiConfig?_sUrl=/mods/cats/id"
        return GET(url)
    }

    suspend fun modContent(id: Int): ModPageResponse {
        val url = "https://gamebanana.com/apiv11/Mod/$id/ProfilePage"
        return GET(url)
    }
}