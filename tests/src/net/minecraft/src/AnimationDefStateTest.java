package net.minecraft.src;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

/**
 * Tests for Aero_AnimationDefinition, Aero_AnimationState, and Aero_AnimationBundle.
 * 25+ tests using the AAA pattern (Arrange, Act, Assert).
 */
public class AnimationDefStateTest {

    // Shared test data
    private Aero_AnimationClip idleClip;
    private Aero_AnimationClip spinClip;
    private Aero_AnimationClip shortClip;
    private Aero_AnimationBundle bundle;

    private static final float TICK = 1f / 20f; // 0.05 seconds
    private static final float EPSILON = 0.0001f;

    @Before
    public void setUp() {
        // Idle clip: 2 seconds, looping, single bone with no movement
        String[] idleBones = {"body"};
        float[][] idleRotTimes = { {0f, 2f} };
        float[][][] idleRotValues = { { {0f, 0f, 0f}, {0f, 0f, 0f} } };
        float[][] idlePosTimes = { {0f} };
        float[][][] idlePosValues = { { {0f, 0f, 0f} } };
        idleClip = new Aero_AnimationClip("idle", true, 2.0f,
                idleBones, idleRotTimes, idleRotValues, idlePosTimes, idlePosValues);

        // Spin clip: 1 second, looping, fan bone rotates 360 degrees
        String[] spinBones = {"fan"};
        float[][] spinRotTimes = { {0f, 1f} };
        float[][][] spinRotValues = { { {0f, 0f, 0f}, {0f, 360f, 0f} } };
        float[][] spinPosTimes = { {0f} };
        float[][][] spinPosValues = { { {0f, 0f, 0f} } };
        spinClip = new Aero_AnimationClip("spinning", true, 1.0f,
                spinBones, spinRotTimes, spinRotValues, spinPosTimes, spinPosValues);

        // Short clip: 0.1 second (2 ticks), non-looping
        String[] shortBones = {"arm"};
        float[][] shortRotTimes = { {0f, 0.1f} };
        float[][][] shortRotValues = { { {0f, 0f, 0f}, {90f, 0f, 0f} } };
        float[][] shortPosTimes = { {0f} };
        float[][][] shortPosValues = { { {0f, 0f, 0f} } };
        shortClip = new Aero_AnimationClip("short", false, 0.1f,
                shortBones, shortRotTimes, shortRotValues, shortPosTimes, shortPosValues);

        // Bundle with all clips
        java.util.HashMap clips = new java.util.HashMap();
        clips.put("idle", idleClip);
        clips.put("spinning", spinClip);
        clips.put("short", shortClip);

        java.util.HashMap pivots = new java.util.HashMap();
        pivots.put("fan", new float[]{0.5f, 0.5f, 0.5f});
        pivots.put("body", new float[]{0.5f, 0f, 0.5f});

        java.util.HashMap childMap = new java.util.HashMap();

        bundle = new Aero_AnimationBundle(clips, pivots, childMap);
    }

    // =======================================================================
    // AnimationDef tests (8 tests)
    // =======================================================================

    @Test
    public void testDefBuilderPatternReturnsThis() {
        // Arrange
        Aero_AnimationDefinition def = new Aero_AnimationDefinition();

        // Act
        Aero_AnimationDefinition returned = def.state(0, "idle");

        // Assert — builder returns the same instance
        assertSame(def, returned);
    }

    @Test
    public void testDefBuilderChainMultiple() {
        // Arrange & Act — chain three calls
        Aero_AnimationDefinition def = new Aero_AnimationDefinition()
                .state(0, "idle")
                .state(1, "spinning")
                .state(2, "short");

        // Assert — all three registered
        assertEquals("idle", def.getClipName(0));
        assertEquals("spinning", def.getClipName(1));
        assertEquals("short", def.getClipName(2));
    }

    @Test
    public void testDefGetClipNameReturnsCorrectName() {
        // Arrange
        Aero_AnimationDefinition def = new Aero_AnimationDefinition().state(0, "idle");

        // Act
        String name = def.getClipName(0);

        // Assert
        assertEquals("idle", name);
    }

    @Test
    public void testDefGetClipNameReturnsNullForUndefined() {
        // Arrange
        Aero_AnimationDefinition def = new Aero_AnimationDefinition().state(0, "idle");

        // Act
        String name = def.getClipName(5);

        // Assert
        assertNull(name);
    }

    @Test
    public void testDefGetClipNameNegativeReturnsNull() {
        // Arrange
        Aero_AnimationDefinition def = new Aero_AnimationDefinition().state(0, "idle");

        // Act
        String name = def.getClipName(-1);

        // Assert
        assertNull(name);
    }

