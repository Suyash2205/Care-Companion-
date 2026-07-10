package com.carecompanion.app.data.repo

import com.carecompanion.app.data.model.AdherenceLogDto
import com.carecompanion.app.data.model.AlertDto
import com.carecompanion.app.data.model.SosEventDto
import com.carecompanion.app.data.model.WheelchairPlaceDto
import com.carecompanion.app.data.remote.SupabaseService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdherenceRepository @Inject constructor(private val api: SupabaseService) {
    suspend fun forDate(elderId: String, date: String): List<AdherenceLogDto> =
        api.getAdherence("eq.$elderId", "eq.$date")

    suspend fun range(elderId: String, fromDate: String): List<AdherenceLogDto> =
        api.getAdherence("eq.$elderId", "gte.$fromDate")

    suspend fun record(log: AdherenceLogDto): AdherenceLogDto = api.upsertAdherence(log).first()

    data class Stats(val taken: Int, val skipped: Int, val missed: Int, val pending: Int) {
        val total get() = taken + skipped + missed + pending
        val adherencePct get() = if (taken + skipped + missed == 0) 0
        else (taken * 100) / (taken + skipped + missed)
    }

    fun stats(logs: List<AdherenceLogDto>) = Stats(
        taken = logs.count { it.status == "taken" },
        skipped = logs.count { it.status == "skipped" },
        missed = logs.count { it.status == "missed" },
        pending = logs.count { it.status == "pending" },
    )
}

@Singleton
class SosRepository @Inject constructor(private val api: SupabaseService) {
    suspend fun trigger(elderId: String, lat: Double?, lng: Double?, accuracy: Double?, address: String?): SosEventDto =
        api.insertSos(SosEventDto(elderId = elderId, lat = lat, lng = lng, accuracy = accuracy, address = address)).first()

    suspend fun history(elderId: String): List<SosEventDto> = api.getSos("eq.$elderId")

    suspend fun updateStatus(id: String, status: String): SosEventDto? =
        api.patchSos("eq.$id", mapOf("status" to status)).firstOrNull()
}

@Singleton
class AlertsRepository @Inject constructor(private val api: SupabaseService) {
    suspend fun list(): List<AlertDto> = api.getAlerts()
    suspend fun markRead(id: String) = api.markAlertRead("eq.$id", mapOf("read" to true))
    fun unreadCount(alerts: List<AlertDto>) = alerts.count { !it.read }
}

@Singleton
class WheelchairRepository @Inject constructor(private val api: SupabaseService) {
    suspend fun places(): List<WheelchairPlaceDto> = api.getWheelchairPlaces()
}
