package com.example.myapplication

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import org.json.JSONArray
import org.json.JSONObject


class RoomLayoutViewModel : ViewModel() {
    private val _corners = MutableStateFlow<List<Offset>>(emptyList())
    private val _currentPosition = MutableStateFlow(Offset(0f, 0f))
    private val _heading = MutableStateFlow(0f)

    val corners: StateFlow<List<Offset>> = _corners
    val currentPosition: StateFlow<Offset> = _currentPosition
    val heading = _heading.asStateFlow()

    private var lastPosition: Offset? = null
    private val minDistance = 20f


    fun updateCurrentPosition(position: Offset) {
        lastPosition?.let { last ->
            val dx = position.x - last.x
            val dy = position.y - last.y
            val distance = kotlin.math.sqrt(dx * dx + dy * dy)

            if (distance > minDistance) {
                _corners.value += position
                lastPosition = position
                _currentPosition.value = position
            }
        } ?: run {
            lastPosition = position
            _corners.value = listOf(position)
            _currentPosition.value = position
        }
    }

    fun updateHeading(newHeading: Float) {
        _heading.value = newHeading + 90  // Convert to screen coordinates
    }

    fun markCorner() {
        _corners.value += _currentPosition.value
    }

    fun saveLayout() {
        viewModelScope.launch {
            val layout = RoomLayout(
                corners = _corners.value,
                timestamp = System.currentTimeMillis()
            )
            // Save layout implementation
        }
    }

    // File operations remain unchanged
    fun saveMapToFile(context: Context, roomLayout: RoomLayout) {
        val jsonObject = JSONObject().apply {
            put("timestamp", roomLayout.timestamp)
            put("corners", JSONArray().apply {
                roomLayout.corners.forEach { corner ->
                    put(JSONObject().apply {
                        put("x", corner.x)
                        put("y", corner.y)
                    })
                }
            })
            put("wifiReferences", JSONArray().apply {
                roomLayout.wifiReferences.forEach { wifiRef ->
                    put(JSONObject().apply {
                        put("ssid", wifiRef.ssid)
                        put("strength", wifiRef.strength)
                        put("estimatedPosition", JSONObject().apply {
                            put("x", wifiRef.estimatedPosition.x)
                            put("y", wifiRef.estimatedPosition.y)
                        })
                    })
                }
            })
        }

        val file = File(context.filesDir, "room_map.json")
        file.writeText(jsonObject.toString())
    }

    fun loadMapFromFile(context: Context): RoomLayout? {
        val file = File(context.filesDir, "room_map.json")
        if (!file.exists()) return null

        val jsonObject = JSONObject(file.readText())
        val timestamp = jsonObject.getLong("timestamp")
        val corners = jsonObject.getJSONArray("corners").let { cornersArray ->
            List(cornersArray.length()) { i ->
                cornersArray.getJSONObject(i).let {
                    Offset(
                        it.getDouble("x").toFloat(),
                        it.getDouble("y").toFloat()
                    )
                }
            }
        }
        val wifiReferences = jsonObject.getJSONArray("wifiReferences").let { wifiArray ->
            List(wifiArray.length()) { i ->
                wifiArray.getJSONObject(i).let { wifiRefJson ->
                    WifiReference(
                        ssid = wifiRefJson.getString("ssid"),
                        strength = wifiRefJson.getInt("strength"),
                        estimatedPosition = wifiRefJson.getJSONObject("estimatedPosition").let {
                            Offset(
                                it.getDouble("x").toFloat(),
                                it.getDouble("y").toFloat()
                            )
                        }
                    )
                }
            }
        }

        return RoomLayout(corners = corners, timestamp = timestamp, wifiReferences = wifiReferences)
    }
}