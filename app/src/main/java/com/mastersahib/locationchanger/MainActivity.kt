package com.mastersahib.locationchanger

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mastersahib.locationchanger.databinding.ActivityMainBinding
import java.security.MessageDigest

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val preferences by lazy { getSharedPreferences(PREFS, MODE_PRIVATE) }
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) startSession()
        else toast(getString(R.string.location_permission_required))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.deviceIdText.text = getString(R.string.device_id_label, hashedDeviceId())
        binding.openDeveloperOptionsButton.setOnClickListener {
            try { startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)) }
            catch (_: Exception) { startActivity(Intent(Settings.ACTION_SETTINGS)) }
        }
        binding.startButton.setOnClickListener { checkPermissionAndStart() }
        binding.stopButton.setOnClickListener {
            stopService(Intent(this, MockLocationService::class.java))
            showServiceStatus(false)
            toast(getString(R.string.location_stopped))
        }
        if (!preferences.getBoolean(KEY_GUIDE_SEEN, false)) {
            MaterialAlertDialogBuilder(this).setTitle(R.string.setup_title).setMessage(R.string.setup_help)
                .setPositiveButton(R.string.setup_ok) { _, _ -> preferences.edit().putBoolean(KEY_GUIDE_SEEN, true).apply() }.show()
        }
        updateLicenseUi()
        showServiceStatus(MockLocationService.isRunning)
    }

    override fun onResume() {
        super.onResume()
        binding.mockAppStatus.text = if (isSelectedAsMockLocationApp()) getString(R.string.mock_app_ready) else getString(R.string.mock_app_not_selected)
        updateLicenseUi()
        showServiceStatus(MockLocationService.isRunning)
    }

    private fun checkPermissionAndStart() {
        val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= 33) permissions += Manifest.permission.POST_NOTIFICATIONS
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) startSession()
        else permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun startSession() {
        if (!isSelectedAsMockLocationApp()) { toast(getString(R.string.select_mock_app_first)); return }
        val intent = Intent(this, MockLocationService::class.java).apply {
            action = MockLocationService.ACTION_START
            putExtra(MockLocationService.EXTRA_LATITUDE, FIXED_LATITUDE)
            putExtra(MockLocationService.EXTRA_LONGITUDE, FIXED_LONGITUDE)
        }
        ContextCompat.startForegroundService(this, intent)
        updateLicenseUi()
        showServiceStatus(true)
    }

    private fun updateLicenseUi() {
        binding.trialRemainingText.text = getString(R.string.full_access_active)
        binding.startButton.isEnabled = !MockLocationService.isRunning
    }

    private fun hashedDeviceId(): String {
        val id = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
        return MessageDigest.getInstance("SHA-256").digest(id.toByteArray()).take(8).joinToString("") { "%02X".format(it) }
    }

    private fun isSelectedAsMockLocationApp(): Boolean {
        val ops = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        return try { ops.checkOpNoThrow(AppOpsManager.OPSTR_MOCK_LOCATION, android.os.Process.myUid(), packageName) == AppOpsManager.MODE_ALLOWED }
        catch (_: Exception) { false }
    }

    private fun showServiceStatus(running: Boolean) {
        binding.serviceStatus.text = getString(if (running) R.string.location_active else R.string.location_inactive)
        binding.stopButton.isEnabled = running
        binding.startButton.isEnabled = !running
    }
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    companion object {
        private const val PREFS = "nrls_app"
        private const val KEY_GUIDE_SEEN = "guide_seen"
        private const val FIXED_LATITUDE = 24.883843
        private const val FIXED_LONGITUDE = 67.024483
    }
}
