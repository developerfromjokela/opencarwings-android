package com.developerfromjokela.opencarwings.ui.main.timers

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.get
import androidx.core.view.removeItemAt
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewModelScope
import com.developerfromjokela.opencarwings.R
import com.developerfromjokela.opencarwings.databinding.FragmentTimerEditBinding
import com.developerfromjokela.opencarwings.utils.CustomDateUtils
import com.developerfromjokela.opencarwings.utils.PreferencesHelper
import com.developerfromjokela.opencarwings.websocket.WSClient
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.MaterialTimePicker.INPUT_MODE_CLOCK
import com.google.android.material.timepicker.TimeFormat.CLOCK_24H
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.openapitools.client.apis.CarsApi
import org.openapitools.client.apis.TokenApi
import org.openapitools.client.infrastructure.ApiClient
import org.openapitools.client.infrastructure.ClientException
import org.openapitools.client.infrastructure.ServerException
import org.openapitools.client.models.CarUpdating
import org.openapitools.client.models.CommandTimerSetting
import org.openapitools.client.models.SendToCarLocation
import org.openapitools.client.models.TokenRefresh
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle


class TimerEditFragment : Fragment() {

    private var timer: CommandTimerSetting? = null

    private lateinit var binding: FragmentTimerEditBinding

    private var dateSelection: LocalDate? = null
    private var timeSelection: OffsetTime? = null

    private var commandType: Int = 0

