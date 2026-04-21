package com.priyanshu.aura.network

/**
 * Data class representing a successfully identified song.
 */
data class SongResult(
    val title: String,
    val artist: String,
    val albumCoverUrl: String? = null,
    val spotifyId: String? = null,
    val youtubeId: String? = null
)

/**
 * V1-compatible facade. Delegates to [IdentificationRepository] so existing call-sites
 * (like the ViewModel) continue to compile without changes if they still reference this.
 *
 * Prefer calling [IdentificationRepository.identifyAudio] directly for new code.
 */
object AcrCloudApi {

    /**
     * Identifies a song from raw WAV audio bytes.
     * Delegates to [IdentificationRepository] which handles the combined
     * Audio Fingerprinting & Humming engine on the server side.
     */
    suspend fun identifySong(audioBytes: ByteArray): SongResult {
        return IdentificationRepository.identifyAudio(audioBytes)
    }
}
