package com.developerfromjokela.opencarwings.ui.main

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavOptions
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.developerfromjokela.opencarwings.MainActivity
import com.developerfromjokela.opencarwings.OpenCARWINGS
import com.developerfromjokela.opencarwings.R
import com.developerfromjokela.opencarwings.databinding.FragmentMainBinding
import com.developerfromjokela.opencarwings.ui.main.evinfo.EVInfoFragment.Companion.ARG_EVINFO
import com.developerfromjokela.opencarwings.ui.main.location.LocationFragment.Companion.ARG_LOCATIONINFO
import com.developerfromjokela.opencarwings.ui.main.notifications.NotificationsFragment.Companion.ARG_NOTIFICATIONS
import com.developerfromjokela.opencarwings.ui.main.notifications.NotificationsListWrapper
import com.developerfromjokela.opencarwings.ui.main.tcusettings.TCUSettingsFragment.Companion.ARG_TCUCONFIG
import com.developerfromjokela.opencarwings.ui.main.tcusettings.TCUSettingsFragment.Companion.ARG_TCUCONFIG_REFRESHING
import com.developerfromjokela.opencarwings.utils.PreferencesHelper
import com.developerfromjokela.opencarwings.websocket.WSClient
import com.developerfromjokela.opencarwings.websocket.WSClientEvent
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.messaging.FirebaseMessaging
import org.openapitools.client.models.AlertHistory
import org.openapitools.client.models.Car
import org.openapitools.client.models.CarSerializerList
import org.openapitools.client.models.EVInfo
import org.openapitools.client.models.LocationInfo
import org.openapitools.client.models.TCUConfiguration

class MainFragment : Fragment() {

    private lateinit var viewModel: MainViewModel
    private var _binding: FragmentMainBinding? = null

