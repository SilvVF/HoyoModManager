package core.api

import net.GET
import net.model.gamebanana.CategoryContentResponse
import net.model.gamebanana.CategoryListResponseItem
import net.model.gamebanana.ModPageResponse
import net.model.gamebanana.UIConfig

object GameBananaApi {

    val BASE_URL = "https://gamebanana.com/apiv11"


    suspend fun categories(id: Int): List<CategoryListResponseItem> {
        val url = "$BASE_URL/Mod/Categories?_idCategoryRow=$id&_sSort=a_to_z&_bShowEmpty=true"
        return GET(url)
    }

    suspend fun categoryContent(id: Int, perPage: Int = 15, page: Int = 1): CategoryContentResponse {
        val url = "$BASE_URL/Mod/Index?_nPerpage=$perPage&_aFilters[Generic_Category]=$id&_nPage=$page"
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