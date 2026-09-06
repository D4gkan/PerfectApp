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

class CarRepository(private val db: PerfectDatabase, context: android.content.Context) {
    private val carDao = db.carDao()
    private val maintenanceDao = db.maintenanceDao()
    private val fuelDao = db.fuelDao()
    private val wealthDao = db.wealthDao()

    private val preferences = context.getSharedPreferences("cars", android.content.Context.MODE_PRIVATE)
    private val selectedId = kotlinx.coroutines.flow.MutableStateFlow(preferences.getLong("selected", 0))
    val cars = carDao.observeCars()
    val primaryCar: Flow<CarEntity?> = kotlinx.coroutines.flow.combine(cars, selectedId) { cars, id -> cars.firstOrNull { it.id == id } ?: cars.firstOrNull() }
    fun selectCar(id: Long) { selectedId.value = id; preferences.edit().putLong("selected", id).apply() }
    suspend fun deleteCar(car: CarEntity) = db.withTransaction {
        carDao.detachRenewals(car.id)
        carDao.deleteOdometerHistory(car.id)
        carDao.deleteCar(car)
    }

    suspend fun upsertCar(car: CarEntity): Long {
        val result = carDao.upsertCar(car)
        val id = if (car.id != 0L) car.id else result
        selectCar(id)
        return id
    }

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
     * Saves a TRY cash fill-up and always creates its matching
     * Wealth expense in the same database transaction.  A failed balance update therefore
     * never leaves a fuel record without its financial counterpart.
     */
    suspend fun addFuel(entry: FuelEntryEntity, carName: String): Long = db.withTransaction {
        require(entry.currencyCode == "TRY" && entry.totalCost.isFinite() && entry.totalCost > 0 && entry.liters.isFinite() && entry.liters > 0)
        val assetId = WealthRepository.CASH_TRY_ID
        val cash = wealthDao.getAsset(assetId)
        if (cash == null) wealthDao.upsertAsset(WealthRepository.DEFAULT_ASSETS.first { it.id == assetId })
        else if (!cash.isActive) wealthDao.updateAsset(cash.copy(isActive = true))
        val transactionId = run {
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
