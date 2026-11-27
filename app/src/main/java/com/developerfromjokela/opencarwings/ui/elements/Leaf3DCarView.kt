package com.developerfromjokela.opencarwings.ui.elements

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.google.android.filament.LightManager
import com.google.android.filament.Material
import com.google.android.filament.MaterialInstance
import com.google.android.filament.View as FilamentView
import io.github.sceneview.SceneView
import io.github.sceneview.SurfaceType
import io.github.sceneview.gesture.CameraGestureDetector
import io.github.sceneview.material.materialOrNull
import io.github.sceneview.math.Color
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberCameraManipulator
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironment
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberFillLightNode
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberRenderInvalidator
import io.github.sceneview.rememberView
import kotlinx.coroutines.delay
import kotlin.math.abs

private data class Ze1PaintSpec(
    val body: FloatArray,
    val roofAndMirrors: FloatArray,
    val isTwoToneBlackRoof: Boolean,
    val metallic: Float,
    val roughness: Float,
    val clearcoat: Float,
    val clearcoatRoughness: Float
)

private val GLOSS_BLACK = floatArrayOf(0.018f, 0.020f, 0.025f, 1.0f)

private fun resolveZe1Paint(carColor: String?): Ze1PaintSpec {
    return when (carColor?.lowercase()?.trim()) {
        "l2_pearlwhite" -> Ze1PaintSpec(
            body = floatArrayOf(0.90f, 0.92f, 0.94f, 1.0f),
            roofAndMirrors = GLOSS_BLACK,
            isTwoToneBlackRoof = true,
            metallic = 0.28f,
            roughness = 0.72f,
            clearcoat = 0.64f,
            clearcoatRoughness = 0.10f
        )
        "l2_vividblue" -> Ze1PaintSpec(
            body = floatArrayOf(0.045f, 0.29f, 0.72f, 1.0f),
            roofAndMirrors = GLOSS_BLACK,
            isTwoToneBlackRoof = true,
            metallic = 0.86f,
            roughness = 0.94f,
            clearcoat = 0.58f,
            clearcoatRoughness = 0.11f
        )
        "l2_gunmetallic" -> {
            val gun = floatArrayOf(0.21f, 0.235f, 0.275f, 1.0f)
            Ze1PaintSpec(
                body = gun,
                roofAndMirrors = gun,
                isTwoToneBlackRoof = false,
                metallic = 0.88f,
                roughness = 0.96f,
                clearcoat = 0.56f,
                clearcoatRoughness = 0.11f
            )
        }
        "l2_jadefrostmetallic" -> {
            val jade = floatArrayOf(0.45f, 0.52f, 0.42f, 1.0f)
            Ze1PaintSpec(
                body = jade,
                roofAndMirrors = jade,
                isTwoToneBlackRoof = false,
                metallic = 0.84f,
                roughness = 0.95f,
                clearcoat = 0.56f,
                clearcoatRoughness = 0.11f
            )
        }
        "l2_superblack" -> {
            val black = floatArrayOf(0.025f, 0.028f, 0.034f, 1.0f)
            Ze1PaintSpec(
                body = black,
                roofAndMirrors = GLOSS_BLACK,
                isTwoToneBlackRoof = true,
                metallic = 0.48f,
                roughness = 0.82f,
                clearcoat = 0.70f,
                clearcoatRoughness = 0.08f
            )
        }
        // 1st-gen LEAF ZE0 / AZE0 monotone OEM paints
        "l_coulisred" -> {
            val red = floatArrayOf(0.360f, 0.024f, 0.032f, 1.0f)
            Ze1PaintSpec(
                body = red,
                roofAndMirrors = red,
                isTwoToneBlackRoof = false,
                metallic = 0.72f,
                roughness = 0.88f,
                clearcoat = 0.62f,
                clearcoatRoughness = 0.09f
            )
        }
        "l_deepblue" -> {
            val blue = floatArrayOf(0.035f, 0.145f, 0.440f, 1.0f)
            Ze1PaintSpec(
                body = blue,
                roofAndMirrors = blue,
                isTwoToneBlackRoof = false,
                metallic = 0.80f,
                roughness = 0.90f,
                clearcoat = 0.60f,
                clearcoatRoughness = 0.10f
            )
        }
        "l_forgedbronze" -> {
            val bronze = floatArrayOf(0.215f, 0.140f, 0.105f, 1.0f)
            Ze1PaintSpec(
                body = bronze,
                roofAndMirrors = bronze,
                isTwoToneBlackRoof = false,
                metallic = 0.78f,
                roughness = 0.90f,
                clearcoat = 0.60f,
                clearcoatRoughness = 0.10f
            )
        }
        "l_gunmetallic" -> {
            val gun = floatArrayOf(0.210f, 0.235f, 0.275f, 1.0f)
            Ze1PaintSpec(
                body = gun,
                roofAndMirrors = gun,
                isTwoToneBlackRoof = false,
                metallic = 0.88f,
                roughness = 0.96f,
                clearcoat = 0.58f,
                clearcoatRoughness = 0.10f
            )
        }
        "l_pearlwhite" -> {
            val pearl = floatArrayOf(0.880f, 0.900f, 0.900f, 1.0f)
            Ze1PaintSpec(
                body = pearl,
                roofAndMirrors = pearl,
                isTwoToneBlackRoof = false,
                metallic = 0.26f,
                roughness = 0.72f,
                clearcoat = 0.64f,
                clearcoatRoughness = 0.10f
            )
        }
        "l_planetblue" -> {
            val planet = floatArrayOf(0.080f, 0.340f, 0.560f, 1.0f)
            Ze1PaintSpec(
                body = planet,
                roofAndMirrors = planet,
                isTwoToneBlackRoof = false,
                metallic = 0.68f,
                roughness = 0.86f,
                clearcoat = 0.60f,
                clearcoatRoughness = 0.10f
            )
        }
        "l_superblack" -> {
            val black = floatArrayOf(0.024f, 0.026f, 0.032f, 1.0f)
            Ze1PaintSpec(
                body = black,
                roofAndMirrors = black,
                isTwoToneBlackRoof = false,
                metallic = 0.50f,
                roughness = 0.82f,
                clearcoat = 0.70f,
                clearcoatRoughness = 0.08f
            )
        }
        else -> Ze1PaintSpec(
            body = floatArrayOf(0.90f, 0.92f, 0.94f, 1.0f),
            roofAndMirrors = GLOSS_BLACK,
            isTwoToneBlackRoof = true,
            metallic = 0.28f,
            roughness = 0.72f,
            clearcoat = 0.64f,
            clearcoatRoughness = 0.10f
        )
    }
}

