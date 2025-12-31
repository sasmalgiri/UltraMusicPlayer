package com.ultramusic.player.audio

import android.content.Context
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for AudioBattleEngine
 *
 * Tests battle modes, presets, state management, and EQ band logic.
 * Note: Actual audio processing requires Android device testing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AudioBattleEngineTest {

    private lateinit var battleEngine: AudioBattleEngine
    private val mockContext: Context = mockk(relaxed = true)

    @Before
    fun setup() {
        battleEngine = AudioBattleEngine(mockContext)
    }

    // ==================== INITIAL STATE TESTS ====================

    @Test
    fun `initial battle mode is OFF`() {
        assertThat(battleEngine.battleMode.value).isEqualTo(BattleMode.OFF)
    }

    @Test
    fun `initial bass level is 500`() {
        assertThat(battleEngine.bassLevel.value).isEqualTo(500)
    }

    @Test
    fun `initial loudness gain is 0`() {
        assertThat(battleEngine.loudnessGain.value).isEqualTo(0)
    }

    @Test
    fun `initial clarity level is 50`() {
        assertThat(battleEngine.clarityLevel.value).isEqualTo(50)
    }

    @Test
    fun `initial spatial level is 500`() {
        assertThat(battleEngine.spatialLevel.value).isEqualTo(500)
    }

    @Test
    fun `initial enabled state is false`() {
        assertThat(battleEngine.isEnabled.value).isFalse()
    }

    @Test
    fun `initial current preset is null`() {
        assertThat(battleEngine.currentPreset.value).isNull()
    }

    // ==================== BASS CONTROL TESTS ====================

    @Test
    fun `setBassBoost updates bass level state`() {
        battleEngine.setBassBoost(800)

        assertThat(battleEngine.bassLevel.value).isEqualTo(800)
    }

    @Test
    fun `setBassBoost clamps value to 0-1000 range`() {
        battleEngine.setBassBoost(-100)
        assertThat(battleEngine.bassLevel.value).isEqualTo(0)

        battleEngine.setBassBoost(1500)
        assertThat(battleEngine.bassLevel.value).isEqualTo(1000)
    }

    @Test
    fun `setBassBoost accepts edge values`() {
        battleEngine.setBassBoost(0)
        assertThat(battleEngine.bassLevel.value).isEqualTo(0)

        battleEngine.setBassBoost(1000)
        assertThat(battleEngine.bassLevel.value).isEqualTo(1000)
    }

    // ==================== LOUDNESS CONTROL TESTS ====================

    @Test
    fun `setLoudness updates loudness gain state`() {
        battleEngine.setLoudness(600)

        assertThat(battleEngine.loudnessGain.value).isEqualTo(600)
    }

    @Test
    fun `setLoudness clamps value to 0-1000 range`() {
        battleEngine.setLoudness(-50)
        assertThat(battleEngine.loudnessGain.value).isEqualTo(0)

        battleEngine.setLoudness(2000)
        assertThat(battleEngine.loudnessGain.value).isEqualTo(1000)
    }

    // ==================== CLARITY CONTROL TESTS ====================

    @Test
    fun `setClarity updates clarity level state`() {
        battleEngine.setClarity(75)

        assertThat(battleEngine.clarityLevel.value).isEqualTo(75)
    }

    @Test
    fun `setClarity clamps value to 0-100 range`() {
        battleEngine.setClarity(-10)
        assertThat(battleEngine.clarityLevel.value).isEqualTo(0)

        battleEngine.setClarity(150)
        assertThat(battleEngine.clarityLevel.value).isEqualTo(100)
    }

    // ==================== SPATIAL/VIRTUALIZER CONTROL TESTS ====================

    @Test
    fun `setVirtualizer updates spatial level state`() {
        battleEngine.setVirtualizer(700)

        assertThat(battleEngine.spatialLevel.value).isEqualTo(700)
    }

    @Test
    fun `setVirtualizer clamps value to 0-1000 range`() {
        battleEngine.setVirtualizer(-100)
        assertThat(battleEngine.spatialLevel.value).isEqualTo(0)

        battleEngine.setVirtualizer(1500)
        assertThat(battleEngine.spatialLevel.value).isEqualTo(1000)
    }

    // ==================== BATTLE MODE TESTS ====================

    @Test
    fun `setBattleMode updates battle mode state`() {
        battleEngine.setBattleMode(BattleMode.BASS_WARFARE)

        assertThat(battleEngine.battleMode.value).isEqualTo(BattleMode.BASS_WARFARE)
    }

    @Test
    fun `setBattleMode OFF resets to default state`() {
        // First set to some mode
        battleEngine.setBattleMode(BattleMode.FULL_ASSAULT)

        // Then set to OFF
        battleEngine.setBattleMode(BattleMode.OFF)

        assertThat(battleEngine.battleMode.value).isEqualTo(BattleMode.OFF)
    }

    @Test
    fun `all battle modes are settable`() {
        BattleMode.entries.forEach { mode ->
            battleEngine.setBattleMode(mode)
            assertThat(battleEngine.battleMode.value).isEqualTo(mode)
        }
    }

    @Test
    fun `BASS_WARFARE mode sets high bass`() {
        battleEngine.setBattleMode(BattleMode.BASS_WARFARE)

        assertThat(battleEngine.bassLevel.value).isEqualTo(1000)
    }

    @Test
    fun `CLARITY_STRIKE mode sets moderate bass`() {
        battleEngine.setBattleMode(BattleMode.CLARITY_STRIKE)

        assertThat(battleEngine.bassLevel.value).isEqualTo(400)
    }

    @Test
    fun `FULL_ASSAULT mode maxes everything`() {
        battleEngine.setBattleMode(BattleMode.FULL_ASSAULT)

        assertThat(battleEngine.bassLevel.value).isEqualTo(1000)
        assertThat(battleEngine.loudnessGain.value).isEqualTo(1000)
    }

    @Test
    fun `SPL_MONSTER mode sets max loudness`() {
        battleEngine.setBattleMode(BattleMode.SPL_MONSTER)

        assertThat(battleEngine.loudnessGain.value).isEqualTo(1000)
    }

    @Test
    fun `CROWD_REACH mode sets max virtualizer`() {
        battleEngine.setBattleMode(BattleMode.CROWD_REACH)

        assertThat(battleEngine.spatialLevel.value).isEqualTo(1000)
    }

    @Test
    fun `MAXIMUM_IMPACT mode sets all to max`() {
        battleEngine.setBattleMode(BattleMode.MAXIMUM_IMPACT)

        assertThat(battleEngine.bassLevel.value).isEqualTo(1000)
        assertThat(battleEngine.loudnessGain.value).isEqualTo(1000)
        assertThat(battleEngine.spatialLevel.value).isEqualTo(1000)
    }

    // ==================== PRESET TESTS ====================

    @Test
    fun `applyPreset updates all values`() {
        val preset = BattlePreset(
            name = "Test Preset",
            bassLevel = 750,
            loudnessGain = 600,
            clarityLevel = 80,
            spatialLevel = 650,
            eqBands = listOf(100, 200, 300, 400, 500)
        )

        battleEngine.applyPreset(preset)

        assertThat(battleEngine.bassLevel.value).isEqualTo(750)
        assertThat(battleEngine.loudnessGain.value).isEqualTo(600)
        assertThat(battleEngine.clarityLevel.value).isEqualTo(80)
        assertThat(battleEngine.spatialLevel.value).isEqualTo(650)
        assertThat(battleEngine.currentPreset.value).isEqualTo(preset)
    }

    @Test
    fun `saveAsPreset captures current state`() {
        battleEngine.setBassBoost(800)
        battleEngine.setLoudness(700)
        battleEngine.setClarity(90)
        battleEngine.setVirtualizer(600)

        val preset = battleEngine.saveAsPreset("My Preset")

        assertThat(preset.name).isEqualTo("My Preset")
        assertThat(preset.bassLevel).isEqualTo(800)
        assertThat(preset.loudnessGain).isEqualTo(700)
        assertThat(preset.clarityLevel).isEqualTo(90)
        assertThat(preset.spatialLevel).isEqualTo(600)
    }

    // ==================== DEFAULT PRESETS TESTS ====================

    @Test
    fun `default presets list is not empty`() {
        assertThat(AudioBattleEngine.DEFAULT_PRESETS).isNotEmpty()
    }

    @Test
    fun `default presets have valid names`() {
        AudioBattleEngine.DEFAULT_PRESETS.forEach { preset ->
            assertThat(preset.name).isNotEmpty()
        }
    }

    @Test
    fun `default presets have valid bass levels`() {
        AudioBattleEngine.DEFAULT_PRESETS.forEach { preset ->
            assertThat(preset.bassLevel).isIn(0..1000)
        }
    }

    @Test
    fun `default presets have valid loudness gains`() {
        AudioBattleEngine.DEFAULT_PRESETS.forEach { preset ->
            assertThat(preset.loudnessGain).isIn(0..1000)
        }
    }

    @Test
    fun `default presets have valid clarity levels`() {
        AudioBattleEngine.DEFAULT_PRESETS.forEach { preset ->
            assertThat(preset.clarityLevel).isIn(0..100)
        }
    }

    @Test
    fun `default presets have valid spatial levels`() {
        AudioBattleEngine.DEFAULT_PRESETS.forEach { preset ->
            assertThat(preset.spatialLevel).isIn(0..1000)
        }
    }

    // ==================== QUICK ACTION TESTS ====================

    @Test
    fun `emergencyBassBoost sets max bass`() {
        battleEngine.emergencyBassBoost()

        assertThat(battleEngine.bassLevel.value).isEqualTo(1000)
        assertThat(battleEngine.loudnessGain.value).isEqualTo(800)
    }

    @Test
    fun `cutThrough sets max clarity`() {
        battleEngine.cutThrough()

        assertThat(battleEngine.clarityLevel.value).isEqualTo(100)
        assertThat(battleEngine.spatialLevel.value).isEqualTo(800)
    }

    @Test
    fun `goNuclear maxes all values`() {
        battleEngine.goNuclear()

        assertThat(battleEngine.bassLevel.value).isEqualTo(1000)
        assertThat(battleEngine.loudnessGain.value).isEqualTo(1000)
        assertThat(battleEngine.clarityLevel.value).isEqualTo(100)
        assertThat(battleEngine.spatialLevel.value).isEqualTo(1000)
    }

    // ==================== ENABLE/DISABLE TESTS ====================

    @Test
    fun `enable updates enabled state`() {
        battleEngine.enable()

        assertThat(battleEngine.isEnabled.value).isTrue()
    }

    // ==================== BATTLE MODE ENUM TESTS ====================

    @Test
    fun `BattleMode has all expected values`() {
        val modes = BattleMode.entries

        assertThat(modes).contains(BattleMode.OFF)
        assertThat(modes).contains(BattleMode.BASS_WARFARE)
        assertThat(modes).contains(BattleMode.CLARITY_STRIKE)
        assertThat(modes).contains(BattleMode.FULL_ASSAULT)
        assertThat(modes).contains(BattleMode.SPL_MONSTER)
        assertThat(modes).contains(BattleMode.CROWD_REACH)
        assertThat(modes).contains(BattleMode.MAXIMUM_IMPACT)
        assertThat(modes).contains(BattleMode.BALANCED_BATTLE)
        assertThat(modes).contains(BattleMode.INDOOR_BATTLE)
    }

    // ==================== EQ BAND DATA CLASS TESTS ====================

    @Test
    fun `EQBand frequencyLabel formats correctly for Hz`() {
        val lowBand = EQBand(0, 60, -1500, 1500, 0)
        assertThat(lowBand.frequencyLabel).contains("Hz")
    }

    @Test
    fun `EQBand frequencyLabel formats correctly for kHz`() {
        val highBand = EQBand(4, 14000, -1500, 1500, 0)
        assertThat(highBand.frequencyLabel).contains("kHz")
    }

    @Test
    fun `EQBand levelDb converts from millibels`() {
        val band = EQBand(0, 60, -1500, 1500, 600)
        assertThat(band.levelDb).isEqualTo(6f) // 600 mB = 6 dB
    }

    // ==================== BATTLE PRESET DATA CLASS TESTS ====================

    @Test
    fun `BattlePreset stores all values correctly`() {
        val preset = BattlePreset(
            name = "Test",
            bassLevel = 500,
            loudnessGain = 600,
            clarityLevel = 70,
            spatialLevel = 800,
            eqBands = listOf(100, 200, -100, 300, 400)
        )

        assertThat(preset.name).isEqualTo("Test")
        assertThat(preset.bassLevel).isEqualTo(500)
        assertThat(preset.loudnessGain).isEqualTo(600)
        assertThat(preset.clarityLevel).isEqualTo(70)
        assertThat(preset.spatialLevel).isEqualTo(800)
        assertThat(preset.eqBands).hasSize(5)
    }

    // ==================== EDGE CASES ====================

    @Test
    fun `multiple rapid state changes work correctly`() {
        repeat(100) { i ->
            battleEngine.setBassBoost(i * 10)
        }

        assertThat(battleEngine.bassLevel.value).isEqualTo(990)
    }

    @Test
    fun `alternating between modes works correctly`() {
        battleEngine.setBattleMode(BattleMode.BASS_WARFARE)
        battleEngine.setBattleMode(BattleMode.CLARITY_STRIKE)
        battleEngine.setBattleMode(BattleMode.BASS_WARFARE)

        assertThat(battleEngine.battleMode.value).isEqualTo(BattleMode.BASS_WARFARE)
        assertThat(battleEngine.bassLevel.value).isEqualTo(1000)
    }
}
