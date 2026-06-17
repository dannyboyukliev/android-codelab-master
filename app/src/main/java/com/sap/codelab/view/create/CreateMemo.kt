package com.sap.codelab.view.create

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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

    private val backgroundLocationLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // granted or denied — memo saves either way; geofence registration checks the grant later
            viewModel.saveMemo()
        }

    private val foregroundLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
            if (!granted) return@registerForActivityResult
            if (needsBackgroundLocation()) {
                backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                viewModel.saveMemo()
            }
        }

    private val notificationsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // granted or denied — proceed to location permission flow either way
            requestLocationPermissions()
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
        viewModel = ViewModelProvider(this)[CreateMemoViewModel::class.java]
        setupMap()
        observeUiState()
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    if (state is CreateMemoUiState.Saved) {
                        setResult(RESULT_OK)
                        finish()
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
            R.id.action_save -> {
                saveMemo()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Saves the memo if the input is valid; otherwise shows the corresponding error messages.
     */
    private fun saveMemo() {
        binding.contentCreateMemo.run {
            viewModel.updateMemo(memoTitle.text.toString(), memoDescription.text.toString())
            if (!viewModel.isMemoValid()) {
                memoTitleContainer.error = getErrorMessage(viewModel.hasTitleError(), R.string.memo_title_empty_error)
                memoDescription.error = getErrorMessage(viewModel.hasTextError(), R.string.memo_text_empty_error)
                return
            }

            if (!viewModel.hasSelectedLocation()) {
                viewModel.saveMemo()
                return
            }

            if (needsNotificationsPermission()) {
                notificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                requestLocationPermissions()
            }
        }
    }

    private fun requestLocationPermissions() {
        if (hasForegroundLocation() && !needsBackgroundLocation()) {
            viewModel.saveMemo()
        } else if (hasForegroundLocation()) {
            backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            foregroundLauncher.launch(foregroundPermissionsToRequest())
        }
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
