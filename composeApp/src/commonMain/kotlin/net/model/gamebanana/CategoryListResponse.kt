package net.model.gamebanana


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CategoryListResponseItem(
    @SerialName("_idRow")
    val idRow: Int,
    @SerialName("_nCategoryCount")
    val nCategoryCount: Int,
    @SerialName("_nItemCount")
    val nItemCount: Int,
    @SerialName("_sName")
    val sName: String,
    @SerialName("_sUrl")
    val sUrl: String
)