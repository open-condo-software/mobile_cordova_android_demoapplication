package ai.doma.core.miniapps.data.repositories

import ai.doma.core.miniapps.data.db.MiniappDB
import ai.doma.core.miniapps.data.db.entities.BeaconRegionEntity
import ai.doma.core.miniapps.data.db.entities.BeaconRegionType
import ai.doma.core.miniapps.data.db.toEntity
import ai.doma.core.miniapps.data.db.toModel
import ai.doma.miniappdemo.ext.logW
import org.altbeacon.beacon.Region
import javax.inject.Inject

class BeaconRegionRepository @Inject constructor(
    private val db: MiniappDB
) {

    private fun getRegionEntity(
        regionEntity: Region,
    ): BeaconRegionEntity? {
        return db.getBeaconRegionDAO().select(
            regionEntity.uniqueId,
            regionEntity.id1?.toString(),
            regionEntity.id2?.toString(),
            regionEntity.id3?.toString(),
        )
    }

    fun addRegionForMonitorion(region: Region, userId: String, residentId: String, miniappId: String) {
        val existedEntity = getRegionEntity(region)

        if (existedEntity != null) {
            logW { "can't add region because already existed entity with fields" }
            return
        }

        db.getBeaconRegionDAO().insert(
            region.toEntity(BeaconRegionType.MONITORING)
        )
    }

    fun addRegionForMonitorion(regionEntity: BeaconRegionEntity) {
        check(regionEntity.type == BeaconRegionType.MONITORING) {
            "adding regionEntity for monitoring with type equals ${BeaconRegionType.RANGING} unsupported"
        }

        db.getBeaconRegionDAO().insert(regionEntity)
    }

    fun addRegionForRanging(region: Region, userId: String, residentId: String, miniappId: String) {
        val existedEntity = getRegionEntity(region)

        if (existedEntity != null) {
            logW { "can't add region because already existed entity with fields" }
            return
        }

        db.getBeaconRegionDAO().insert(
            region.toEntity(BeaconRegionType.RANGING)
        )
    }

    fun addRegionForRanging(regionEntity: BeaconRegionEntity) {
        check(regionEntity.type == BeaconRegionType.RANGING) {
            "adding regionEntity for monitoring with type equals ${BeaconRegionType.MONITORING} unsupported"
        }

        db.getBeaconRegionDAO().insert(regionEntity)
    }

    fun getAllMonitoringRegions(): List<Region> {
        return db.getBeaconRegionDAO().selectAll()
            .filter { it.type == BeaconRegionType.MONITORING }
            .map { it.toModel() }
    }

    fun getAllMonitoringRegionEntities(): List<BeaconRegionEntity> {
        return db.getBeaconRegionDAO().selectAll()
            .filter { it.type == BeaconRegionType.MONITORING }
    }

    // TODO: warning usage?
    fun getMonitoringRegions(
        identifier: String,
    ): List<BeaconRegionEntity> {
        return db.getBeaconRegionDAO().select(
            identifier, BeaconRegionType.MONITORING
        )
    }

    // TODO: warning usage?
    fun getRangingRegions(
        identifier: String,
    ): List<BeaconRegionEntity> {
        return db.getBeaconRegionDAO().select(
            identifier, BeaconRegionType.RANGING
        )
    }

    fun getAllRangingRegions(): List<Region> {
        return db.getBeaconRegionDAO().selectAll()
            .filter { it.type == BeaconRegionType.RANGING }
            .map { it.toModel() }
    }

    fun getAllRangingRegionEntities(): List<BeaconRegionEntity> {
        return db.getBeaconRegionDAO().selectAll()
            .filter { it.type == BeaconRegionType.RANGING }
    }



    suspend fun addRegionForMonitoringSuspend(regionEntity: BeaconRegionEntity) {
        check(regionEntity.type == BeaconRegionType.MONITORING) {
            "adding regionEntity for monitoring with type equals ${BeaconRegionType.RANGING} unsupported"
        }

        db.getBeaconRegionDAO().insertSuspend(regionEntity)
    }

    suspend fun getAllMonitoringRegionsSuspend(): List<Region> {
        return db.getBeaconRegionDAO().selectAllSuspend()
            .filter { it.type == BeaconRegionType.MONITORING }
            .map { it.toModel() }
    }

    suspend fun getAllRangingRegionsSuspend(): List<Region> {
        return db.getBeaconRegionDAO().selectAllSuspend()
            .filter { it.type == BeaconRegionType.RANGING }
            .map { it.toModel() }
    }

    fun removeRegion(regionEntity: BeaconRegionEntity) {
        db.getBeaconRegionDAO().delete(regionEntity)
    }

//    fun removeRegionForRanging(region: Region) {
//        val id = region.uniqueId
//        val uuid = region.id1.toString()
//        val major = region.id2.toString()
//        val minor = region.id3.toString()
//
//        val regionEntity = db.getBeaconRegionDAO().select(
//            id, uuid, major, minor, BeaconRegionType.RANGING
//        ) ?: return
//        db.getBeaconRegionDAO().delete(regionEntity)
//    }

    fun updateOrCreateRegion(regionEntity: BeaconRegionEntity) {
        db.getBeaconRegionDAO().updateOrCreate(regionEntity)
    }

}