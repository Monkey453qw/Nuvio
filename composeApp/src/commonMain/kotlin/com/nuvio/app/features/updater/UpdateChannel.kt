package com.nuvio.app.features.updater

enum class UpdateChannel(val storedValue: String) {
    STABLE("stable"),
    BETA("beta"),

    /**
     * Builds from this fork. They are published to
     * [CUSTOM_REPO] under the `custom-latest` tag and are always pre-release
     * versions (`<upstream version>-custom.<build>`), so this channel behaves
     * like beta for release selection but reads from the fork's repository.
     */
    CUSTOM("custom"),
    ;

    companion object {
        const val CUSTOM_REPO = "Monkey453qw/Nuvio"
        const val CUSTOM_RELEASE_TAG = "custom-latest"

        fun fromStoredValue(value: String?): UpdateChannel? = entries.firstOrNull {
            it.storedValue.equals(value, ignoreCase = true)
        }

        fun defaultForVersion(versionName: String): UpdateChannel =
            if (VersionUtils.isPrerelease(versionName)) BETA else STABLE
    }
}
