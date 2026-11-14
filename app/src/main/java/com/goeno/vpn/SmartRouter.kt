package com.goeno.vpn

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

object SmartRouter {

    data class Node(
        val name: String,
        val host: String,
        val port: Int,
        val xrayJsonConfig: String
    )

    suspend fun testLatency(host: String, port: Int, timeoutMs: Int = 1500): Long = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
            }
            System.currentTimeMillis() - start
        } catch (e: Exception) {
            Long.MAX_VALUE
        }
    }

    suspend fun pickBestNode(nodes: List<Node>): Node? = withContext(Dispatchers.IO) {
        if (nodes.isEmpty()) return@withContext null
        nodes.map { node ->
            async { node to testLatency(node.host, node.port) }
        }.awaitAll()
            .filter { it.second != Long.MAX_VALUE }
            .minByOrNull { it.second }
            ?.first ?: nodes.first()
    }
}