private fun MaterialInstance.setBaseColorRgba(rgba: FloatArray) {
    try {
        setParameter("baseColorFactor", rgba[0], rgba[1], rgba[2], rgba[3])
    } catch (_: Exception) {
    }
}

private fun MaterialInstance.setFloatParam(name: String, value: Float) {
    try {
        if (materialOrNull?.hasParameter(name) == true) {
            setParameter(name, value)
        }
    } catch (_: Exception) {
    }
}

private fun MaterialInstance.setEmissiveHdr(r: Float, g: Float, b: Float, strength: Float = 1.0f) {
    try {
        val mat = materialOrNull
        val param = mat?.parameters?.firstOrNull { it.name == "emissiveFactor" }
        when (param?.type) {
            Material.ParameterType.FLOAT3 -> setParameter("emissiveFactor", r, g, b)
            Material.ParameterType.FLOAT4 -> setParameter("emissiveFactor", r, g, b, 1.0f)
            else -> setParameter("emissiveFactor", r, g, b)
        }
        if (mat?.hasParameter("emissiveStrength") == true) {
            setParameter("emissiveStrength", strength)
        }
    } catch (_: Exception) {
    }
}

private data class Ze1RigHandles(
    val doorNode: Node?,
    val plugAcNode: Node?,
    val plugDcNode: Node?,
    val plugAcRestPos: Position,
    val plugDcRestPos: Position,
    val wheelNodes: List<Node>,
    val carPaintMats: List<MaterialInstance>,
    val roofAndMirrorMats: List<MaterialInstance>,
    val headlightLedMats: List<MaterialInstance>,
    val headlightDrlMats: List<MaterialInstance>,
    val headlightScatterMats: List<MaterialInstance>,
    val foglightLedMats: List<MaterialInstance>,
    val headlightGlassMats: List<MaterialInstance>,
    val taillightLedMats: List<MaterialInstance>,
    val taillightGlowMats: List<MaterialInstance>,
    val taillightRedLensMats: List<MaterialInstance>,
    val taillightGlassMats: List<MaterialInstance>,
    val acCableMats: List<MaterialInstance>,
    val bayLedMats: List<MaterialInstance>,
    val dashLed1Mats: List<MaterialInstance>,
    val dashLed2Mats: List<MaterialInstance>,
    val dashLed3Mats: List<MaterialInstance>
)

