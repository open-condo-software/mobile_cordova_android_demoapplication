package ai.doma.core.miniapps.data.db

import ai.doma.core.miniapps.data.db.entities.BeaconRegionEntity
import ai.doma.core.miniapps.data.db.entities.BeaconRegionType
import org.altbeacon.beacon.Identifier
import org.altbeacon.beacon.Region

fun BeaconRegionEntity.toModel(): Region {
    return Region(
        identifier,
        Identifier.parse(uuid),
        major?.let { Identifier.parse(it) },
        minor?.let { Identifier.parse(it) }
    )
}

fun Region.toEntity(
    type: BeaconRegionType
): BeaconRegionEntity {
    return BeaconRegionEntity(
        id = 0,
        identifier = uniqueId,
        uuid = getIdentifier(0)?.toString(),
        major = getIdentifier(1)?.toString(),
        minor = getIdentifier(2)?.toString(),
        type = type
    )
}