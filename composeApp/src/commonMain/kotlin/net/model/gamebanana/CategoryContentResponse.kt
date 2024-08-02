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
        @SerialName("_aGame")
        val aGame: AGame,
        @SerialName("_aPreviewMedia")
        val aPreviewMedia: APreviewMedia,
        @SerialName("_aRootCategory")
        val aRootCategory: ARootCategory,
        @SerialName("_aSubmitter")
        val aSubmitter: ASubmitter,
        @SerialName("_aTags")
        val aTags: List<String>,
        @SerialName("_bHasContentRatings")
        val bHasContentRatings: Boolean,
        @SerialName("_bHasFiles")
        val bHasFiles: Boolean,
        @SerialName("_bIsObsolete")
        val bIsObsolete: Boolean,
        @SerialName("_bIsOwnedByAccessor")
        val bIsOwnedByAccessor: Boolean,
        @SerialName("_bWasFeatured")
        val bWasFeatured: Boolean,
        @SerialName("_idRow")
        val idRow: Int,
        @SerialName("_nLikeCount")
        val nLikeCount: Int,
        @SerialName("_nPostCount")
        val nPostCount: Int,
        @SerialName("_nViewCount")
        val nViewCount: Int,
        @SerialName("_sIconClasses")
        val sIconClasses: String,
        @SerialName("_sInitialVisibility")
        val sInitialVisibility: String,
        @SerialName("_sModelName")
        val sModelName: String,
        @SerialName("_sName")
        val sName: String,
        @SerialName("_sProfileUrl")
        val sProfileUrl: String,
        @SerialName("_sSingularTitle")
        val sSingularTitle: String,
        @SerialName("_sVersion")
        val sVersion: String,
        @SerialName("_tsDateAdded")
        val tsDateAdded: Int,
        @SerialName("_tsDateModified")
        val tsDateModified: Int,
        @SerialName("_tsDateUpdated")
        val tsDateUpdated: Int
    ) {
        @Serializable
        data class AGame(
            @SerialName("_idRow")
            val idRow: Int,
            @SerialName("_sIconUrl")
            val sIconUrl: String,
            @SerialName("_sName")
            val sName: String,
            @SerialName("_sProfileUrl")
            val sProfileUrl: String
        )

        @Serializable
        data class APreviewMedia(
            @SerialName("_aImages")
            val aImages: List<AImage>
        ) {
            @Serializable
            data class AImage(
                @SerialName("_hFile100")
                val hFile100: Int,
                @SerialName("_hFile220")
                val hFile220: Int,
                @SerialName("_hFile530")
                val hFile530: Int,
                @SerialName("_hFile800")
                val hFile800: Int,
                @SerialName("_sBaseUrl")
                val sBaseUrl: String,
                @SerialName("_sCaption")
                val sCaption: String,
                @SerialName("_sFile")
                val sFile: String,
                @SerialName("_sFile100")
                val sFile100: String,
                @SerialName("_sFile220")
                val sFile220: String,
                @SerialName("_sFile530")
                val sFile530: String,
                @SerialName("_sFile800")
                val sFile800: String,
                @SerialName("_sType")
                val sType: String,
                @SerialName("_wFile100")
                val wFile100: Int,
                @SerialName("_wFile220")
                val wFile220: Int,
                @SerialName("_wFile530")
                val wFile530: Int,
                @SerialName("_wFile800")
                val wFile800: Int
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
            val bHasRipe: Boolean,
            @SerialName("_bIsOnline")
            val bIsOnline: Boolean,
            @SerialName("_idRow")
            val idRow: Int,
            @SerialName("_sAvatarUrl")
            val sAvatarUrl: String,
            @SerialName("_sHdAvatarUrl")
            val sHdAvatarUrl: String,
            @SerialName("_sHovatarUrl")
            val sHovatarUrl: String,
            @SerialName("_sName")
            val sName: String,
            @SerialName("_sProfileUrl")
            val sProfileUrl: String,
            @SerialName("_sSubjectShaperCssCode")
            val sSubjectShaperCssCode: String,
            @SerialName("_sUpicUrl")
            val sUpicUrl: String
        )
    }
}