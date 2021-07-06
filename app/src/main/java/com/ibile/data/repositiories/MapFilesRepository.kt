package com.ibile.data.repositiories

import android.content.Context
import android.text.format.DateFormat
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ibile.core.currentContext
import com.ibile.data.SharedPref
import com.ibile.features.auth.AuthFragment
import com.ibile.utils.extensions.restartApp
import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.Completable
import io.reactivex.Observable
import java.io.File
import java.util.*

class MapFilesRepository(
    val context: Context,
    private val sharedPref: SharedPref,
    private val gson: Gson = Gson()
) {

    private val _mapFiles: BehaviorRelay<List<MapFile>> = BehaviorRelay.create()
    private val mapsDataJsonFile: File

        get() = File(context.filesDir, MAPS_DETAILS_FILE_NAME)

    init {
        Log.d("wasd", "init: ")
        _mapFiles.accept(initializeMapFiles(mapsDataJsonFile))
    }


    private fun initializeMapFiles(jsonFile: File): List<MapFile> = if (jsonFile.createNewFile()) {
        val name = "Map - ${DateFormat.format("dd/MM/yy hh:mm", Calendar.getInstance())}"
        val defaultMap = MapFile(name, DEFAULT_DB_NAME)
        sharedPref.currentMapFileId = defaultMap.id
        mapsDataJsonFile.writer().use { gson.toJson(listOf(defaultMap), MAP_FILES_TYPE_TOKEN, it) }

        Log.d(
            "wasd",
            "initializeMapFiles: writer -> name =  ${defaultMap.name}  , id = ${defaultMap.id}  db name = ${defaultMap.dbName}"
        )
        listOf(defaultMap)
    } else {
        val storedMapList =
            mapsDataJsonFile.reader().use { gson.fromJson<List<MapFile>>(it, MAP_FILES_TYPE_TOKEN) }

        for (i in storedMapList) {
            Log.d(
                "wasd",
                "initializeMapFiles: reader -> name =  ${i.name}  , id = ${i.id}  db name = ${i.dbName}"
            )
        }
        storedMapList
    }


    fun getMapFiles(): Observable<List<MapFile>> = _mapFiles.hide()

    fun updateMapFile(mapFile: MapFile) = Completable.create { emitter ->
        val updatedMapFiles = _mapFiles.value!!.map { if (it.id == mapFile.id) mapFile else it }
        updateMapFiles(updatedMapFiles)
        emitter.onComplete()
    }


    fun addNewMapFile(mapFile: MapFile) = Completable.create { emitter ->
        val updatedMapFiles = _mapFiles.value!! + listOf(mapFile)
        updateMapFiles(updatedMapFiles)
        emitter.onComplete()
    }

    /**
     *
     * TODO: maybe delete storage files (e.g. marker images) associated with map file
     * @param mapFile
     */
    fun deleteMapFile(mapFile: MapFile) = Completable.create { emitter ->
        if (mapFile.id == sharedPref.currentMapFileId) throw RuntimeException("Cannot delete current map file")
        context.deleteDatabase(mapFile.dbName)

        val updatedMapFiles = _mapFiles.value!!.filter { it.id != mapFile.id }
        updateMapFiles(updatedMapFiles)
        emitter.onComplete()
    }

    fun updateMapFiles(updatedList: List<MapFile>) {
        mapsDataJsonFile.writer().use { gson.toJson(updatedList, MAP_FILES_TYPE_TOKEN, it) }
        val updatedResult =
            mapsDataJsonFile.reader().use { gson.fromJson<List<MapFile>>(it, MAP_FILES_TYPE_TOKEN) }
        _mapFiles.accept(updatedResult)
    }

    fun myUpdateMapFiles(updatedList: List<MapFile>, parent: AuthFragment) {
        try {
            Log.d("wasd", "updateMapFiles: start")
            sharedPref.currentMapFileId = updatedList[0].id
            mapsDataJsonFile.writer().use { gson.toJson(updatedList, MAP_FILES_TYPE_TOKEN, it) }
            val updatedResult =
                mapsDataJsonFile.reader()
                    .use { gson.fromJson<List<MapFile>>(it, MAP_FILES_TYPE_TOKEN) }
            _mapFiles.accept(updatedResult)
        } catch (e: IndexOutOfBoundsException) {
            e.printStackTrace()
        }finally {
            parent.currentContext.restartApp(parent.requireActivity()::class)
        }
    }

    companion object {
        private const val MAPS_DETAILS_FILE_NAME = "com_ibile_maps.json"
        private const val DEFAULT_DB_NAME = "ibile-markers"
        private val MAP_FILES_TYPE_TOKEN = object : TypeToken<List<MapFile>>() {}.type
    }


}

data class MapFile(
    val name: String = "",
    val dbName: String = "",
    val id: String = UUID.randomUUID().toString()
)
