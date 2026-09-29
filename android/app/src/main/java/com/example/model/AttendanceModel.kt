package com.example.model

import java.time.DayOfWeek
import java.time.LocalDate

enum class SlotStatus {
    PRESENT,
    ABSENT,
    CANCELLED
}

data class LectureSlot(
    val id: String,
    val slotNumber: String,
    val timeRange: String,
    val subject: String,
    val originalSubject: String = subject,
    val weight: Float,
    val originalWeight: Float = weight,
    val status: SlotStatus = SlotStatus.PRESENT,
    val isProxy: Boolean = false,
    val proxyNote: String = "",
    val isExtra: Boolean = false
) {
    // Effective attendance count only applies if weight > 0 and not cancelled
    val isCredited: Boolean get() = weight > 0f
}

data class DayAttendance(
    val date: LocalDate,
    val isHoliday: Boolean = false,
    val holidayReason: String? = null,
    val isSunday: Boolean = false,
    val slots: List<LectureSlot> = emptyList()
)

data class AttendanceMetrics(
    val baselineAttended: Float,
    val baselineConducted: Float,
    val loggedAttended: Float,
    val loggedConducted: Float,
    val totalAttended: Float,
    val totalConducted: Float,
    val currentPercentage: Float,
    val futureProjectedConducted: Float,
    val totalProjectedConducted: Float,
    val projectedMaxAttended: Float,
    val projectedFinalPercentage: Float,
    val safeBunkBalance: Int,
    val isDanger: Boolean,
    val consecutiveClassesToRecover: Int,
    val remainingSemesterDays: Int,
    val remainingLectures: Float
)

data class DayBunkCost(
    val dayOfWeek: DayOfWeek,
    val name: String,
    val totalCount: Int,
    val impactLevel: String, // "LOWEST", "LOW", "MEDIUM", "HIGH", "CRITICAL"
    val description: String
)

enum class UserRole {
    STUDENT,
    ADMIN
}

data class StudentProfile(
    val userId: String = "",
    val name: String = BunkIQConstants.STUDENT_NAME,
    val usn: String = BunkIQConstants.STUDENT_USN,
    val email: String = "",
    val role: UserRole = UserRole.STUDENT,
    val baselineAttended: Float = BunkIQConstants.BASELINE_ATTENDED,
    val baselineConducted: Float = BunkIQConstants.BASELINE_CONDUCTED,
    val currentPercentage: Float = 0f,
    val totalAttended: Float = 0f,
    val totalConducted: Float = 0f,
    val safeBunkBalance: Int = 0,
    val isDanger: Boolean = false,
    val lastActiveMillis: Long = System.currentTimeMillis()
)

data class ClassBroadcast(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val tag: String = "NOTICE", // "CAE_EXAM", "SCHEDULE_CHANGE", "HOLIDAY", "EXTRA_CLASS", "URGENT"
    val targetDate: String = "",
    val postedBy: String = "CR Atharva",
    val timestampMillis: Long = System.currentTimeMillis()
)

data class ClassScheduleOverride(
    val id: String = "",
    val date: String = "", // e.g. "2026-10-10"
    val slotNumber: String = "", // e.g. "P1", "P3", "ALL_DAY"
    val type: String = "PROXY", // "PROXY", "CANCELLED", "CAE_EXAM", "EXTRA_CLASS", "HOLIDAY"
    val newSubject: String = "",
    val weight: Float = 1f,
    val note: String = "",
    val author: String = "CR Atharva"
)

object BunkIQConstants {
    const val STUDENT_NAME = "Atharva Jadhav"
    const val STUDENT_USN = "CS25134"
    const val SUPER_ADMIN_EMAIL = "atharvajadhav21052007@gmail.com"
    const val ADMIN_PASSKEY = "BUNK2026"
    val SEMESTER_START: LocalDate = LocalDate.of(2026, 7, 6)
    val SEMESTER_END: LocalDate = LocalDate.of(2026, 10, 17) // Last working day
    val BASELINE_DATE: LocalDate = LocalDate.of(2026, 8, 31)
    const val BASELINE_ATTENDED = 138.5f
    const val BASELINE_CONDUCTED = 169.0f
    const val TARGET_THRESHOLD = 75.0f

