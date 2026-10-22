package com.dave_cli.proxybox.ui

import com.dave_cli.proxybox.data.db.ProfileEntity

object CoolVpnDisplay {
    fun countryFlag(profile: ProfileEntity): String {
        val text = "${profile.name} ${profile.rawUri}".lowercase()
        return when {
            hasAny(text, "united kingdom", "uk", "england", "great britain", "wielka brytania", "anglia") -> "🇬🇧"
            hasAny(text, "poland", "pl", "polska") -> "🇵🇱"
            hasAny(text, "germany", "deutschland", "de", "niemcy", "niemie") -> "🇩🇪"
            hasAny(text, "netherlands", "nl", "holand", "niderland") -> "🇳🇱"
            hasAny(text, "france", "fr", "francja") -> "🇫🇷"
            hasAny(text, "usa", "us", "united states", "stany zjednoczone") -> "🇺🇸"
            hasAny(text, "canada", "ca", "kanada") -> "🇨🇦"
            hasAny(text, "switzerland", "ch", "szwajcaria") -> "🇨🇭"
            hasAny(text, "sweden", "se", "szwecja") -> "🇸🇪"
            hasAny(text, "norway", "no", "norwegia") -> "🇳🇴"
            hasAny(text, "finland", "fi", "finlandia") -> "🇫🇮"
            hasAny(text, "denmark", "dk", "dania") -> "🇩🇰"
            hasAny(text, "austria", "at", "austria") -> "🇦🇹"
            hasAny(text, "italy", "it", "włoch", "wlochy") -> "🇮🇹"
            hasAny(text, "spain", "es", "hiszpania") -> "🇪🇸"
            hasAny(text, "czech", "cz", "czechy") -> "🇨🇿"
            hasAny(text, "slovakia", "sk", "słowacja", "slowacja") -> "🇸🇰"
            hasAny(text, "lithuania", "lt", "litwa") -> "🇱🇹"
            hasAny(text, "latvia", "lv", "łotwa", "lotwa") -> "🇱🇻"
            hasAny(text, "estonia", "ee", "estonia") -> "🇪🇪"
            hasAny(text, "ukraine", "ua", "ukraina") -> "🇺🇦"
            else -> ""
        }
    }

    private fun hasAny(text: String, vararg values: String): Boolean =
        values.any { value ->
            if (value.length <= 2) {
                Regex("""(^|[^a-z0-9])${Regex.escape(value)}([^a-z0-9]|$)""").containsMatchIn(text)
            } else text.contains(value)
        }
}
