package com.veloce.core

import com.veloce.core.math.*
import com.veloce.core.format.*
import kotlin.test.*

class GeoMathTest {
    @Test fun coincidentPointsHaveZeroDistance() = assertEquals(0.0, haversineMeters(0.0, 0.0, 0.0, 0.0))
    @Test fun oneDegreeOnEquator() = assertEquals(111_195.08, haversineMeters(0.0, 0.0, 0.0, 1.0), 0.1)
    @Test fun dateLineUsesShortArc() = assertEquals(22_239.016, haversineMeters(0.0, 179.9, 0.0, -179.9), 0.1)
    @Test fun antipodesAreFinite() = assertEquals(20_015_114.44, haversineMeters(0.0, 0.0, 0.0, 180.0), 0.1)
    @Test fun paceHandlesStationaryInvalidAndCeiling() {
        assertNull(paceMinutesPerKm(0.0, 60.0))
        assertNull(paceMinutesPerKm(1000.0, 0.0))
        assertNull(paceMinutesPerKm(Double.NaN, 1.0))
        assertNull(paceMinutesPerKm(1000.0, 2401.0))
        assertEquals(40.0, paceMinutesPerKm(1000.0, 2400.0))
        assertEquals(5.0, paceMinutesPerKm(1000.0, 300.0))
    }
    @Test fun durationDoesNotWrapAt24Hours() {
        assertEquals("00:00:00", formatDuration(-1))
        assertEquals("01:01:01", formatDuration(3661))
        assertEquals("100:00:00", formatDuration(360000))
        assertEquals("06:00", formatPace(5.999))
    }
}
