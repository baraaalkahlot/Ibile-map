package com.ibile.core

import androidx.room.Room
import com.google.android.libraries.places.api.net.PlacesClient
import com.ibile.LocationSearchHandler
import com.ibile.MainActivity
import com.ibile.MainFragment
import com.ibile.UIStateHandler
import com.ibile.data.database.Database
import com.ibile.data.repositiories.MarkersRepository
import com.ibile.databinding.FragmentMainBinding
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    scope<MainFragment> {
        scoped { (binding: FragmentMainBinding) -> UIStateHandler(binding) }
    }
    scope<MainActivity> {
        scoped { (placesClient: PlacesClient) -> LocationSearchHandler(placesClient) }
    }
    single { Room.databaseBuilder(androidContext(), Database::class.java, "ibile-markers").build() }
    single { MarkersRepository(get<Database>().markerDao()) }
}
