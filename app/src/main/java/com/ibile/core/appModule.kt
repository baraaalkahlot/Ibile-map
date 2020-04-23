package com.ibile.core

import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.firebase.auth.FirebaseAuth
import com.ibile.data.SharedPref
import com.ibile.data.database.Database
import com.ibile.data.repositiories.*
import com.ibile.features.main.addpolygonpoi.AddPolygonPoiViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { Database.build(androidContext()) }
    single { MarkersRepository(get<Database>().markerDao()) }
    single { FoldersRepository(get<Database>().foldersDao(), get<Database>().foldersWithMarkersDao()) }

    single { Places.createClient(androidContext()) }
    single { LocationServices.getFusedLocationProviderClient(androidContext()) }
    single { (androidContext() as Application).iconPack }
    single { ImageRepository(androidApplication()) }
    viewModel { AddPolygonPoiViewModel(androidContext(), get()) }
    single { AuthRepository(FirebaseAuth.getInstance()) }
    single { SharedPref(androidContext()) }
    single { BillingRepository(androidContext(), get()) }
}
