package com.developerfromjokela.opencarwings

import com.developerfromjokela.opencarwings.ui.main.isZe1Vehicle
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleModelUtilsTest {

    @Test
    fun ze1VehiclesReturnTrue() {
        assertTrue(isZe1Vehicle(vin = "SJNFAAZE1U0000001", tcuVer = "TCU033", vehicleCode1 = 0, colorValue = "l2_gunmetallic"))
        assertTrue(isZe1Vehicle(vin = "SJNFAAZE1U0000002", tcuVer = "TCU033", vehicleCode1 = null, colorValue = null))
        assertTrue(isZe1Vehicle(vin = "SJNFAAZE1U0000003", tcuVer = null, vehicleCode1 = null, colorValue = "l2_vividblue"))
    }

    @Test
    fun ze0Aze0AndEnv200VehiclesReturnFalse() {
        // ZE0
        assertFalse(isZe1Vehicle(vin = "JN1FAAZE0U0000001", tcuVer = null, vehicleCode1 = 0, colorValue = "l_pearlwhite"))
        assertFalse(isZe1Vehicle(vin = "JN1FAAZE0U0000002", tcuVer = "TCU031", vehicleCode1 = 0, colorValue = "l_superblack"))
        assertFalse(isZe1Vehicle(vin = "OTHERVIN000000004", tcuVer = "TCU033", vehicleCode1 = 0, colorValue = "l_pearlwhite"))
        assertFalse(isZe1Vehicle(vin = "OTHERVIN000000005", tcuVer = "TCU033", vehicleCode1 = 0, colorValue = "l_coulisred"))
        // AZE0 / AZ0
        assertFalse(isZe1Vehicle(vin = "SJNFAAZE0U0000003", tcuVer = null, vehicleCode1 = 92, colorValue = "l2_pearlwhite"))
        assertFalse(isZe1Vehicle(vin = "SJNFAAZE0U0000004", tcuVer = null, vehicleCode1 = 146, colorValue = "l2_pearlwhite"))
        assertFalse(isZe1Vehicle(vin = "SJNFAAZE0U0000005", tcuVer = "TCU032", vehicleCode1 = 92, colorValue = "l_pearlwhite"))
        // e-NV200 (24 kWh and 40 kWh TCU033)
        assertFalse(isZe1Vehicle(vin = "VZK00000000000001", tcuVer = null, vehicleCode1 = 0, colorValue = "env200_white"))
        assertFalse(isZe1Vehicle(vin = "VZK00000000000002", tcuVer = "TCU033", vehicleCode1 = 0, colorValue = "env200_white"))
        assertFalse(isZe1Vehicle(vin = "OTHERVIN000000003", tcuVer = "TCU033", vehicleCode1 = 0, colorValue = "env200_white"))
    }
}
