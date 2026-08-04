package com.lmspilot.configuration.cls

private val HEX_COLOR = Regex("^#[0-9A-Fa-f]{6}([0-9A-Fa-f]{2})?$")

data class BrandingSpec(
    val systemName: String,
    val introduction: String?,
    val primaryColor: String,
    val secondaryColor: String,
    val backgroundColor: String,
    val textColor: String,
) {
    init {
        require(systemName.isNotBlank() && systemName.length <= 240) { "Invalid systemName" }
        listOf(primaryColor, secondaryColor, backgroundColor, textColor).forEach {
            require(HEX_COLOR.matches(it)) { "Invalid color: $it" }
        }
    }
}
