package net.model.gamebanana


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CategoryContentResponse(
    @SerialName("_aMetadata")
    val aMetadata: AMetadata,
    @SerialName("_aRecords")
    val aRecords: List<ARecord>
) {
    @Serializable
    data class AMetadata(
        @SerialName("_bIsComplete")
        val bIsComplete: Boolean,
        @SerialName("_nPerpage")
        val nPerpage: Int,
        @SerialName("_nRecordCount")
        val nRecordCount: Int
    )

    @Serializable
    data class ARecord(
        @SerialName("_aPreviewMedia")
        val aPreviewMedia: APreviewMedia? = null,
        @SerialName("_aRootCategory")
        val aRootCategory: ARootCategory? = null,
        @SerialName("_aSubmitter")
        val aSubmitter: ASubmitter? = null,
        @SerialName("_aTags")
        val aTags: List<String>,
        @SerialName("_idRow")
        val idRow: Int,
        @SerialName("_nLikeCount")
        val nLikeCount: Int? = null,
        @SerialName("_nPostCount")
        val nPostCount: Int? = null,
        @SerialName("_nViewCount")
        val nViewCount: Int? = null,
        @SerialName("_sModelName")
        val sModelName: String? = null,
        @SerialName("_sName")
        val sName: String? = null,
        @SerialName("_sOfficialDownloadUrl")
        val sOfficialDownloadUrl: String? = null,
        @SerialName("_sProfileUrl")
        val sProfileUrl: String? = null,
        @SerialName("_sSingularTitle")
        val sSingularTitle: String? = null,
        @SerialName("_sState")
        val sState: String? = null,
        @SerialName("_sVersion")
        val sVersion: String? = null,
        @SerialName("_tsDateAdded")
        val tsDateAdded: Int? = null,
        @SerialName("_tsDateModified")
        val tsDateModified: Int? = null,
        @SerialName("_tsDateUpdated")
        val tsDateUpdated: Int? = null
    ) {

        @Serializable
        data class APreviewMedia(
            @SerialName("_aImages")
            val aImages: List<AImage>? = null,
            @SerialName("_aMetadata")
            val aMetadata: AMetadata? = null
        ) {
            @Serializable
            data class AImage(
                @SerialName("_hFile100")
                val hFile100: Int? = null,
                @SerialName("_hFile220")
                val hFile220: Int? = null,
                @SerialName("_hFile530")
                val hFile530: Int? = null,
                @SerialName("_hFile800")
                val hFile800: Int? = null,
                @SerialName("_sBaseUrl")
                val sBaseUrl: String? = null,
                @SerialName("_sCaption")
                val sCaption: String? = null,
                @SerialName("_sFile")
                val sFile: String? = null,
                @SerialName("_sFile100")
                val sFile100: String? = null,
                @SerialName("_sFile220")
                val sFile220: String? = null,
                @SerialName("_sFile530")
                val sFile530: String? = null,
                @SerialName("_sFile800")
                val sFile800: String? = null,
                @SerialName("_sType")
                val sType: String? = null,
                @SerialName("_wFile100")
                val wFile100: Int? = null,
                @SerialName("_wFile220")
                val wFile220: Int? = null,
                @SerialName("_wFile530")
                val wFile530: Int? = null,
                @SerialName("_wFile800")
                val wFile800: Int? = null
            )

            @Serializable
            data class AMetadata(
                @SerialName("_nPostCount")
                val nPostCount: Int,
                @SerialName("_sSnippet")
                val sSnippet: String,
                @SerialName("_sState")
                val sState: String
            )
        }

        @Serializable
        data class ARootCategory(
            @SerialName("_sIconUrl")
            val sIconUrl: String,
            @SerialName("_sName")
            val sName: String,
            @SerialName("_sProfileUrl")
            val sProfileUrl: String
        )

        @Serializable
        data class ASubmitter(
            @SerialName("_bHasRipe")
            val bHasRipe: Boolean? = null,
            @SerialName("_bIsOnline")
            val bIsOnline: Boolean? = null,
            @SerialName("_idRow")
            val idRow: Int? = null,
            @SerialName("_sAvatarUrl")
            val sAvatarUrl: String? = null,
            @SerialName("_sHdAvatarUrl")
            val sHdAvatarUrl: String? = null,
            @SerialName("_sName")
            val sName: String? = null,
            @SerialName("_sProfileUrl")
            val sProfileUrl: String? = null,
            @SerialName("_sSubjectShaperCssCode")
            val sSubjectShaperCssCode: String? = null,
            @SerialName("_sUpicUrl")
            val sUpicUrl: String? = null
        )
    }
}