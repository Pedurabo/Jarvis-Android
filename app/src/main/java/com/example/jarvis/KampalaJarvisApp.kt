package com.example.jarvis

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.jarvis.core.ai.SyncManager
import com.example.jarvis.skills.*
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class KampalaJarvisApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var skillRegistry: SkillRegistry

    @Inject
    lateinit var trafficSkill: TrafficSkill

    @Inject
    lateinit var medicationSkill: MedicationSkill

    @Inject
    lateinit var phoneAutomationSkill: PhoneAutomationSkill

    @Inject
    lateinit var marketNewsSkill: MarketNewsSkill

    @Inject
    lateinit var locationSkill: LocationSkill

    @Inject
    lateinit var weatherSkill: WeatherSkill

    @Inject
    lateinit var herbalIdSkill: HerbalIdSkill

    @Inject
    lateinit var kampalaTrafficSkill: KampalaTrafficSkill

    @Inject
    lateinit var umemePowerSkill: UmemePowerSkill

    @Inject
    lateinit var kampalaTransportSkill: KampalaTransportSkill

    @Inject
    lateinit var marketPriceSkill: MarketPriceSkill

    @Inject
    lateinit var kampalaWeatherSkill: KampalaWeatherSkill

    @Inject
    lateinit var firestore: FirebaseFirestore

    @Inject
    lateinit var syncManager: SyncManager

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Register skills
        registerSkills()

        verifyFirebase()

        // Initialize Sync Strategy
        // Schedule periodic sync for background maintenance
        syncManager.schedulePeriodicSync()
        // Run an immediate sync on app launch to get latest cloud data
        syncManager.scheduleImmediateSync()
    }

    private fun registerSkills() {
        skillRegistry.registerSkill(weatherSkill)
        skillRegistry.registerSkill(trafficSkill)
        skillRegistry.registerSkill(medicationSkill)
        skillRegistry.registerSkill(phoneAutomationSkill)
        skillRegistry.registerSkill(marketNewsSkill)
        skillRegistry.registerSkill(locationSkill)
        skillRegistry.registerSkill(herbalIdSkill)
        skillRegistry.registerSkill(kampalaTrafficSkill)
        skillRegistry.registerSkill(umemePowerSkill)
        skillRegistry.registerSkill(kampalaTransportSkill)
        skillRegistry.registerSkill(marketPriceSkill)
        skillRegistry.registerSkill(kampalaWeatherSkill)
    }

    private fun verifyFirebase() {
        val testData = hashMapOf(
            "app_name" to "Jarvis",
            "initialized_at" to System.currentTimeMillis(),
            "status" to "active"
        )

        firestore.collection("system_status")
            .document("init_test")
            .set(testData)
            .addOnSuccessListener {
                Log.d("FirebaseInit", "Firestore verification successful!")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseInit", "Firestore verification failed", e)
            }
    }
}
