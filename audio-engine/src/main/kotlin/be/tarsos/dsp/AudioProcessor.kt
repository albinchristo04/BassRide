// Minimal stub — replaces the be.tarsos.dsp:core dependency which is unavailable
// via any public Maven repository.
package be.tarsos.dsp

/**
 * DSP processing unit that receives audio buffers one at a time.
 *
 * Stub that matches the subset of the TarsosDSP AudioProcessor interface used by
 * [TarsosEqEngine][com.velcuri.bassride.audio.tarsos.TarsosEqEngine].
 */
interface AudioProcessor {
    /**
     * Process the given [audioEvent] in-place. Return `true` to continue the chain,
     * `false` to stop processing further processors.
     */
    fun process(audioEvent: AudioEvent): Boolean

    /** Called when the audio stream ends. Release any state here. */
    fun processingFinished()
}
