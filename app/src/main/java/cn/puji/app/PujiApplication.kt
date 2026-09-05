package cn.puji.app

import android.app.Application
import cn.puji.app.data.DashboardRepository
import cn.puji.app.data.InventoryRepository
import cn.puji.app.data.ProductRepository
import cn.puji.app.data.PujiDatabase

class PujiApplication : Application() {
    val database by lazy { PujiDatabase(this) }
    val products by lazy { ProductRepository(database) }
    val inventory by lazy { InventoryRepository(database) }
    val dashboard by lazy { DashboardRepository(database) }
}

