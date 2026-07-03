package ru.coffeepoint.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.coffeepoint.core.data.local.entity.CoffeeStationEntity

@Dao
interface CoffeeStationDao {

    @Query("SELECT * FROM coffee_station")
    suspend fun getAll(): List<CoffeeStationEntity>

    @Query("SELECT * FROM coffee_station WHERE id = :id")
    suspend fun getById(id: Int): CoffeeStationEntity?

    @Query("DELETE FROM coffee_station WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT MAX(id) FROM coffee_station")
    suspend fun getMaxId(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CoffeeStationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<CoffeeStationEntity>)

}