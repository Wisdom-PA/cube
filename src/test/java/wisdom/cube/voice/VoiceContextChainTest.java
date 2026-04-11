package wisdom.cube.voice;

import org.junit.jupiter.api.Test;
import wisdom.cube.core.AutomationEngine;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoiceContextChainTest {

    @Test
    void followUpTurnItOffUsesLastRoom() {
        AtomicLong clock = new AtomicLong(0L);
        VoiceContextChain ctx = new VoiceContextChain(clock::get);
        ctx.recordAutomationSuccess(new AutomationEngine.Intent("set_light", "living_room", "on"));
        clock.addAndGet(30_000L);
        Optional<AutomationEngine.Intent> r = ctx.resolveFollowUp("turn it off");
        assertEquals("set_light", r.orElseThrow().type());
        assertEquals("living_room", r.orElseThrow().targets());
        assertEquals("off", r.orElseThrow().parameters());
    }

    @Test
    void followUpExpiredAfterSixtySeconds() {
        AtomicLong clock = new AtomicLong(0L);
        VoiceContextChain ctx = new VoiceContextChain(clock::get);
        ctx.recordAutomationSuccess(new AutomationEngine.Intent("set_light", "kitchen", "on"));
        clock.addAndGet(VoiceContextChain.DEFAULT_MAX_AGE_MS + 1);
        assertTrue(ctx.resolveFollowUp("turn it off").isEmpty());
    }

    @Test
    void clearDropsContext() {
        VoiceContextChain ctx = new VoiceContextChain();
        ctx.recordAutomationSuccess(new AutomationEngine.Intent("set_light", "kitchen", "on"));
        ctx.clear();
        assertFalse(ctx.isFresh(VoiceContextChain.DEFAULT_MAX_AGE_MS));
    }

    @Test
    void brighterAdjustsBrightness() {
        AtomicLong clock = new AtomicLong(0L);
        VoiceContextChain ctx = new VoiceContextChain(clock::get);
        ctx.recordAutomationSuccess(new AutomationEngine.Intent("set_brightness", "living_room", "0.5"));
        Optional<AutomationEngine.Intent> r = ctx.resolveFollowUp("make it brighter");
        assertEquals("set_brightness", r.orElseThrow().type());
        assertEquals("0.7", r.orElseThrow().parameters());
    }

    @Test
    void dimmerLowersBrightnessAndClamps() {
        VoiceContextChain ctx = new VoiceContextChain();
        ctx.recordAutomationSuccess(new AutomationEngine.Intent("set_brightness", "kitchen", "0.1"));
        Optional<AutomationEngine.Intent> r = ctx.resolveFollowUp("dim the lights");
        assertEquals("0.0", r.orElseThrow().parameters());
    }

    @Test
    void turnItOnFromContext() {
        VoiceContextChain ctx = new VoiceContextChain();
        ctx.recordAutomationSuccess(new AutomationEngine.Intent("set_light", "kitchen", "off"));
        Optional<AutomationEngine.Intent> r = ctx.resolveFollowUp("switch it on");
        assertEquals("on", r.orElseThrow().parameters());
        assertEquals("kitchen", r.orElseThrow().targets());
    }

    @Test
    void emptyTranscriptNoFollowUp() {
        VoiceContextChain ctx = new VoiceContextChain();
        ctx.recordAutomationSuccess(new AutomationEngine.Intent("set_light", "kitchen", "on"));
        assertTrue(ctx.resolveFollowUp("   ").isEmpty());
    }

    @Test
    void invalidLastBrightnessFallsBackToHalf() {
        VoiceContextChain ctx = new VoiceContextChain();
        ctx.recordAutomationSuccess(new AutomationEngine.Intent("set_brightness", "r", "not-a-number"));
        Optional<AutomationEngine.Intent> r = ctx.resolveFollowUp("brighter");
        assertEquals("0.7", r.orElseThrow().parameters());
    }

    @Test
    void noContextBeforeRecord() {
        assertTrue(new VoiceContextChain().resolveFollowUp("turn it off").isEmpty());
    }

    @Test
    void switchItOffMatches() {
        VoiceContextChain ctx = new VoiceContextChain();
        ctx.recordAutomationSuccess(new AutomationEngine.Intent("set_light", "living_room", "on"));
        Optional<AutomationEngine.Intent> r = ctx.resolveFollowUp("switch it off");
        assertEquals("off", r.orElseThrow().parameters());
    }

    @Test
    void turnItOnWithWordsAround() {
        VoiceContextChain ctx = new VoiceContextChain();
        ctx.recordAutomationSuccess(new AutomationEngine.Intent("set_light", "bedroom", "off"));
        Optional<AutomationEngine.Intent> r = ctx.resolveFollowUp("please turn it on now");
        assertEquals("on", r.orElseThrow().parameters());
        assertEquals("bedroom", r.orElseThrow().targets());
    }

    @Test
    void dimmerKeywordLowersBrightness() {
        VoiceContextChain ctx = new VoiceContextChain();
        ctx.recordAutomationSuccess(new AutomationEngine.Intent("set_brightness", "office", "0.8"));
        Optional<AutomationEngine.Intent> r = ctx.resolveFollowUp("dimmer");
        assertEquals(0.6, Double.parseDouble(r.orElseThrow().parameters()), 1e-9);
    }
}
