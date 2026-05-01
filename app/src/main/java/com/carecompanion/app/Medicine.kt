package com.carecompanion.app

import android.net.Uri
import java.util.UUID

enum class MealTiming { Before, After }

data class MedicineSchedule(
    val label: String,
    val time: String,
    val enabled: Boolean = true,
    val withWater: Boolean = false,
    val mealTiming: MealTiming = MealTiming.Before
)

data class Medicine(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val dosage: String = "",
    val form: String = "Tablet",
    val isActive: Boolean = true,
    val instructions: List<String> = emptyList(),
    val schedules: List<MedicineSchedule> = emptyList(),
    val pillImageUri: Uri? = null,
    val packetFrontUri: Uri? = null,
    val packetBackUri: Uri? = null
)
