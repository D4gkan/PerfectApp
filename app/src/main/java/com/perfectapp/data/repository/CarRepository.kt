package com.perfectapp.data.repository

import com.perfectapp.data.PerfectDatabase
import com.perfectapp.data.entities.CarEntity
import com.perfectapp.data.entities.MaintenanceEntity
import com.perfectapp.data.entities.OdometerEntryEntity
import com.perfectapp.data.entities.FuelEntryEntity
import com.perfectapp.data.entities.TransactionEntity
import com.perfectapp.data.entities.TransactionType
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class CarRepository(private val db: PerfectDatabase) {
    private val carDao = db.carDao()
    private val maintenanceDao = db.maintenanceDao()
    private val fuelDao = db.fuelDao()
    private val wealthDao = db.wealthDao()

    val primaryCar: Flow<CarEntity?> = carDao.observePrimaryCar()

    suspend fun upsertCar(car: CarEntity): Long = carDao.upsertCar(car)

    /** Adds a new odometer reading and keeps the car's `currentOdometerKm` in sync,
     *  so every other screen (Home dashboard, maintenance countdowns) reflects it immediately. */
    suspend fun addOdometerEntry(car: CarEntity, odometerKm: Long, date: LocalDate = LocalDate.now()) {
        carDao.insertOdometerEntry(OdometerEntryEntity(carId = car.id, date = date, odometerKm = odometerKm))
        if (odometerKm > car.currentOdometerKm) {
            carDao.upsertCar(car.copy(currentOdometerKm = odometerKm))
        }
    }

    fun odometerHistory(carId: Long): Flow<List<OdometerEntryEntity>> = carDao.observeOdometerHistory(carId)
    fun fuelForCar(carId: Long): Flow<List<FuelEntryEntity>> = fuelDao.observeForCar(carId)
    /**
     * Saves a fill-up and, when it was paid from an in-app asset, creates its matching
     * Wealth expense in the same database transaction.  A failed balance update therefore
     * never leaves a fuel record without its financial counterpart.
     */
    suspend fun addFuel(entry: FuelEntryEntity, fundingAssetId: Long?, carName: String): Long = db.withTransaction {
        val transactionId = fundingAssetId?.let { assetId ->
            wealthDao.applyTransaction(
                TransactionEntity(
                    date = entry.date,
                    type = TransactionType.EXPENSE,
                    category = "Fuel: $carName",
                    amount = entry.totalCost,
                    currencyCode = entry.currencyCode,
                    assetId = assetId,
                    note = entry.notes
                )
            )
        }
        fuelDao.insert(entry.copy(transactionId = transactionId))
    }

    fun maintenanceForCar(carId: Long): Flow<List<MaintenanceEntity>> = maintenanceDao.observeForCar(carId)

    suspend fun addMaintenance(item: MaintenanceEntity): Long = maintenanceDao.insert(item)

    suspend fun markMaintenanceComplete(item: MaintenanceEntity) =
        maintenanceDao.update(item.copy(completedAt = LocalDate.now()))

    suspend fun deleteMaintenance(item: MaintenanceEntity) = maintenanceDao.delete(item)

    fun kmRemaining(car: CarEntity?): Long? {
        val next = car?.nextMaintenanceKm ?: return null
        val current = car.currentOdometerKm
        return (next - current).coerceAtLeast(0)
    }

    /** km remaining for a specific maintenance item, based on the car's current odometer. */
    fun kmRemainingFor(car: CarEntity?, item: MaintenanceEntity): Long? {
        val due = item.dueAtOdometerKm ?: return null
        val current = car?.currentOdometerKm ?: return null
        return (due - current).coerceAtLeast(0)
    }
}
