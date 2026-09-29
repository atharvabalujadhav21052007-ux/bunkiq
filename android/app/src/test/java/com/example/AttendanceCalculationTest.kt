package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AttendanceRepository
import com.example.model.BunkIQConstants
import com.example.model.SlotStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AttendanceCalculationTest {

    private lateinit var repository: AttendanceRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        repository = AttendanceRepository(context)
        repository.resetToDefaults()
    }

    @Test
    fun verifyWeekdayBunkCosts() {
        assertEquals(5, BunkIQConstants.getStandardDayCount(DayOfWeek.MONDAY))
        assertEquals(3, BunkIQConstants.getStandardDayCount(DayOfWeek.TUESDAY))
        assertEquals(6, BunkIQConstants.getStandardDayCount(DayOfWeek.WEDNESDAY))
        assertEquals(1, BunkIQConstants.getStandardDayCount(DayOfWeek.THURSDAY))
        assertEquals(3, BunkIQConstants.getStandardDayCount(DayOfWeek.FRIDAY))
        assertEquals(5, BunkIQConstants.getStandardDayCount(DayOfWeek.SATURDAY))
        assertEquals(0, BunkIQConstants.getStandardDayCount(DayOfWeek.SUNDAY))
    }

    @Test
    fun verifyHistoricalBaselineAndSept12Metrics() {
        val sept12 = LocalDate.of(2026, 9, 12)
        val metrics = repository.calculateMetrics(sept12)

        // Baseline must be 138.5 / 169.0
        assertEquals(138.5f, metrics.baselineAttended, 0.01f)
        assertEquals(169.0f, metrics.baselineConducted, 0.01f)

        // On Sept 12: current percentage must be above 75%
        assertTrue("Current percentage should be >= 75%", metrics.currentPercentage >= 75.0f)
        assertTrue("Safe bunk balance should be positive", metrics.safeBunkBalance > 0)
        assertFalse("Should not be in danger threshold initially", metrics.isDanger)
    }

    @Test
    fun verifySlotStatusUpdateAndProxyReplacement() {
        val date = LocalDate.of(2026, 9, 15) // Tuesday
        val day = repository.getDayAttendance(date)
        assertTrue(day.slots.isNotEmpty())

        val targetSlot = day.slots.first { it.subject == "DMGT" }
        repository.updateSlotStatus(date, targetSlot.id, SlotStatus.ABSENT)

        val updatedDay = repository.getDayAttendance(date)
        val updatedSlot = updatedDay.slots.first { it.id == targetSlot.id }
        assertEquals(SlotStatus.ABSENT, updatedSlot.status)

        // Test proxy replacement
        repository.replaceSlot(date, targetSlot.id, "CAO Proxy", 2f, "Faculty on leave")
        val proxyDay = repository.getDayAttendance(date)
        val proxySlot = proxyDay.slots.first { it.id == targetSlot.id }
        assertEquals("CAO Proxy", proxySlot.subject)
        assertEquals(2f, proxySlot.weight, 0.01f)
        assertTrue(proxySlot.isProxy)
        assertEquals("Faculty on leave", proxySlot.proxyNote)
    }

    @Test
    fun verifyExportAndImportJson() {
        val json = repository.exportJson()
        assertTrue("Exported JSON should contain BunkIQ", json.contains("BunkIQ"))
        assertTrue("Exported JSON should contain student name", json.contains("Atharva Jadhav"))

        val success = repository.importJson(json)
        assertTrue("Import JSON should succeed", success)
    }

    @Test
    fun verifyCampusGeofenceAutoAttendanceAndAutoBunk() {
        val tuesday = LocalDate.of(2026, 9, 15)
        val config = repository.getCampusConfig()

        // 1. Simulate Inside Campus coordinates (within 20m) at 11:30 AM (during DMGT lecture)
        val insideLat = config.latitude + 0.0001
        val insideLng = config.longitude + 0.0001
        val insideEvent = repository.processLocationUpdate(
            lat = insideLat,
            lng = insideLng,
            targetDate = tuesday,
            targetTime = java.time.LocalTime.of(11, 30)
        )

        assertTrue("Should be inside campus", insideEvent.isInside)
        val dayAfterInside = repository.getDayAttendance(tuesday)
        val dmgtSlot = dayAfterInside.slots.first { it.subject == "DMGT" }
        assertEquals("DMGT should be marked PRESENT on entering campus", SlotStatus.PRESENT, dmgtSlot.status)

        // 2. Simulate Outside Campus coordinates (~2km away) at 11:45 AM (stepping outside)
        val outsideLat = config.latitude + 0.02
        val outsideLng = config.longitude + 0.02
        val outsideEvent = repository.processLocationUpdate(
            lat = outsideLat,
            lng = outsideLng,
            targetDate = tuesday,
            targetTime = java.time.LocalTime.of(11, 45)
        )

        assertFalse("Should be outside campus", outsideEvent.isInside)
        val dayAfterOutside = repository.getDayAttendance(tuesday)
        val dmgtSlotAfterExit = dayAfterOutside.slots.first { it.subject == "DMGT" }
        assertEquals("DMGT should be auto-bunked (ABSENT) on leaving campus", SlotStatus.ABSENT, dmgtSlotAfterExit.status)
    }

    @Test
    fun verifyLectureTimeParser() {
        assertTrue(com.example.model.LectureTimeParser.isTimeInSlot("09:00 - 10:00", java.time.LocalTime.of(9, 30)))
        assertFalse(com.example.model.LectureTimeParser.isTimeInSlot("09:00 - 10:00", java.time.LocalTime.of(10, 30)))
        // 02:00 - 04:00 is afternoon (14:00 - 16:00)
        assertTrue(com.example.model.LectureTimeParser.isTimeInSlot("02:00 - 04:00", java.time.LocalTime.of(14, 45)))
        assertFalse(com.example.model.LectureTimeParser.isTimeInSlot("02:00 - 04:00", java.time.LocalTime.of(13, 30)))
    }
}
