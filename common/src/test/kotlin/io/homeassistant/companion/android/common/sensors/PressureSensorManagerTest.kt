package io.homeassistant.companion.android.common.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager as AndroidSensorManager
import io.homeassistant.companion.android.common.data.servers.ServerManager
import io.homeassistant.companion.android.database.sensor.Sensor as SensorEntity
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class PressureSensorManagerTest {

    private val pressureSensor = mockk<Sensor>()
    private val androidSensorManager = mockk<AndroidSensorManager>(relaxed = true) {
        every { getDefaultSensor(Sensor.TYPE_PRESSURE) } returns pressureSensor
    }
    private val context = mockk<Context>(relaxed = true) {
        every { getSystemService(AndroidSensorManager::class.java) } returns androidSensorManager
        every { getSystemService(Context.SENSOR_SERVICE) } returns androidSensorManager
    }
    private val sensorRepository = mockk<SensorRepository>(relaxed = true)
    private val manager = PressureSensorManager(context, sensorRepository, mockk<ServerManager>(relaxed = true))

    init {
        coEvery { sensorRepository.get(PressureSensorManager.pressureSensor.id) } returns listOf(
            SensorEntity(id = PressureSensorManager.pressureSensor.id, serverId = 1, enabled = true, state = ""),
        )
    }

    @AfterEach
    fun tearDown() = runTest {
        // The listener state is shared by all instances, release it for the next test
        manager.onSensorUpdateFinished()
    }

    @Test
    fun `Given a listener waiting for its first event when updating again within the timeout then it is not re-registered`() = runTest {
        manager.requestSensorUpdate()
        manager.requestSensorUpdate()

        verify(exactly = 1) { androidSensorManager.registerListener(manager, pressureSensor, any<Int>()) }
        verify(exactly = 0) { androidSensorManager.unregisterListener(manager) }
    }

    @Test
    fun `Given a listener without event when the update finishes then it is unregistered and registered again on the next update`() = runTest {
        manager.requestSensorUpdate()
        manager.onSensorUpdateFinished()

        verify(exactly = 1) { androidSensorManager.unregisterListener(manager) }

        manager.onSensorUpdateFinished()
        verify(exactly = 1) { androidSensorManager.unregisterListener(manager) }

        manager.requestSensorUpdate()
        verify(exactly = 2) { androidSensorManager.registerListener(manager, pressureSensor, any<Int>()) }
    }
}
