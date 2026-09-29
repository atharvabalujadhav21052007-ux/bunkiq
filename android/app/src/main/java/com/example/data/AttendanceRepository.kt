package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.R
import com.example.location.CampusLocationManager
import com.example.model.AttendanceMetrics
import com.example.model.BunkIQConstants
import com.example.model.CampusGeofenceConfig
import com.example.model.DayAttendance
import com.example.model.GeofenceLogEvent
import com.example.model.GeofenceStatus
import com.example.model.LectureSlot
import com.example.model.LectureTimeParser
import com.example.model.SlotStatus
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

class AttendanceRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("bunkiq_prefs", Context.MODE_PRIVATE)

    private val databaseId: String = context.getString(R.string.firestore_database_id)
    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance(databaseId)
        } catch (e: Exception) {
            Log.w(TAG, "Firestore not initialized in current context: ${e.message}")
            null
        }
    }

    private val currentAuthUser get() = try {
        Firebase.auth.currentUser
    } catch (e: Exception) {
        null
    }

    companion object {
        private const val TAG = "AttendanceRepo"
        private const val KEY_DATA_JSON = "saved_attendance_data_v1"
        private const val KEY_GEOFENCE_CONFIG = "saved_geofence_config_v1"
        private const val KEY_GEOFENCE_LOGS = "saved_geofence_logs_v1"
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    }

    private var dayDataCache: MutableMap<LocalDate, DayAttendance> = mutableMapOf()
    private var campusConfig: CampusGeofenceConfig = loadCampusConfig()
    private val geofenceLogs: MutableList<GeofenceLogEvent> = loadGeofenceLogs().toMutableList()
    private var currentGeofenceStatus: GeofenceStatus = GeofenceStatus.UNKNOWN

    init {
        loadData()
    }

    fun getDayAttendance(date: LocalDate): DayAttendance {
        val cached = dayDataCache[date]
        if (cached != null) return cached

        // Build default day
        val isSunday = date.dayOfWeek == DayOfWeek.SUNDAY
        val holidayReason = BunkIQConstants.HOLIDAYS[date]
        val isHoliday = holidayReason != null || isSunday

        val slots = if (isHoliday) {
            emptyList()
        } else {
            val prefix = date.format(DATE_FORMATTER)
            BunkIQConstants.getDefaultSlotsForDay(date.dayOfWeek, prefix)
        }

        val day = DayAttendance(
            date = date,
            isHoliday = isHoliday,
            holidayReason = holidayReason ?: if (isSunday) "Sunday Off" else null,
            isSunday = isSunday,
            slots = slots
        )
        dayDataCache[date] = day
        return day
    }

    fun updateSlotStatus(date: LocalDate, slotId: String, newStatus: SlotStatus) {
        val currentDay = getDayAttendance(date)
        val updatedSlots = currentDay.slots.map { slot ->
            if (slot.id == slotId) slot.copy(status = newStatus) else slot
        }
        dayDataCache[date] = currentDay.copy(slots = updatedSlots)
        saveData()
    }

    fun replaceSlot(
        date: LocalDate,
        slotId: String,
        newSubject: String,
        newWeight: Float,
        proxyNote: String
    ) {
        val currentDay = getDayAttendance(date)
        val updatedSlots = currentDay.slots.map { slot ->
            if (slot.id == slotId) {
                slot.copy(
                    subject = newSubject,
                    weight = newWeight,
                    isProxy = true,
                    proxyNote = proxyNote
                )
            } else {
                slot
            }
        }
        dayDataCache[date] = currentDay.copy(slots = updatedSlots)
        saveData()
    }

    fun resetSlotToDefault(date: LocalDate, slotId: String) {
        val currentDay = getDayAttendance(date)
        val updatedSlots = currentDay.slots.map { slot ->
            if (slot.id == slotId) {
                slot.copy(
                    subject = slot.originalSubject,
                    weight = slot.originalWeight,
                    isProxy = false,
                    proxyNote = ""
                )
            } else {
                slot
            }
        }
        dayDataCache[date] = currentDay.copy(slots = updatedSlots)
        saveData()
    }

    fun addExtraSlot(
        date: LocalDate,
        slotNumber: String,
        timeRange: String,
        subject: String,
        weight: Float
    ) {
        val currentDay = getDayAttendance(date)
        val newSlot = LectureSlot(
            id = "extra_${System.currentTimeMillis()}",
            slotNumber = slotNumber.ifBlank { "Extra" },
            timeRange = timeRange.ifBlank { "Surprise Slot" },
            subject = subject.ifBlank { "Extra Lecture" },
            originalSubject = subject,
            weight = weight,
            originalWeight = weight,
            status = SlotStatus.PRESENT,
            isProxy = false,
            proxyNote = "",
            isExtra = true
        )
        dayDataCache[date] = currentDay.copy(slots = currentDay.slots + newSlot)
        saveData()
    }

    fun removeSlot(date: LocalDate, slotId: String) {
        val currentDay = getDayAttendance(date)
        dayDataCache[date] = currentDay.copy(slots = currentDay.slots.filterNot { it.id == slotId })
        saveData()
    }

    fun calculateMetrics(activeDate: LocalDate): AttendanceMetrics {
        var loggedAttended = 0f
        var loggedConducted = 0f

        // Count logged days from Sept 1, 2026 up to activeDate (or all logged dates <= activeDate)
        // Ensure all dates between Sept 1 and activeDate are computed
        var checkDate = LocalDate.of(2026, 9, 1)
        val lastLoggedDate = if (activeDate.isAfter(checkDate)) activeDate else checkDate

        while (!checkDate.isAfter(lastLoggedDate)) {
            val day = getDayAttendance(checkDate)
            for (slot in day.slots) {
                when (slot.status) {
                    SlotStatus.PRESENT -> {
                        loggedAttended += slot.weight
                        loggedConducted += slot.weight
                    }
                    SlotStatus.ABSENT -> {
                        loggedConducted += slot.weight
                    }
                    SlotStatus.CANCELLED -> {
                        // Not conducted, weight not counted
                    }
                }
            }
            checkDate = checkDate.plusDays(1)
        }

        val totalAttended = BunkIQConstants.BASELINE_ATTENDED + loggedAttended
        val totalConducted = BunkIQConstants.BASELINE_CONDUCTED + loggedConducted
        val currentPercentage = if (totalConducted > 0) (totalAttended / totalConducted) * 100f else 0f

        // Calculate future projected conducted from (lastLoggedDate + 1) to SEMESTER_END (2026-10-17)
        var futureProjectedConducted = 0f
        var futureDate = lastLoggedDate.plusDays(1)
        var remainingDaysCount = 0

        while (!futureDate.isAfter(BunkIQConstants.SEMESTER_END)) {
            val isSunday = futureDate.dayOfWeek == DayOfWeek.SUNDAY
            val isHoliday = BunkIQConstants.HOLIDAYS.containsKey(futureDate) || isSunday

            if (!isHoliday) {
                remainingDaysCount++
                // If user has already configured this future day
                val cached = dayDataCache[futureDate]
                if (cached != null && cached.slots.isNotEmpty()) {
                    val count = cached.slots.filter { it.status != SlotStatus.CANCELLED }.sumOf { it.weight.toDouble() }.toFloat()
                    futureProjectedConducted += count
                } else {
                    futureProjectedConducted += BunkIQConstants.getStandardDayCount(futureDate.dayOfWeek).toFloat()
                }
            }
            futureDate = futureDate.plusDays(1)
        }

        val totalProjectedConducted = totalConducted + futureProjectedConducted
        val projectedMaxAttended = totalAttended + futureProjectedConducted
        val projectedFinalPercentage = if (totalProjectedConducted > 0) {
            (projectedMaxAttended / totalProjectedConducted) * 100f
        } else {
            currentPercentage
        }

        // Safe Bunk Balance: how many future classes can be missed before falling below 75%
        // (projectedMaxAttended - bunks) / totalProjectedConducted >= 0.75
        // bunks <= projectedMaxAttended - 0.75 * totalProjectedConducted
        val rawSafeBunks = floor(projectedMaxAttended - (0.75f * totalProjectedConducted)).toInt()
        val safeBunkBalance = max(0, rawSafeBunks)

        val isDanger = currentPercentage < BunkIQConstants.TARGET_THRESHOLD || rawSafeBunks <= 0

        // Consecutive classes to restore 75%:
        // (totalAttended + X) / (totalConducted + X) >= 0.75
        // X >= (0.75 * totalConducted - totalAttended) / 0.25 = 3 * totalConducted - 4 * totalAttended
        val consecutiveClassesToRecover = if (currentPercentage < BunkIQConstants.TARGET_THRESHOLD) {
            max(0, ceil(3.0 * totalConducted - 4.0 * totalAttended).toInt())
        } else {
            0
        }

        return AttendanceMetrics(
            baselineAttended = BunkIQConstants.BASELINE_ATTENDED,
            baselineConducted = BunkIQConstants.BASELINE_CONDUCTED,
            loggedAttended = loggedAttended,
            loggedConducted = loggedConducted,
            totalAttended = totalAttended,
            totalConducted = totalConducted,
            currentPercentage = currentPercentage,
            futureProjectedConducted = futureProjectedConducted,
            totalProjectedConducted = totalProjectedConducted,
            projectedMaxAttended = projectedMaxAttended,
            projectedFinalPercentage = projectedFinalPercentage,
            safeBunkBalance = safeBunkBalance,
            isDanger = isDanger,
            consecutiveClassesToRecover = consecutiveClassesToRecover,
            remainingSemesterDays = remainingDaysCount,
            remainingLectures = futureProjectedConducted
        )
    }

    private fun loadData() {
        val savedJson = prefs.getString(KEY_DATA_JSON, null)
        if (savedJson.isNullOrBlank()) {
            seedDefaultHistoricalData()
            saveData()
            return
        }

        try {
            val root = JSONObject(savedJson)
            val daysArray = root.optJSONArray("days") ?: JSONArray()
            dayDataCache.clear()

            for (i in 0 until daysArray.length()) {
                val dayObj = daysArray.getJSONObject(i)
                val dateStr = dayObj.getString("date")
                val date = LocalDate.parse(dateStr, DATE_FORMATTER)
                val isHoliday = dayObj.optBoolean("isHoliday", false)
                val holidayReason = dayObj.optString("holidayReason", "").ifBlank { null }
                val isSunday = dayObj.optBoolean("isSunday", false)

                val slotsArray = dayObj.optJSONArray("slots") ?: JSONArray()
                val slots = mutableListOf<LectureSlot>()
                for (j in 0 until slotsArray.length()) {
                    val sObj = slotsArray.getJSONObject(j)
                    slots.add(
                        LectureSlot(
                            id = sObj.getString("id"),
                            slotNumber = sObj.getString("slotNumber"),
                            timeRange = sObj.getString("timeRange"),
                            subject = sObj.getString("subject"),
                            originalSubject = sObj.optString("originalSubject", sObj.getString("subject")),
                            weight = sObj.getDouble("weight").toFloat(),
                            originalWeight = sObj.optDouble("originalWeight", sObj.getDouble("weight")).toFloat(),
                            status = SlotStatus.valueOf(sObj.getString("status")),
                            isProxy = sObj.optBoolean("isProxy", false),
                            proxyNote = sObj.optString("proxyNote", ""),
                            isExtra = sObj.optBoolean("isExtra", false)
                        )
                    )
                }

                dayDataCache[date] = DayAttendance(
                    date = date,
                    isHoliday = isHoliday,
                    holidayReason = holidayReason,
                    isSunday = isSunday,
                    slots = slots
                )
            }
        } catch (e: Exception) {
            seedDefaultHistoricalData()
            saveData()
        }
    }

    private fun seedDefaultHistoricalData() {
        dayDataCache.clear()

        // 1. Generate Sept 1 to Sept 9 (All regular attended)
        var d = LocalDate.of(2026, 9, 1)
        val sept9 = LocalDate.of(2026, 9, 9)
        while (!d.isAfter(sept9)) {
            val isSunday = d.dayOfWeek == DayOfWeek.SUNDAY
            if (isSunday) {
                dayDataCache[d] = DayAttendance(d, isHoliday = true, holidayReason = "Sunday Off", isSunday = true, slots = emptyList())
            } else {
                val prefix = d.format(DATE_FORMATTER)
                val slots = BunkIQConstants.getDefaultSlotsForDay(d.dayOfWeek, prefix)
                dayDataCache[d] = DayAttendance(d, slots = slots)
            }
            d = d.plusDays(1)
        }

        // 2. Sept 10 (Thu): Absent on ES (1 missed)
        val d10 = LocalDate.of(2026, 9, 10)
        val d10Slots = BunkIQConstants.getDefaultSlotsForDay(d10.dayOfWeek, "2026-09-10").map { slot ->
            if (slot.subject == "ES") slot.copy(status = SlotStatus.ABSENT) else slot
        }
        dayDataCache[d10] = DayAttendance(d10, slots = d10Slots)

        // 3. Sept 11 (Fri): Absent full day (3 missed: FCC-LAB, DSA, OE)
        val d11 = LocalDate.of(2026, 9, 11)
        val d11Slots = BunkIQConstants.getDefaultSlotsForDay(d11.dayOfWeek, "2026-09-11").map { slot ->
            if (slot.weight > 0f) slot.copy(status = SlotStatus.ABSENT) else slot
        }
        dayDataCache[d11] = DayAttendance(d11, slots = d11Slots)

        // 4. Sept 12 (Sat): Absent 1st half (2 missed: DSA, DMGT-T1; 2nd half was cancelled/free)
        val d12 = LocalDate.of(2026, 9, 12)
        val d12Slots = BunkIQConstants.getDefaultSlotsForDay(d12.dayOfWeek, "2026-09-12").map { slot ->
            when (slot.slotNumber) {
                "P1", "P2" -> slot.copy(status = SlotStatus.ABSENT) // DSA, DMGT-T1
                "P4" -> slot.copy(status = SlotStatus.CANCELLED)    // ES (cancelled)
                "P5-6" -> slot.copy(status = SlotStatus.CANCELLED)  // Open Elective (cancelled)
                else -> slot // CEP/FP
            }
        }
        dayDataCache[d12] = DayAttendance(d12, slots = d12Slots)

        // 5. Explicit Holidays:
        BunkIQConstants.HOLIDAYS.forEach { (date, reason) ->
            dayDataCache[date] = DayAttendance(date, isHoliday = true, holidayReason = reason, slots = emptyList())
        }
    }

    fun saveData() {
        val root = JSONObject()
        root.put("version", 1)
        root.put("student", BunkIQConstants.STUDENT_NAME)
        root.put("usn", BunkIQConstants.STUDENT_USN)

        val daysArray = JSONArray()
        dayDataCache.values.sortedBy { it.date }.forEach { day ->
            val dayObj = JSONObject()
            dayObj.put("date", day.date.format(DATE_FORMATTER))
            dayObj.put("isHoliday", day.isHoliday)
            dayObj.put("holidayReason", day.holidayReason ?: "")
            dayObj.put("isSunday", day.isSunday)

            val slotsArray = JSONArray()
            day.slots.forEach { slot ->
                val sObj = JSONObject()
                sObj.put("id", slot.id)
                sObj.put("slotNumber", slot.slotNumber)
                sObj.put("timeRange", slot.timeRange)
                sObj.put("subject", slot.subject)
                sObj.put("originalSubject", slot.originalSubject)
                sObj.put("weight", slot.weight)
                sObj.put("originalWeight", slot.originalWeight)
                sObj.put("status", slot.status.name)
                sObj.put("isProxy", slot.isProxy)
                sObj.put("proxyNote", slot.proxyNote)
                sObj.put("isExtra", slot.isExtra)
                slotsArray.put(sObj)
            }
            dayObj.put("slots", slotsArray)
            daysArray.put(dayObj)
        }
        root.put("days", daysArray)

        prefs.edit().putString(KEY_DATA_JSON, root.toString()).apply()
    }

    fun exportJson(): String {
        val root = JSONObject()
        root.put("exportedAt", System.currentTimeMillis())
        root.put("appName", "BunkIQ")
        root.put("student", BunkIQConstants.STUDENT_NAME)
        root.put("usn", BunkIQConstants.STUDENT_USN)
        root.put("baselineAttended", BunkIQConstants.BASELINE_ATTENDED)
        root.put("baselineConducted", BunkIQConstants.BASELINE_CONDUCTED)

        val daysArray = JSONArray()
        dayDataCache.values.sortedBy { it.date }.forEach { day ->
            val dayObj = JSONObject()
            dayObj.put("date", day.date.format(DATE_FORMATTER))
            dayObj.put("isHoliday", day.isHoliday)
            dayObj.put("holidayReason", day.holidayReason ?: "")
            dayObj.put("isSunday", day.isSunday)

            val slotsArray = JSONArray()
            day.slots.forEach { slot ->
                val sObj = JSONObject()
                sObj.put("id", slot.id)
                sObj.put("slotNumber", slot.slotNumber)
                sObj.put("timeRange", slot.timeRange)
                sObj.put("subject", slot.subject)
                sObj.put("weight", slot.weight)
                sObj.put("status", slot.status.name)
                sObj.put("isProxy", slot.isProxy)
                sObj.put("proxyNote", slot.proxyNote)
                sObj.put("isExtra", slot.isExtra)
                slotsArray.put(sObj)
            }
            dayObj.put("slots", slotsArray)
            daysArray.put(dayObj)
        }
        root.put("days", daysArray)

        return root.toString(2)
    }

    fun importJson(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)
            val daysArray = root.getJSONArray("days")
            val newMap = mutableMapOf<LocalDate, DayAttendance>()

            for (i in 0 until daysArray.length()) {
                val dayObj = daysArray.getJSONObject(i)
                val date = LocalDate.parse(dayObj.getString("date"), DATE_FORMATTER)
                val isHoliday = dayObj.optBoolean("isHoliday", false)
                val holidayReason = dayObj.optString("holidayReason", "").ifBlank { null }
                val isSunday = dayObj.optBoolean("isSunday", false)

                val slotsArray = dayObj.optJSONArray("slots") ?: JSONArray()
                val slots = mutableListOf<LectureSlot>()
                for (j in 0 until slotsArray.length()) {
                    val sObj = slotsArray.getJSONObject(j)
                    slots.add(
                        LectureSlot(
                            id = sObj.getString("id"),
                            slotNumber = sObj.getString("slotNumber"),
                            timeRange = sObj.getString("timeRange"),
                            subject = sObj.getString("subject"),
                            originalSubject = sObj.optString("originalSubject", sObj.getString("subject")),
                            weight = sObj.getDouble("weight").toFloat(),
                            originalWeight = sObj.optDouble("originalWeight", sObj.getDouble("weight")).toFloat(),
                            status = SlotStatus.valueOf(sObj.getString("status")),
                            isProxy = sObj.optBoolean("isProxy", false),
                            proxyNote = sObj.optString("proxyNote", ""),
                            isExtra = sObj.optBoolean("isExtra", false)
                        )
                    )
                }

                newMap[date] = DayAttendance(
                    date = date,
                    isHoliday = isHoliday,
                    holidayReason = holidayReason,
                    isSunday = isSunday,
                    slots = slots
                )
            }

            dayDataCache.clear()
            dayDataCache.putAll(newMap)
            saveData()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun resetToDefaults() {
        seedDefaultHistoricalData()
        saveData()
    }

    // -------------------------------------------------------------
    // Campus Geofence & Location-based Auto Attendance / Auto Bunk
    // -------------------------------------------------------------
    fun getCampusConfig(): CampusGeofenceConfig = campusConfig

    fun updateCampusConfig(newConfig: CampusGeofenceConfig) {
        campusConfig = newConfig
        saveCampusConfig()
        val user = currentAuthUser
        if (user != null) {
            syncConfigToFirestore(user.uid)
        }
    }

    fun getGeofenceStatus(): GeofenceStatus = currentGeofenceStatus

    fun getGeofenceLogs(): List<GeofenceLogEvent> = geofenceLogs.toList()

    fun processGeofenceTransition(isInside: Boolean): GeofenceLogEvent {
        val config = getCampusConfig()
        val lat = if (isInside) config.latitude else config.latitude + 0.015
        val lng = if (isInside) config.longitude else config.longitude + 0.015
        return processLocationUpdate(lat, lng)
    }

    fun processLocationUpdate(
        lat: Double,
        lng: Double,
        targetDate: LocalDate = LocalDate.now(),
        targetTime: LocalTime = LocalTime.now()
    ): GeofenceLogEvent {
        val distance = CampusLocationManager.calculateDistanceMeters(
            lat,
            lng,
            campusConfig.latitude,
            campusConfig.longitude
        )
        val isInside = distance <= campusConfig.radiusMeters
        currentGeofenceStatus = if (isInside) GeofenceStatus.INSIDE_CAMPUS else GeofenceStatus.OUTSIDE_CAMPUS

        val timeStr = String.format(
            java.util.Locale.US,
            "%02d:%02d",
            targetTime.hour,
            targetTime.minute
        )

        var actionTaken = "CHECKED"
        var targetSubject = "Campus Status"
        var slotNum = "N/A"
        var message = if (isInside) {
            "Inside Campus boundary (${distance.toInt()}m from center). Radar active."
        } else {
            "Outside Campus boundary (${distance.toInt()}m from center). Auto-Bunk radar active."
        }

        if (campusConfig.autoAttendanceEnabled) {
            val day = getDayAttendance(targetDate)
            if (!day.isHoliday && day.slots.isNotEmpty()) {
                // Find currently active slot
                var matchingSlot = day.slots.firstOrNull { slot ->
                    LectureTimeParser.isTimeInSlot(slot.timeRange, targetTime)
                }

                // If not in middle of a slot, look for upcoming or nearest slot
                if (matchingSlot == null) {
                    matchingSlot = day.slots.firstOrNull { it.status == SlotStatus.PRESENT || it.status == SlotStatus.ABSENT }
                        ?: day.slots.firstOrNull()
                }

                if (matchingSlot != null) {
                    targetSubject = matchingSlot.subject
                    slotNum = matchingSlot.slotNumber

                    if (isInside) {
                        if (matchingSlot.status != SlotStatus.PRESENT) {
                            updateSlotStatus(targetDate, matchingSlot.id, SlotStatus.PRESENT)
                            actionTaken = "AUTO_PRESENT"
                            message = "Inside Campus (${distance.toInt()}m). Auto-Marked PRESENT for $targetSubject ($slotNum)!"
                        } else {
                            actionTaken = "CONFIRMED_PRESENT"
                            message = "At Campus (${distance.toInt()}m). $targetSubject ($slotNum) is marked PRESENT."
                        }
                    } else {
                        // Outside campus during lecture -> AUTO BUNK
                        if (matchingSlot.status != SlotStatus.ABSENT) {
                            updateSlotStatus(targetDate, matchingSlot.id, SlotStatus.ABSENT)
                            actionTaken = "AUTO_BUNK"
                            message = "Left Campus (${distance.toInt()}m away). Auto-Bunk triggered for $targetSubject ($slotNum)!"
                        } else {
                            actionTaken = "CONFIRMED_BUNK"
                            message = "Outside Campus (${distance.toInt()}m away). $targetSubject ($slotNum) is marked ABSENT (Bunked)."
                        }
                    }
                }
            } else if (day.isHoliday) {
                message = "Holiday (${day.holidayReason ?: "No Lectures"}). No attendance action needed."
            }
        }

        val event = GeofenceLogEvent(
            id = "geo_${System.currentTimeMillis()}",
            timestamp = System.currentTimeMillis(),
            timeFormatted = timeStr,
            isInside = isInside,
            actionTaken = actionTaken,
            subject = targetSubject,
            slotNumber = slotNum,
            distanceMeters = distance,
            message = message
        )

        geofenceLogs.add(0, event)
        if (geofenceLogs.size > 25) {
            geofenceLogs.removeAt(geofenceLogs.lastIndex)
        }
        saveGeofenceLogs()

        val user = currentAuthUser
        if (user != null) {
            syncDayToFirestore(user.uid, targetDate)
        }

        return event
    }

    private fun loadCampusConfig(): CampusGeofenceConfig {
        val raw = prefs.getString(KEY_GEOFENCE_CONFIG, null) ?: return CampusGeofenceConfig()
        return try {
            val obj = JSONObject(raw)
            CampusGeofenceConfig(
                name = obj.optString("name", "Campus Main Gate"),
                latitude = obj.optDouble("latitude", 18.5204),
                longitude = obj.optDouble("longitude", 73.8567),
                radiusMeters = obj.optDouble("radiusMeters", 200.0).toFloat(),
                autoAttendanceEnabled = obj.optBoolean("autoAttendanceEnabled", true)
            )
        } catch (e: Exception) {
            CampusGeofenceConfig()
        }
    }

    private fun saveCampusConfig() {
        val obj = JSONObject()
        obj.put("name", campusConfig.name)
        obj.put("latitude", campusConfig.latitude)
        obj.put("longitude", campusConfig.longitude)
        obj.put("radiusMeters", campusConfig.radiusMeters.toDouble())
        obj.put("autoAttendanceEnabled", campusConfig.autoAttendanceEnabled)
        prefs.edit().putString(KEY_GEOFENCE_CONFIG, obj.toString()).apply()
    }

    private fun loadGeofenceLogs(): List<GeofenceLogEvent> {
        val raw = prefs.getString(KEY_GEOFENCE_LOGS, null) ?: return emptyList()
        val list = mutableListOf<GeofenceLogEvent>()
        return try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    GeofenceLogEvent(
                        id = o.optString("id", "$i"),
                        timestamp = o.optLong("timestamp", 0L),
                        timeFormatted = o.optString("timeFormatted", "--:--"),
                        isInside = o.optBoolean("isInside", false),
                        actionTaken = o.optString("actionTaken", "CHECKED"),
                        subject = o.optString("subject", ""),
                        slotNumber = o.optString("slotNumber", ""),
                        distanceMeters = o.optDouble("distanceMeters", 0.0).toFloat(),
                        message = o.optString("message", "")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveGeofenceLogs() {
        val arr = JSONArray()
        geofenceLogs.take(25).forEach { event ->
            val o = JSONObject()
            o.put("id", event.id)
            o.put("timestamp", event.timestamp)
            o.put("timeFormatted", event.timeFormatted)
            o.put("isInside", event.isInside)
            o.put("actionTaken", event.actionTaken)
            o.put("subject", event.subject)
            o.put("slotNumber", event.slotNumber)
            o.put("distanceMeters", event.distanceMeters.toDouble())
            o.put("message", event.message)
            arr.put(o)
        }
        prefs.edit().putString(KEY_GEOFENCE_LOGS, arr.toString()).apply()
    }

    // -------------------------------------------------------------
    // Cloud Firestore Sync (Scoped to Authenticated User)
    // -------------------------------------------------------------
    fun syncUserProfile(userId: String, email: String, displayName: String) {
        try {
            val userDoc = mapOf(
                "userId" to userId,
                "email" to email,
                "displayName" to displayName,
                "campusLat" to campusConfig.latitude,
                "campusLng" to campusConfig.longitude,
                "geofenceRadiusMeters" to campusConfig.radiusMeters,
                "autoAttendanceEnabled" to campusConfig.autoAttendanceEnabled,
                "lastCampusStatus" to currentGeofenceStatus.name,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )
            firestore?.collection("users")?.document(userId)?.set(userDoc)
                ?.addOnSuccessListener {
                    Log.d(TAG, "User profile synced with Firestore")
                }
                ?.addOnFailureListener { e ->
                    Log.w(TAG, "Failed to sync user profile", e)
                }
        } catch (e: Exception) {
            Log.w(TAG, "Firestore sync error", e)
        }
    }

    private fun syncConfigToFirestore(userId: String) {
        try {
            val updates = mapOf(
                "campusLat" to campusConfig.latitude,
                "campusLng" to campusConfig.longitude,
                "geofenceRadiusMeters" to campusConfig.radiusMeters,
                "autoAttendanceEnabled" to campusConfig.autoAttendanceEnabled,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )
            firestore?.collection("users")?.document(userId)?.update(updates)
        } catch (e: Exception) {
            Log.w(TAG, "Config sync skipped", e)
        }
    }

    fun syncDayToFirestore(userId: String, date: LocalDate) {
        val day = getDayAttendance(date)
        try {
            val slotsData = day.slots.map { slot ->
                mapOf(
                    "id" to slot.id,
                    "slotNumber" to slot.slotNumber,
                    "timeRange" to slot.timeRange,
                    "subject" to slot.subject,
                    "weight" to slot.weight,
                    "status" to slot.status.name,
                    "isProxy" to slot.isProxy,
                    "proxyNote" to slot.proxyNote,
                    "isExtra" to slot.isExtra
                )
            }

            val docData = mapOf(
                "date" to date.format(DATE_FORMATTER),
                "userId" to userId,
                "isHoliday" to day.isHoliday,
                "holidayReason" to (day.holidayReason ?: ""),
                "isSunday" to day.isSunday,
                "slotsCount" to day.slots.size,
                "slotsJson" to slotsData.toString(),
                "updatedAt" to com.google.firebase.Timestamp.now()
            )

            firestore?.collection("users")?.document(userId)
                ?.collection("days")?.document(date.format(DATE_FORMATTER))
                ?.set(docData)
        } catch (e: Exception) {
            Log.w(TAG, "Error syncing day to Firestore", e)
        }
    }
}
