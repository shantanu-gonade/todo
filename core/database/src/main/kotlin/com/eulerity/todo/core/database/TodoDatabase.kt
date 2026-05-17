package com.eulerity.todo.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TaskEntity::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class TodoDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        /**
         * Migration 1→2: adds the `category` column (TEXT NOT NULL DEFAULT 'NONE').
         * Existing rows automatically receive NONE — no data loss.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE tasks ADD COLUMN category TEXT NOT NULL DEFAULT 'NONE'"
                )
            }
        }
    }
}
