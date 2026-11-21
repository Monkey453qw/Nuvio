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

        /** Marks a build produced by this fork, see the custom CI workflow. */
        private const val CUSTOM_VERSION_MARKER = "-custom."

        fun fromStoredValue(value: String?): UpdateChannel? = entries.firstOrNull {
            it.storedValue.equals(value, ignoreCase = true)
        }

        /**
         * Custom builds default to their own channel so a fresh install never
         * offers the official build on top of the fork.
         */
        fun defaultForVersion(versionName: String): UpdateChannel = when {
            versionName.contains(CUSTOM_VERSION_MARKER, ignoreCase = true) -> CUSTOM
            VersionUtils.isPrerelease(versionName) -> BETA
            else -> STABLE
        }
    }
}
