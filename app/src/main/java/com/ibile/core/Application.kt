package com.ibile.core

import android.app.Application
import com.airbnb.mvrx.mock.MvRxMocks
import com.google.android.libraries.places.api.Places
import com.ibile.R
import com.maltaisn.icondialog.pack.IconPack
import com.maltaisn.icondialog.pack.IconPackLoader
import com.maltaisn.iconpack.defaultpack.createDefaultIconPack
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class Application : Application() {
    val iconPack: IconPack by lazy { loadIconPack() }

    override fun onCreate() {
        super.onCreate()
        MvRxMocks.install(applicationContext)
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.google_maps_api_key))
        }
        startKoin {
            androidLogger()
            androidContext(this@Application)
            modules(appModule)
        }
    }

    private fun loadIconPack(): IconPack {
        val loader = IconPackLoader(this)
        val defaultIconPack = createDefaultIconPack(loader)
        val pack = loader.load(R.xml.extra_map_icons, 0, defaultIconPack.locales, defaultIconPack)

        val parent = pack.parent!!
        val familyPeopleCategory = parent.categories[0]!!
        val buildingPlacesCategory = parent.categories[7]!!
        parent.categories[0] = buildingPlacesCategory
        parent.categories[7] = familyPeopleCategory
        val icons = parent.icons
        icons.forEach { (key, icon) ->
            if (icon.categoryId == 0 || icon.categoryId == 7) {
                val newCategoryId: Int = if (icon.categoryId == 0) 7 else 0
                val iconModified = icon.copy(categoryId = newCategoryId)
                icons[key] = iconModified
            }
        }

        pack.loadDrawables()

        return pack
    }
}