    val HOLIDAYS: Map<LocalDate, String> = mapOf(
        LocalDate.of(2026, 9, 14) to "Ganesh Chaturthi",
        LocalDate.of(2026, 9, 18) to "Mahalaxmi Pooja",
        LocalDate.of(2026, 10, 2) to "Mahatma Gandhi Jayanti"
    )

    fun getDayBunkCosts(): List<DayBunkCost> = listOf(
        DayBunkCost(
            dayOfWeek = DayOfWeek.THURSDAY,
            name = "Thursday",
            totalCount = 1,
            impactLevel = "LOWEST",
            description = "Best day to bunk! Only 1 attendance count penalty (ES)."
        ),
        DayBunkCost(
            dayOfWeek = DayOfWeek.TUESDAY,
            name = "Tuesday",
            totalCount = 3,
            impactLevel = "LOW",
            description = "Low impact: 3 counts (DMGT, DSA-LAB 1 total, TG)."
        ),
        DayBunkCost(
            dayOfWeek = DayOfWeek.FRIDAY,
            name = "Friday",
            totalCount = 3,
            impactLevel = "LOW",
            description = "Manageable: 3 counts (FCC-LAB 1 total, DSA, Open Elective)."
        ),
        DayBunkCost(
            dayOfWeek = DayOfWeek.SATURDAY,
            name = "Saturday",
            totalCount = 5,
            impactLevel = "HIGH",
            description = "Heavy penalty: 5 counts (DSA, DMGT-T1, ES, Open Elective 2 counts)."
        ),
        DayBunkCost(
            dayOfWeek = DayOfWeek.MONDAY,
            name = "Monday",
            totalCount = 5,
            impactLevel = "HIGH",
            description = "Heavy penalty: 5 counts (MDM, CAO, DMGT, DMGT-T2, ED)."
        ),
        DayBunkCost(
            dayOfWeek = DayOfWeek.WEDNESDAY,
            name = "Wednesday",
            totalCount = 6,
            impactLevel = "CRITICAL",
            description = "MAXIMUM DANGER: 6 attendance counts in 1 day! Avoid bunking."
        )
    )

    fun getDefaultSlotsForDay(dayOfWeek: DayOfWeek, datePrefix: String = ""): List<LectureSlot> {
        return when (dayOfWeek) {
            DayOfWeek.MONDAY -> listOf(
                LectureSlot("${datePrefix}_m1", "P1", "09:00 - 10:00", "MDM", weight = 1f),
                LectureSlot("${datePrefix}_m2", "P2", "10:00 - 11:00", "CAO", weight = 1f),
                LectureSlot("${datePrefix}_m3", "P3", "11:15 - 12:15", "DMGT", weight = 1f),
                LectureSlot("${datePrefix}_m4", "P4", "12:15 - 01:15", "DMGT-T2", weight = 1f),
                LectureSlot("${datePrefix}_m5", "P5", "02:00 - 03:00", "ED", weight = 1f),
                LectureSlot("${datePrefix}_m6", "P6", "03:00 - 04:00", "CEP/FP", weight = 0f)
            )
            DayOfWeek.TUESDAY -> listOf(
                LectureSlot("${datePrefix}_t1", "P1-2", "09:00 - 11:00", "MDM/Lib (Free)", weight = 0f),
                LectureSlot("${datePrefix}_t2", "P3", "11:15 - 12:15", "DMGT", weight = 1f),
                LectureSlot("${datePrefix}_t3", "P4-5", "12:15 - 02:15", "DSA-LAB", weight = 1f),
                LectureSlot("${datePrefix}_t4", "P6", "02:15 - 03:15", "TG", weight = 1f)
            )
            DayOfWeek.WEDNESDAY -> listOf(
                LectureSlot("${datePrefix}_w1", "P1", "09:00 - 10:00", "CAO", weight = 1f),
                LectureSlot("${datePrefix}_w2", "P2", "10:00 - 11:00", "DSA", weight = 1f),
                LectureSlot("${datePrefix}_w3", "P3", "11:15 - 12:15", "DMGT", weight = 1f),
                LectureSlot("${datePrefix}_w4", "P4", "12:15 - 01:15", "ED", weight = 1f),
                LectureSlot("${datePrefix}_w5", "P5-6", "02:00 - 04:00", "MDM", weight = 2f)
            )
            DayOfWeek.THURSDAY -> listOf(
                LectureSlot("${datePrefix}_th1", "P1-2", "09:00 - 11:00", "MDM/Lib (Free)", weight = 0f),
                LectureSlot("${datePrefix}_th2", "P3", "11:15 - 12:15", "Co-Activity", weight = 0f),
                LectureSlot("${datePrefix}_th3", "P4", "12:15 - 01:15", "ES", weight = 1f),
                LectureSlot("${datePrefix}_th4", "P5-6", "02:00 - 04:00", "CEP/FP", weight = 0f)
            )
            DayOfWeek.FRIDAY -> listOf(
                LectureSlot("${datePrefix}_f1", "P1-2", "09:00 - 11:00", "FCC-LAB", weight = 1f),
                LectureSlot("${datePrefix}_f2", "P3", "11:15 - 12:15", "DSA", weight = 1f),
                LectureSlot("${datePrefix}_f3", "P4-5", "12:15 - 02:15", "Forum", weight = 0f),
                LectureSlot("${datePrefix}_f4", "P6", "02:15 - 03:15", "Open Elective", weight = 1f)
            )
            DayOfWeek.SATURDAY -> listOf(
                LectureSlot("${datePrefix}_s1", "P1", "09:00 - 10:00", "DSA", weight = 1f),
                LectureSlot("${datePrefix}_s2", "P2", "10:00 - 11:00", "DMGT-T1", weight = 1f),
                LectureSlot("${datePrefix}_s3", "P3", "11:15 - 12:15", "CEP/FP", weight = 0f),
                LectureSlot("${datePrefix}_s4", "P4", "12:15 - 01:15", "ES", weight = 1f),
                LectureSlot("${datePrefix}_s5", "P5-6", "02:00 - 04:00", "Open Elective", weight = 2f)
            )
            DayOfWeek.SUNDAY -> emptyList()
        }
    }

