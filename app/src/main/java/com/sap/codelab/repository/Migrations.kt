package com.sap.codelab.repository

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migrates the memo table from version 1 to 2, changing reminderLatitude and reminderLongitude
 * from INTEGER to REAL so they can hold decimal coordinates. SQLite cannot alter a column's type,
 * so the table is recreated and its rows copied over.
 */
internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE memo_new (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "title TEXT NOT NULL, " +
                "description TEXT NOT NULL, " +
                "reminderDate INTEGER NOT NULL, " +
                "reminderLatitude REAL NOT NULL, " +
                "reminderLongitude REAL NOT NULL, " +
                "isDone INTEGER NOT NULL)"
        )
        db.execSQL(
            "INSERT INTO memo_new (id, title, description, reminderDate, reminderLatitude, reminderLongitude, isDone) " +
                "SELECT id, title, description, reminderDate, reminderLatitude, reminderLongitude, isDone FROM memo"
        )
        db.execSQL("DROP TABLE memo")
        db.execSQL("ALTER TABLE memo_new RENAME TO memo")
    }
}
