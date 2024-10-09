package ai.doma.core.miniapps.services

import ai.doma.core.DI.CoreComponent
import ai.doma.core.DI.InjectHelper
import ai.doma.core.miniapps.data.db.entities.BeaconRegionEntity
import ai.doma.core.miniapps.data.db.entities.BeaconRegionType
import ai.doma.core.miniapps.data.db.toEntity
import ai.doma.core.miniapps.data.repositories.BeaconRegionRepository
import ai.doma.miniappdemo.ext.logD
import ai.doma.miniappdemo.ext.logE
import ai.doma.miniappdemo.ext.logW
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconConsumer
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.BeaconParser
import org.altbeacon.beacon.MonitorNotifier
import org.altbeacon.beacon.RangeNotifier
import org.altbeacon.beacon.Region
import org.altbeacon.beacon.logging.LogManager
import org.altbeacon.beacon.logging.Logger
import org.altbeacon.beacon.powersave.BackgroundPowerSaver
import org.altbeacon.beacon.service.RangedBeacon
import org.altbeacon.beacon.service.RunningAverageRssiFilter


private const val DEFAULT_FOREGROUND_SCAN_PERIOD: Int = 1100
private const val DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD = 0
private const val DEFAULT_SAMPLE_EXPIRATION_MILLISECOND = 20000

@SuppressLint("StaticFieldLeak")
object BeaconScanner {

    @Volatile
//    var activeMiniappId: String? = null
    private lateinit var coreComponent: CoreComponent
    private val scope = CoroutineScope(Dispatchers.IO)
    private var repository: BeaconRegionRepository? = null
    private var context: Context? = null
    private var backgroundPowerSaver: BackgroundPowerSaver? = null
    var iBeaconManager: BeaconManager? = null
        private set

    private var outerMonitorNotifier: MonitorNotifier? = null
    private var outerRangeNotifier: RangeNotifier? = null

    private var beaconNotifier: BeaconNotifier? = null


    fun init(
        repository: BeaconRegionRepository,
        context: Context
    ) {
        beaconNotifier = BeaconNotifier(context)

        logD("BeaconWorker") { "emitter initing start" }
        coreComponent = InjectHelper.provideCoreComponent(context)
        this.repository = repository
        this.context = context.applicationContext
        iBeaconManager = BeaconManager.getInstanceForApplication(context.applicationContext)

        iBeaconManager!!.foregroundBetweenScanPeriod = DEFAULT_FOREGROUND_BETWEEN_SCAN_PERIOD.toLong()
        iBeaconManager!!.foregroundScanPeriod = DEFAULT_FOREGROUND_SCAN_PERIOD.toLong()

        if (iBeaconManager?.isAnyConsumerBound != true) {
            logD("BeaconWorker") { "configuring..." }
            iBeaconManager?.setEnableScheduledScanJobs(false)
            iBeaconManager?.setBackgroundBetweenScanPeriod(0);
            iBeaconManager?.setBackgroundScanPeriod(1100);
            iBeaconManager?.setIntentScanningStrategyEnabled(true)
        }

        BeaconManager.setRssiFilterImplClass(RunningAverageRssiFilter::class.java)
        RunningAverageRssiFilter.setSampleExpirationMilliseconds(
            DEFAULT_SAMPLE_EXPIRATION_MILLISECOND.toLong()
        )
        RangedBeacon.setSampleExpirationMilliseconds(DEFAULT_SAMPLE_EXPIRATION_MILLISECOND.toLong())
        initLocationManager()
        initMonitorNotifier()
        initRangeNotifier()

//        iBeaconManager?.bindInternal(this)

        logD("BeaconWorker") { "emitter initing finish" }

        LogManager.setLogger(object : Logger {
            override fun v(tag: String?, message: String?, vararg args: Any?) {
                logD("BeaconEmitterLogger") { "$tag : $message : ${args.joinToString()}" }
            }

            override fun v(t: Throwable?, tag: String?, message: String?, vararg args: Any?) {
                logD("BeaconEmitterLogger") { "$tag : $message : ${args.joinToString()}" }
            }

            override fun d(tag: String?, message: String?, vararg args: Any?) {
                logD("BeaconEmitterLogger") { "$tag : $message : ${args.joinToString()}" }
            }

            override fun d(t: Throwable?, tag: String?, message: String?, vararg args: Any?) {
                logD("BeaconEmitterLogger") { "$tag : $message : ${args.joinToString()}" }
            }

            override fun i(tag: String?, message: String?, vararg args: Any?) {
                logD("BeaconEmitterLogger") { "$tag : $message : ${args.joinToString()}" }
            }

            override fun i(t: Throwable?, tag: String?, message: String?, vararg args: Any?) {
                logD("BeaconEmitterLogger") { "$tag : $message : ${args.joinToString()}" }
            }

            override fun w(tag: String?, message: String?, vararg args: Any?) {
                logW("BeaconEmitterLogger") { "$tag : $message : ${args.joinToString()}" }
            }

            override fun w(t: Throwable?, tag: String?, message: String?, vararg args: Any?) {
                logW("BeaconEmitterLogger") { "$tag : $message : ${args.joinToString()}" }
            }

            override fun e(tag: String?, message: String?, vararg args: Any?) {
                logE("BeaconEmitterLogger") { "$tag : $message : ${args.joinToString()}" }
            }

            override fun e(t: Throwable?, tag: String?, message: String?, vararg args: Any?) {
                logE("BeaconEmitter") { "$tag : $message : ${args.joinToString()}" }
            }
        })

//        context.bindService(Intent(context, BeaconBinder::class.java), object : ServiceConnection {
//            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
//                logD("BeaconEmitter") { "Connected" }
//                (service as? BeaconBinder)?.let { beaconBinder ->
//                    logD("BeaconEmitter") { "get binder with service: ${beaconBinder.service}" }
//                }
//
//            }
//
//            override fun onServiceDisconnected(name: ComponentName?) {
////                TODO("Not yet implemented")
//            }
//
//        }, Context.BIND_AUTO_CREATE)
//        iBeaconManager?.backgroundMode = true
    }

