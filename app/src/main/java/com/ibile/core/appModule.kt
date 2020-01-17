package com.ibile.core

import androidx.room.Room
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
    single { Room.databaseBuilder(androidContext(), Database::class.java, "ibile-markers").build() }
    single { MarkersRepository(get<Database>().markerDao()) }
}
