package com.loitp.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.loitp.model.Widget

@Dao
interface WidgetsDao {
    @Query("SELECT * FROM widgets")
    fun getWidgets(): List<Widget>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrUpdate(widget: Widget): Long

    @Query("DELETE FROM widgets WHERE widget_id = :widgetId")
    fun deleteWidgetId(widgetId: Int)
}