    private var serverReceiver: BroadcastReceiver? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel =  ViewModelProvider.create(
            this,
            factory = MainViewModel.Factory
        )[MainViewModel::class]

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.main_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: android.view.MenuItem): Boolean {
                when(menuItem.itemId) {
                    R.id.app_settings -> {
                        val prefUtil = PreferencesHelper(requireContext())
                        MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.settings).setMessage(getString(R.string.settings_username, prefUtil.username)).setNegativeButton(R.string.close) {dlg, _ ->
                            dlg.dismiss()
                        }.setPositiveButton(R.string.sign_out) {dlg, _ ->
                            viewModel.signOut()
                            WSClient.getInstance().disconnect()
                            dlg.dismiss()
                            prefUtil.clearAll()
                            val navBuilder = NavOptions.Builder()
                            val navOptions: NavOptions = navBuilder.setPopUpTo(R.id.loginFragment, false).build()
                            Navigation.findNavController(view).navigate(R.id.loginFragment, null, navOptions)
                        }.show()
                    }
                }
                return true
            }
        }, viewLifecycleOwner)

        // Setup RecyclerView
        binding.menuItems.layoutManager = LinearLayoutManager(context)
        binding.menuItems.adapter = HomeTabsAdapter(context, emptyList()) // Implement MenuAdapter
        (binding.menuItems.adapter as HomeTabsAdapter).setClickListener(object :
            HomeTabsAdapter.ItemClickListener {
            override fun onItemClick(view: View?, position: Int) {
                val item = (binding.menuItems.adapter as HomeTabsAdapter).mData[position]
                if (item.id == 1) {
                    findNavController().navigate(R.id.action_mainFragment_to_EVInfoFragment, Bundle().apply {
                        putString(ARG_EVINFO, WSClient.moshi.adapter(EVInfo::class.java).toJson(viewModel.uiState.value?.car?.evInfo))
                    })
                }
                if (item.id == 2) {
                    findNavController().navigate(R.id.action_mainFragment_to_locationFragment, Bundle().apply {
                        putString(ARG_LOCATIONINFO, WSClient.moshi.adapter(LocationInfo::class.java).toJson(viewModel.uiState.value?.car?.location))
                    })
                }
                if (item.id == 3) {
                    findNavController().navigate(R.id.action_mainFragment_to_notificationsFragment, Bundle().apply {
                        putString(ARG_NOTIFICATIONS, WSClient.moshi.adapter(NotificationsListWrapper::class.java).toJson(
                            NotificationsListWrapper(viewModel.notificationsState.value ?: emptyList())))
                    })
                }
                if (item.id == 4) {
                    findNavController().navigate(R.id.action_mainFragment_to_TCUSettingsFragment, Bundle().apply {
                        putString(ARG_TCUCONFIG, WSClient.moshi.adapter(TCUConfiguration::class.java).toJson(viewModel.uiState.value?.car?.tcuConfiguration))
                        putBoolean(ARG_TCUCONFIG_REFRESHING, viewModel.uiState.value?.isCommandExecuting ?: false)
                    })
                }
            }
        })

        // Swipe to refresh
        binding.swipeRefreshHome.setOnRefreshListener {
            viewModel.onRefresh()
        }

        // Action buttons
        binding.chgActionButton.setOnClickListener {
            MaterialAlertDialogBuilder(it.context)
                .setNegativeButton(android.R.string.cancel) { dlg, _ ->
                    dlg.cancel()
                }
                .setPositiveButton(R.string.start_charging) { dlg, _ ->
                    dlg.cancel()
                    viewModel.onChargeAction()
                }
                .setTitle(R.string.start_charging)
                .setMessage(R.string.are_you_sure).show()
        }
        binding.acActionButton.setOnClickListener {
            MaterialAlertDialogBuilder(it.context)
                .setNegativeButton(android.R.string.cancel) { dlg, _ ->
                    dlg.cancel()
                }
                .setPositiveButton(if(viewModel.uiState.value?.isAcOn == true)  R.string.stop else R.string.start) { dlg, _ ->
                    dlg.cancel()
                    viewModel.onAcAction()
                }
                .setTitle(if(viewModel.uiState.value?.isAcOn == true)  R.string.ac_off_confirm_dialog_title else R.string.ac_on_confirm_dialog_title)
                .setMessage(R.string.are_you_sure).show()
        }

        // Observe UI state
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            if (state.forceLogout) {
                val navBuilder = NavOptions.Builder()
                val navOptions: NavOptions = navBuilder.setPopUpTo(R.id.loginFragment, false).build()
                Navigation.findNavController(view).navigate(R.id.loginFragment, null, navOptions)
                return@observe
            }
            val spinner2 = (requireActivity() as MainActivity).findViewById<Spinner>(R.id.spinner_toolbar)
            if (spinner2 != null) {
                val mArrayAdapter = NavAdapter(requireContext(), state.cars.map { CarPickerItem(it) })
                spinner2.onItemSelectedListener = object : OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        val selectedCarData = mArrayAdapter.getItem(position)?.car
                        val prefsManager = PreferencesHelper(requireContext())
                        if (selectedCarData != null && selectedCarData.vin != prefsManager.activeCarVin) {
                            prefsManager.activeCarVin = selectedCarData.vin
                            viewModel.onCarChange()
                        }
                    }

                    override fun onNothingSelected(p0: AdapterView<*>?) {

                    }
                }
                if (state.car != null)
                    spinner2.setSelection(state.cars.indexOfFirst { cD -> cD.vin == state.car.vin  })
                spinner2.adapter = mArrayAdapter
            }
            binding.swipeRefreshHome.isEnabled = !state.isCommandExecuting
            binding.mainContent.visibility = if (state.isFirstTimeLoading) View.INVISIBLE else View.VISIBLE
            binding.initialLoadingProgress.visibility = if (state.isFirstTimeLoading) View.VISIBLE else View.GONE
            binding.swipeRefreshHome.isRefreshing = state.isRefreshing
            binding.carUpdatingProgress.visibility = if (state.isLoading || state.isCommandExecuting) View.VISIBLE else View.GONE
            binding.battPercent.text = state.batteryPercent
            binding.carStatus.setText(state.carStatus)
            var layers = emptyList<Drawable>()

            state.carImageResId?.let {
                layers += ContextCompat.getDrawable(requireContext(), it)
            }

            if (state.isRunning) {
                layers += ContextCompat.getDrawable(requireContext(), R.drawable.l_hd)
            }

            if (state.isPluggedIn || state.isCharging || state.isQuickCharging) {
                layers += ContextCompat.getDrawable(requireContext(), R.drawable.l_cp)
            }

            if (state.isCharging) {
                layers += ContextCompat.getDrawable(requireContext(), R.drawable.l_chg)
            }

            if (state.isQuickCharging) {
                layers += ContextCompat.getDrawable(requireContext(), R.drawable.l_q_chg)
            }

            if (state.carGear != 0) {
                val anim =
                    ContextCompat.getDrawable(requireContext(), R.drawable.l_tireanim) as AnimationDrawable
                layers += anim
                anim.start()
            }

            val newDrawable = LayerDrawable(layers.toTypedArray())
            binding.carStatusImage.setImageDrawable(newDrawable)
            binding.rangeAcOn.text = state.rangeAcOn
            binding.rangeAcOff.text = state.rangeAcOff
            binding.leafSegment.setActiveSegments(state.activeSegments)
            binding.leafSegment.setCharging(state.isCharging, state.isQuickCharging)
            binding.chgActionButton.isEnabled = !state.isCommandExecuting
            binding.acActionButton.isEnabled = !state.isCommandExecuting
            binding.chgActionButton.backgroundTintList = ColorStateList.valueOf(getColorFromAttr(if (!state.isCharging && !state.isQuickCharging)
                com.google.android.material.R.attr.colorButtonNormal else com.google.android.material.R.attr.colorPrimary))
            binding.chgActionButton.imageTintList = ColorStateList.valueOf(getColorFromAttr(if (!state.isCharging && !state.isQuickCharging)
                com.google.android.material.R.attr.colorOnBackground else com.google.android.material.R.attr.colorPrimaryInverse))
            binding.acActionButton.backgroundTintList = ColorStateList.valueOf(getColorFromAttr(if (!state.isAcOn)
                com.google.android.material.R.attr.colorButtonNormal else com.google.android.material.R.attr.colorPrimary))
            binding.acActionButton.imageTintList = ColorStateList.valueOf(getColorFromAttr(if (!state.isAcOn)
                com.google.android.material.R.attr.colorOnBackground else com.google.android.material.R.attr.colorPrimaryInverse))
            binding.plugActionButton.backgroundTintList = ColorStateList.valueOf(getColorFromAttr(if (!state.isPlugActionEnabled)
                com.google.android.material.R.attr.colorButtonNormal else com.google.android.material.R.attr.colorPrimary))
            binding.plugActionButton.imageTintList = ColorStateList.valueOf(getColorFromAttr(if (!state.isPlugActionEnabled)
                com.google.android.material.R.attr.colorOnBackground else com.google.android.material.R.attr.colorPrimaryInverse))
            binding.chgActionProgress.visibility = if (state.isChgActionInProgress) View.VISIBLE else View.INVISIBLE
            binding.acActionProgress.visibility = if (state.isAcActionInProgress) View.VISIBLE else View.INVISIBLE
            binding.carModel.text = state.carModel
            binding.carInformation.text = state.vin
            binding.tcuId.text = "TCU ID: ${state.tcuId}"
            binding.naviId.text = "Navi ID: ${state.naviId}"
            binding.tcuSoft.text = getString(R.string.tcu_software, state.tcuSoftware)
            binding.battSoh.text = "SOH: ${state.soh}"
            binding.capBars.text = getString(R.string.capacity_bars, state.capacityBars)
            binding.battPackCap.text = getString(R.string.battery_cap, state.batteryCapacity)
            binding.lastUpdatedDate.text = getString(R.string.last_updated_format, state.lastUpdated)
            (binding.menuItems.adapter as HomeTabsAdapter).updateItems(state.menuItems)
            state.genericError?.let {
                context?.let {ctx ->
                    MaterialAlertDialogBuilder(ctx)
                        .setTitle(it)
                        .setMessage(state.error)
                        .setCancelable(!state.fatalError)
                        .setPositiveButton(R.string.close) {dlg, _ ->
                            dlg.dismiss()
                            if (state.fatalError) {
                                requireActivity().finish()
                            }
                        }
                        .show()
                }
            }
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("MF", "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }
            // Get new FCM registration token
            val token = task.result
            viewModel.updateTokenMeta(token)
        })
    }

    private fun getColorFromAttr(attrId: Int): Int {
        return try {
            context?.resources?.getColor(attrId, requireContext().theme) ?: 0
        } catch (ignored: java.lang.Exception) {
            val typedValue = TypedValue()
            val theme = requireContext().theme
            theme.resolveAttribute(attrId, typedValue, true)
            @ColorInt val color = typedValue.data
            color
        }
    }

    private fun onServerEvent(event: WSClientEvent?) {
        event?.let {
            when (it) {
                is WSClientEvent.Connected -> {
                    Log.d("MainActivity", "Connected, silent: ${it.silent}")
                    if (viewModel.firstSocketConnection.value == false) {
                        viewModel.firstSocketConnection.value = true
                    } else {
                        viewModel.refreshCurrentCarInfo()
                    }
                    if (!it.silent) {
                        showTopSnackAlert(getString(R.string.connected))
                    } else {}
                }
                is WSClientEvent.Disconnected -> Log.d("MainActivity", "Disconnected")
                is WSClientEvent.Reconnecting -> {
                    Log.d("MainActivity", "Reconnecting")
                    showTopSnackAlert(getString(R.string.reconnecting))
                }
                is WSClientEvent.ClientError -> {
                    Log.e("MainActivity", "Error: ${it.error}")
                    context?.let { ctx ->
                        MaterialAlertDialogBuilder(ctx)
                            .setTitle(R.string.internal_app_error)
                            .setMessage(it.error)
                            .setPositiveButton(R.string.close) {dlg, _ ->
                                dlg.dismiss()
                            }
                            .show()
                    }
                }
                is WSClientEvent.Alert -> {
                    val list: MutableList<AlertHistory> = viewModel.notificationsState.value?.toMutableList() ?: mutableListOf()
                    list.add(0, it.alert)
                    viewModel.notificationsState.value = list
                    showTopSnackAlert(if (it.alert.car != null) "${it.alert.car.nickname ?: it.alert.car.vin}: ${it.alert.typeDisplay}" else it.alert.typeDisplay, it.alert.additionalData)
                }
                is WSClientEvent.ServerAck -> Log.d("MainActivity", "Server acknowledged")
                is WSClientEvent.UpdatedCarInfo -> {
                    Log.d("MainActivity", "Car info updated: ${it.car.vin}")
                    viewModel.updateUiState(it.car)
                }
            }
        }
    }

    private fun showTopSnackAlert(title: String, message: String? = null) {
        val snackbar = Snackbar.make(
            requireActivity().findViewById(android.R.id.content),
            "", // Empty message since we'll use a custom view
            Snackbar.LENGTH_LONG
        )


        // Apply Material 3 styling
        snackbar.setBackgroundTint(getColorAttr(com.google.android.material.R.attr.colorSurface, R.color.surface))
        snackbar.setActionTextColor(getColorAttr(com.google.android.material.R.attr.colorPrimary, R.color.primary))

        // Replace default Snackbar content with custom layout
        val snackbarView = snackbar.view
        val snackbarTextView = snackbarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        snackbarTextView.visibility = View.GONE // Hide default text


        val customView = layoutInflater.inflate(R.layout.alert_snackbar_layout, null)
        val titleView = customView.findViewById<TextView>(R.id.snackbar_title)
        val subtitleView = customView.findViewById<TextView>(R.id.snackbar_subtitle)


        // Set title and subtitle text
        titleView.text = title
        message?.let {
            subtitleView.text = it
            subtitleView.visibility = View.VISIBLE
        }

        // Add custom view to Snackbar's ViewGroup
        snackbarView.setBackgroundColor(Color.TRANSPARENT)
        (snackbarView as ViewGroup).removeAllViews()
        snackbarView.addView(customView)

        // Position Snackbar at the top
        val params = snackbarView.layoutParams as FrameLayout.LayoutParams
        params.gravity = android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL
        params.topMargin = 64 // Margin from the top
        snackbarView.layoutParams = params

        // Show the Snackbar
        snackbar.show()
    }

    // Helper to resolve theme attribute colors with fallback
    private fun getColorAttr(attr: Int, fallback: Int): Int {
        val typedArray = requireContext().theme.obtainStyledAttributes(intArrayOf(attr))
        val color = typedArray.getColor(0, resources.getColor(fallback, requireContext().theme))
        typedArray.recycle()
        return color
    }

    override fun onResume() {
        super.onResume()
        serverReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.hasExtra("type")) {
                    try {
                        when (intent.getStringExtra("type")) {
                            "alert" -> {
                                onServerEvent(
                                    intent.getStringExtra("alert")?.let { alertStr ->
                                        WSClient.moshi.adapter(AlertHistory::class.java)
                                            .fromJson(
                                                alertStr
                                            )?.let {
                                                WSClientEvent.Alert(
                                                    it
                                                )
                                            }
                                    }
                                )
                            }
                            "carInfo" -> {
                                onServerEvent(
                                    intent.getStringExtra("car")?.let { alertStr ->
                                        WSClient.moshi.adapter(Car::class.java)
                                            .fromJson(
                                                alertStr
                                            )?.let {
                                                WSClientEvent.UpdatedCarInfo(
                                                    it
                                                )
                                            }
                                    }
                                )
                            }
                            "connected" -> {
                                onServerEvent(WSClientEvent.Connected(intent.getBooleanExtra("silent", false)))
                            }
                            "disconnected" -> {
                                onServerEvent(WSClientEvent.Disconnected)
                            }
                            "reconnecting" -> {
                                onServerEvent(WSClientEvent.Reconnecting)
                            }
                            "serverAck" -> {
                                onServerEvent(WSClientEvent.ServerAck)
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
                ContextCompat.RECEIVER_NOT_EXPORTED
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

    class HomeTabsAdapter internal constructor(
        context: Context?,
        var mData: List<MenuItem>
    ) : RecyclerView.Adapter<HomeTabsAdapter.ViewHolder>() {
        private val mInflater: LayoutInflater
        private var mClickListener: ItemClickListener? = null

        init {
            mInflater = LayoutInflater.from(context)
        }

        // inflates the row layout from xml when needed
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = mInflater.inflate(R.layout.home_tab_view, parent, false)
            return ViewHolder(view, mClickListener)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val action = mData[position]
            holder.tabName.setText(action.title)
            holder.tabIcon.setImageResource(action.icon)
            holder.tabSubTitle.visibility = if (action.subTitle == null || action.subTitle!!.isEmpty()) View.GONE else View.VISIBLE
            holder.tabSubTitle.text = action.subTitle
            holder.clickListener = mClickListener
        }

        override fun getItemCount(): Int {
            return mData.size
        }

        class ViewHolder internal constructor(itemView: View, var clickListener: ItemClickListener?) : RecyclerView.ViewHolder(itemView),
            View.OnClickListener {
            val tabName: TextView = itemView.findViewById(R.id.tabName)
            val tabSubTitle: TextView = itemView.findViewById(R.id.tabExtraInfo)
            val tabIcon: ImageView = itemView.findViewById(R.id.tabIcon)

            init {
                itemView.setOnClickListener(this)
            }

            override fun onClick(view: View) {
                clickListener?.onItemClick(view, adapterPosition)
            }
        }

        fun setClickListener(itemClickListener: ItemClickListener?) {
            mClickListener = itemClickListener
        }

        interface ItemClickListener {
            fun onItemClick(view: View?, position: Int)
        }

        fun updateItems(newItems: List<MenuItem>) {
            mData = newItems
            notifyDataSetChanged()
        }
    }

    private class CarPickerItem(val car: CarSerializerList) {
        override fun toString(): String {
            return car.nickname ?: car.vin
        }
    }

    private class NavAdapter(
        context: Context,
        var objects: List<CarPickerItem> = emptyList()
    ) : ArrayAdapter<CarPickerItem?>(
        context,
        android.R.layout.simple_spinner_item,
        objects as List<CarPickerItem?>
    ) {
        init {
            setDropDownViewResource(R.layout.menu_car)
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val textView =
                View.inflate(context, android.R.layout.simple_spinner_item, null) as TextView
            textView.setText(getItem(position)?.toString())
            textView.textSize = 20F
            return textView
        }

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            var row: View? = null
            if (convertView == null) {
                row = LayoutInflater.from(context)
                    .inflate(
                        R.layout.menu_car, parent,
                        false
                    )
            } else {
                row = convertView
            }
            row?.findViewById<TextView>(R.id.txt_title)?.text = getItem(position)?.car?.nickname
            row?.findViewById<TextView>(R.id.txt_vin)?.text = getItem(position)?.car?.vin
            return row!!
        }
    }

}