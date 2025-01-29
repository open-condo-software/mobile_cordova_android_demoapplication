package ai.doma.core.miniapps.data.db

import ai.doma.core.miniapps.data.db.entities.BeaconRegionEntity
import ai.doma.core.miniapps.data.db.entities.BeaconRegionType
import androidx.room.TypeConverter
import org.altbeacon.beacon.Identifier
import org.altbeacon.beacon.Region

class Converters {

    @TypeConverter
    fun convert(type: BeaconRegionType): String {
        return type.name
    }

    @TypeConverter
    fun convert(str: String?): BeaconRegionType? {
        return str?.let { BeaconRegionType.valueOf(it) }
    }

}
