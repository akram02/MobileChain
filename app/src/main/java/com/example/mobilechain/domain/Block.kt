package com.example.mobilechain.domain

import java.time.Instant

class Block {
    lateinit var  timestamp: String
    lateinit var  lastHash: String
    lateinit var  hash: String
    lateinit var  data: Any
    lateinit var uuid: String
    lateinit var previousUuid: String

    constructor()

    constructor(timestamp: Instant, lastHash: String, hash: String, data: Any, uuid: String, previousUuid: String) {
        this.timestamp = timestamp.toEpochMilli().toString()
        this.lastHash = lastHash
        this.hash = hash
        this.data = data
        this.uuid = uuid
        this.previousUuid = previousUuid
    }

    override fun equals(other: Any?): Boolean {
        if (other==null) return false
        other as Block

        return timestamp == other.timestamp && lastHash == other.lastHash && hash == other.hash && data == other.data && uuid == other.uuid && previousUuid == other.previousUuid
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + lastHash.hashCode()
        result = 31 * result + hash.hashCode()
        result = 31 * result + data.hashCode()
        result = 31 * result + uuid.hashCode()
        result = 31 * result + previousUuid.hashCode()
        return result
    }
}