package net.model.gamebanana


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ModPageResponse(
//    @SerialName("_aAttributes")
//    val aAttributes: List<Any>,
    @SerialName("_aCategory")
    val aCategory: ACategory,
    @SerialName("_aContentRatings")
    val aContentRatings: Map<String, String> = mapOf(),
//    @SerialName("_aContributingStudios")
//    val aContributingStudios: List<Any>,
    @SerialName("_aCredits")
    val aCredits: List<ACredit>,
    @SerialName("_aEmbeddables")
    val aEmbeddables: AEmbeddables,
    @SerialName("_aFiles")
    val aFiles: List<AFile>,
    @SerialName("_aGame")
    val aGame: AGame,
    @SerialName("_aLicenseChecklist")
    val aLicenseChecklist: ALicenseChecklist,
    @SerialName("_aPreviewMedia")
    val aPreviewMedia: APreviewMedia,
    @SerialName("_aSubmitter")
    val aSubmitter: ASubmitter,
    @SerialName("_aSuperCategory")
    val aSuperCategory: ASuperCategory,
//    @SerialName("_aTags")
//    val aTags: List<Any>,
    @SerialName("_bHasUpdates")
    val bHasUpdates: Boolean,
    @SerialName("_bIsFlagged")
    val bIsFlagged: Boolean? = null,
    @SerialName("_bIsObsolete")
    val bIsObsolete: Boolean? = null,
    @SerialName("_bIsPorted")
    val bIsPorted: Boolean? = null,
    @SerialName("_bIsPrivate")
    val bIsPrivate: Boolean? = null,
    @SerialName("_bIsTrashed")
    val bIsTrashed: Boolean? = null,
    @SerialName("_bIsWithheld")
    val bIsWithheld: Boolean? = null,
    @SerialName("_bShowRipePromo")
    val bShowRipePromo: Boolean? = null,
    @SerialName("_idAccessorSubscriptionRow")
    val idAccessorSubscriptionRow: Int? = null,
    @SerialName("_idRow")
    val idRow: Int,
    @SerialName("_nAllTodosCount")
    val nAllTodosCount: Int? = null,
    @SerialName("_nDownloadCount")
    val nDownloadCount: Int? = null,
    @SerialName("_nLikeCount")
    val nLikeCount: Int? = null,
    @SerialName("_nPostCount")
    val nPostCount: Int? = null,
    @SerialName("_nStatus")
    val nStatus: String? = null,
    @SerialName("_nSubscriberCount")
    val nSubscriberCount: Int? = null,
    @SerialName("_nThanksCount")
    val nThanksCount: Int? = null,
    @SerialName("_nUpdatesCount")
    val nUpdatesCount: Int? = null,
    @SerialName("_nViewCount")
    val nViewCount: Int? = null,
    @SerialName("_sCommentsMode")
    val sCommentsMode: String? = null,
    @SerialName("_sDownloadUrl")
    val sDownloadUrl: String? = null,
    @SerialName("_sInitialVisibility")
    val sInitialVisibility: String? = null,
    @SerialName("_sLicense")
    val sLicense: String? = null,
    @SerialName("_sName")
    val sName: String? = null,
    @SerialName("_sProfileUrl")
    val sProfileUrl: String? = null,
    @SerialName("_sText")
    val sText: String? = null,
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
    data class ACategory(
        @SerialName("_idRow")
        val idRow: Int,
        @SerialName("_sIconUrl")
        val sIconUrl: String? = null,
        @SerialName("_sModelName")
        val sModelName: String? = null,
        @SerialName("_sName")
        val sName: String? = null,
        @SerialName("_sProfileUrl")
        val sProfileUrl: String? = null
    )

    @Serializable
    data class ACredit(
        @SerialName("_aAuthors")
        val aAuthors: List<AAuthor> = emptyList(),
        @SerialName("_sGroupName")
        val sGroupName: String? = null
    ) {
        @Serializable
        data class AAuthor(
            @SerialName("_bIsOnline")
            val bIsOnline: Boolean? = null,
            @SerialName("_idRow")
            val idRow: Int? = null,
            @SerialName("_sName")
            val sName: String? = null,
            @SerialName("_sProfileUrl")
            val sProfileUrl: String? = null,
            @SerialName("_sRole")
            val sRole: String? = null,
            @SerialName("_sUpicUrl")
            val sUpicUrl: String? = null
        )
    }

    @Serializable
    data class AEmbeddables(
        @SerialName("_aVariants")
        val aVariants: List<String>,
        @SerialName("_sEmbeddableImageBaseUrl")
        val sEmbeddableImageBaseUrl: String
    )

    @Serializable
    data class AFile(
        @SerialName("_bContainsExe")
        val bContainsExe: Boolean,
        @SerialName("_idRow")
        val idRow: Int,
        @SerialName("_nDownloadCount")
        val nDownloadCount: Int,
        @SerialName("_nFilesize")
        val nFilesize: Int,
        @SerialName("_sAnalysisResult")
        val sAnalysisResult: String,
        @SerialName("_sAnalysisResultCode")
        val sAnalysisResultCode: String,
        @SerialName("_sAnalysisState")
        val sAnalysisState: String,
        @SerialName("_sAvastAvResult")
        val sAvastAvResult: String,
        @SerialName("_sClamAvResult")
        val sClamAvResult: String,
        @SerialName("_sDescription")
        val sDescription: String,
        @SerialName("_sDownloadUrl")
        val sDownloadUrl: String,
        @SerialName("_sFile")
        val sFile: String,
        @SerialName("_sMd5Checksum")
        val sMd5Checksum: String,
        @SerialName("_tsDateAdded")
        val tsDateAdded: Int
    )

    @Serializable
    data class AGame(
        @SerialName("_bAccessorIsSubscribed")
        val bAccessorIsSubscribed: Boolean? = null,
        @SerialName("_bHasSubmissionQueue")
        val bHasSubmissionQueue: Boolean? = null,
        @SerialName("_idAccessorSubscriptionRow")
        val idAccessorSubscriptionRow: Int? = null,
        @SerialName("_idRow")
        val idRow: Int,
        @SerialName("_nSubscriberCount")
        val nSubscriberCount: Int? = null,
        @SerialName("_sAbbreviation")
        val sAbbreviation: String? = null,
        @SerialName("_sBannerUrl")
        val sBannerUrl: String? = null,
        @SerialName("_sIconUrl")
        val sIconUrl: String? = null,
        @SerialName("_sName")
        val sName: String? = null,
        @SerialName("_sProfileUrl")
        val sProfileUrl: String? = null
    )

    @Serializable
    data class ALicenseChecklist(
        @SerialName("ask")
        val ask: List<String> = emptyList(),
        @SerialName("no")
        val no: List<String> = emptyList(),
        @SerialName("yes")
        val yes: List<String> = emptyList()
    )

    @Serializable
    data class APreviewMedia(
        @SerialName("_aImages")
        val aImages: List<AImage>
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
            val sBaseUrl: String,
            @SerialName("_sFile")
            val sFile: String,
            @SerialName("_sFile100")
            val sFile100: String? = null,
            @SerialName("_sFile220")
            val sFile220: String? = null,
            @SerialName("_sFile530")
            val sFile530: String? = null,
            @SerialName("_sFile800")
            val sFile800: String?= null,
            @SerialName("_sType")
            val sType: String,
            @SerialName("_wFile100")
            val wFile100: Int? = null,
            @SerialName("_wFile220")
            val wFile220: Int?= null,
            @SerialName("_wFile530")
            val wFile530: Int?= null,
            @SerialName("_wFile800")
            val wFile800: Int? = null
        )
    }

    @Serializable
    data class ASubmitter(
        @SerialName("_bAccessorIsBuddy")
        val bAccessorIsBuddy: Boolean? = null,
        @SerialName("_bAccessorIsSubscribed")
        val bAccessorIsSubscribed: Boolean? = null,
        @SerialName("_bBuddyRequestExistsWithAccessor")
        val bBuddyRequestExistsWithAccessor: Boolean? = null,
        @SerialName("_bHasRipe")
        val bHasRipe: Boolean? = null,
        @SerialName("_bIsOnline")
        val bIsOnline: Boolean? = null,
        @SerialName("_idRow")
        val idRow: Int,
        @SerialName("_nBuddyCount")
        val nBuddyCount: Int? = null,
        @SerialName("_nPoints")
        val nPoints: Int? = null,
        @SerialName("_nPointsRank")
        val nPointsRank: Int? = null,
        @SerialName("_nSubscriberCount")
        val nSubscriberCount: Int? = null,
        @SerialName("_sAvatarUrl")
        val sAvatarUrl: String? = null,
        @SerialName("_sCooltipCssCode")
        val sCooltipCssCode: String? = null,
        @SerialName("_sHdAvatarUrl")
        val sHdAvatarUrl: String? = null,
        @SerialName("_sHonoraryTitle")
        val sHonoraryTitle: String? = null,
        @SerialName("_sLocation")
        val sLocation: String? = null,
        @SerialName("_sMedalsUrl")
        val sMedalsUrl: String? = null,
        @SerialName("_sName")
        val sName: String? = null,
        @SerialName("_sOfflineTitle")
        val sOfflineTitle: String? = null,
        @SerialName("_sOnlineTitle")
        val sOnlineTitle: String? = null,
        @SerialName("_sPointsUrl")
        val sPointsUrl: String? = null,
        @SerialName("_sProfileUrl")
        val sProfileUrl: String? = null,
        @SerialName("_sSigUrl")
        val sSigUrl: String? = null,
        @SerialName("_sSubjectShaperCssCode")
        val sSubjectShaperCssCode: String? = null,
        @SerialName("_sUpicUrl")
        val sUpicUrl: String? = null,
        @SerialName("_sUserTitle")
        val sUserTitle: String? = null,
        @SerialName("_tsJoinDate")
        val tsJoinDate: Int? = null
    )

    @Serializable
    data class ASuperCategory(
        @SerialName("_idRow")
        val idRow: Int,
        @SerialName("_sIconUrl")
        val sIconUrl: String? = null,
        @SerialName("_sModelName")
        val sModelName: String? = null,
        @SerialName("_sName")
        val sName: String? = null,
        @SerialName("_sProfileUrl")
        val sProfileUrl: String? = null
    )
}