package com.developerfromjokela.opencarwings.ui.utils

import android.os.Bundle
import android.text.InputType
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.setFragmentResult
import com.developerfromjokela.opencarwings.R
import com.developerfromjokela.opencarwings.databinding.BottomsheetSetpinBinding
import com.developerfromjokela.opencarwings.databinding.OtpDigitBinding
import com.developerfromjokela.opencarwings.ui.login.LoginResult
import com.developerfromjokela.opencarwings.ui.utils.CommandPinBottomSheetFragment.Companion.BUNDLE_PIN
import com.developerfromjokela.opencarwings.utils.PreferencesHelper
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import org.openapitools.client.apis.AccountApi
import org.openapitools.client.infrastructure.ApiClient
import org.openapitools.client.infrastructure.ClientError
import org.openapitools.client.infrastructure.ClientException
import org.openapitools.client.infrastructure.ServerException
import org.openapitools.client.models.APIError
import org.openapitools.client.models.PinChange
import kotlin.printStackTrace

class SetupPinBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomsheetSetpinBinding? = null
    private val binding get() = _binding!!

    private val pinLength = 4
    private val otpLength = 6

    private var savingPin = false

    private lateinit var preferencesHelper: PreferencesHelper
    private val pinBoxes = mutableListOf<TextInputEditText>()
    private val confirmBoxes = mutableListOf<TextInputEditText>()
    private val otpBoxes = mutableListOf<TextInputEditText>()

    private var otpRequired = false

    companion object {
        const val TAG = "SetupPinBottomSheet"

        const val REQUEST_KEY = "pin_setup_request"

        private const val ARG_OTP_REQUIRED = "arg_otp_required"
        private const val ARG_ERROR = "arg_error"

        fun newInstance(otpRequired: Boolean = false, errorMessage: String? = null) =
            SetupPinBottomSheetFragment().apply {
                arguments = bundleOf(
                    ARG_OTP_REQUIRED to otpRequired,
                    ARG_ERROR to errorMessage
                )
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetSetpinBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = true

        preferencesHelper = PreferencesHelper(view.context)

        otpRequired = arguments?.getBoolean(ARG_OTP_REQUIRED) == true

        buildDigitRow(binding.pinBoxContainer, pinBoxes, pinLength, isPassword = true)
        buildDigitRow(binding.confirmPinBoxContainer, confirmBoxes, pinLength, isPassword = true)

        if (otpRequired) {
            binding.otpSection.visibility = View.VISIBLE
            buildDigitRow(binding.otpBoxContainer, otpBoxes, otpLength, isPassword = false)
        }

        arguments?.getString(ARG_ERROR)?.let { error ->
            binding.pinErrorText.text = error
            binding.pinErrorText.visibility = View.VISIBLE
        }

        binding.pinSubmitButton.setOnClickListener {
           submit()
        }

        pinBoxes.firstOrNull()?.requestFocus()
    }

    private fun buildDigitRow(
        container: ViewGroup,
        boxes: MutableList<TextInputEditText>,
        length: Int,
        isPassword: Boolean
    ) {
        container.removeAllViews()
        boxes.clear()

        for (i in 0 until length) {
            val digitBinding = OtpDigitBinding.inflate(layoutInflater, container, false)
            val box = digitBinding.digitBox
            if (isPassword) {
                box.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            }
            container.addView(digitBinding.root)
            boxes.add(box)

            box.doOnTextChanged { text, _, _, count ->
                if (!text.isNullOrEmpty() && count == 1 && i < length - 1) {
                    boxes[i + 1].requestFocus()
                }
                updateSubmitEnabled()
            }

            box.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL &&
                    event.action == KeyEvent.ACTION_DOWN &&
                    box.text.isNullOrEmpty() && i > 0
                ) {
                    boxes[i - 1].apply {
                        requestFocus()
                        text?.clear()
                    }
                    true
                } else false
            }
        }
    }

    private fun allFilled(boxes: List<TextInputEditText>) =
        boxes.isNotEmpty() && boxes.all { !it.text.isNullOrBlank() }

    private fun updateSubmitEnabled() {
        val pinOk = allFilled(pinBoxes) && allFilled(confirmBoxes)
        val otpOk = !otpRequired || allFilled(otpBoxes)
        binding.pinSubmitButton.isEnabled = pinOk && otpOk && !savingPin
    }

    private fun submit() {
        val pin = pinBoxes.joinToString("") { it.text.toString() }
        val confirm = confirmBoxes.joinToString("") { it.text.toString() }

        if (pin != confirm) {
            binding.pinErrorText.text = getString(R.string.pin_mismatch)
            binding.pinErrorText.visibility = View.VISIBLE
            confirmBoxes.forEach { it.text?.clear() }
            confirmBoxes.firstOrNull()?.requestFocus()
            return
        }

        if (otpRequired && !allFilled(otpBoxes)) return
        savingPin = true
        updateSubmitEnabled()
        // send request
        ApiClient.apiKey["Authorization"] = preferencesHelper.accessToken ?: ""
        CoroutineScope(newSingleThreadContext("name")).launch {
            withContext(Dispatchers.IO) {
                try {
                    AccountApi().accountPinCreate(PinChange(
                        newPin = pin,
                        newPinConfirm = confirm,
                        otpCode = otpBoxes.joinToString("") { it.text.toString() }
                    ))
                    withContext(Dispatchers.Main) {
                        setFragmentResult(REQUEST_KEY, bundleOf(BUNDLE_PIN to pin))
                        dismiss()
                    }
                } catch (e: ClientException) {
                    withContext(Dispatchers.Main) {
                        binding.pinErrorText.visibility = View.VISIBLE
                        if (e.statusCode == 403) {
                            try {
                                val resp: String? = (e.response as? ClientError<APIError>)?.body as? String
                                if (resp != null) {
                                    val jsonObj = Gson().fromJson(resp, APIError::class.java)
                                    binding.pinErrorText.text = jsonObj?.error
                                    return@withContext
                                }
                            } catch (_: Exception) {}
                        }
                        binding.pinErrorText.text =  "Client error ${e.statusCode}"
                    }
                } catch (e: ServerException) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        binding.pinErrorText.text =  getString(R.string.server_unavailable)
                        binding.pinErrorText.visibility = View.VISIBLE
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        binding.pinErrorText.text = e.message
                        binding.pinErrorText.visibility = View.VISIBLE
                    }
                }

            }
        }

        savingPin = false
        updateSubmitEnabled()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}