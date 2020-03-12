package com.ibile.core

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.ibile.data.database.Database
import com.ibile.data.repositiories.FoldersRepository
import com.ibile.data.repositiories.ImageRepository
import com.ibile.data.repositiories.MarkersRepository
import com.ibile.features.main.addpolygonpoi.AddPolygonPoiViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single {
        Room.databaseBuilder(androidContext(), Database::class.java, "ibile-markers")
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // create the default folder that all markers get added to with the default values
                    db.execSQL("INSERT into folders VALUES (\"Default folder\", 1, 3000, -3394005, 1)")
                }
            })
            .build()
    }
    single { MarkersRepository(get<Database>().markerDao()) }
    single { FoldersRepository(get<Database>().foldersDao()) }
    single { Places.createClient(androidContext()) }
    single { LocationServices.getFusedLocationProviderClient(androidContext()) }
    single { (androidContext() as Application).iconPack }
    single { ImageRepository(androidApplication()) }
    viewModel { AddPolygonPoiViewModel(androidContext(), get()) }
}
