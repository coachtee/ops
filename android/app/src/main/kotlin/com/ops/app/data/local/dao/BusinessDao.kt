package com.ops.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ops.app.data.local.entities.BusinessEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessDao {

    /** One business per install in V1 (see DISCOVERY.md, "Risks and assumptions"). */
    @Query("SELECT * FROM business LIMIT 1")
    fun observe(): Flow<BusinessEntity?>

    @Query("SELECT * FROM business LIMIT 1")
    suspend fun get(): BusinessEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BusinessEntity)

    @Query("DELETE FROM business")
    suspend fun clear()
}
