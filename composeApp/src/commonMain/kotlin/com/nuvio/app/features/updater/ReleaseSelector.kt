package com.nuvio.app.features.updater

internal object ReleaseSelector {
    private val prereleaseNamePattern = Regex(
        "(?:^|[\\s._-])(alpha|beta|rc|preview)(?:[\\s._-]|$)",
        RegexOption.IGNORE_CASE
    )

    /**
     * Custom builds carry their version inside the release name/title
     * (`1.1.0-beta.2-custom.42`) while the tag stays stable so the updater can
     * always resolve the same URL. Pull the first version-looking token out.
     */
    private val embeddedVersionPattern = Regex("\\d+\\.\\d+\\.\\d+(?:-[0-9A-Za-z.-]+)?")

    fun eligibleReleases(
        releases: List<GitHubReleaseDto>,
        channel: UpdateChannel
    ): List<GitHubReleaseDto> = releases
        .asSequence()
        .filterNot(GitHubReleaseDto::draft)
        .mapNotNull { release ->
            val version = releaseVersion(release, channel) ?: return@mapNotNull null
            ReleaseCandidate(
                release = release,
                version = version,
                prerelease = isPrerelease(release, version)
            )
        }
        // Stable is the only channel that ignores pre-releases; beta and custom
        // builds are published as pre-releases by design.
        .filter { candidate -> channel != UpdateChannel.STABLE || !candidate.prerelease }
        .sortedByDescending(ReleaseCandidate::version)
        .map(ReleaseCandidate::release)
        .toList()

    internal fun releaseVersion(
        release: GitHubReleaseDto,
        channel: UpdateChannel = UpdateChannel.STABLE,
    ): SemanticVersion? = if (channel == UpdateChannel.CUSTOM) {
        VersionUtils.parse(embeddedVersion(release.name))
            ?: VersionUtils.parse(embeddedVersion(release.tagName))
    } else {
        VersionUtils.parse(release.tagName) ?: VersionUtils.parse(release.name)
    }

    private fun embeddedVersion(text: String?): String? =
        text?.let { embeddedVersionPattern.find(it)?.value }

    private fun isPrerelease(
        release: GitHubReleaseDto,
        version: SemanticVersion
    ): Boolean = release.prerelease ||
        version.prerelease.isNotEmpty() ||
        prereleaseNamePattern.containsMatchIn(release.name.orEmpty())

    private data class ReleaseCandidate(
        val release: GitHubReleaseDto,
        val version: SemanticVersion,
        val prerelease: Boolean
    )
}
