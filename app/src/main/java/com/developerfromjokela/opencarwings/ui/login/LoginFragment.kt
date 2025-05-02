package com.developerfromjokela.opencarwings.ui.login

import com.developerfromjokela.opencarwings.R
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavOptions
import androidx.navigation.Navigation
import com.developerfromjokela.opencarwings.databinding.FragmentLoginBinding
import com.developerfromjokela.opencarwings.utils.PreferencesHelper
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging


class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private lateinit var preferencesHelper: PreferencesHelper
    private lateinit var loginViewModel: LoginViewModel

    companion object {
        val serverOptions = arrayOf("opencarwings.viaaq.eu", "custom")
    }


    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        preferencesHelper = PreferencesHelper(requireContext())
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Navigate to main if signed in
        if (preferencesHelper.refreshToken != null) {
            val navBuilder = NavOptions.Builder()
            val navOptions: NavOptions = navBuilder.setPopUpTo(R.id.mainFragment, false).build()
            Navigation.findNavController(view).navigate(R.id.mainFragment, null, navOptions)
            return
        }

        // Initialize ViewModel
        loginViewModel = ViewModelProvider(this).get(LoginViewModel::class.java)

        val usernameEditText = binding.username
        val passwordEditText = binding.password
        val customServerURLField = binding.serverUrl
        val serverPicker = binding.selectServer
        val loginButton = binding.login
        val loadingProgressBar = binding.loading

        // Set up server picker
        val adapter = ArrayAdapter(requireContext(), R.layout.server_dropdown_item, serverOptions)
        serverPicker.setAdapter(adapter)

        binding.serverTextInputLayout.visibility = View.GONE
        serverPicker.setText(serverOptions[0], false)

        // Observe form state
        loginViewModel.loginFormState.observe(viewLifecycleOwner, Observer { formState ->
            loginButton.isEnabled = formState.isDataValid

            // Show errors if any
            binding.textInputLayout.error = formState.usernameError?.let { getString(it) }
            binding.passwordLayout.error = formState.passwordError?.let { getString(it) }
            binding.textInputLayout2.error = formState.serverError?.let { getString(it) }
            binding.serverTextInputLayout.error = formState.serverUrlError?.let { getString(it) }

            // Show/hide custom server URL field based on server selection
            binding.serverTextInputLayout.visibility =
                if (serverPicker.text.toString() == "custom") View.VISIBLE else View.GONE
        })

        // Observe login result
        loginViewModel.loginResult.observe(viewLifecycleOwner) { loginResult ->
            loadingProgressBar.visibility = View.GONE
            if (loginResult.success) {
                preferencesHelper.username = usernameEditText.text?.toString()
                preferencesHelper.server = if (serverPicker.text.toString() == "custom") customServerURLField.text.toString() else "https://"+serverPicker.text.toString()
                preferencesHelper.refreshToken = loginResult.response!!.refresh
                preferencesHelper.accessToken = loginResult.response.access
                Navigation.findNavController(view).navigate(R.id.mainFragment)
            } else {
                loginResult.error?.let { showLoginFailed(it) }
                loginResult.errorString?.let { showLoginFailedString(it) }
            }
        }

        // Set up text watchers
        val afterTextChangedListener = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                loginViewModel.loginDataChanged(
                    usernameEditText.text.toString(),
                    passwordEditText.text.toString(),
                    serverPicker.text.toString(),
                    customServerURLField.text.toString()
                )
            }
        }

        usernameEditText.addTextChangedListener(afterTextChangedListener)
        passwordEditText.addTextChangedListener(afterTextChangedListener)
        customServerURLField.addTextChangedListener(afterTextChangedListener)

        // Handle server picker selection
        serverPicker.setOnItemClickListener { _, _, _, _ ->
            loginViewModel.loginDataChanged(
                usernameEditText.text.toString(),
                passwordEditText.text.toString(),
                serverPicker.text.toString(),
                customServerURLField.text.toString()
            )
        }

        // Handle login button click
        loginButton.setOnClickListener {
            loadingProgressBar.visibility = View.VISIBLE
            loginViewModel.login(
                usernameEditText.text.toString(),
                passwordEditText.text.toString(),
                if (serverPicker.text.toString() == "custom") customServerURLField.text.toString() else "https://"+serverPicker.text.toString()
            )
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("MF", "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }
            // Get new FCM registration token
            val token = task.result
            loginViewModel._fcmToken.value = token
        })
    }

    private fun showLoginFailed(errorStringRes: Int) {
        Toast.makeText(context, errorStringRes, Toast.LENGTH_SHORT).show()
    }

    private fun showLoginFailedString(errorString: String) {
        Toast.makeText(context, errorString, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}