    @JvmStatic
    fun startMonitoring(region: Region) {
        val repository = repository ?: return

        val currRegions = repository.getMonitoringRegions(
            region.uniqueId
        )

        region.mapToMonitoringEntity()?.let {
            if (currRegions.isNotEmpty()) {
                repository.updateOrCreateRegion(it.copy(id = currRegions.first().id))
            } else {
                repository.addRegionForMonitorion(it)
            }
        }

        iBeaconManager?.startMonitoring(region)
    }

    @JvmStatic
    fun stopMonitoring(region: Region) {
        region.mapToMonitoringEntity()?.let {
            repository?.getMonitoringRegions(
                it.identifier
            )?.forEach { entity ->
                repository?.removeRegion(entity)
            }
        }
        iBeaconManager?.stopMonitoring(region)
    }

    @JvmStatic
    fun startRangingBeacons(region: Region, minAccuracyValue: Double) {
        val repository = repository ?: return

        val currRegions = repository.getRangingRegions(region.uniqueId)

        region.mapToRangingEntity(minAccuracyValue)?.let {
            if (currRegions.isNotEmpty()) {
                repository.updateOrCreateRegion(it.copy(id = currRegions.first().id))
            } else {
                repository.addRegionForRanging(it)
            }
        }

        iBeaconManager?.startRangingBeacons(region)
    }

    @JvmStatic
    fun stopRangingBeacons(region: Region) {
        region.mapToRangingEntity()?.let {
            repository?.getRangingRegions(
                it.identifier
            )?.forEach { entity ->
                repository?.removeRegion(entity)
            }
        }

        iBeaconManager?.stopRangingBeacons(region)
    }

    @JvmStatic
    fun registerMonitorNotifier(outerMonitorNotifier: MonitorNotifier) {
//        activeMiniappId?.let {
            this.outerMonitorNotifier = outerMonitorNotifier
//        }
    }

    @JvmStatic
    fun registerRangeNotifier(outerRangeNotifier: RangeNotifier) {
//        activeMiniappId?.let {
            this.outerRangeNotifier = outerRangeNotifier
//        }
    }

    @JvmStatic
    fun clearNotifiers() {
        outerMonitorNotifier = null
        outerRangeNotifier = null
    }

