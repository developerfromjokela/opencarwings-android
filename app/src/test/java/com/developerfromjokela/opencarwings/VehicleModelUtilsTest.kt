package com.developerfromjokela.opencarwings

import com.developerfromjokela.opencarwings.ui.main.Vehicle3DModel
import com.developerfromjokela.opencarwings.ui.main.isZe1Vehicle
import com.developerfromjokela.opencarwings.ui.main.resolveVehicle3DModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleModelUtilsTest {

    @Test
    fun ze1VehiclesResolveToZe1() {
        assertTrue(isZe1Vehicle(vin = "SJNFAAZE1U0000001", tcuVer = "TCU033", vehicleCode1 = 0, colorValue = "l2_gunmetallic"))
        assertEquals(Vehicle3DModel.ZE1, resolveVehicle3DModel(vin = "SJNFAAZE1U0000002", tcuVer = "TCU033", vehicleCode1 = null, colorValue = null))
        assertEquals(Vehicle3DModel.ZE1, resolveVehicle3DModel(vin = "SJNFAAZE1U0000003", tcuVer = null, vehicleCode1 = null, colorValue = "l2_vividblue"))
    }

    @Test
    fun ze0AndAze0VehiclesResolveToZe0AndNotZe1() {
        // ZE0
        assertFalse(isZe1Vehicle(vin = "JN1FAAZE0U0000001", tcuVer = null, vehicleCode1 = 0, colorValue = "l_pearlwhite"))
        assertEquals(Vehicle3DModel.ZE0, resolveVehicle3DModel(vin = "JN1FAAZE0U0000001", tcuVer = null, vehicleCode1 = 0, colorValue = "l_pearlwhite"))
        assertEquals(Vehicle3DModel.ZE0, resolveVehicle3DModel(vin = "JN1FAAZE0U0000002", tcuVer = "TCU031", vehicleCode1 = 0, colorValue = "l_superblack"))
        assertEquals(Vehicle3DModel.ZE0, resolveVehicle3DModel(vin = "OTHERVIN000000004", tcuVer = "TCU033", vehicleCode1 = 0, colorValue = "l_pearlwhite"))
        assertEquals(Vehicle3DModel.ZE0, resolveVehicle3DModel(vin = "OTHERVIN000000005", tcuVer = "TCU033", vehicleCode1 = 0, colorValue = "l_coulisred"))
        // AZE0 / AZ0
        assertEquals(Vehicle3DModel.ZE0, resolveVehicle3DModel(vin = "SJNFAAZE0U0000003", tcuVer = null, vehicleCode1 = 92, colorValue = "l2_pearlwhite"))
        assertEquals(Vehicle3DModel.ZE0, resolveVehicle3DModel(vin = "SJNFAAZE0U0000004", tcuVer = null, vehicleCode1 = 146, colorValue = "l2_pearlwhite"))
        assertEquals(Vehicle3DModel.ZE0, resolveVehicle3DModel(vin = "SJNFAAZE0U0000005", tcuVer = "TCU032", vehicleCode1 = 92, colorValue = "l_pearlwhite"))
    }

    @Test
    fun env200VehiclesAlwaysResolveToLegacy2D() {
        assertEquals(Vehicle3DModel.NONE, resolveVehicle3DModel(vin = "VZK00000000000001", tcuVer = null, vehicleCode1 = 0, colorValue = "env200_white"))
        assertEquals(Vehicle3DModel.NONE, resolveVehicle3DModel(vin = "VZK00000000000002", tcuVer = "TCU033", vehicleCode1 = 0, colorValue = "env200_white"))
        assertEquals(Vehicle3DModel.NONE, resolveVehicle3DModel(vin = "OTHERVIN000000003", tcuVer = "TCU033", vehicleCode1 = 0, colorValue = "env200_white"))
        assertFalse(isZe1Vehicle(vin = "VZK00000000000002", tcuVer = "TCU033", vehicleCode1 = 0, colorValue = "env200_white"))
    }
}
