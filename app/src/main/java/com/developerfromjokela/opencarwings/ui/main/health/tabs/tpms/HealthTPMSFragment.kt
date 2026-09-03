package com.developerfromjokela.opencarwings.ui.main.health.tabs.tpms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.developerfromjokela.opencarwings.OpenCARWINGS
import com.developerfromjokela.opencarwings.R
import com.developerfromjokela.opencarwings.utils.LocaleUnitUtils
import com.developerfromjokela.opencarwings.websocket.WSClient
import com.developerfromjokela.opencarwings.websocket.WSClientEvent
import org.maplibre.android.style.expressions.Expression.NumberFormatOption.locale
import org.openapitools.client.models.Car
import org.openapitools.client.models.VehicleHealthInfo
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Calendar


class HealthTPMSFragment : Fragment() {

    private var serverReceiver: BroadcastReceiver? = null

    private var healthInfo: VehicleHealthInfo? = null
    private var color: String = ""

    private lateinit var tpmsFR: TextView
    private lateinit var tpmsFL: TextView
    private lateinit var tpmsRR: TextView
    private lateinit var tpmsRL: TextView
    private lateinit var mileage: TextView
    private lateinit var maintenanceAlert: TextView
    private lateinit var lastUpdated: TextView

    private lateinit var tpmsLight: ImageView
    private lateinit var carOverlay: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            healthInfo =
                it.getString(ARG_HEALTHINFO)
                    ?.let { it1 -> WSClient.moshi.adapter(VehicleHealthInfo::class.java).fromJson(it1) }
            color = it.getString(ARG_COLOR, "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_health_tpms, container, false)

        tpmsFR = view.findViewById(R.id.tpmsFR)
        tpmsFL = view.findViewById(R.id.tpmsFL)
        tpmsRR = view.findViewById(R.id.tpmsRR)
        tpmsRL = view.findViewById(R.id.tpmsRL)
        mileage = view.findViewById(R.id.mileage)
        maintenanceAlert = view.findViewById(R.id.maintenanceAlert)
        lastUpdated = view.findViewById(R.id.tpmsLastUpdated)
        tpmsLight = view.findViewById(R.id.tpmsWarnIcon)
        carOverlay = view.findViewById(R.id.carOverlay)

        updateUIState()
        return view
    }

    private fun updateUIState() {
        val (valFR, unitFR) = LocaleUnitUtils.convertTirePressure(healthInfo?.tpmsFrFloat?.toDouble() ?: healthInfo?.tpmsFr?.toDouble() ?: 0.0)
        val (valFL, unitFL) = LocaleUnitUtils.convertTirePressure(healthInfo?.tpmsFlFloat?.toDouble() ?: healthInfo?.tpmsFl?.toDouble() ?: 0.0)
        val (valRR, unitRR) = LocaleUnitUtils.convertTirePressure(healthInfo?.tpmsRrFloat?.toDouble() ?: healthInfo?.tpmsRr?.toDouble() ?: 0.0)
        val (valRL, unitRL) = LocaleUnitUtils.convertTirePressure(healthInfo?.tpmsRlFloat?.toDouble() ?: healthInfo?.tpmsRl?.toDouble() ?: 0.0)
        tpmsFR.text = resources.getString(R.string.tpmsReading, if(valFR == 0.0) "--" else pressureFormatter.format(valFR), unitFR)
        tpmsFL.text = resources.getString(R.string.tpmsReading, if(valFL == 0.0) "--" else pressureFormatter.format(valFL), unitFL)
        tpmsRR.text = resources.getString(R.string.tpmsReading, if(valRR == 0.0) "--" else pressureFormatter.format(valRR), unitRR)
        tpmsRL.text = resources.getString(R.string.tpmsReading, if(valRL == 0.0) "--" else pressureFormatter.format(valRL), unitRL)

        val (milValue, milUnit) = LocaleUnitUtils.convertDistance(healthInfo?.mileage?.toDouble() ?: 0.0)
        mileage.text = resources.getString(R.string.maintenanceMileage, (healthInfo?.mileage?.let { "${milValue.toInt()} $milUnit" } ?: "--"))
        maintenanceAlert.text = resources.getString(R.string.maintenanceAlert, (if (healthInfo?.maintenanceAlert == true) getString(R.string.yes) else getString(R.string.no)))

        lastUpdated.text = healthInfo?.lastUpdated?.atZoneSameInstant(ZoneId.systemDefault())
            ?.toLocalDateTime()?.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT))
            ?: "---"

        tpmsLight.visibility = if (healthInfo?.tpmsLight == true) View.VISIBLE else View.INVISIBLE

        val carImageResId = when (color) {
            "l_coulisred" -> R.drawable.o_l_coulisred
            "l_deepblue" -> R.drawable.o_l_deepblue
            "l_forgedbronze" -> R.drawable.o_l_forgedbronze
            "l_gunmetallic" -> R.drawable.o_l_gunmetallic
            "l_pearlwhite" -> R.drawable.o_l_pearlwhite
            "l_planetblue" -> R.drawable.o_l_planetblue
            "l_superblack" -> R.drawable.o_l_superblack
            "l2_pearlwhite" -> R.drawable.o_l2_pearlwhite
            "l2_gunmetallic" -> R.drawable.o_l2_gunmetallic
            "l2_jadefrostmetallic" -> R.drawable.o_l2_jadefrostmetallic
            "l2_superblack" -> R.drawable.o_l2_superblack
            "l2_vividblue" -> R.drawable.o_l2_vividblue
            "env200_white" -> R.drawable.o_env200_white
            else -> null
        }

        carImageResId?.let { carOverlay.setImageResource(it) }
    }


    override fun onResume() {
        super.onResume()
        serverReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.hasExtra("type")) {
                    try {
                        when (intent.getStringExtra("type")) {
                            "carInfo" -> {
                                intent.getStringExtra("car")?.let { alertStr ->
                                    WSClient.moshi.adapter(Car::class.java)
                                        .fromJson(
                                            alertStr
                                        )?.let {
                                            WSClientEvent.UpdatedCarInfo(
                                                it
                                            )
                                        }
                                }?.let {
                                    healthInfo = it.car.vehHealth
                                    color = it.car.color?.value ?: ""
                                    updateUIState()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        context?.let {
            ContextCompat.registerReceiver(
                it,
                serverReceiver,
                IntentFilter(OpenCARWINGS.WS_BROADCAST),
                ContextCompat.RECEIVER_EXPORTED
            )
        }
    }

    override fun onPause() {
        super.onPause()
        serverReceiver?.let {
            context?.unregisterReceiver(it)
            serverReceiver = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serverReceiver?.let {
            context?.unregisterReceiver(it)
            serverReceiver = null
        }
    }

    companion object {
        const val ARG_HEALTHINFO = "healthinfo"
        const val ARG_COLOR = "color"

        private val pressureFormatter =
            DecimalFormat("0.##", DecimalFormatSymbols.getInstance())

        @JvmStatic
        fun newInstance(healthInfo: VehicleHealthInfo?, color: String) =
            HealthTPMSFragment().apply {
                arguments = Bundle().apply {
                    healthInfo?.let {
                        putString(ARG_HEALTHINFO, WSClient.moshi.adapter(VehicleHealthInfo::class.java).toJson(it))
                        putString(ARG_COLOR, color)
                    }
                }
            }
    }
}