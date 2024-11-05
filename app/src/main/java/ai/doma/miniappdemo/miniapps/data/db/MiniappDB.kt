package ai.doma.core.miniapps.data.db

import ai.doma.core.miniapps.data.dao.BeaconRegionDAO
import ai.doma.core.miniapps.data.db.entities.BeaconRegionEntity
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [BeaconRegionEntity::class],
    version = 4,
)
@TypeConverters(Converters::class)
abstract class MiniappDB: RoomDatabase() {
    abstract fun getBeaconRegionDAO(): BeaconRegionDAO
}