    @Test
    public void testDefMultipleStatesRegistered() {
        // Arrange
        Aero_AnimationDefinition def = new Aero_AnimationDefinition()
                .state(0, "idle")
                .state(1, "spinning")
                .state(5, "short"); // sparse: skip IDs 2-4

        // Act & Assert
        assertEquals("idle", def.getClipName(0));
        assertEquals("spinning", def.getClipName(1));
        assertNull(def.getClipName(2)); // gap
        assertNull(def.getClipName(3)); // gap
        assertNull(def.getClipName(4)); // gap
        assertEquals("short", def.getClipName(5));
    }

    @Test
    public void testDefCreateStateReturnsNonNull() {
        // Arrange
        Aero_AnimationDefinition def = new Aero_AnimationDefinition().state(0, "idle");

        // Act
        Aero_AnimationState state = def.createState(bundle);

        // Assert
        assertNotNull(state);
    }

    @Test
    public void testDefCreateStateLinksDefAndBundle() {
        // Arrange
        Aero_AnimationDefinition def = new Aero_AnimationDefinition().state(0, "idle");

        // Act
        Aero_AnimationState state = def.createState(bundle);

        // Assert — state references the correct def and bundle
        assertSame(def, state.getDef());
        assertSame(bundle, state.getBundle());
    }

    // =======================================================================
    // AnimationState tests (17 tests)
    // =======================================================================

    @Test
    public void testStateInitialStateIsZero() {
        // Arrange
        Aero_AnimationDefinition def = new Aero_AnimationDefinition().state(0, "idle");

        // Act
        Aero_AnimationState state = def.createState(bundle);

        // Assert
        assertEquals(0, state.currentState);
    }

    @Test
    public void testStateTickAdvancesTime() {
        // Arrange
        Aero_AnimationDefinition def = new Aero_AnimationDefinition().state(0, "idle");
        Aero_AnimationState state = def.createState(bundle);

        // Act — one tick = 1/20 second
        state.tick();

        // Assert — interpolated time at partialTick=1 should be 0.05
        assertEquals(TICK, state.getInterpolatedTime(1f), EPSILON);
    }

    @Test
    public void testStateMultipleTicksAccumulateTime() {
        // Arrange
        Aero_AnimationDefinition def = new Aero_AnimationDefinition().state(0, "idle");
        Aero_AnimationState state = def.createState(bundle);

        // Act — 10 ticks = 0.5 seconds
        for (int i = 0; i < 10; i++) {
            state.tick();
        }

        // Assert
        assertEquals(10 * TICK, state.getInterpolatedTime(1f), EPSILON);
    }

    @Test
    public void testStateSetStateToDifferentClipResetsTime() {
        // Arrange
        Aero_AnimationDefinition def = new Aero_AnimationDefinition()
                .state(0, "idle")
                .state(1, "spinning");
        Aero_AnimationState state = def.createState(bundle);

        // Advance 5 ticks in idle
        for (int i = 0; i < 5; i++) {
            state.tick();
        }

        // Act — switch to spinning (different clip name)
        state.setState(1);

        // Assert — time reset to 0
        assertEquals(0f, state.getInterpolatedTime(1f), EPSILON);
        assertEquals(1, state.currentState);
    }

    @Test
    public void testStateSetStateToSameStateIsNoOp() {
        // Arrange
        Aero_AnimationDefinition def = new Aero_AnimationDefinition().state(0, "idle");
        Aero_AnimationState state = def.createState(bundle);

        // Advance 5 ticks
        for (int i = 0; i < 5; i++) {
            state.tick();
        }
        float timeBefore = state.getInterpolatedTime(1f);

        // Act — set to same state (0)
        state.setState(0);

        // Assert — time preserved
        assertEquals(timeBefore, state.getInterpolatedTime(1f), EPSILON);
    }

    @Test
    public void testStateSetStateSameClipNamePreservesTime() {
        // Arrange — two states mapped to the same clip name
        Aero_AnimationDefinition def = new Aero_AnimationDefinition()
                .state(0, "idle")
                .state(1, "idle"); // same clip as state 0
        Aero_AnimationState state = def.createState(bundle);

        // Advance 5 ticks
        for (int i = 0; i < 5; i++) {
            state.tick();
        }
        float timeBefore = state.getInterpolatedTime(1f);

        // Act — switch from state 0 to state 1 (same clip name "idle")
        state.setState(1);

        // Assert — time NOT reset because clip name didn't change
        assertEquals(timeBefore, state.getInterpolatedTime(1f), EPSILON);
        assertEquals(1, state.currentState);
    }

