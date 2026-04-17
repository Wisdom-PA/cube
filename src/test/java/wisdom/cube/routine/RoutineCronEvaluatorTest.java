package wisdom.cube.routine;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoutineCronEvaluatorTest {

    @Test
    void matchesMinuteAndHourWhenDayFieldsAreStars() {
        ZonedDateTime t = ZonedDateTime.of(2026, 4, 17, 18, 0, 0, 0, ZoneOffset.UTC);
        assertTrue(RoutineCronEvaluator.matches("0 18 * * *", t));
        assertFalse(RoutineCronEvaluator.matches("1 18 * * *", t));
        assertFalse(RoutineCronEvaluator.matches("0 7 * * *", t));
    }

    @Test
    void rejectsNonDailyPatterns() {
        ZonedDateTime t = ZonedDateTime.of(2026, 4, 17, 18, 0, 0, 0, ZoneOffset.UTC);
        assertFalse(RoutineCronEvaluator.matches("0 18 1 * *", t));
        assertFalse(RoutineCronEvaluator.matches("bad", t));
    }

    @Test
    void rejectsWrongFieldCountOrNonNumericFields() {
        ZonedDateTime t = ZonedDateTime.of(2026, 4, 17, 18, 0, 0, 0, ZoneOffset.UTC);
        assertFalse(RoutineCronEvaluator.matches("0 18 * *", t));
        assertFalse(RoutineCronEvaluator.matches("* 18 * * *", t));
        assertFalse(RoutineCronEvaluator.matches("0 * * * *", t));
        assertFalse(RoutineCronEvaluator.matches("60 18 * * *", t));
        assertFalse(RoutineCronEvaluator.matches("0 24 * * *", t));
    }
}
