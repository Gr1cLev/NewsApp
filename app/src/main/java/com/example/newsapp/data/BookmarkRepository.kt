package com.example.newsapp.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

object BookmarkRepository {

    const val GUEST_PROFILE_ID: String = "guest"

    private const val BOOKMARK_FILE_NAME = "bookmarks_data.json"

    private val lock = Any()

    fun readBookmarks(context: Context, profileId: String): Set<Int> = synchronized(lock) {
        val store = loadStore(context)
        return@synchronized store.bookmarks[profileId]?.toSet() ?: emptySet()
    }

    fun persistBookmarks(context: Context, profileId: String, bookmarks: Set<Int>) = synchronized(lock) {
        val store = loadStore(context)
        if (bookmarks.isEmpty()) {
            store.bookmarks.remove(profileId)
        } else {
            store.bookmarks[profileId] = bookmarks.toMutableSet()
        }
        saveStore(context, store)
    }

    private fun loadStore(context: Context): BookmarkStore {
        val file = bookmarksFile(context)
        if (!file.exists()) {
            return BookmarkStore(mutableMapOf())
        }
        val raw = runCatching { file.readText() }.getOrDefault("")
        if (raw.isBlank()) {
            return BookmarkStore(mutableMapOf())
        }
        val root = JSONObject(raw)
        val profiles = root.optJSONObject("profiles") ?: JSONObject()
        val map = mutableMapOf<String, MutableSet<Int>>()
        val keys = profiles.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val idsArray = profiles.optJSONArray(key) ?: continue
            val ids = mutableSetOf<Int>()
            for (index in 0 until idsArray.length()) {
                ids.add(idsArray.optInt(index))
            }
            map[key] = ids
        }
        return BookmarkStore(map)
    }

    private fun saveStore(context: Context, store: BookmarkStore) {
        val root = JSONObject().apply {
            put("profiles", JSONObject().apply {
                store.bookmarks.forEach { (profileId, ids) ->
                    put(profileId, JSONArray().apply {
                        ids.forEach { id -> put(id) }
                    })
                }
            })
        }
        val file = bookmarksFile(context)
        runCatching {
            file.parentFile?.mkdirs()
            file.writeText(root.toString(2))
        }.getOrElse { error ->
            throw IOException("Failed to write bookmark data", error)
        }
    }

    private fun bookmarksFile(context: Context): File {
        val preferredDir = context.getExternalFilesDir(null) ?: context.filesDir
        return File(preferredDir, BOOKMARK_FILE_NAME)
    }

    private data class BookmarkStore(
        val bookmarks: MutableMap<String, MutableSet<Int>>
    )
}

