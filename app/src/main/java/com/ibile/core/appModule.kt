package com.ibile.core

import androidx.room.Room
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.ibile.AddShapeViewModel
import com.ibile.data.database.Database
import com.ibile.data.repositiories.MarkersRepository
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { Room.databaseBuilder(androidContext(), Database::class.java, "ibile-markers").build() }
    single { MarkersRepository(get<Database>().markerDao()) }
    viewModel { AddShapeViewModel(androidContext()) }
    single { Places.createClient(androidContext()) }
    single { LocationServices.getFusedLocationProviderClient(androidContext()) }
    single { (androidContext() as Application).iconPack }
}
