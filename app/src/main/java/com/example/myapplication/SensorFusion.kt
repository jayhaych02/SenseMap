package com.example.myapplication

import kotlin.math.sqrt
import android.os.Parcel
import android.os.Parcelable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

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
    val currentStage: Stage = Stage.DATA_COLLECTION,
    val acceleration: Float = 0f
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readFloat(),
        parcel.readFloat(),
        parcel.readFloat(),
        FitnessLevel.valueOf(parcel.readString() ?: FitnessLevel.BEGINNER.name),
        Stage.valueOf(parcel.readString() ?: Stage.DATA_COLLECTION.name),
        parcel.readFloat()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(steps)
        parcel.writeFloat(distance)
        parcel.writeFloat(pace)
        parcel.writeFloat(calories)
        parcel.writeString(fitnessLevel.name)
        parcel.writeString(currentStage.name)
        parcel.writeFloat(acceleration)
    }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<SensingData> {
        override fun createFromParcel(parcel: Parcel) = SensingData(parcel)
        override fun newArray(size: Int) = arrayOfNulls<SensingData?>(size)
    }
}

class SensorFusion(private val sensorManager: SensorManager) : SensorEventListener {
    companion object {
        private const val SAMPLING_RATE = 0.25f
        private const val STEP_LENGTH = 0.75f
        private const val FILTER_ALPHA = 0.8f
        private const val MAX_ACCELERATION = 50f
        private const val PACE_SCALE = 0.086f
        private const val CALORIES_PER_STEP = 0.04f
        private const val CALORIES_PER_METER = 0.05f
        private const val PACE_CALORIE_MULTIPLIER = 0.1f
    }

    private var stepCount = 0
    private var initialStepCount: Int? = null
    private var totalDistance = 0f
    private var startTime = System.currentTimeMillis()
    private var lastFiltered = FloatArray(3)
    private var currentLinearAccel = FloatArray(3)
    private var scaleDistance = 6f
    private var onDataUpdated: ((SensingData) -> Unit)? = null

    init {
        initializeSensors()
    }

    private fun initializeSensors() {
        val linearAccelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        val stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        sensorManager.registerListener(this, linearAccelSensor, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_GAME)
    }

    @Synchronized
    fun process(acceleration: FloatArray): SensingData {
        require(acceleration.size == 3) { "Invalid acceleration data size: ${acceleration.size}" }
        val currentTime = System.currentTimeMillis()
        var stage = Stage.DATA_COLLECTION

        // Process acceleration
        val bounded = acceleration.map {
            it.coerceIn(-MAX_ACCELERATION, MAX_ACCELERATION)
        }.toFloatArray()

        stage = Stage.PREPROCESSING
        val filtered = FloatArray(3) { i ->
            FILTER_ALPHA * lastFiltered[i] + (1 - FILTER_ALPHA) * bounded[i]
        }.also { lastFiltered = it }

        stage = Stage.FEATURE_EXTRACTION
        val magnitude = sqrt(filtered.sumOf { it * it.toDouble() }.toFloat())

        stage = Stage.CLASSIFICATION
        val distance = totalDistance * scaleDistance
        val pace = calculatePace(currentTime) * PACE_SCALE
        val calories = calculateCalories(stepCount, distance, pace)
        val fitnessLevel = determineFitnessLevel(calories)

        return SensingData(
            steps = stepCount,
            distance = distance,
            pace = pace,
            calories = calories,
            fitnessLevel = fitnessLevel,
            currentStage = stage,
            acceleration = magnitude
        )
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                currentLinearAccel = event.values.clone()
                onDataUpdated?.invoke(process(currentLinearAccel))
            }
            Sensor.TYPE_STEP_COUNTER -> {
                val steps = event.values[0].toInt()
                if (initialStepCount == null) {
                    initialStepCount = steps
                }
                stepCount = steps - (initialStepCount ?: steps)
                totalDistance = stepCount * STEP_LENGTH
            }
        }
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
    fun reset() {
        stepCount = 0
        initialStepCount = null
        totalDistance = 0f
        startTime = System.currentTimeMillis()
        lastFiltered = FloatArray(3)
        currentLinearAccel = FloatArray(3)
    }

    private fun calculatePace(currentTime: Long): Float {
        val elapsedMinutes = (currentTime - startTime) / 60000f
        return if (totalDistance > 0 && elapsedMinutes > 0)
            elapsedMinutes / (totalDistance / 1000)
        else 0f
    }

    fun setOnDataUpdatedListener(listener: (SensingData) -> Unit) {
        onDataUpdated = listener
    }

    fun cleanup() {
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}