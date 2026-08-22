package com.developerfromjokela.opencarwings.ui.utils

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.os.bundleOf
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.setFragmentResult
import com.developerfromjokela.opencarwings.R
import com.developerfromjokela.opencarwings.databinding.BottomsheetOtpBinding
import com.developerfromjokela.opencarwings.databinding.OtpDigitBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText

class CommandPinBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomsheetOtpBinding? = null
    private val binding get() = _binding!!

    private val digitCount = 4
    private val boxes = mutableListOf<TextInputEditText>()

    companion object {
        const val REQUEST_KEY = "pin_request"
        const val BUNDLE_PIN = "pin_code"
        const val TAG = "PinBottomSheet"
        private const val ARG_ERROR = "arg_error"

        fun newInstance(errorMessage: String? = null) = CommandPinBottomSheetFragment().apply {
            arguments = bundleOf(ARG_ERROR to errorMessage)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetOtpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = true
        binding.otpTitle.setText(R.string.command_pin_title)
        binding.otpSubtitle.setText(R.string.command_pin_subtitle)

        buildBoxes()

        arguments?.getString(ARG_ERROR)?.let { error ->
            binding.otpErrorText.text = error
            binding.otpErrorText.visibility = View.VISIBLE
        }

        binding.otpSubmitButton.visibility = View.GONE

        boxes.firstOrNull()?.requestFocus()
    }

    private fun buildBoxes() {
        binding.otpBoxContainer.removeAllViews()
        boxes.clear()

        for (i in 0 until digitCount) {
            val digitBinding = OtpDigitBinding.inflate(
                layoutInflater, binding.otpBoxContainer, false
            )
            val box = digitBinding.digitBox
            binding.otpBoxContainer.addView(digitBinding.root)
            boxes.add(box)

            box.doOnTextChanged { text, _, _, count ->
                if (!text.isNullOrEmpty() && count == 1) {
                    // Move to next box
                    if (i < digitCount - 1) {
                        boxes[i + 1].requestFocus()
                    } else {
                        box.clearFocus()
                        // Auto-submit when the last digit is filled
                        if (allFilled()) submit()
                    }
                }
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

            // Handle pasting a full code into any box
            box.setOnEditorActionListener { v, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE && allFilled()) {
                    submit()
                    true
                } else false
            }

            if (i == 0) {
                // Support pasting the whole code at once into the first box
                box.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                    override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        val pasted = s?.toString().orEmpty()
                        if (pasted.length > 1 && pasted.all { it.isDigit() }) {
                            distributeCode(pasted)
                        }
                    }
                })
            }
        }
    }

    private fun distributeCode(code: String) {
        val digits = code.take(digitCount)
        digits.forEachIndexed { index, c ->
            boxes[index].setText(c.toString())
        }
        if (digits.length == digitCount) {
            boxes.last().clearFocus()
            submit()
        } else {
            boxes[digits.length].requestFocus()
        }
    }

    private fun allFilled() = boxes.all { !it.text.isNullOrBlank() }

    private fun submit() {
        if (!allFilled()) return
        val code = boxes.joinToString("") { it.text.toString() }
        setFragmentResult(REQUEST_KEY, bundleOf(BUNDLE_PIN to code))
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}