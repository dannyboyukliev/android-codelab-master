package com.sap.codelab.view.detail

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import com.sap.codelab.databinding.ActivityViewMemoBinding
import com.sap.codelab.model.Memo
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker

internal const val BUNDLE_MEMO_ID: String = "memoId"

/**
 * Activity that allows a user to see the details of a memo.
 */
@AndroidEntryPoint
internal class ViewMemo : AppCompatActivity() {

    private lateinit var binding: ActivityViewMemoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = packageName
        binding = ActivityViewMemoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        // Initialize views with the passed memo id
        val model = ViewModelProvider(this)[ViewMemoViewModel::class.java]
        if (savedInstanceState == null) {
            // Observe the memo state flow for changes
            lifecycleScope.launch {
                model.memo.collect { value ->
                    value?.let { memo ->
                        // Update the UI whenever the memo changes
                        updateUI(memo)
                    }
                }
            }
            val id = intent.getLongExtra(BUNDLE_MEMO_ID, -1)
            model.loadMemo(id)
        }
    }

    /**
     * Updates the UI with the given memo details.
     *
     * @param memo - the memo whose details are to be displayed.
     */
    private fun updateUI(memo: Memo) {
        binding.contentCreateMemo.run {
            memoTitle.setText(memo.title)
            memoDescription.setText(memo.description)
            memoTitle.isEnabled = false
            memoDescription.isEnabled = false

            val hasLocation = memo.reminderLatitude != 0.0 || memo.reminderLongitude != 0.0
            if (hasLocation) {
                map.visibility = View.VISIBLE
                setupMap(GeoPoint(memo.reminderLatitude, memo.reminderLongitude))
            } else {
                map.visibility = View.GONE
            }
        }
    }

    private fun setupMap(point: GeoPoint) {
        binding.contentCreateMemo.map.run {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(false)
            controller.setZoom(15.0)
            controller.setCenter(point)
            val marker = Marker(this)
            marker.position = point
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            overlays.add(marker)
        }
    }
}