    val consumer = object : BeaconConsumer {
        override fun onBeaconServiceConnect() {
            logD("BeaconWorker") { "> onBeaconServiceConnect" }
        }

        override fun getApplicationContext(): Context {
            logD("BeaconWorker") { "> getApplicationContext" }
            val context = context?.applicationContext ?: run {
                throw Exception("FUCK!")
            }

            return context
        }

        override fun bindService(intent: Intent, connection: ServiceConnection, mode: Int): Boolean {
            logD("BeaconWorker") { "> bindService" }
            return context?.bindService(intent, connection, mode) ?: false
        }

        override fun unbindService(connection: ServiceConnection) {
            logD("BeaconWorker") { "> unbindService" }
            context?.unbindService(connection)
        }
    }
    private fun initLocationManager() {
        iBeaconManager!!.beaconParsers.add(BeaconParser().setBeaconLayout("m:0-3=4c000215,i:4-19,i:20-21,i:22-23,p:24-24"))
        iBeaconManager!!.beaconParsers.add(BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"))
        iBeaconManager!!.bind(consumer)
    }

    private fun initMonitorNotifier() {
        iBeaconManager?.addMonitorNotifier(object : MonitorNotifier {
            override fun didEnterRegion(region: Region) {
                logD("BeaconEmitter") { "didEnterRegion: $region <<<<" }
                if (activeMiniappId == outerMonitorNotifier?.first) {
                    outerMonitorNotifier?.didEnterRegion(region)
                } else {
                    scope.launch(Dispatchers.IO) {
                        getMonitoringEntity(region)?.let { regionEntity ->
                            beaconNotifier?.didEnterRegion(regionEntity)
                        }
                    }
                }
            }

            override fun didExitRegion(region: Region) {
                logD("BeaconEmitter") { "didExitRegion: $region <<<<" }
                if (activeMiniappId != null && activeMiniappId == outerMonitorNotifier?.first) {
                    outerMonitorNotifier?.second?.didExitRegion(region)
                } else {
                    scope.launch(Dispatchers.IO) {
                        getMonitoringEntity(region)?.let { regionEntity ->
                            beaconNotifier?.didExitRegion(regionEntity)
                        }
                    }
                }

            }

            override fun didDetermineStateForRegion(state: Int, region: Region) {
                logD("BeaconEmitter") { "didDetermineStateForRegion: $state $region <<<<" }
                if (activeMiniappId == outerMonitorNotifier?.first) {
                    outerMonitorNotifier?.second?.didDetermineStateForRegion(state, region)
                }
            }
        })
    }

    private fun initRangeNotifier() {
        iBeaconManager?.addRangeNotifier(object : RangeNotifier {
            override fun didRangeBeaconsInRegion(
                beacons: MutableCollection<Beacon>?,
                region: Region
            ) {
                logD("BeaconEmitter") { "didRangeBeaconsInRegion: $region ${beacons?.joinToString { it.toString() + " (${
                    Math.round(it.distance * 100.0) / 100.0})" }} <<<<" }
                if (activeMiniappId != null && activeMiniappId == outerRangeNotifier?.first) {
                    outerRangeNotifier?.second?.didRangeBeaconsInRegion(beacons, region)
                } else {
                    scope.launch(Dispatchers.IO) {
                        getRangingEntity(region)?.let {
                            beaconNotifier?.didRangeBeaconsInRegion(it, beacons.orEmpty())
                        }
                    }
                }
            }
        })
    }


    private fun Region.mapToMonitoringEntity(): BeaconRegionEntity? =
        mapToEntity(BeaconRegionType.MONITORING)

    private fun Region.mapToRangingEntity(minAccuracyValue: Double = 1.0): BeaconRegionEntity? =
        mapToEntity(BeaconRegionType.RANGING)?.copy(minAccuracyValue = minAccuracyValue)

    private fun Region.mapToEntity(type: BeaconRegionType): BeaconRegionEntity? {
        return toEntity(type)
    }

    private fun getMonitoringEntity(region: Region): BeaconRegionEntity? {

        return repository?.getAllMonitoringRegionEntities()?.filter {
            it.identifier == region.uniqueId
                    && it.uuid == region.id1?.toString()
                    && it.major == region.id2?.toString()
                    && it.minor == region.id3?.toString()
        }?.firstOrNull()
    }

    private fun getRangingEntity(region: Region): BeaconRegionEntity? {
        return repository?.getAllRangingRegionEntities()?.filter {
            it.identifier == region.uniqueId
                    && it.uuid == region.id1?.toString()
                    && it.major == region.id2?.toString()
                    && it.minor == region.id3?.toString()
        }?.firstOrNull()
    }

    fun runEmitting() = scope.async(Dispatchers.Main) {
        logD("BeaconWorker") { "runEmitting start 1" }
        logD("BeaconWorker") { "runEmitting start 2" }
        val repository = repository ?: return@async
        logD("BeaconWorker") { "runEmitting start 3" }
        val iBeaconManager = iBeaconManager ?: return@async
        logD("BeaconWorker") { "runEmitting start 4" }

        val monitoringRegions = repository.getAllMonitoringRegionsSuspend().filter {
            iBeaconManager.monitoredRegions.none { monitored ->
                monitored.uniqueId == it.uniqueId
                        && monitored.id1 == it.id1
                        && monitored.id2 == it.id2
                        && monitored.id3 == it.id3
            }
        }

        logD("BeaconWorker") { "runEmitting start 5" }

        val rangingRegions = repository.getAllRangingRegionsSuspend().filter {
            iBeaconManager.rangedRegions.none { ranging ->
                ranging.uniqueId == it.uniqueId
                        && ranging.id1 == it.id1
                        && ranging.id2 == it.id2
                        && ranging.id3 == it.id3
            }
        }

        logD("BeaconWorker") { "monitoringRegions: [${monitoringRegions.joinToString()}]" }
        logD("BeaconWorker") { "rangingRegions: [${rangingRegions.joinToString()}]" }

        monitoringRegions.forEach(iBeaconManager::startMonitoring)
        rangingRegions.forEach(iBeaconManager::startRangingBeacons)
    }

    fun stopScanning() {
        iBeaconManager?.monitoredRegions?.forEach(iBeaconManager!!::stopMonitoring)
        iBeaconManager?.rangedRegions?.forEach(iBeaconManager!!::stopRangingBeacons)
        iBeaconManager?.unbind(consumer)
    }

}
