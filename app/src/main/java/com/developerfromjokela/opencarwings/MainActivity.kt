package com.developerfromjokela.opencarwings

import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Spinner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.color.DynamicColors
import android.Manifest
import android.graphics.Color
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.developerfromjokela.opencarwings.utils.UpdateUtils.isAppUpToDate
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    private var topMargin = 0

    private val listener = NavController.OnDestinationChangedListener { controller, destination, arguments ->
        findViewById<Spinner>(R.id.spinner_toolbar).visibility = if (destination.id == R.id.mainFragment) View.VISIBLE else View.GONE
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            // FCM SDK (and your app) can post notifications.
        } else {
            // TODO: Inform user that that your app will not show notifications.
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DynamicColors.applyToActivityIfAvailable(this)
        setContentView(R.layout.activity_main)
        val toolbar: MaterialToolbar = findViewById(R.id.materialToolbar);
        setSupportActionBar(toolbar)

        val abc = AppBarConfiguration.Builder(R.id.mainFragment, R.id.loginFragment).build();
        val navHostFragment = supportFragmentManager.fragments.first() as NavHostFragment
        navController = navHostFragment.navController
        NavigationUI.setupActionBarWithNavController(this, navHostFragment.navController, abc)
        askNotificationPermission()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ViewCompat.setOnApplyWindowInsetsListener(findViewById<View>(android.R.id.content)) { _, insets ->
                val safeInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                topMargin = safeInsets.top
                insets
            }
        }

        lifecycleScope.launch {
            val upToDate = isAppUpToDate(this@MainActivity)
            if (upToDate) {
                println("App is up to date")
            } else {
                println("Update available!")
                showTopSnackAlert(getString(R.string.update_available), getString(R.string.update_avail_desc))
            }
        }
    }

    private fun getColorAttr(attr: Int, fallback: Int): Int {
        val typedArray = theme.obtainStyledAttributes(intArrayOf(attr))
        val color = typedArray.getColor(0, resources.getColor(fallback, theme))
        typedArray.recycle()
        return color
    }

    private fun showTopSnackAlert(title: String, message: String? = null) {
        val rootVw: View = findViewById(android.R.id.content)
        val snackbar = Snackbar.make(
            rootVw,
            "", // Empty message since we'll use a custom view
            Snackbar.LENGTH_LONG
        )
        snackbar.setDuration(4000)


        // Apply Material 3 styling
        snackbar.setBackgroundTint(getColorAttr(com.google.android.material.R.attr.colorSurface, R.color.surface))
        snackbar.setActionTextColor(getColorAttr(com.google.android.material.R.attr.colorPrimaryContainer, R.color.primary))

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
        params.topMargin = topMargin+450
        snackbarView.layoutParams = params

        // Show the Snackbar
        snackbar.show()
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // TODO: display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                //       If the user selects "No thanks," allow the user to continue without notifications.
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onBackPressed() {
        if (navController.currentDestination?.id == R.id.loginFragment || navController.currentDestination?.id == R.id.mainFragment) {
            finish()
        } else if (!(navController.navigateUp() || super.onSupportNavigateUp())) {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        navController.addOnDestinationChangedListener(listener)
    }

    override fun onPause() {
        navController.removeOnDestinationChangedListener(listener)
        super.onPause()
    }

    override fun onSupportNavigateUp(): Boolean {
        if (navController.currentDestination?.id == R.id.loginFragment || navController.currentDestination?.id == R.id.mainFragment) {
            finish()
            return true
        }
        if (!(navController.navigateUp() || super.onSupportNavigateUp())) {
            onBackPressed()
        }
        return true
    }

}