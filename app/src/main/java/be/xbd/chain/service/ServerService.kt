package be.xbd.chain.service

import org.springframework.http.ResponseEntity
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestTemplate
import java.net.NetworkInterface

fun myServerSet(port: String): HashSet<String> {
    val serverSet = HashSet<String>()
    val interfaceEnumeration = NetworkInterface.getNetworkInterfaces()
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
    return serverSet
}

fun cleanServerSet(serverSet: HashSet<String>, port: String) {
    serverSet.clear()
    serverSet.addAll(myServerSet(port))
    println(serverSet)
}


fun collectAndMergeServer(localServerSet: HashSet<String>): HashSet<String> {
    val factory = SimpleClientHttpRequestFactory()
    factory.setConnectTimeout(100)
    factory.setReadTimeout(25000)
    val restTemplate = RestTemplate(factory)

    // prevent ConcurrentModificationException
    val localCopy = HashSet(localServerSet)
    for (server in localCopy) {
        try {
            val remoteServer: ResponseEntity<Collection<*>> = restTemplate.postForEntity("http://$server/add-all-server", localServerSet, Collection::class.java)
            if (remoteServer.body != null) {
                localServerSet.addAll(remoteServer.body as Collection<String>)
            }
        } catch (e: Exception) {
            println("Connection timeout for $server")
        }
    }
    if (localCopy != localServerSet) {
        localServerSet.addAll(localCopy) // if someone cleared by clean endpoint
        collectAndMergeServer(localServerSet)
    }

    return localServerSet
}
