package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AttendanceRepository
import com.example.location.CampusLocationManager
import com.example.model.AttendanceMetrics
import com.example.model.BunkIQConstants
import com.example.model.CampusGeofenceConfig
import com.example.model.DayAttendance
import com.example.model.DayBunkCost
import com.example.model.GeofenceLogEvent
import com.example.model.GeofenceStatus
import com.example.model.SlotStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AttendanceRepository(application)

    // Default to Saturday, Sept 12, 2026 (the active historical baseline point)
    private val _selectedDate = MutableStateFlow(LocalDate.of(2026, 9, 12))
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _dayAttendance = MutableStateFlow(repository.getDayAttendance(_selectedDate.value))
    val dayAttendance: StateFlow<DayAttendance> = _dayAttendance.asStateFlow()

    private val _metrics = MutableStateFlow(repository.calculateMetrics(_selectedDate.value))
    val metrics: StateFlow<AttendanceMetrics> = _metrics.asStateFlow()

    private val _whatIfBunks = MutableStateFlow(0)
    val whatIfBunks: StateFlow<Int> = _whatIfBunks.asStateFlow()

    // Campus Geofence & Location States
    private val _campusConfig = MutableStateFlow(repository.getCampusConfig())
    val campusConfig: StateFlow<CampusGeofenceConfig> = _campusConfig.asStateFlow()

    private val _geofenceStatus = MutableStateFlow(repository.getGeofenceStatus())
    val geofenceStatus: StateFlow<GeofenceStatus> = _geofenceStatus.asStateFlow()

    private val _geofenceLogs = MutableStateFlow(repository.getGeofenceLogs())
    val geofenceLogs: StateFlow<List<GeofenceLogEvent>> = _geofenceLogs.asStateFlow()

    private val _isFetchingLocation = MutableStateFlow(false)
    val isFetchingLocation: StateFlow<Boolean> = _isFetchingLocation.asStateFlow()

    private val _lastEventNotification = MutableStateFlow<GeofenceLogEvent?>(null)
    val lastEventNotification: StateFlow<GeofenceLogEvent?> = _lastEventNotification.asStateFlow()

    val dayBunkCosts: List<DayBunkCost> = BunkIQConstants.getDayBunkCosts()

    init {
        CampusLocationManager.setupSilentHardwareGeofence(application, _campusConfig.value)
    }

    fun updateCampusConfig(config: CampusGeofenceConfig) {
        repository.updateCampusConfig(config)
        _campusConfig.value = config
        CampusLocationManager.setupSilentHardwareGeofence(getApplication(), config)
    }

    fun simulateGeofenceStep(isInside: Boolean) {
        val config = _campusConfig.value
        // If inside: place coordinates within 20 meters of campus center
        // If outside: place coordinates ~800 meters away from campus center
        val lat = if (isInside) config.latitude + 0.0001 else config.latitude + 0.008
        val lng = if (isInside) config.longitude + 0.0001 else config.longitude + 0.008

        val event = repository.processLocationUpdate(
            lat = lat,
            lng = lng,
            targetDate = _selectedDate.value,
            targetTime = LocalTime.now()
        )

        _geofenceStatus.value = repository.getGeofenceStatus()
        _geofenceLogs.value = repository.getGeofenceLogs()
        _lastEventNotification.value = event
        refresh()
    }

    fun refreshLocationFromGps(context: Context) {
        if (!CampusLocationManager.hasLocationPermission(context)) {
            _geofenceStatus.value = GeofenceStatus.PERMISSION_DENIED
            return
        }

        _isFetchingLocation.value = true
        CampusLocationManager.fetchCurrentLocation(context) { loc ->
            _isFetchingLocation.value = false
            if (loc != null) {
                val event = repository.processLocationUpdate(
                    lat = loc.latitude,
                    lng = loc.longitude,
                    targetDate = _selectedDate.value,
                    targetTime = LocalTime.now()
                )
                _geofenceStatus.value = repository.getGeofenceStatus()
                _geofenceLogs.value = repository.getGeofenceLogs()
                _lastEventNotification.value = event
                refresh()
            }
        }
    }

    fun dismissLastNotification() {
        _lastEventNotification.value = null
    }

    fun onUserAuthenticated(userId: String, email: String, displayName: String) {
        viewModelScope.launch {
            repository.syncUserProfile(userId, email, displayName)
            repository.syncDayToFirestore(userId, _selectedDate.value)
        }
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        refresh()
    }

    fun goToPreviousDay() {
        val newDate = _selectedDate.value.minusDays(1)
        if (!newDate.isBefore(BunkIQConstants.SEMESTER_START)) {
            selectDate(newDate)
        }
    }

    fun goToNextDay() {
        val newDate = _selectedDate.value.plusDays(1)
        if (!newDate.isAfter(BunkIQConstants.SEMESTER_END)) {
            selectDate(newDate)
        }
    }

    fun goToToday() {
        selectDate(LocalDate.of(2026, 9, 12))
    }

    fun setSlotStatus(slotId: String, status: SlotStatus) {
        repository.updateSlotStatus(_selectedDate.value, slotId, status)
        refresh()
    }

    fun replaceSlot(slotId: String, newSubject: String, newWeight: Float, note: String) {
        repository.replaceSlot(_selectedDate.value, slotId, newSubject, newWeight, note)
        refresh()
    }

    fun resetSlotToDefault(slotId: String) {
        repository.resetSlotToDefault(_selectedDate.value, slotId)
        refresh()
    }

    fun addExtraSlot(slotNumber: String, timeRange: String, subject: String, weight: Float) {
        repository.addExtraSlot(_selectedDate.value, slotNumber, timeRange, subject, weight)
        refresh()
    }

    fun removeSlot(slotId: String) {
        repository.removeSlot(_selectedDate.value, slotId)
        refresh()
    }

    fun setWhatIfBunks(count: Int) {
        _whatIfBunks.value = count.coerceAtLeast(0)
    }

    fun calculateWhatIfPercentage(extraBunks: Int): Float {
        val currentMetrics = _metrics.value
        val simulatedAttended = currentMetrics.projectedMaxAttended - extraBunks
        return if (currentMetrics.totalProjectedConducted > 0) {
            (simulatedAttended / currentMetrics.totalProjectedConducted) * 100f
        } else {
            0f
        }
    }

    fun exportData(): String {
        return repository.exportJson()
    }

    fun importData(json: String): Boolean {
        val success = repository.importJson(json)
        if (success) {
            refresh()
        }
        return success
    }

    fun resetAllData() {
        repository.resetToDefaults()
        refresh()
    }

    private fun refresh() {
        _dayAttendance.value = repository.getDayAttendance(_selectedDate.value)
        _metrics.value = repository.calculateMetrics(_selectedDate.value)
    }
}
