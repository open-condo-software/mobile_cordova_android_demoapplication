package ai.doma.core.miniapps.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "beacon_region_entity")
data class BeaconRegionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val identifier: String,
    val uuid: String?,
    val major: String?,
    val minor: String?,
    val type: BeaconRegionType,
    val minAccuracyValue: Double = 1.0
)
