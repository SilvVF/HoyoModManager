package net.model.genshin


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GiCharacter(
    @SerialName("affiliation")
    val affiliation: String,
    @SerialName("associationType")
    val associationType: String,
    @SerialName("birthday")
    val birthday: String,
    @SerialName("birthdaymmdd")
    val birthdaymmdd: String,
    @SerialName("bodyType")
    val bodyType: String,
    @SerialName("constellation")
    val constellation: String,
    @SerialName("costs")
    val costs: Costs,
    @SerialName("cv")
    val cv: Cv,
    @SerialName("description")
    val description: String,
    @SerialName("elementText")
    val elementText: String,
    @SerialName("elementType")
    val elementType: String,
    @SerialName("gender")
    val gender: String,
    @SerialName("id")
    val id: Int,
    @SerialName("name")
    val name: String,
    @SerialName("qualityType")
    val qualityType: String,
    @SerialName("rarity")
    val rarity: Int,
    @SerialName("region")
    val region: String,
    @SerialName("substatText")
    val substatText: String,
    @SerialName("substatType")
    val substatType: String,
    @SerialName("title")
    val title: String,
    @SerialName("weaponText")
    val weaponText: String,
    @SerialName("weaponType")
    val weaponType: String
) {
    @Serializable
    data class Costs(
        @SerialName("ascend1")
        val ascend1: List<Ascend1>,
        @SerialName("ascend2")
        val ascend2: List<Ascend2>,
        @SerialName("ascend3")
        val ascend3: List<Ascend3>,
        @SerialName("ascend4")
        val ascend4: List<Ascend4>,
        @SerialName("ascend5")
        val ascend5: List<Ascend5>,
        @SerialName("ascend6")
        val ascend6: List<Ascend6>
    ) {
        @Serializable
        data class Ascend1(
            @SerialName("count")
            val count: Int,
            @SerialName("id")
            val id: Int,
            @SerialName("name")
            val name: String
        )

        @Serializable
        data class Ascend2(
            @SerialName("count")
            val count: Int,
            @SerialName("id")
            val id: Int,
            @SerialName("name")
            val name: String
        )

        @Serializable
        data class Ascend3(
            @SerialName("count")
            val count: Int,
            @SerialName("id")
            val id: Int,
            @SerialName("name")
            val name: String
        )

        @Serializable
        data class Ascend4(
            @SerialName("count")
            val count: Int,
            @SerialName("id")
            val id: Int,
            @SerialName("name")
            val name: String
        )

        @Serializable
        data class Ascend5(
            @SerialName("count")
            val count: Int,
            @SerialName("id")
            val id: Int,
            @SerialName("name")
            val name: String
        )

        @Serializable
        data class Ascend6(
            @SerialName("count")
            val count: Int,
            @SerialName("id")
            val id: Int,
            @SerialName("name")
            val name: String
        )
    }

    @Serializable
    data class Cv(
        @SerialName("chinese")
        val chinese: String,
        @SerialName("english")
        val english: String,
        @SerialName("japanese")
        val japanese: String,
        @SerialName("korean")
        val korean: String
    )
}