    fun getStandardDayCount(dayOfWeek: DayOfWeek): Int {
        return when (dayOfWeek) {
            DayOfWeek.MONDAY -> 5
            DayOfWeek.TUESDAY -> 3
            DayOfWeek.WEDNESDAY -> 6
            DayOfWeek.THURSDAY -> 1
            DayOfWeek.FRIDAY -> 3
            DayOfWeek.SATURDAY -> 5
            DayOfWeek.SUNDAY -> 0
        }
    }
}

enum class GeofenceStatus {
    INSIDE_CAMPUS,
    OUTSIDE_CAMPUS,
    UNKNOWN,
    PERMISSION_DENIED
}

data class CampusGeofenceConfig(
    val name: String = "Campus Main Gate",
    val latitude: Double = 18.5204, // Default Pune college coordinates
    val longitude: Double = 73.8567,
    val radiusMeters: Float = 200f,
    val autoAttendanceEnabled: Boolean = true
)

data class GeofenceLogEvent(
    val id: String,
    val timestamp: Long,
    val timeFormatted: String,
    val isInside: Boolean,
    val actionTaken: String, // "AUTO_PRESENT", "AUTO_BUNK", "CHECKED"
    val subject: String,
    val slotNumber: String,
    val distanceMeters: Float,
    val message: String
)

object LectureTimeParser {
    /**
     * Parse timeRange strings like "09:00 - 10:00", "12:15 - 01:15", "02:00 - 04:00"
     * and check if targetLocalTime falls between start and end.
     */
    fun isTimeInSlot(timeRange: String, now: java.time.LocalTime): Boolean {
        val parts = timeRange.split("-").map { it.trim() }
        if (parts.size != 2) return false
        val start = parseSlotTime(parts[0], isEndTime = false) ?: return false
        val end = parseSlotTime(parts[1], isEndTime = true) ?: return false

        return !now.isBefore(start) && now.isBefore(end)
    }

    fun parseSlotTime(raw: String, isEndTime: Boolean): java.time.LocalTime? {
        val timeParts = raw.split(":").map { it.trim() }
        if (timeParts.size != 2) return null
        var hour = timeParts[0].toIntOrNull() ?: return null
        val minute = timeParts[1].toIntOrNull() ?: return null

        // College slots from 01 to 06 are PM (afternoon), 08 to 11 are AM (morning), 12 is 12 PM
        if (hour in 1..6) {
            hour += 12
        }

        return try {
            java.time.LocalTime.of(hour, minute)
        } catch (e: Exception) {
            null
        }
    }
}

