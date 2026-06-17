package com.sap.codelab.view.create

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.AndroidEntryPoint
import com.sap.codelab.R
import com.sap.codelab.databinding.ActivityCreateMemoBinding
import com.sap.codelab.utils.extensions.applySystemBarInsets
import com.sap.codelab.utils.extensions.empty
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

/**
 * Activity that allows a user to create a new Memo.
 */
@AndroidEntryPoint
internal class CreateMemo : AppCompatActivity() {

    private lateinit var binding: ActivityCreateMemoBinding
    private lateinit var viewModel: CreateMemoViewModel
    private lateinit var map: MapView
    private var marker: Marker? = null

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    private val backgroundLocationLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                uncheckLocationReminder()
                if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                    showPermissionPermanentlyDeniedDialog()
                }
            }
        }

    private val foregroundLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
            if (!granted) {
                uncheckLocationReminder()
                if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    showPermissionPermanentlyDeniedDialog()
                }
                return@registerForActivityResult
            }
            centerMapOnCurrentLocation()
            if (needsBackgroundLocation()) {
                backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        }

    private val notificationsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                uncheckLocationReminder()
                if (!shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                    showPermissionPermanentlyDeniedDialog()
                }
            } else {
                requestLocationPermissions()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // osmdroid requires a user agent to be set before any map is loaded.
        Configuration.getInstance().userAgentValue = packageName
        binding = ActivityCreateMemoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.appBar.applySystemBarInsets(top = true, horizontal = true)
        binding.contentCreateMemo.root.applySystemBarInsets(bottom = true, horizontal = true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        viewModel = ViewModelProvider(this)[CreateMemoViewModel::class.java]
        setupMap()
        observeUiState()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackNavigation()
            }
        })
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is CreateMemoUiState.Saved -> {
                            setResult(RESULT_OK)
                            finish()
                        }
                        is CreateMemoUiState.ValidationFailed -> {
                            binding.contentCreateMemo.run {
                                memoTitleContainer.error = getErrorMessage(state.titleError, R.string.memo_title_empty_error)
                                memoDescription.error = getErrorMessage(state.textError, R.string.memo_text_empty_error)
                            }
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    /**
     * Configures the embedded map with a default tile source, zoom and center.
     */
    private fun setupMap() {
        map = binding.contentCreateMemo.map
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.controller.setZoom(15.0)
        map.controller.setCenter(GeoPoint(48.1351, 11.5820))

        val eventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(point: GeoPoint): Boolean {
                setPin(point)
                return true
            }

            override fun longPressHelper(point: GeoPoint): Boolean = false
        }
        map.overlays.add(MapEventsOverlay(eventsReceiver))

        val lat = viewModel.selectedLatitude
        val lng = viewModel.selectedLongitude
        if (lat != null && lng != null) {
            val point = GeoPoint(lat, lng)
            setPin(point)
            map.controller.setCenter(point)
        } else {
            centerMapOnCurrentLocation()
        }

        binding.contentCreateMemo.locationReminderCheckbox.setOnCheckedChangeListener { buttonView, isChecked ->
            map.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!buttonView.isPressed) return@setOnCheckedChangeListener
            if (isChecked) {
                requestPermissionsForLocationReminder()
            } else {
                marker?.let { map.overlays.remove(it) }
                marker = null
                map.invalidate()
                viewModel.clearLocation()
            }
        }
    }

    @SuppressWarnings("MissingPermission")
    private fun centerMapOnCurrentLocation() {
        if (!hasForegroundLocation()) return
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location ?: return@addOnSuccessListener
            map.controller.animateTo(GeoPoint(location.latitude, location.longitude))
        }
    }

    /**
     * Places (or moves) the location pin at the given point and forwards it to the ViewModel.
     */
    private fun setPin(point: GeoPoint) {
        val pin = marker ?: Marker(map).also {
            it.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            map.overlays.add(it)
            marker = it
        }
        pin.position = point
        map.invalidate()
        viewModel.setLocation(point.latitude, point.longitude)
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_create_memo, menu)
        return true
    }

    /**
     * Handles actionbar interactions.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                handleBackNavigation()
                true
            }

            R.id.action_save -> {
                saveMemo()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun saveMemo() {
        binding.contentCreateMemo.run {
            viewModel.updateMemo(memoTitle.text.toString(), memoDescription.text.toString())
            viewModel.saveMemo()
        }
    }

    private fun requestPermissionsForLocationReminder() {
        if (needsNotificationsPermission()) {
            notificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            requestLocationPermissions()
        }
    }

    private fun requestLocationPermissions() {
        if (hasForegroundLocation() && !needsBackgroundLocation()) return
        if (hasForegroundLocation()) {
            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            foregroundLauncher.launch(foregroundPermissionsToRequest())
        }
    }

    private fun uncheckLocationReminder() {
        binding.contentCreateMemo.locationReminderCheckbox.isChecked = false
    }

    private fun needsNotificationsPermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
    }

    private fun hasForegroundLocation() =
        ContextCompat.checkSelfPermission(this,
            Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    private fun needsBackgroundLocation(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) !=
                PackageManager.PERMISSION_GRANTED
    }

    private fun foregroundPermissionsToRequest(): Array<String> = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
    }.toTypedArray()

    private fun handleBackNavigation() {
        if (hasUnsavedChanges()) {
            AlertDialog.Builder(this)
                .setTitle(R.string.discard_changes_title)
                .setMessage(R.string.discard_changes_message)
                .setPositiveButton(R.string.discard_changes_confirm) { _, _ -> finish() }
                .setNegativeButton(R.string.discard_changes_cancel, null)
                .show()
        } else {
            finish()
        }
    }

    private fun hasUnsavedChanges(): Boolean {
        return binding.contentCreateMemo.run {
            memoTitle.text?.isNotEmpty() == true || memoDescription.text?.isNotEmpty() == true || viewModel.hasSelectedLocation()
        }
    }

    private fun showPermissionPermanentlyDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.permission_denied_title)
            .setMessage(R.string.permission_denied_message)
            .setPositiveButton(R.string.permission_denied_open_settings) { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
            }
            .setNegativeButton(R.string.permission_denied_cancel, null)
            .show()
    }

    /**
     * Returns the error message if there is an error, or an empty string otherwise.
     *
     * @param hasError          - whether there is an error.
     * @param errorMessageResId - the resource id of the error message to show.
     * @return the error message if there is an error, or an empty string otherwise.
     */
    private fun getErrorMessage(hasError: Boolean, @StringRes errorMessageResId: Int): String {
        return if (hasError) {
            getString(errorMessageResId)
        } else {
            String.empty()
        }
    }
}
