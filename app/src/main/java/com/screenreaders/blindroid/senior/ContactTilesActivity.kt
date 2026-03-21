package com.screenreaders.blindroid.senior

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.screenreaders.blindroid.R

class ContactTilesActivity : AppCompatActivity() {
    private lateinit var layoutSpinner: Spinner
    private lateinit var tilesGrid: RecyclerView
    private lateinit var adapter: ContactTileAdapter
    private var tiles: MutableList<ContactTile?> = mutableListOf()
    private var pendingIndex: Int = -1
    private val pickContactLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) return@registerForActivityResult
        val uri = result.data?.data ?: return@registerForActivityResult
        handleContactPicked(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact_tiles)

        layoutSpinner = findViewById(R.id.tilesLayoutSpinner)
        tilesGrid = findViewById(R.id.tilesGrid)

        val presets = listOf(
            PresetItem(0, getString(R.string.contact_tiles_layout_3), 3, 1),
            PresetItem(1, getString(R.string.contact_tiles_layout_6), 6, 2),
            PresetItem(2, getString(R.string.contact_tiles_layout_8), 8, 2),
            PresetItem(3, getString(R.string.contact_tiles_layout_15), 15, 3)
        )
        val presetLabels = presets.map { it.label }
        val presetAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, presetLabels)
        presetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        layoutSpinner.adapter = presetAdapter
        val presetIndex = ContactTileStore.getLayoutPreset(this).coerceIn(0, presets.lastIndex)
        layoutSpinner.setSelection(presetIndex, false)

        adapter = ContactTileAdapter(tiles, ::handleTileClick, ::handleTileLongClick)
        tilesGrid.adapter = adapter
        tilesGrid.layoutManager = GridLayoutManager(this, presets[presetIndex].columns)
        tiles = ContactTileStore.normalizedTiles(this, presets[presetIndex].count)
        adapter.submit(tiles)

        layoutSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>?,
                view: android.view.View?,
                position: Int,
                id: Long
            ) {
                val preset = presets.getOrElse(position) { presets[0] }
                ContactTileStore.setLayoutPreset(this@ContactTilesActivity, preset.id)
                tilesGrid.layoutManager = GridLayoutManager(this@ContactTilesActivity, preset.columns)
                tiles = ContactTileStore.normalizedTiles(this@ContactTilesActivity, preset.count)
                adapter.submit(tiles)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }
    }

    private fun handleTileClick(index: Int) {
        val tile = tiles.getOrNull(index)
        if (tile == null) {
            pickContact(index)
            return
        }
        callNumber(tile.phone)
    }

    private fun handleTileLongClick(index: Int) {
        val tile = tiles.getOrNull(index)
        if (tile == null) {
            pickContact(index)
            return
        }
        AlertDialog.Builder(this)
            .setTitle(tile.name.ifBlank { tile.phone })
            .setItems(
                arrayOf(
                    getString(R.string.contact_tile_edit),
                    getString(R.string.contact_tile_remove)
                )
            ) { _, which ->
                when (which) {
                    0 -> pickContact(index)
                    1 -> {
                        tiles[index] = null
                        ContactTileStore.saveTiles(this, tiles)
                        adapter.submit(tiles)
                    }
                }
            }
            .show()
    }

    private fun pickContact(index: Int) {
        if (!hasPermission(Manifest.permission.READ_CONTACTS)) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS), REQ_CONTACTS)
            pendingIndex = index
            return
        }
        pendingIndex = index
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
        pickContactLauncher.launch(intent)
    }

    private fun callNumber(number: String) {
        if (number.isBlank()) return
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))
        if (hasPermission(Manifest.permission.CALL_PHONE)) {
            try {
                startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(this, R.string.contact_tile_call_failed, Toast.LENGTH_SHORT).show()
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), REQ_CALL)
        }
    }

    private fun handleContactPicked(uri: Uri) {
        val cursor = contentResolver.query(
            uri,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null,
            null,
            null
        ) ?: return
        cursor.use {
            if (it.moveToFirst()) {
                val name = it.getString(0) ?: ""
                val phone = it.getString(1) ?: ""
                if (pendingIndex in tiles.indices) {
                    tiles[pendingIndex] = ContactTile(name, phone)
                    ContactTileStore.saveTiles(this, tiles)
                    adapter.submit(tiles)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_CONTACTS) {
            if (grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
                if (pendingIndex >= 0) pickContact(pendingIndex)
            } else {
                Toast.makeText(this, R.string.contact_tile_permission, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    private data class PresetItem(val id: Int, val label: String, val count: Int, val columns: Int)

    companion object {
        private const val REQ_CONTACTS = 1102
        private const val REQ_CALL = 1103
    }
}
