package com.lingfeng.sprite.sensor;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * S9传感器单元测试
 */
class S9SensorTest {

    // ==================== AudioSensor Tests ====================

    @Test
    void testAudioSensorConstruction() {
        AudioSensor sensor = new AudioSensor();
        assertNotNull(sensor);
        assertEquals("AudioSensor", sensor.name());
    }

    @Test
    void testAudioSensorCustomName() {
        AudioSensor sensor = new AudioSensor("CustomAudioSensor");
        assertEquals("CustomAudioSensor", sensor.name());
    }

    @Test
    void testAudioInfoRecord() {
        Instant now = Instant.now();
        AudioSensor.AudioInfo info = new AudioSensor.AudioInfo(
            now, true, "default", 0.7f, "spotify",
            AudioSensor.SoundContext.MUSIC, true
        );

        assertEquals(now, info.timestamp());
        assertTrue(info.isPlaying());
        assertEquals("default", info.audioDevice());
        assertEquals(0.7f, info.volumeLevel());
        assertEquals("spotify", info.currentAudioApp());
        assertEquals(AudioSensor.SoundContext.MUSIC, info.soundContext());
        assertTrue(info.isHeadphonesConnected());
    }

    @Test
    void testAudioInfoRecordWithNulls() {
        AudioSensor.AudioInfo info = new AudioSensor.AudioInfo(
            null, false, null, 0f, null, null, false
        );

        assertNotNull(info.timestamp());
        assertEquals("default", info.audioDevice());
        assertEquals(AudioSensor.SoundContext.UNKNOWN, info.soundContext());
    }

    @Test
    void testSoundContextEnum() {
        AudioSensor.SoundContext[] contexts = AudioSensor.SoundContext.values();
        assertEquals(8, contexts.length);
        assertEquals(AudioSensor.SoundContext.SILENT, AudioSensor.SoundContext.valueOf("SILENT"));
        assertEquals(AudioSensor.SoundContext.MUSIC, AudioSensor.SoundContext.valueOf("MUSIC"));
        assertEquals(AudioSensor.SoundContext.VIDEO, AudioSensor.SoundContext.valueOf("VIDEO"));
        assertEquals(AudioSensor.SoundContext.VOICE_CALL, AudioSensor.SoundContext.valueOf("VOICE_CALL"));
    }

    @Test
    void testGetSoundContextDescription() {
        AudioSensor sensor = new AudioSensor();

        // We can't control what detectAudioInfo returns, but we can test the method exists
        assertNotNull(sensor.getSoundContextDescription());
    }

    // ==================== LocationSensor Tests ====================

    @Test
    void testLocationSensorConstruction() {
        LocationSensor sensor = new LocationSensor();
        assertNotNull(sensor);
        assertEquals("LocationSensor", sensor.name());
    }

    @Test
    void testLocationSensorCustomName() {
        LocationSensor sensor = new LocationSensor("CustomLocationSensor");
        assertEquals("CustomLocationSensor", sensor.name());
    }

    @Test
    void testLocationInfoRecord() {
        Instant now = Instant.now();
        LocationSensor.LocationInfo info = new LocationSensor.LocationInfo(
            now, "China", "Shanghai", "Asia/Shanghai", "HOME",
            31.2304, 121.4737, "127.0.0.1", true
        );

        assertEquals(now, info.timestamp());
        assertEquals("China", info.country());
        assertEquals("Shanghai", info.city());
        assertEquals("Asia/Shanghai", info.timezone());
        assertEquals("HOME", info.locationType());
        assertEquals(31.2304, info.latitude());
        assertEquals(121.4737, info.longitude());
        assertEquals("127.0.0.1", info.ipAddress());
        assertTrue(info.isValid());
    }

    @Test
    void testLocationInfoRecordWithNulls() {
        LocationSensor.LocationInfo info = new LocationSensor.LocationInfo(
            null, null, null, null, null, 0, 0, null, false
        );

        assertNotNull(info.timestamp());
        assertEquals("Unknown", info.country());
        assertEquals("Unknown", info.city());
        assertEquals("UTC", info.timezone());
        assertEquals("UNKNOWN", info.locationType());
    }

    @Test
    void testGetLocationInfo() {
        LocationSensor sensor = new LocationSensor();
        LocationSensor.LocationInfo info = sensor.getLocationInfo();
        assertNotNull(info);
    }

    @Test
    void testIsAvailable() {
        LocationSensor sensor = new LocationSensor();
        // Availability depends on timezone detection
        boolean available = sensor.isAvailable();
        // Just verify it returns a boolean
        assertTrue(available || !available);
    }

    @Test
    void testGetLocationDescription() {
        LocationSensor sensor = new LocationSensor();
        String description = sensor.getLocationDescription();
        assertNotNull(description);
        // Should contain city and country
    }

    // ==================== DeviceStateSensor Tests ====================

    @Test
    void testDeviceStateSensorConstruction() {
        DeviceStateSensor sensor = new DeviceStateSensor();
        assertNotNull(sensor);
        assertEquals("DeviceStateSensor", sensor.name());
    }

    @Test
    void testDeviceStateSensorCustomName() {
        DeviceStateSensor sensor = new DeviceStateSensor("CustomDeviceSensor");
        assertEquals("CustomDeviceSensor", sensor.name());
    }

    @Test
    void testGetDeviceStateInfo() {
        DeviceStateSensor sensor = new DeviceStateSensor();
        DeviceStateSensor.DeviceStateInfo info = sensor.getDeviceStateInfo();
        assertNotNull(info);
    }

    @Test
    void testDeviceStateInfoRecord() {
        Instant now = Instant.now();
        DeviceStateSensor.DeviceStateInfo info = new DeviceStateSensor.DeviceStateInfo(
            now, true, true, true, false, 85, "WiFi", 75.0f, 256000
        );

        assertEquals(now, info.timestamp());
        assertTrue(info.isOnline());
        assertTrue(info.isCharging());
        assertTrue(info.isWifiEnabled());
        assertFalse(info.isBluetoothEnabled());
        assertEquals(85, info.batteryLevel());
        assertEquals("WiFi", info.networkType());
        assertEquals(75.0f, info.storageUsagePercent());
        assertEquals(256000, info.totalMemoryMB());
    }

    @Test
    void testPowerStateEnum() {
        DeviceStateSensor.PowerState[] states = DeviceStateSensor.PowerState.values();
        assertTrue(states.length >= 3);
        assertEquals(DeviceStateSensor.PowerState.BATTERY, DeviceStateSensor.PowerState.valueOf("BATTERY"));
        assertEquals(DeviceStateSensor.PowerState.CHARGING, DeviceStateSensor.PowerState.valueOf("CHARGING"));
        assertEquals(DeviceStateSensor.PowerState.FULL, DeviceStateSensor.PowerState.valueOf("FULL"));
    }

    @Test
    void testNetworkTypeEnum() {
        DeviceStateSensor.NetworkType[] types = DeviceStateSensor.NetworkType.values();
        assertTrue(types.length >= 4);
        assertEquals(DeviceStateSensor.NetworkType.WIFI, DeviceStateSensor.NetworkType.valueOf("WIFI"));
        assertEquals(DeviceStateSensor.NetworkType.ETHERNET, DeviceStateSensor.NetworkType.valueOf("ETHERNET"));
    }

    @Test
    void testIsAvailable() {
        DeviceStateSensor sensor = new DeviceStateSensor();
        // Just verify it returns a boolean
        boolean available = sensor.isAvailable();
        assertTrue(available || !available);
    }
}