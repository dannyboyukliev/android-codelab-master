package com.sap.codelab.view.home

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.coroutineScope
import dagger.hilt.android.AndroidEntryPoint
import com.sap.codelab.R
import com.sap.codelab.databinding.ActivityHomeBinding
import com.sap.codelab.model.Memo
import com.sap.codelab.notification.NotificationHelper
import com.sap.codelab.view.create.CreateMemo
import com.sap.codelab.view.detail.BUNDLE_MEMO_ID
import com.sap.codelab.view.detail.ViewMemo
import kotlinx.coroutines.launch

/**
 * The main activity of the app. Shows a list of recorded memos and lets the user add new memos.
 */
@AndroidEntryPoint
internal class Home : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var viewModel: HomeViewModel
    private lateinit var menuItemShowAll: MenuItem
    private lateinit var menuItemShowOpen: MenuItem
    private val createMemoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            viewModel.refreshMemos()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        viewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        // Setup the adapter and the recycler view
        setupRecyclerView(initializeAdapter())

        binding.fab.setOnClickListener {
            // Handles clicks on the FAB button > creates a new Memo
            createMemoLauncher.launch(Intent(this@Home, CreateMemo::class.java))
        }
        binding.contentHome.btnTestNotification.setOnClickListener {
            val fakeMemo = Memo(
                id = 1L,
                title = "Test Memo",
                description = "This is a test notification body that might be longer than 140 characters to verify truncation works correctly in the status bar.",
                reminderDate = 0L,
                reminderLatitude = 0.0,
                reminderLongitude = 0.0
            )
            NotificationHelper(this).notify(fakeMemo)
        }
        viewModel.loadOpenMemos()
    }

    /**
     * Initializes the adapter and sets the needed callbacks.
     */
    private fun initializeAdapter() : MemoAdapter {
        val adapter = MemoAdapter(mutableListOf(), { view ->
            // Implementation for when the user selects a row to show the detail view
            showMemo((view.tag as Memo).id)
        }, { checkbox, isChecked ->
            // Implementation for when the user marks a memo as completed
            viewModel.updateMemo(checkbox.tag as Memo, isChecked)
            viewModel.refreshMemos()
        })
        lifecycle.coroutineScope.launch {
            viewModel.memos.collect { memos ->
                adapter.setItems(memos)
            }
        }
        return adapter
    }

    /**
     * Opens the Memo detail view for the given memoId.
     *
     * @param memoId    - the id of the memo to be shown.
     */
    private fun showMemo(memoId: Long) {
        val intent = Intent(this@Home, ViewMemo::class.java)
        intent.putExtra(BUNDLE_MEMO_ID, memoId)
        startActivity(intent)
    }

    /**
     * Initializes the recycler view to display the list of memos.
     */
    private fun setupRecyclerView(adapter: MemoAdapter) {
        binding.contentHome.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@Home, LinearLayoutManager.VERTICAL, false)
            this.adapter = adapter
            addItemDecoration(DividerItemDecoration(this@Home, (layoutManager as LinearLayoutManager).orientation))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)
        menuItemShowAll = menu.findItem(R.id.action_show_all)
        menuItemShowOpen = menu.findItem(R.id.action_show_open)
        return true
    }

    /**
     * Handles actionbar interactions.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_show_all -> {
                viewModel.loadAllMemos()
                //Switch available menu options
                menuItemShowAll.isVisible = false
                menuItemShowOpen.isVisible = true
                true
            }
            R.id.action_show_open -> {
                viewModel.loadOpenMemos()
                //Switch available menu options
                menuItemShowOpen.isVisible = false
                menuItemShowAll.isVisible = true
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}