private data class DashLedStates(
    val led1On: Boolean = false,
    val led1Blink: Boolean = false,
    val led2On: Boolean = false,
    val led2Blink: Boolean = false,
    val led3On: Boolean = false,
    val led3Blink: Boolean = false
) {
    val anyBlinking: Boolean get() = led1Blink || led2Blink || led3Blink
}

/** Resolves Nissan LEAF ZE1 3-LED dashboard status indicator pattern per official manual. */
private fun resolveDashLedStates(
    isCharging: Boolean,
    isQuickCharging: Boolean,
    batterySocPercent: Float,
    isClimateOn: Boolean,
    isChargeTimerActive: Boolean
): DashLedStates {
    return when {
        isCharging || isQuickCharging -> {
            val soc = batterySocPercent.coerceIn(0f, 100f)
            when {
                soc < 33.34f -> DashLedStates(led1Blink = true)
                soc < 66.67f -> DashLedStates(led1On = true, led2Blink = true)
                else -> DashLedStates(led1On = true, led2On = true, led3Blink = true)
            }
        }
        isClimateOn || isChargeTimerActive -> DashLedStates(led3Blink = true)
        else -> DashLedStates()
    }
}

private fun MaterialInstance.setDashLedLit(lit: Boolean) {
    if (lit) {
        setBaseColorRgba(floatArrayOf(0.35f, 0.90f, 1.0f, 1.0f))
        setEmissiveHdr(0.06f, 0.72f, 1.0f, 80.0f)
    } else {
        setBaseColorRgba(floatArrayOf(0.30f, 0.33f, 0.38f, 1.0f))
        setEmissiveHdr(0f, 0f, 0f, 1.0f)
    }
}

/**
 * Populates [Node]'s cached TRS fields (`_position`, `_quaternion`, `_scale`) from Filament's
 * TransformManager so subsequent `node.rotation = ...` writes preserve glTF pivot translations.
 */
private fun Node?.syncFromTransformManager(): Node? = this?.apply {
    transform = transform
}

private fun extractRigHandles(modelNode: ModelNode): Ze1RigHandles {
    val nodeByName = modelNode.nodes.associateBy { it.name }
    val allMats = modelNode.renderableNodes.flatMap { rn ->
        (0 until rn.primitiveCount).map { idx -> rn.getMaterialInstanceAt(idx) }
    }

    fun matsNamed(vararg names: String): List<MaterialInstance> {
        val target = names.toSet()
        return allMats.filter { it.name in target }
    }

    val door = nodeByName["ChargePortDoor"].syncFromTransformManager()
    val plugAc = nodeByName["ChargePlugAC"].syncFromTransformManager()
    val plugDc = nodeByName["ChargePlugDC"].syncFromTransformManager()
    val wheels = listOfNotNull(
        nodeByName["Wheel_FL"].syncFromTransformManager(),
        nodeByName["Wheel_FR"].syncFromTransformManager(),
        nodeByName["Wheel_RL"].syncFromTransformManager(),
        nodeByName["Wheel_RR"].syncFromTransformManager()
    )

    return Ze1RigHandles(
        doorNode = door,
        plugAcNode = plugAc,
        plugDcNode = plugDc,
        plugAcRestPos = plugAc?.position ?: Position(),
        plugDcRestPos = plugDc?.position ?: Position(),
        wheelNodes = wheels,
        carPaintMats = matsNamed("CarPaint"),
        roofAndMirrorMats = matsNamed("CarPaint_Color_2", "CarPaint_Mirror_Map_C"),
        headlightLedMats = matsNamed("Headlight_LED"),
        headlightDrlMats = matsNamed("Headlight_DRL"),
        headlightScatterMats = matsNamed("Headlight_Scatter"),
        foglightLedMats = matsNamed("Foglight_LED"),
        headlightGlassMats = matsNamed("Headlight_Glass"),
        taillightLedMats = matsNamed("Taillight_LED"),
        taillightGlowMats = matsNamed("Taillight_Glow"),
        taillightRedLensMats = matsNamed("Taillight_Red_Lens"),
        taillightGlassMats = matsNamed("Taillight_Glass"),
        acCableMats = matsNamed("ChargeCable_AC", "ChargeCable_DC"),
        bayLedMats = matsNamed("ChargeBay_LED"),
        dashLed1Mats = matsNamed("DashLED_1"),
        dashLed2Mats = matsNamed("DashLED_2"),
        dashLed3Mats = matsNamed("DashLED_3")
    )
}

