package com.example.mobilechain.service

import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate


fun collectAndMergeServer(remoteServerSet: HashSet<String>, localServerSet: HashSet<String>): HashSet<String> {
    val restTemplate = RestTemplate()

    for (server in localServerSet) {
        val remoteServer: ResponseEntity<Collection<*>> = restTemplate.postForEntity("http://$server/add-all-server", remoteServerSet, Collection::class.java)
        if (remoteServer.body != null) {
            remoteServerSet.addAll(remoteServer.body as Collection<String>)
        }
    }
    if (remoteServerSet != localServerSet) {
        localServerSet.addAll(remoteServerSet)
        collectAndMergeServer(HashSet(localServerSet), localServerSet)
    }

    return localServerSet
}
