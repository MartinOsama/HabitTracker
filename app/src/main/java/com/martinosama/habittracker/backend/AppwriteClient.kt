package com.martinosama.habittracker.backend

import android.content.Context
import io.appwrite.Client
import io.appwrite.services.Storage

object AppwriteClient {
    private lateinit var client: Client
    lateinit var storage: Storage

    fun init(context: Context) {
        client = Client(context)
            .setEndpoint("https://cloud.appwrite.io/v1")
            .setProject("67c5b7900039311024c6")
            .setSelfSigned(true)

        storage = Storage(client)
    }
}