private fun applyZe1PaintMaterials(rig: Ze1RigHandles, carColor: String?) {
    val paint = resolveZe1Paint(carColor)
    rig.carPaintMats.forEach {
        it.setBaseColorRgba(paint.body)
        it.setFloatParam("metallicFactor", paint.metallic)
        it.setFloatParam("roughnessFactor", paint.roughness)
        it.setFloatParam("clearCoatFactor", paint.clearcoat)
        it.setFloatParam("clearCoatRoughnessFactor", paint.clearcoatRoughness)
    }
    rig.roofAndMirrorMats.forEach {
        it.setBaseColorRgba(paint.roofAndMirrors)
        if (paint.isTwoToneBlackRoof) {
            it.setFloatParam("metallicFactor", 0.25f)
            it.setFloatParam("roughnessFactor", 0.42f)
            it.setFloatParam("clearCoatFactor", 0.88f)
            it.setFloatParam("clearCoatRoughnessFactor", 0.045f)
        } else {
            it.setFloatParam("metallicFactor", paint.metallic)
            it.setFloatParam("roughnessFactor", paint.roughness)
            it.setFloatParam("clearCoatFactor", paint.clearcoat)
            it.setFloatParam("clearCoatRoughnessFactor", paint.clearcoatRoughness)
        }
    }
}

