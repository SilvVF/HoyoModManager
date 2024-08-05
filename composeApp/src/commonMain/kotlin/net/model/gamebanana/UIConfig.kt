package net.model.gamebanana


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UIConfig(
    @SerialName("_aContentRatingBehaviors")
    val aContentRatingBehaviors: AContentRatingBehaviors? = null,
    @SerialName("_aGame")
    val aGame: AGame? = null,
    @SerialName("_aLatestRipeUpdate")
    val aLatestRipeUpdate: ALatestRipeUpdate? = null,
    @SerialName("_aSection")
    val aSection: ASection? = null,
    @SerialName("_aTrendingGames")
    val aTrendingGames: List<ATrendingGame?>? = null,
    @SerialName("_bHasFreeThank")
    val bHasFreeThank: Boolean? = null,
    @SerialName("_bIsLoggedIn")
    val bIsLoggedIn: Boolean? = null,
    @SerialName("_bIsOver18")
    val bIsOver18: Boolean? = null,
    @SerialName("_bTodayIsUnlockSale")
    val bTodayIsUnlockSale: Boolean? = null,
    @SerialName("_dsDateOfBirth")
    val dsDateOfBirth: String? = null,
    @SerialName("_idMemberRow")
    val idMemberRow: Int? = null,
    @SerialName("_nDiscordMembersOnlineCount")
    val nDiscordMembersOnlineCount: Int? = null,
    @SerialName("_nMembersOnlineCount")
    val nMembersOnlineCount: Int? = null,
    @SerialName("_nTotalMembersCount")
    val nTotalMembersCount: Int? = null,
    @SerialName("_sAvatarUrl")
    val sAvatarUrl: String? = null
) {
    @Serializable
    data class AContentRatingBehaviors(
        @SerialName("au")
        val au: String? = null,
        @SerialName("bg")
        val bg: String? = null,
        @SerialName("cp")
        val cp: String? = null,
        @SerialName("du")
        val du: String? = null,
        @SerialName("ft")
        val ft: String? = null,
        @SerialName("iv")
        val iv: String? = null,
        @SerialName("nu")
        val nu: String? = null,
        @SerialName("pn")
        val pn: String? = null,
        @SerialName("ps")
        val ps: String? = null,
        @SerialName("rp")
        val rp: String? = null,
        @SerialName("sa")
        val sa: String? = null,
        @SerialName("sc")
        val sc: String? = null,
        @SerialName("st")
        val st: String? = null,
        @SerialName("tu")
        val tu: String? = null
    )

    @Serializable
    data class AGame(
        @SerialName("_idRow")
        val idRow: Int? = null,
        @SerialName("_sAbbreviation")
        val sAbbreviation: String? = null,
        @SerialName("_sBackgroundUrl")
        val sBackgroundUrl: String? = null,
        @SerialName("_sIconUrl")
        val sIconUrl: String? = null,
        @SerialName("_sName")
        val sName: String? = null,
        @SerialName("_sProfileUrl")
        val sProfileUrl: String? = null
    )

    @Serializable
    data class ALatestRipeUpdate(
        @SerialName("_idRow")
        val idRow: Int? = null,
        @SerialName("_sName")
        val sName: String? = null,
        @SerialName("_sProfileUrl")
        val sProfileUrl: String? = null,
        @SerialName("_sText")
        val sText: String? = null,
        @SerialName("_tsDateAdded")
        val tsDateAdded: Int? = null
    )


    @Serializable
    data class ASection(
        @SerialName("_sModelName")
        val sModelName: String? = null,
        @SerialName("_sPluralTitle")
        val sPluralTitle: String? = null,
        @SerialName("_sSingularTitle")
        val sSingularTitle: String? = null
    )

    @Serializable
    data class ATrendingGame(
        @SerialName("_idRow")
        val idRow: Int? = null,
        @SerialName("_sAbbreviation")
        val sAbbreviation: String? = null,
        @SerialName("_sIconUrl")
        val sIconUrl: String? = null,
        @SerialName("_sName")
        val sName: String? = null,
        @SerialName("_sProfileUrl")
        val sProfileUrl: String? = null
    )
}