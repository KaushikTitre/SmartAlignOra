package com.example.smartalignoraapplication

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.example.smartalignoraapplication.controller.BleViewModel
import com.example.smartalignoraapplication.ui.screens.AboutScreen
import com.example.smartalignoraapplication.ui.screens.MainShell
import com.example.smartalignoraapplication.ui.screens.SplashScreen

class MainActivity : ComponentActivity() {

    val viewModel: BleViewModel by viewModels()

    // ✅ splash → about → main
    private var currentScreen by mutableStateOf("splash")


    // ---------------- BT enable ----------------

    val enableBtLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                viewModel.startScan()
            } else {
                viewModel.status.value = "Bluetooth required"
            }
        }


    // ---------------- BT permission ----------------

    val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (results.values.all { it }) {

                if (!viewModel.isBluetoothEnabled()) {
                    enableBtLauncher.launch(
                        Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    )
                } else {
                    viewModel.startScan()
                }

            } else {
                viewModel.status.value = "Bluetooth permission required"
            }
        }


    // ---------------- Location permission ----------------

    val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->

            val granted = results.values.all { it }

            viewModel.onLocationPermissionResult(granted)
        }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── Start GPS hardware immediately if permission was granted previously ─
        // This ensures lastKnownLocation is populated well before any fall fires.
        // We use ACCESS_FINE_LOCATION as the primary check.
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineLocationGranted) {
            // Notify ViewModel: permission is confirmed, start LocationManager now
            viewModel.onLocationPermissionResult(true)
        }

        setContent {

            when (currentScreen) {

                // ✅ Splash first
                "splash" -> SplashScreen(
                    onNavigateToAbout = {
                        currentScreen = "about"
                    }
                )


                // ✅ About second
                "about" -> AboutScreen(
                    onBack = {},
                    onGetStarted = {
                        currentScreen = "main"
                    }
                )


                // ✅ Main app
                "main" -> MainShell(
                    viewModel = viewModel,

                    onRequestBtPermission = {
                        requestBluetoothPermissions()
                    },

                    onEnableBluetooth = {
                        enableBtLauncher.launch(
                            Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                        )
                    },

                    onRequestLocationPerm = {
                        requestLocationPermissions()
                    }
                )
            }
        }
    }



    // ================================
    // BLUETOOTH PERMISSION
    // ================================

    fun requestBluetoothPermissions() {

        val perms =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )

            } else {

                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }


        val granted = perms.all {

            ContextCompat.checkSelfPermission(
                this,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }


        if (granted) {

            if (!viewModel.isBluetoothEnabled()) {

                enableBtLauncher.launch(
                    Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                )

            } else {

                viewModel.startScan()
            }

        } else {

            permissionLauncher.launch(perms)
        }
    }



    // ================================
    // LOCATION PERMISSION
    // ================================

    fun requestLocationPermissions() {

        val perms = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val granted = perms.all {

            ContextCompat.checkSelfPermission(
                this,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }


        if (granted) {

            viewModel.onLocationPermissionResult(true)

        } else {

            locationPermissionLauncher.launch(perms)
        }
    }
}