    @Test
    public void testStateGetCurrentClipReturnsCorrectClip() {
        // Arrange
        Aero_AnimationDefinition def = new Aero_AnimationDefinition()
                .state(0, "idle")
                .state(1, "spinning");
        Aero_AnimationState state = def.createState(bundle);

        // Act
        Aero_AnimationClip clip = state.getCurrentClip();

        // Assert — state 0 maps to "idle" clip
        assertNotNull(clip);
        assertEquals("idle", clip.name);
    }

    @Test
    public void testStateGetCurrentClipAfterStateChange() {
        // Arrange
        Aero_AnimationDefinition def = new Aero_AnimationDefinition()
                .state(0, "idle")
                .state(1, "spinning");
        Aero_AnimationState state = def.createState(bundle);

        // Act
        state.setState(1);
        Aero_AnimationClip clip = state.getCurrentClip();

        // Assert
        assertNotNull(clip);
        assertEquals("spinning", clip.name);
    }

    @Test
    public void testStateGetCurrentClipReturnsNullForUndefinedState() {
        // Arrange — state 2 is not defined
        Aero_AnimationDefinition def = new Aero_AnimationDefinition().state(0, "idle");
        Aero_AnimationState state = def.createState(bundle);

        // Act — force currentState to undefined (bypass setState check)
        state.currentState = 99;
        Aero_AnimationClip clip = state.getCurrentClip();

        // Assert
        assertNull(clip);
    }

    @Test
    public void testStateInterpolatedTimeAtZeroReturnsPrevTime() {
        // Arrange
        Aero_AnimationDefinition def = new Aero_AnimationDefinition().state(0, "idle");
        Aero_AnimationState state = def.createState(bundle);

        // Tick twice so prevTime = TICK, currentTime = 2*TICK
        state.tick();
        state.tick();

        // Act — partialTick = 0 should return prevTime
        float time = state.getInterpolatedTime(0f);

        // Assert — prevTime is the time after the first tick
        assertEquals(TICK, time, EPSILON);
    }

    @Test
    public void testStateInterpolatedTimeAtOneReturnsCurrentTime() {
        // Arrange
        Aero_AnimationDefinition def = new Aero_AnimationDefinition().state(0, "idle");
        Aero_AnimationState state = def.createState(bundle);

        state.tick();
        state.tick();

        // Act — partialTick = 1 should return currentTime
        float time = state.getInterpolatedTime(1f);

        // Assert
        assertEquals(2 * TICK, time, EPSILON);
    }

    @Test
    public void testStateInterpolatedTimeAtHalfReturnsMidpoint() {
        // Arrange
        Aero_AnimationDefinition def = new Aero_AnimationDefinition().state(0, "idle");
        Aero_AnimationState state = def.createState(bundle);

        state.tick();
        state.tick();

        // Act — partialTick = 0.5 should return midpoint
        float time = state.getInterpolatedTime(0.5f);

        // Assert — midpoint between TICK and 2*TICK = 1.5*TICK
        assertEquals(1.5f * TICK, time, EPSILON);
    }

    @Test
    public void testStateLoopWrapsAtClipLength() {
        // Arrange — spinning clip is 1.0 second long, looping
        Aero_AnimationDefinition def = new Aero_AnimationDefinition().state(0, "spinning");
        Aero_AnimationState state = def.createState(bundle);

        // Act — tick 21 times = 1.05 seconds, should wrap past 1.0
        for (int i = 0; i < 21; i++) {
            state.tick();
        }

        // Assert — time should have wrapped (be less than clip length)
        float time = state.getInterpolatedTime(1f);
        assertTrue("Time should wrap: " + time, time < spinClip.length);
        assertEquals(21 * TICK - spinClip.length, time, EPSILON);
    }

    @Test
    public void testStateNonLoopClampsAtClipLength() {
        // Arrange — short clip is 0.1 second (2 ticks), non-looping
        Aero_AnimationDefinition def = new Aero_AnimationDefinition().state(0, "short");
        Aero_AnimationState state = def.createState(bundle);

        // Act — tick 10 times (0.5 seconds), well past the 0.1s clip
        for (int i = 0; i < 10; i++) {
            state.tick();
        }

        // Assert — time should clamp at clip length
        float time = state.getInterpolatedTime(1f);
        assertEquals(shortClip.length, time, EPSILON);
    }

