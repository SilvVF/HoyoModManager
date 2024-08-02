package core.api

import net.GET
import net.model.gamebanana.CategoryContentResponse

object GameBananaApi {

    val BASE_URL = "https://gamebanana.com/apiv11"


    suspend fun categories(id: Int) {
        val url = "$BASE_URL/Mod/Categories?_idCategoryRow=$id&_sSort=a_to_z&_bShowEmpty=true"
    }

    suspend fun categoryContent(id: Int, perPage: Int = 15, page: Int = 1): CategoryContentResponse {
        val url = "$BASE_URL/Mod/Index?_nPerpage=$perPage&_aFilters[Generic_Category]=$id&_nPage=$page"
        return GET<CategoryContentResponse>(url)
    }
}