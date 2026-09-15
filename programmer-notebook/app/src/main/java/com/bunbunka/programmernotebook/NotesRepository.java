package com.bunbunka.programmernotebook;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public final class NotesRepository {
    private static final String DB_NAME = "programmer_notes.db";
    private static final int DB_VERSION = 1;
    private final DbHelper helper;

    public NotesRepository(Context context) {
        helper = new DbHelper(context.getApplicationContext());
    }

    public List<Note> list(String query) {
        SQLiteDatabase db = helper.getReadableDatabase();
        List<Note> result = new ArrayList<>();
        String normalized = query == null ? "" : query.trim();
        Cursor cursor;
        if (normalized.isEmpty()) {
            cursor = db.query("notes", null, null, null, null, null, "updated_at DESC");
        } else {
            String like = "%" + normalized + "%";
            cursor = db.query(
                    "notes",
                    null,
                    "title LIKE ? COLLATE NOCASE OR content LIKE ? COLLATE NOCASE",
                    new String[]{like, like},
                    null,
                    null,
                    "updated_at DESC"
            );
        }
        try {
            while (cursor.moveToNext()) result.add(fromCursor(cursor));
        } finally {
            cursor.close();
        }
        return result;
    }

    public Note get(long id) {
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.query("notes", null, "id = ?", new String[]{String.valueOf(id)}, null, null, null, "1");
        try {
            return cursor.moveToFirst() ? fromCursor(cursor) : null;
        } finally {
            cursor.close();
        }
    }

    public long save(long id, String title, String content) {
        String cleanTitle = title == null ? "" : title;
        String cleanContent = content == null ? "" : content;
        if (id <= 0 && !NoteLogic.shouldPersist(cleanTitle, cleanContent)) return -1;

        ContentValues values = new ContentValues();
        values.put("title", cleanTitle);
        values.put("content", cleanContent);
        values.put("updated_at", System.currentTimeMillis());
        SQLiteDatabase db = helper.getWritableDatabase();

        if (id > 0) {
            int changed = db.update("notes", values, "id = ?", new String[]{String.valueOf(id)});
            if (changed > 0) return id;
        }
        return db.insertOrThrow("notes", null, values);
    }

    public void delete(long id) {
        if (id > 0) {
            helper.getWritableDatabase().delete("notes", "id = ?", new String[]{String.valueOf(id)});
        }
    }

    public void close() {
        helper.close();
    }

    private static Note fromCursor(Cursor cursor) {
        return new Note(
                cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                cursor.getString(cursor.getColumnIndexOrThrow("title")),
                cursor.getString(cursor.getColumnIndexOrThrow("content")),
                cursor.getLong(cursor.getColumnIndexOrThrow("updated_at"))
        );
    }

    private static final class DbHelper extends SQLiteOpenHelper {
        DbHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE notes (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "title TEXT NOT NULL DEFAULT ''," +
                    "content TEXT NOT NULL DEFAULT ''," +
                    "updated_at INTEGER NOT NULL" +
                    ")");
            db.execSQL("CREATE INDEX idx_notes_updated_at ON notes(updated_at DESC)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // Version 1 has no migrations yet.
        }
    }
}