@Composable
fun Leaf3DCarView(
    modifier: Modifier = Modifier,
    carColor: String? = null,
    modelAssetPath: String = "models/ze1.glb",
    isPluggedIn: Boolean = false,
    isCharging: Boolean = false,
    isQuickCharging: Boolean = false,
    isChargingInterrupted: Boolean = false,
    isMoving: Boolean = false,
    isReverse: Boolean = false,
    isHeadlightsOn: Boolean = false,
    batterySocPercent: Float = 0f,
    isClimateOn: Boolean = false,
    isChargeTimerActive: Boolean = false
) {
    val engine = rememberEngine()
    val view = rememberView(engine)
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val renderInvalidator = rememberRenderInvalidator()
    val modelInstance = rememberModelInstance(modelLoader, modelAssetPath)
    var rig by remember(modelAssetPath) { mutableStateOf<Ze1RigHandles?>(null) }
    val isZe0 = modelAssetPath.contains("ze0", ignoreCase = true)

    val isChargeDoorOpen = isPluggedIn || isCharging || isQuickCharging || isChargingInterrupted
    val chargingSequence by animateFloatAsState(
        targetValue = if (rig != null && isChargeDoorOpen) 1f else 0f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "ze1ChargeDoorSequence"
    )

    val baseEnvironment = rememberEnvironment(environmentLoader, isOpaque = false)
    val environment = remember(baseEnvironment) {
        baseEnvironment.apply { indirectLight?.intensity = 14_500f }
    }
    val mainLightNode = rememberMainLightNode(engine) {
        intensity = 28_000f
        lightDirection = Direction(-0.32f, -0.78f, -0.54f)
    }
    val fillLightNode = rememberFillLightNode(engine) {
        intensity = 11_000f
        lightDirection = Direction(-0.22f, -0.45f, -0.86f)
    }

    var cameraResetKey by remember { mutableIntStateOf(0) }
    val cameraManipulator = rememberCameraManipulator(
        orbitHomePosition = Position(x = 0.049f, y = 0.257f, z = 1.012f),
        targetPosition = Position(x = 0.155f, y = -0.134f, z = 0f),
        key = cameraResetKey
    ).apply {
        (this as? CameraGestureDetector.DefaultCameraManipulator)?.isDoubleTapZoomEnabled = false
    }

    // Wheel spin state (degrees around local wheel X-axis)
    var wheelAngleDeg by remember { mutableFloatStateOf(0f) }
    var wheelSpeedDegPerSec by remember { mutableFloatStateOf(0f) }
    var lastWheelTickNanos by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isMoving) {
        lastWheelTickNanos = 0L
    }

    // Drive materials reactively whenever vehicle state or color changes
    LaunchedEffect(
        rig,
        carColor,
        isHeadlightsOn,
        isMoving,
        isCharging,
        isQuickCharging,
        isChargingInterrupted,
        isPluggedIn,
        isChargeDoorOpen,
        isClimateOn,
        isChargeTimerActive
    ) {
        val currentRig = rig ?: return@LaunchedEffect
        applyZe1PaintMaterials(currentRig, carColor)

        val lightsActive = isHeadlightsOn || isMoving
        val dashLedActive = isCharging || isQuickCharging || isClimateOn || isChargeTimerActive || isChargeDoorOpen
        environment.indirectLight?.intensity = if (lightsActive) 11_000f else 14_500f
        mainLightNode.intensity = if (lightsActive) 20_000f else 28_000f
        fillLightNode.intensity = if (lightsActive) 8_500f else 11_000f
        view.bloomOptions = view.bloomOptions.apply {
            enabled = true
            blendMode = FilamentView.BloomOptions.BlendMode.ADD
            quality = FilamentView.QualityLevel.HIGH
            levels = if (lightsActive) 5 else 4
            resolution = 480
            threshold = true
            highlight = 140.0f
            strength = when {
                lightsActive -> 0.30f
                dashLedActive -> 0.34f
                else -> 0.08f
            }
        }
        currentRig.headlightLedMats.forEach {
            if (lightsActive) {
                it.setBaseColorRgba(floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f))
                it.setEmissiveHdr(0.91f, 0.96f, 1.0f, 24.0f)
            } else {
                it.setBaseColorRgba(floatArrayOf(0.18f, 0.22f, 0.28f, 1.0f))
                it.setEmissiveHdr(0f, 0f, 0f, 1.0f)
            }
        }
        currentRig.headlightDrlMats.forEach {
            if (lightsActive) {
                if (isZe0) {
                    it.setBaseColorRgba(floatArrayOf(0.55f, 0.86f, 1.0f, 1.0f))
                    it.setEmissiveHdr(0.45f, 0.82f, 1.0f, 6.0f)
                } else {
                    it.setBaseColorRgba(floatArrayOf(0.84f, 0.90f, 0.96f, 1.0f))
                    it.setEmissiveHdr(0.84f, 0.92f, 1.0f, 4.5f)
                }
            } else {
                val base = if (isZe0) floatArrayOf(0.22f, 0.56f, 0.94f, 1.0f) else floatArrayOf(0.62f, 0.66f, 0.72f, 1.0f)
                it.setBaseColorRgba(base)
                it.setEmissiveHdr(0f, 0f, 0f, 1.0f)
            }
        }
        currentRig.headlightScatterMats.forEach {
            if (lightsActive) {
                it.setBaseColorRgba(floatArrayOf(0.84f, 0.88f, 0.92f, 1.0f))
                it.setEmissiveHdr(0f, 0f, 0f, 1.0f)
            } else {
                it.setBaseColorRgba(floatArrayOf(0.88f, 0.91f, 0.94f, 1.0f))
                it.setEmissiveHdr(0f, 0f, 0f, 1.0f)
            }
        }
        currentRig.foglightLedMats.forEach {
            if (lightsActive) {
                it.setBaseColorRgba(floatArrayOf(1.0f, 0.88f, 0.62f, 1.0f))
                it.setEmissiveHdr(1.0f, 0.78f, 0.40f, 17.0f)
            } else {
                it.setBaseColorRgba(floatArrayOf(0.90f, 0.92f, 0.95f, 1.0f))
                it.setEmissiveHdr(0f, 0f, 0f, 1.0f)
            }
        }
        currentRig.headlightGlassMats.forEach {
            it.setBaseColorRgba(floatArrayOf(0.85f, 0.90f, 0.96f, 0.04f))
            it.setEmissiveHdr(0f, 0f, 0f, 1.0f)
        }
        currentRig.taillightLedMats.forEach {
            if (lightsActive) {
                it.setBaseColorRgba(floatArrayOf(1.0f, 0.06f, 0.015f, 1.0f))
                it.setEmissiveHdr(1.0f, 0.055f, 0.008f, 18.0f)
            } else {
                it.setBaseColorRgba(floatArrayOf(0.58f, 0.024f, 0.028f, 1.0f))
                it.setEmissiveHdr(0f, 0f, 0f, 1.0f)
            }
        }
        currentRig.taillightGlowMats.forEach {
            if (lightsActive) {
                it.setBaseColorRgba(floatArrayOf(0.95f, 0.035f, 0.015f, 0.30f))
                it.setEmissiveHdr(1.0f, 0.032f, 0.005f, 4.8f)
            } else {
                it.setBaseColorRgba(floatArrayOf(0.62f, 0.022f, 0.028f, 0.36f))
                it.setEmissiveHdr(0f, 0f, 0f, 1.0f)
            }
        }
        currentRig.taillightRedLensMats.forEach {
            if (lightsActive) {
                it.setBaseColorRgba(floatArrayOf(0.76f, 0.014f, 0.014f, 0.24f))
            } else {
                it.setBaseColorRgba(floatArrayOf(0.65f, 0.014f, 0.018f, 0.44f))
            }
            it.setEmissiveHdr(0f, 0f, 0f, 1.0f)
        }
        currentRig.taillightGlassMats.forEach {
            it.setBaseColorRgba(floatArrayOf(0.92f, 0.95f, 0.98f, 0.05f))
            it.setEmissiveHdr(0f, 0f, 0f, 1.0f)
        }

        val cableColor = when {
            isChargingInterrupted -> floatArrayOf(0.86f, 0.14f, 0.12f, 1.0f)
            isCharging || isQuickCharging -> floatArrayOf(0.10f, 0.66f, 0.22f, 1.0f)
            else -> floatArrayOf(0.12f, 0.44f, 0.88f, 1.0f)
        }
        currentRig.acCableMats.forEach { it.setBaseColorRgba(cableColor) }

        currentRig.bayLedMats.forEach {
            when {
                !isChargeDoorOpen -> it.setEmissiveHdr(0f, 0f, 0f, 1.0f)
                isChargingInterrupted -> it.setEmissiveHdr(0.95f, 0.12f, 0.08f, 14.0f)
                isCharging || isQuickCharging -> it.setEmissiveHdr(0.10f, 1.0f, 0.28f, 14.0f)
                else -> it.setEmissiveHdr(0.12f, 0.46f, 1.0f, 14.0f)
            }
        }
        renderInvalidator.requestRender()
    }

    // Drive upper dashboard 3 blue status LEDs (charge SOC progress + remote HVAC / charge timer blink)
    val dashLedStates = resolveDashLedStates(
        isCharging = isCharging,
        isQuickCharging = isQuickCharging,
        batterySocPercent = batterySocPercent,
        isClimateOn = isClimateOn,
        isChargeTimerActive = isChargeTimerActive
    )
    var blinkOn by remember { mutableStateOf(true) }
    LaunchedEffect(rig, dashLedStates.anyBlinking) {
        if (!dashLedStates.anyBlinking) {
            blinkOn = false
        } else {
            while (true) {
                blinkOn = true
                delay(550L)
                blinkOn = false
                delay(450L)
            }
        }
    }
    LaunchedEffect(rig, dashLedStates, blinkOn) {
        val currentRig = rig ?: return@LaunchedEffect
        val led1Lit = dashLedStates.led1On || (dashLedStates.led1Blink && blinkOn)
        val led2Lit = dashLedStates.led2On || (dashLedStates.led2Blink && blinkOn)
        val led3Lit = dashLedStates.led3On || (dashLedStates.led3Blink && blinkOn)
        currentRig.dashLed1Mats.forEach { it.setDashLedLit(led1Lit) }
        currentRig.dashLed2Mats.forEach { it.setDashLedLit(led2Lit) }
        currentRig.dashLed3Mats.forEach { it.setDashLedLit(led3Lit) }
        renderInvalidator.requestRender()
    }

    // Drive charge port lid hinge & plug insertion smoothly (in ze1 meter coordinates)
    LaunchedEffect(rig, chargingSequence, isChargeDoorOpen, isQuickCharging) {
        val currentRig = rig ?: return@LaunchedEffect
        val doorFraction = (chargingSequence / 0.45f).coerceIn(0f, 1f)
        val doorOpenDeg = doorFraction * 78f
        currentRig.doorNode?.rotation = Rotation(x = -doorOpenDeg, y = 0f, z = 0f)

        val plugFraction = ((chargingSequence - 0.35f) / 0.65f).coerceIn(0f, 1f)
        val plugVisible = isChargeDoorOpen && plugFraction > 0.02f
        val pullOut = 1f - plugFraction
        val plugOffset = Position(x = 0f, y = 0.16f * pullOut, z = 0.17f * pullOut)

        currentRig.plugAcNode?.let { acNode ->
            acNode.isVisible = plugVisible && !isQuickCharging
            acNode.position = Position(
                x = currentRig.plugAcRestPos.x + plugOffset.x,
                y = currentRig.plugAcRestPos.y + plugOffset.y,
                z = currentRig.plugAcRestPos.z + plugOffset.z
            )
        }
        currentRig.plugDcNode?.let { dcNode ->
            dcNode.isVisible = plugVisible && isQuickCharging
            dcNode.position = Position(
                x = currentRig.plugDcRestPos.x + plugOffset.x,
                y = currentRig.plugDcRestPos.y + plugOffset.y,
                z = currentRig.plugDcRestPos.z + plugOffset.z
            )
        }
        renderInvalidator.requestRender()
    }

    val wheelAnimActive = rig != null && (isMoving || abs(wheelSpeedDegPerSec) > 0.5f)

    SceneView(
        modifier = modifier.fillMaxSize(),
        engine = engine,
        view = view,
        modelLoader = modelLoader,
        environmentLoader = environmentLoader,
        renderInvalidator = renderInvalidator,
        isOpaque = false,
        autoCenterContent = true,
        autoFitContent = false,
        disallowParentIntercept = true,
        environment = environment,
        mainLightNode = mainLightNode,
        fillLightNode = fillLightNode,
        cameraManipulator = cameraManipulator,
        onGestureListener = rememberOnGestureListener(
            onDoubleTap = { _, _ -> cameraResetKey++ }
        ),
        onAnimationFrame = if (wheelAnimActive) {
            { frameTimeNanos ->
                val dt = if (lastWheelTickNanos == 0L) {
                    0f
                } else {
                    ((frameTimeNanos - lastWheelTickNanos) / 1_000_000_000.0)
                        .toFloat()
                        .coerceIn(0f, 0.05f)
                }
                lastWheelTickNanos = frameTimeNanos

                val targetSpeed = when {
                    !isMoving -> 0f
                    isReverse -> -480f
                    else -> 540f
                }
                val maxDelta = 960f * dt
                val diff = targetSpeed - wheelSpeedDegPerSec
                wheelSpeedDegPerSec = when {
                    abs(diff) <= maxDelta -> targetSpeed
                    diff > 0f -> wheelSpeedDegPerSec + maxDelta
                    else -> wheelSpeedDegPerSec - maxDelta
                }

                if (abs(wheelSpeedDegPerSec) > 0.1f && dt > 0f) {
                    wheelAngleDeg = (wheelAngleDeg + wheelSpeedDegPerSec * dt) % 360f
                    val rot = Rotation(x = wheelAngleDeg, y = 0f, z = 0f)
                    rig?.wheelNodes?.forEach { it.rotation = rot }
                }
            }
        } else {
            null
        }
    ) {
        val lampActiveForRim = isHeadlightsOn || isMoving
        // Front & side studio rim lights so clearcoat body curves, charge bay, and wheels pop cleanly
        LightNode(
            type = LightManager.Type.DIRECTIONAL,
            intensity = if (lampActiveForRim) 7_500f else 10_000f,
            color = Color(0.96f, 0.98f, 1.0f, 1.0f),
            direction = Direction(0.18f, -0.22f, -0.95f)
        )
        LightNode(
            type = LightManager.Type.DIRECTIONAL,
            intensity = if (lampActiveForRim) 4_200f else 5_500f,
            color = Color(0.92f, 0.95f, 1.0f, 1.0f),
            direction = Direction(0.70f, -0.25f, -0.65f)
        )

        modelInstance?.let { instance ->
            val lampLightsActive = isHeadlightsOn || isMoving
            ModelNode(
                modelInstance = instance,
                autoAnimate = false,
                scaleToUnits = 1.45f,
                centerOrigin = Position(x = 0f, y = 0f, z = 0f),
                rotation = Rotation(x = 0f, y = 40f, z = 0f),
                apply = {
                    val handles = extractRigHandles(this)
                    handles.plugAcNode?.isVisible = false
                    handles.plugDcNode?.isVisible = false
                    rig = handles
                }
            ) {
                // Directional Spot lights aimed strictly outward along each lamp's optical axis
                // so illumination hits adjacent bumper/grille surfaces without leaking inside cabin/tires
                LightNode(
                    type = LightManager.Type.SPOT,
                    intensity = if (lampLightsActive) 16_000f else 0f,
                    position = if (isZe0) Position(x = -0.66f, y = 0.85f, z = 1.75f) else Position(x = -0.68f, y = 0.72f, z = 1.98f),
                    direction = Direction(x = 0.16f, y = -0.45f, z = 0.88f),
                    color = Color(0.88f, 0.95f, 1.0f, 1.0f),
                    apply = {
                        falloff(0.48f)
                        spotLightCone(0.45f, 1.15f)
                    }
                )
                LightNode(
                    type = LightManager.Type.SPOT,
                    intensity = if (lampLightsActive) 16_000f else 0f,
                    position = if (isZe0) Position(x = 0.66f, y = 0.85f, z = 1.75f) else Position(x = 0.68f, y = 0.72f, z = 1.98f),
                    direction = Direction(x = -0.16f, y = -0.45f, z = 0.88f),
                    color = Color(0.88f, 0.95f, 1.0f, 1.0f),
                    apply = {
                        falloff(0.48f)
                        spotLightCone(0.45f, 1.15f)
                    }
                )
                LightNode(
                    type = LightManager.Type.SPOT,
                    intensity = if (lampLightsActive) 11_000f else 0f,
                    position = if (isZe0) Position(x = -0.73f, y = 0.40f, z = 1.96f) else Position(x = -0.72f, y = 0.37f, z = 1.94f),
                    direction = Direction(x = -0.08f, y = -0.35f, z = 0.93f),
                    color = Color(1.0f, 0.74f, 0.34f, 1.0f),
                    apply = {
                        falloff(0.35f)
                        spotLightCone(0.45f, 1.15f)
                    }
                )
                LightNode(
                    type = LightManager.Type.SPOT,
                    intensity = if (lampLightsActive) 11_000f else 0f,
                    position = if (isZe0) Position(x = 0.73f, y = 0.40f, z = 1.96f) else Position(x = 0.72f, y = 0.37f, z = 1.94f),
                    direction = Direction(x = 0.08f, y = -0.35f, z = 0.93f),
                    color = Color(1.0f, 0.74f, 0.34f, 1.0f),
                    apply = {
                        falloff(0.35f)
                        spotLightCone(0.45f, 1.15f)
                    }
                )
                LightNode(
                    type = LightManager.Type.SPOT,
                    intensity = if (lampLightsActive) 12_000f else 0f,
                    position = if (isZe0) Position(x = -0.68f, y = 1.16f, z = -1.72f) else Position(x = -0.78f, y = 1.02f, z = -1.96f),
                    direction = Direction(x = -0.12f, y = -0.62f, z = -0.77f),
                    color = Color(1.0f, 0.045f, 0.008f, 1.0f),
                    apply = {
                        falloff(0.38f)
                        spotLightCone(0.40f, 1.05f)
                    }
                )
                LightNode(
                    type = LightManager.Type.SPOT,
                    intensity = if (lampLightsActive) 12_000f else 0f,
                    position = if (isZe0) Position(x = 0.68f, y = 1.16f, z = -1.72f) else Position(x = 0.78f, y = 1.02f, z = -1.96f),
                    direction = Direction(x = 0.12f, y = -0.62f, z = -0.77f),
                    color = Color(1.0f, 0.045f, 0.008f, 1.0f),
                    apply = {
                        falloff(0.38f)
                        spotLightCone(0.40f, 1.05f)
                    }
                )
            }
        }
    }
}

