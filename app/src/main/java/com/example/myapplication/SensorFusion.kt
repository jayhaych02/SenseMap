package com.example.myapplication

import kotlin.math.sqrt
import android.os.Parcel
import android.os.Parcelable

enum class Stage {
    DATA_COLLECTION, PREPROCESSING, FEATURE_EXTRACTION, CLASSIFICATION
}

enum class FitnessLevel {
    BEGINNER, INTERMEDIATE, ADVANCED
}

data class SensingData(
    val steps: Int = 0,
    val distance: Float = 0f,
    val pace: Float = 0f,
    val calories: Float = 0f,
    val fitnessLevel: FitnessLevel = FitnessLevel.BEGINNER,
    val currentStage: Stage = Stage.DATA_COLLECTION
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readFloat(),
        parcel.readFloat(),
        parcel.readFloat(),
        FitnessLevel.valueOf(parcel.readString() ?: FitnessLevel.BEGINNER.name),
        Stage.valueOf(parcel.readString() ?: Stage.DATA_COLLECTION.name)
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(steps)
        parcel.writeFloat(distance)
        parcel.writeFloat(pace)
        parcel.writeFloat(calories)
        parcel.writeString(fitnessLevel.name)
        parcel.writeString(currentStage.name)
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<SensingData> {
        override fun createFromParcel(parcel: Parcel) = SensingData(parcel)
        override fun newArray(size: Int) = arrayOfNulls<SensingData?>(size)
    }
}

class SensorFusion {
    companion object {
        private const val SAMPLING_RATE = 0.25f
        private const val STEPS_PER_SECOND = 4f
        private const val STEPS_MULTIPLIER = STEPS_PER_SECOND / SAMPLING_RATE
        private const val STEP_LENGTH = 0.75f
        private const val PEAK_THRESHOLD = 12f
        private const val MIN_STEP_INTERVAL = 250L
        private const val FILTER_ALPHA = 0.8f
        private const val MAX_ACCELERATION = 50f
        private const val PACE_SCALE = 0.086f
        private const val CALORIES_PER_STEP = 0.04f
        private const val CALORIES_PER_METER = 0.05f
        private const val PACE_CALORIE_MULTIPLIER = 0.1f
    }

    private var stepCount = 0
    private var totalDistance = 0f
    private var startTime = System.currentTimeMillis()
    private var lastStepTime = 0L
    private var isInStep = false
    private var lastFiltered = FloatArray(3)
    private var scaleDistance = 6f

    @Synchronized
    fun process(acceleration: FloatArray): SensingData {
        require(acceleration.size == 3) { "Invalid acceleration data size: ${acceleration.size}" }
        val currentTime = System.currentTimeMillis()
        var stage = Stage.DATA_COLLECTION

        val bounded = acceleration.map {
            it.coerceIn(-MAX_ACCELERATION, MAX_ACCELERATION)
        }.toFloatArray()

        stage = Stage.PREPROCESSING
        val filtered = FloatArray(3) { i ->
            FILTER_ALPHA * lastFiltered[i] + (1 - FILTER_ALPHA) * bounded[i]
        }.also { lastFiltered = it }

        stage = Stage.FEATURE_EXTRACTION
        val magnitude = sqrt(filtered.sumOf { it * it.toDouble() }).toFloat()

        stage = Stage.CLASSIFICATION
        detectStep(magnitude, currentTime)

        val steps = stepCount * STEPS_MULTIPLIER.toInt()
        val distance = totalDistance * scaleDistance
        val pace = calculatePace(currentTime) * PACE_SCALE
        val calories = calculateCalories(steps, distance, pace)
        val fitnessLevel = determineFitnessLevel(calories)

        return SensingData(
            steps = steps,
            distance = distance,
            pace = pace,
            calories = calories,
            fitnessLevel = fitnessLevel,
            currentStage = stage
        )
    }

    private fun calculateCalories(steps: Int, distance: Float, pace: Float): Float {
        return (steps * CALORIES_PER_STEP) +
                (distance * CALORIES_PER_METER) +
                (pace * PACE_CALORIE_MULTIPLIER)
    }

    private fun determineFitnessLevel(calories: Float): FitnessLevel {
        return when {
            calories > 50 -> FitnessLevel.ADVANCED
            calories > 25 -> FitnessLevel.INTERMEDIATE
            else -> FitnessLevel.BEGINNER
        }
    }

    @Synchronized
    private fun detectStep(magnitude: Float, currentTime: Long) {
        if (magnitude > PEAK_THRESHOLD && !isInStep &&
            (currentTime - lastStepTime) > MIN_STEP_INTERVAL) {
            stepCount++
            totalDistance += STEP_LENGTH
            lastStepTime = currentTime
            isInStep = true
        } else if (magnitude < PEAK_THRESHOLD) {
            isInStep = false
        }
    }

    @Synchronized
    fun reset() {
        stepCount = 0
        totalDistance = 0f
        startTime = System.currentTimeMillis()
        lastStepTime = 0L
        isInStep = false
        lastFiltered = FloatArray(3)
    }

    private fun calculatePace(currentTime: Long): Float {
        val elapsedMinutes = (currentTime - startTime) / 60000f
        return if (totalDistance > 0 && elapsedMinutes > 0)
            elapsedMinutes / (totalDistance / 1000)
        else 0f
    }
}