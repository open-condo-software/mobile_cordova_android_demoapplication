package ai.doma.core.miniapps.data.dao

import ai.doma.core.miniapps.data.db.entities.BeaconRegionEntity
import ai.doma.core.miniapps.data.db.entities.BeaconRegionType
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.altbeacon.beacon.Region

@Dao
interface BeaconRegionDAO {

    @Query("""SELECT * FROM beacon_region_entity WHERE id = :id""")
    fun select(id: Long): BeaconRegionEntity?

    @Query("""SELECT * FROM beacon_region_entity WHERE identifier = :identifier AND type=:type""")
    fun select(identifier: String, type: BeaconRegionType): List<BeaconRegionEntity>

    @Query("""SELECT * FROM beacon_region_entity""")
    fun selectAll(): List<BeaconRegionEntity>

    @Query("""
        SELECT * FROM beacon_region_entity WHERE 
            identifier = :identifier AND
            uuid = :uuid AND
            major = :major AND
            minor = :minor
        """)
    fun select(
        identifier: String, uuid: String?, major: String?, minor: String?
    ): BeaconRegionEntity?

    @Insert
    fun insert(regionEntity: BeaconRegionEntity)

    @Insert
    fun insert(regionEntity: List<BeaconRegionEntity>)

    @Delete
    fun delete(region: BeaconRegionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun updateOrCreate(region: BeaconRegionEntity)





    @Query("""SELECT * FROM beacon_region_entity WHERE id = :id""")
    suspend fun selectSuspend(id: Long): BeaconRegionEntity?

    @Query("""SELECT * FROM beacon_region_entity WHERE uuid = :uuid""")
    suspend fun selectSuspend(uuid: String): BeaconRegionEntity?

    @Insert
    suspend fun insertSuspend(regionEntity: BeaconRegionEntity)

    @Insert
    suspend fun insertSuspend(regionEntity: List<BeaconRegionEntity>)


    @Query("""SELECT * FROM beacon_region_entity""")
    suspend fun selectAllSuspend(): List<BeaconRegionEntity>



}