@Composable
fun Leaf3DTopDownCarView(
    modifier: Modifier = Modifier,
    carColor: String? = null,
    modelAssetPath: String = "models/ze1.glb"
) {
    val engine = rememberEngine()
    val view = rememberView(engine)
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)
    val renderInvalidator = rememberRenderInvalidator()
    val modelInstance = rememberModelInstance(modelLoader, modelAssetPath)
    var rig by remember(modelAssetPath) { mutableStateOf<Ze1RigHandles?>(null) }

    val baseEnvironment = rememberEnvironment(environmentLoader, isOpaque = false)
    val environment = remember(baseEnvironment) {
        baseEnvironment.apply { indirectLight?.intensity = 16_000f }
    }
    val mainLightNode = rememberMainLightNode(engine) {
        intensity = 30_000f
        lightDirection = Direction(0.28f, -0.35f, -0.89f)
    }
    val fillLightNode = rememberFillLightNode(engine) {
        intensity = 12_000f
        lightDirection = Direction(-0.45f, 0.25f, -0.86f)
    }
    val cameraNode = rememberCameraNode(engine) {
        focalLength = 65.0
        position = Position(x = 0f, y = 0f, z = 3.05f)
        lookAt(Position(x = 0f, y = 0f, z = 0f))
    }

    LaunchedEffect(rig, carColor) {
        val currentRig = rig ?: return@LaunchedEffect
        applyZe1PaintMaterials(currentRig, carColor)
        view.bloomOptions = view.bloomOptions.apply {
            enabled = false
        }
        renderInvalidator.requestRender()
    }

    SceneView(
        modifier = modifier.fillMaxSize(),
        surfaceType = SurfaceType.TextureSurface,
        engine = engine,
        view = view,
        modelLoader = modelLoader,
        environmentLoader = environmentLoader,
        renderInvalidator = renderInvalidator,
        isOpaque = false,
        autoCenterContent = true,
        autoFitContent = false,
        environment = environment,
        mainLightNode = mainLightNode,
        fillLightNode = fillLightNode,
        cameraNode = cameraNode,
        cameraManipulator = null
    ) {
        modelInstance?.let { instance ->
            ModelNode(
                modelInstance = instance,
                autoAnimate = false,
                scaleToUnits = 1.0f,
                centerOrigin = Position(x = 0f, y = 0f, z = 0f),
                rotation = Rotation(x = 90f, y = 0f, z = 180f),
                apply = {
                    val handles = extractRigHandles(this)
                    handles.plugAcNode?.isVisible = false
                    handles.plugDcNode?.isVisible = false
                    rig = handles
                }
            )
        }
    }
}