    private lateinit var preferencesHelper: PreferencesHelper


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            timer =
                it.getString(ARG_TIMER)
                    ?.let { it1 -> WSClient.moshi.adapter(CommandTimerSetting::class.java).fromJson(it1) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentTimerEditBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferencesHelper = PreferencesHelper(requireContext())
        var serverUrl = PreferencesHelper(requireContext()).server ?: "";
        if (!serverUrl.startsWith("https://")) {
            serverUrl = "https://${serverUrl}"
        }
        System.getProperties().setProperty(ApiClient.baseUrlKey, serverUrl)

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.timer_edit_menu, menu)
                if (timer == null) {
                    menu.removeItemAt(0)
                }
            }

            override fun onMenuItemSelected(menuItem: android.view.MenuItem): Boolean {
                when(menuItem.itemId) {
                    R.id.save -> {
                        save()
                    }
                    R.id.delete -> {
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(getString(R.string.delete_timer))
                            .setMessage(getString(R.string.delete_timer_msg))
                            .setPositiveButton(R.string.delete
                            ) { p0, p1 -> delete() }
                            .setNegativeButton(android.R.string.cancel
                            ) { p0, p1 -> p0?.dismiss() }.show()
                    }
                    android.R.id.home -> {
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
                return true
            }
        }, viewLifecycleOwner)

        binding.timerModeTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                // Handle tab select
                if (tab?.position == 0) {
                    binding.weekdayChecks.visibility = View.GONE
                    binding.timerDatePicker.visibility = View.VISIBLE
                } else if (tab?.position == 1) {
                    binding.weekdayChecks.visibility = View.VISIBLE
                    binding.timerDatePicker.visibility = View.GONE
                }
            }
            override fun onTabReselected(tab: TabLayout.Tab?) {}
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
        })

        val adapter = ArrayAdapter(requireContext(), R.layout.server_dropdown_item, arrayOf(R.string.refresh_data, R.string.charge_start, R.string.ac_on, R.string.ac_off, R.string.read_configuration).map { getString(it) })

        binding.timerTime.setOnClickListener {
            val item = MaterialTimePicker.Builder().setTitleText(getString(R.string.select_time)).setInputMode(INPUT_MODE_CLOCK).setHour(timeSelection?.hour ?: 0).setMinute(timeSelection?.minute ?: 0).setTimeFormat(CLOCK_24H).build()
            item.addOnPositiveButtonClickListener {
                timeSelection = OffsetTime.of(item.hour, item.minute,0, 0, ZonedDateTime.now().offset)
                binding.timerTime.setText(CustomDateUtils.formatToLocalTime(timeSelection))
            }
            item.show(childFragmentManager, "TimerTimeSelector")
        }

        binding.timerDate.setOnClickListener {
            val item = MaterialDatePicker.Builder.datePicker()
                .setTitleText(getString(R.string.select_date))
                .setSelection(dateSelection?.atStartOfDay()?.toEpochSecond(ZoneOffset.UTC)?.times(1000))
                .build()
            item.show(childFragmentManager, "TimerDateSelector")
            item.addOnPositiveButtonClickListener {
                dateSelection = LocalDateTime.ofEpochSecond(item.selection?.div(1000) ?: 0, 0,
                    ZoneOffset.UTC)?.toLocalDate()
                binding.timerDate.setText(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format(dateSelection))
            }
        }

        binding.timerCommand.onItemClickListener = AdapterView.OnItemClickListener { p0, p1, p2, p3 -> commandType = p2+1 }

        if (timer != null) {
            (requireActivity() as AppCompatActivity).supportActionBar?.title = timer!!.name
            binding.timerName.setText(timer!!.name)
            binding.timerEnabled.isChecked = timer!!.enabled == true
            binding.timerCommand.setText(timer!!.commandTypeDisplay)
            binding.timerCommand.setAdapter(adapter)
            binding.timerCommand.setSelection(timer!!.commandType?.minus(1) ?: 0)
            commandType = timer!!.commandType ?: 1
            binding.timerModeTabs.selectTab(binding.timerModeTabs.getTabAt(timer!!.timerType ?: 0))
            binding.timerTime.setText(CustomDateUtils.parseAndFormatToLocalTimerTime(timer!!.time))
            timeSelection = CustomDateUtils.parseToLocalTimerTime(timer!!.time)?.withOffsetSameInstant(ZonedDateTime.now().offset)

            if (timer!!.timerType == 1) {
                binding.timerMon.isChecked = timer!!.weekdayMon == true
                binding.timerTue.isChecked = timer!!.weekdayTue == true
                binding.timerWed.isChecked = timer!!.weekdayWed == true
                binding.timerThu.isChecked = timer!!.weekdayThu == true
                binding.timerFri.isChecked = timer!!.weekdayFri == true
                binding.timerSat.isChecked = timer!!.weekdaySat == true
                binding.timerSun.isChecked = timer!!.weekdaySun == true
            }
            if (timer!!.timerType == 0) {
                dateSelection = timer!!.date
                binding.timerDate.setText(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).format(dateSelection))
            }
        } else {
            binding.timerCommand.setText(null)
            binding.timerCommand.setAdapter(adapter)
            binding.timerCommand.clearListSelection()
        }
    }

    private fun save() {

        if (timeSelection == null) {
            binding.timerTime.setError(getString(R.string.select_time))
            return
        }

        if (commandType < 1) {
            binding.timerCommand.setError(getString(R.string.select_command))
            return
        }

        if (binding.timerModeTabs.selectedTabPosition == 0 && dateSelection == null) {
            binding.timerDate.setError(getString(R.string.select_date))
            return
        } else if (binding.timerModeTabs.selectedTabPosition == 1) {
            if (!binding.timerMon.isChecked && !binding.timerTue.isChecked && !binding.timerWed.isChecked && !binding.timerThu.isChecked && !binding.timerFri.isChecked && !binding.timerSat.isChecked && !binding.timerSun.isChecked) {
                binding.timerMon.setError(getString(R.string.select_one_weekday))
                return
            }
        }


        val newTimer = timer ?: CommandTimerSetting(name = binding.timerName.text?.toString() ?: "", time = CustomDateUtils.formatToUTCTimerTime(timeSelection) ?: "")

        newTimer.id = timer?.id
        newTimer.time = CustomDateUtils.formatToUTCTimerTime(timeSelection) ?: ""
        newTimer.timerType = binding.timerModeTabs.selectedTabPosition
        newTimer.name = binding.timerName.text?.toString() ?: ""
        if (newTimer.timerType == 0)
            newTimer.date = dateSelection
        else {
            newTimer.weekdayMon = binding.timerMon.isChecked
            newTimer.weekdayTue = binding.timerTue.isChecked
            newTimer.weekdayWed = binding.timerWed.isChecked
            newTimer.weekdayThu = binding.timerThu.isChecked
            newTimer.weekdayFri = binding.timerFri.isChecked
            newTimer.weekdaySat = binding.timerSat.isChecked
            newTimer.weekdaySun = binding.timerSun.isChecked
        }

        newTimer.commandType = commandType
        newTimer.enabled = binding.timerEnabled.isChecked

        println(newTimer)

        val loadingView = Spinner(requireContext())
        val loadingDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.deleting_timer)
            .setCancelable(false)
            .setView(loadingView)
            .create()
        loadingDialog.show()
        try {
            CoroutineScope(Dispatchers.IO).launch {
                ApiClient.apiKey["Authorization"] = preferencesHelper.accessToken ?: ""
                withContext(Dispatchers.IO) {
                    if (timer == null)
                        CarsApi().apiCarTimersCreate(preferencesHelper.activeCarVin!!, newTimer)
                    else
                        CarsApi().apiCarTimersPartialUpdate(preferencesHelper.activeCarVin!!, newTimer.id?.toString() ?: "", newTimer)
                }
            }
            loadingDialog.dismiss()
            parentFragmentManager.popBackStack()
        } catch (e: ClientException) {
            loadingDialog.dismiss()
            if (e.statusCode != 401) {
                showError("Client error ${e.statusCode}", getString(R.string.server_unavailable))
            } else {
                // renew token
                CoroutineScope(Dispatchers.IO).launch {
                    renewToken {
                        save()
                    }
                }
            }
        } catch (e: ServerException) {
            loadingDialog.dismiss()
            e.printStackTrace()
            showError(getString(R.string.server_unavailable), if (e.statusCode != 503) "Server error ${e.statusCode}" else null)
        } catch (e: Exception) {
            loadingDialog.dismiss()
            e.printStackTrace()
            showError(getString(R.string.internal_app_error), e.message)
        }
    }

    private fun delete() {
        val loadingView = Spinner(requireContext())
        val loadingDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.deleting_timer)
            .setCancelable(false)
            .setView(loadingView)
            .create()
        loadingDialog.show()
        try {
            if (timer == null) {
                throw Exception("Timer is null")
            }

            CoroutineScope(Dispatchers.IO).launch {
                ApiClient.apiKey["Authorization"] = preferencesHelper.accessToken ?: ""
                withContext(Dispatchers.IO) {
                    CarsApi().apiCarTimersDelete(preferencesHelper.activeCarVin!!, timer!!.id?.toString() ?: "")
                }
            }
            loadingDialog.dismiss()
            parentFragmentManager.popBackStack()
        } catch (e: ClientException) {
            loadingDialog.dismiss()
            if (e.statusCode != 401) {
                showError("Client error ${e.statusCode}", getString(R.string.server_unavailable))
            } else {
                // renew token
                CoroutineScope(Dispatchers.IO).launch {
                    renewToken {
                        delete()
                    }
                }
            }
        } catch (e: ServerException) {
            loadingDialog.dismiss()
            e.printStackTrace()
            showError(getString(R.string.server_unavailable), if (e.statusCode != 503) "Server error ${e.statusCode}" else null)
        } catch (e: Exception) {
            loadingDialog.dismiss()
            e.printStackTrace()
            showError(getString(R.string.internal_app_error), e.message)
        }
    }

    private suspend fun renewToken(retryFunc: () -> Unit) {
        try {
            ApiClient.apiKey["Authorization"] = preferencesHelper.accessToken ?: ""
            withContext(Dispatchers.IO) {
                val result = TokenApi().apiTokenRefreshCreate(TokenRefresh(preferencesHelper.refreshToken ?: "", preferencesHelper.accessToken ?: ""))
                preferencesHelper.accessToken = result.access
            }
            ApiClient.apiKey["Authorization"] = preferencesHelper.accessToken ?: ""
            retryFunc()
        } catch (e: ClientException) {
            showError("Client error ${e.statusCode}", getString(R.string.server_unavailable))
        } catch (e: ServerException) {
            e.printStackTrace()
            showError(getString(R.string.server_unavailable), if (e.statusCode != 503) "Server error ${e.statusCode}" else null)
        } catch (e: Exception) {
            e.printStackTrace()
            showError(getString(R.string.internal_app_error), e.message)
        }
    }

    private fun showError(title: String, message: String?) {
        MaterialAlertDialogBuilder(requireContext()).setTitle(title).setMessage(message).setPositiveButton(android.R.string.ok, {p0, p1 -> {p0.dismiss()}}).show()
    }

    companion object {
        const val ARG_TIMER = "timer"

        @JvmStatic
        fun newInstance(timer: CommandTimerSetting) =
            TimerEditFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TIMER, WSClient.moshi.adapter(CommandTimerSetting::class.java).toJson(timer))
                }
            }
    }
}