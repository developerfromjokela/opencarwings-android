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

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController

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