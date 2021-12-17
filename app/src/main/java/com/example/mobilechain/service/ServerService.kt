package com.example.mobilechain.service

import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import java.net.NetworkInterface

fun cleanServerSet(serverSet: HashSet<String>, port: String) {
    val interfaceEnumeration = NetworkInterface.getNetworkInterfaces()
    serverSet.clear()
    while (interfaceEnumeration.hasMoreElements()) {
        val networkInterface = interfaceEnumeration.nextElement()
        val inetEnumeration = networkInterface.inetAddresses
        while (inetEnumeration.hasMoreElements()) {
            val inetAddress = inetEnumeration.nextElement()
            val host: String? = inetAddress.hostAddress
            if (host != null && host.length<=15 && host.length>9)
                serverSet.add("$host:$port")
        }
    }
    println(serverSet)
}


fun collectAndMergeServer(localServerSet: HashSet<String>): HashSet<String> {
    val restTemplate = RestTemplate()

    // prevent ConcurrentModificationException
    val localCopy = HashSet(localServerSet)
    for (server in localCopy) {
        val remoteServer: ResponseEntity<Collection<*>> = restTemplate.postForEntity("http://$server/add-all-server", localServerSet, Collection::class.java)
        if (remoteServer.body != null) {
            localServerSet.addAll(remoteServer.body as Collection<String>)
        }
    }
    if (localCopy != localServerSet) {
        localServerSet.addAll(localCopy) // if someone cleared by clean endpoint
        collectAndMergeServer(localServerSet)
    }

    return localServerSet
}
