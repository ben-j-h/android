package io.homeassistant.companion.android.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager as AndroidSensorManager
import io.homeassistant.companion.android.common.data.servers.ServerManager
import io.homeassistant.companion.android.common.sensors.SensorRepository
import io.homeassistant.companion.android.database.sensor.Sensor as SensorEntity
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class OnBodySensorManagerTest {

    private val offBodySensor = mockk<Sensor>()
    private val androidSensorManager = mockk<AndroidSensorManager>(relaxed = true) {
        every { getDefaultSensor(Sensor.TYPE_LOW_LATENCY_OFFBODY_DETECT, true) } returns offBodySensor
    }
    private val context = mockk<Context>(relaxed = true) {
        every { getSystemService(AndroidSensorManager::class.java) } returns androidSensorManager
        every { getSystemService(Context.SENSOR_SERVICE) } returns androidSensorManager
    }
    private val sensorRepository = mockk<SensorRepository>(relaxed = true)
    private val manager = OnBodySensorManager(context, sensorRepository, mockk<ServerManager>(relaxed = true))

    private fun setEnabled(enabled: Boolean) {
        coEvery { sensorRepository.get(OnBodySensorManager.onBodySensor.id) } returns listOf(
            SensorEntity(id = OnBodySensorManager.onBodySensor.id, serverId = 1, enabled = enabled, state = ""),
        )
    }

    @Test
    fun `Given on body sensor enabled when updating twice then listener is registered once and kept until disabled`() = runTest {
        setEnabled(true)

        manager.requestSensorUpdate()
        manager.requestSensorUpdate()

        verify(exactly = 1) { androidSensorManager.registerListener(manager, offBodySensor, any<Int>()) }
        verify(exactly = 0) { androidSensorManager.unregisterListener(manager) }

        setEnabled(false)
        manager.requestSensorUpdate()
        manager.requestSensorUpdate()

        verify(exactly = 1) { androidSensorManager.unregisterListener(manager) }

        setEnabled(true)
        manager.requestSensorUpdate()

        verify(exactly = 2) { androidSensorManager.registerListener(manager, offBodySensor, any<Int>()) }

        // Leave the shared listener state as we found it for other tests
        setEnabled(false)
        manager.requestSensorUpdate()
    }
}
