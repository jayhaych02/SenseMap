package com.example.myapplication

import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.geometry.Offset
import kotlin.math.abs

class SensorFusion {
    private var positionX = 0f
    private var positionY = 0f
    private var heading = 0f
    private var stepLength = 0.75f
    private var lastAccelTime = 0L
    private var velocityX = 0f
    private var velocityY = 0f

    // Motion detection threshold
    private val movementThreshold = 0.1f
    private val velocityDecay = 0.95f

    // Kalman filter variables
    private var estimatedError = 1.0f
    private val measurementError = 0.1f
    private val processError = 0.1f

    private fun kalmanFilter(measurement: Float, estimate: Float): Float {
        val kalmanGain = estimatedError / (estimatedError + measurementError)
        val newEstimate = estimate + kalmanGain * (measurement - estimate)
        estimatedError = (1 - kalmanGain) * estimatedError + processError
        return newEstimate
    }

    fun updateHeading(azimuth: Float) {
        // Apply Kalman filtering to heading
        heading = kalmanFilter(azimuth, heading)
    }

    fun processStep() {
        val radians = Math.toRadians(heading.toDouble())
        val dx = (stepLength * cos(radians)).toFloat()
        val dy = (stepLength * sin(radians)).toFloat()

        // Apply Kalman filtering to position updates
        positionX = kalmanFilter(positionX + dx, positionX)
        positionY = kalmanFilter(positionY + dy, positionY)
    }

    fun processAccelerometer(x: Float, y: Float, z: Float): Offset {
        val currentTime = System.nanoTime()
        if (lastAccelTime == 0L) {
            lastAccelTime = currentTime
            return Offset(positionX, positionY)
        }

        val dt = ((currentTime - lastAccelTime) / 1e9f).coerceAtMost(0.1f)
        lastAccelTime = currentTime

        // Apply movement threshold to reduce drift
        if (abs(x) > movementThreshold || abs(y) > movementThreshold) {
            velocityX = velocityX * velocityDecay + x * dt
            velocityY = velocityY * velocityDecay + y * dt

            // Apply Kalman filtering to position updates
            positionX = kalmanFilter(positionX + velocityX * dt, positionX)
            positionY = kalmanFilter(positionY + velocityY * dt, positionY)
        } else {
            // Decay velocity when no significant movement is detected
            velocityX *= velocityDecay
            velocityY *= velocityDecay
        }

        return Offset(positionX, positionY)
    }

    fun getCurrentPosition(): Offset {
        return Offset(positionX, positionY)
    }

    fun reset() {
        positionX = 0f
        positionY = 0f
        velocityX = 0f
        velocityY = 0f
        lastAccelTime = 0L
    }
}