    @Test
    public void testStateNbtRoundTrip() {
        // Arrange
        Aero_AnimationDefinition def = new Aero_AnimationDefinition()
                .state(0, "idle")
                .state(1, "spinning");
        Aero_AnimationState state = def.createState(bundle);

        // Advance to state 1 and tick several times
        state.tick();
        state.setState(1);
        for (int i = 0; i < 5; i++) {
            state.tick();
        }
        int savedState = state.currentState;
        float savedTime = state.getInterpolatedTime(1f);

        // Act — write to NBT and read into a fresh state
        NBTTagCompound nbt = new NBTTagCompound();
        state.writeToNBT(nbt);

        Aero_AnimationState restored = def.createState(bundle);
        restored.readFromNBT(nbt);

        // Assert — state and time preserved
        assertEquals(savedState, restored.currentState);
        assertEquals(savedTime, restored.getInterpolatedTime(1f), EPSILON);
    }

    @Test
    public void testStateNbtReadRestoresPrevTimeEqualsTime() {
        // Arrange — after readFromNBT, prevTime should equal playbackTime
        // so there is no jump on the first rendered frame
        Aero_AnimationDefinition def = new Aero_AnimationDefinition().state(0, "idle");
        Aero_AnimationState state = def.createState(bundle);

        // Tick a few times and save
        for (int i = 0; i < 10; i++) {
            state.tick();
        }
        NBTTagCompound nbt = new NBTTagCompound();
        state.writeToNBT(nbt);

        // Act — restore into fresh state
        Aero_AnimationState restored = def.createState(bundle);
        restored.readFromNBT(nbt);

        // Assert — partialTick 0 and 1 should return the same value
        // (prevTime == currentTime, so interpolation yields a constant)
        float at0 = restored.getInterpolatedTime(0f);
        float at1 = restored.getInterpolatedTime(1f);
        assertEquals("prevTime should equal time after readFromNBT", at0, at1, EPSILON);
    }

    @Test
    public void testStateNbtAbsentKeysDefaultToZero() {
        // Arrange — empty NBT (simulates old save without animation data)
        Aero_AnimationDefinition def = new Aero_AnimationDefinition().state(0, "idle");
        Aero_AnimationState state = def.createState(bundle);

        // Tick a few times first
        for (int i = 0; i < 5; i++) {
            state.tick();
        }

        // Act — read from empty NBT
        NBTTagCompound emptyNbt = new NBTTagCompound();
        state.readFromNBT(emptyNbt);

        // Assert — defaults: state=0, time=0
        assertEquals(0, state.currentState);
        assertEquals(0f, state.getInterpolatedTime(1f), EPSILON);
    }

    @Test
    public void testStateTickWithNoClipKeepsTimeAtZero() {
        // Arrange — state 0 has no clip defined
        Aero_AnimationDefinition def = new Aero_AnimationDefinition(); // no states registered
        Aero_AnimationState state = def.createState(bundle);

        // Act
        state.tick();
        state.tick();

        // Assert — time stays at 0 since there's no clip
        assertEquals(0f, state.getInterpolatedTime(1f), EPSILON);
    }

    // =======================================================================
    // AnimBundle tests (5 tests)
    // =======================================================================

    @Test
    public void testBundleGetClipReturnsExistingClip() {
        // Arrange — bundle set up in setUp()

        // Act
        Aero_AnimationClip clip = bundle.getClip("idle");

        // Assert
        assertNotNull(clip);
        assertEquals("idle", clip.name);
    }

    @Test
    public void testBundleGetClipReturnsNullForMissing() {
        // Arrange — bundle set up in setUp()

        // Act
        Aero_AnimationClip clip = bundle.getClip("nonexistent");

        // Assert
        assertNull(clip);
    }

    @Test
    public void testBundleGetClipNullNameReturnsNull() {
        // Arrange — bundle set up in setUp()

        // Act
        Aero_AnimationClip clip = bundle.getClip(null);

        // Assert
        assertNull(clip);
    }

    @Test
    public void testBundleGetPivotReturnsExistingPivot() {
        // Arrange — bundle has "fan" pivot at [0.5, 0.5, 0.5]

        // Act
        float[] pivot = bundle.getPivot("fan");

        // Assert
        assertNotNull(pivot);
        assertEquals(3, pivot.length);
        assertEquals(0.5f, pivot[0], EPSILON);
        assertEquals(0.5f, pivot[1], EPSILON);
        assertEquals(0.5f, pivot[2], EPSILON);
    }

    @Test
    public void testBundleGetPivotReturnZeroForMissing() {
        // Arrange — bundle has no "leg" pivot

        // Act
        float[] pivot = bundle.getPivot("leg");

        // Assert — should return [0, 0, 0]
        assertNotNull(pivot);
        assertEquals(3, pivot.length);
        assertEquals(0f, pivot[0], EPSILON);
        assertEquals(0f, pivot[1], EPSILON);
        assertEquals(0f, pivot[2], EPSILON);
    }
}
