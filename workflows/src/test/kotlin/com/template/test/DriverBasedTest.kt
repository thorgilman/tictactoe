package com.template.test

import com.template.flows.StartGameFlow
import com.template.flows.SubmitTurnFlow



import com.template.states.BoardState
import com.template.webserver.Controller
import com.template.webserver.NodeRPCConnection
import junit.framework.Assert.assertEquals
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.startFlow
import net.corda.core.messaging.vaultTrackBy
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.expect
import net.corda.testing.core.expectEvents
import net.corda.testing.core.singleIdentity
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.NodeHandle
import net.corda.testing.driver.driver
import net.corda.testing.node.TestCordapp
import net.corda.testing.node.User
import org.junit.Test

class DriverBasedTest {

    @Test
    fun test () {

        driver(DriverParameters(startNodesInProcess = true, cordappsForAllNodes = listOf(
                TestCordapp.findCordapp("com.template.contracts"),
                TestCordapp.findCordapp("com.template.flows"),
                TestCordapp.findCordapp("com.template.webserver")))) {

            val userX = User("playerX", "testPassword1", permissions = setOf("ALL"))
            val userO = User("playerO", "testPassword2", permissions = setOf("ALL"))

            val (playerX: NodeHandle, playerO: NodeHandle) = listOf(
                    startNode(providedName = CordaX500Name("PlayerX", "New York", "US"), rpcUsers = listOf(userX)),
                    startNode(providedName = CordaX500Name("PlayerO", "New York", "US"), rpcUsers = listOf(userO))
            ).map { it.getOrThrow() }

            val playerXWebserver = startWebserver(playerX).toCompletableFuture().get()
            val playerOWebserver = startWebserver(playerO).toCompletableFuture().get()




            val playerXProxy = CordaRPCClient(playerX.rpcAddress).start("playerX", "testPassword1").proxy
            val playerOProxy = CordaRPCClient(playerO.rpcAddress).start("playerO", "testPassword1").proxy

            val playerXCordaRpcConnection = NodeRPCConnection(host = playerXWebserver.listenAddress.host, username = userX.username, password = userX.password, rpcPort = playerXWebserver.listenAddress.port).rpcConnection
            val playerXController = Controller(playerXCordaRpcConnection)
            val playerOCordaRpcConnection = NodeRPCConnection(host = playerOWebserver.listenAddress.host, username = userO.username, password = userO.password, rpcPort = playerOWebserver.listenAddress.port).rpcConnection
            val playerOController = Controller(playerOCordaRpcConnection)




            val playerXVaultUpdates = playerXProxy.vaultTrackBy<BoardState>().updates
            val playerOVaultUpdates = playerOProxy.vaultTrackBy<BoardState>().updates

            // #1: StartGameFlow
            playerOProxy.startFlow(::StartGameFlow, playerX.nodeInfo.singleIdentity()).returnValue.getOrThrow()
            playerOVaultUpdates.expectEvents {
                expect { update ->
                    val state = update.produced.first().state.data
                    assertEquals(state.isPlayerXTurn, false)
                    assertEquals(state.getCurrentPlayerParty(), playerO.nodeInfo.singleIdentity())
                }
            }
            playerXVaultUpdates.expectEvents {
                expect { update ->
                    val state = update.produced.first().state.data
                    assertEquals(state.isPlayerXTurn, false)
                    assertEquals(state.getCurrentPlayerParty(), playerO.nodeInfo.singleIdentity())
                }
            }

            // #2: SubmitTurnFlow
            val tx2 = playerOProxy.startFlow(::SubmitTurnFlow, 0, 0).returnValue.getOrThrow()
            val boardState = playerOProxy.vaultQuery(BoardState::class.java).states.single().state.data
            assertEquals(tx2.tx.outputStates.single(), boardState)

        }


//    @Test
//    fun `node test`() = withDriver {
//        val (playerXHandle, playerOHandle) = startNodes(playerX, playerO)
//        val playerXClient = CordaRPCClient(playerXHandle.rpcAddress)
//        val playerOClient = CordaRPCClient(playerOHandle.rpcAddress)
//        val playerXProxy = playerXClient.start("playerX", "password").proxy
//        val playerOProxy = playerOClient.start("playerO", "password").proxy
//
//
//        val stx = playerXProxy.startFlow(::StartGameFlow, playerX.party).returnValue.getOrThrow()
//
//
//
//
//    }

//    // Runs a test inside the Driver DSL, which provides useful functions for starting nodes, etc.
//    private fun withDriver(test: DriverDSL.() -> Unit) = driver(DriverParameters(isDebug = true, startNodesInProcess = true)) { test() }
//
//    // Makes an RPC call to retrieve another node's name from the network map.
//    private fun NodeHandle.resolveName(name: CordaX500Name) = rpc.wellKnownPartyFromX500Name(name)!!.name
//
//    // Resolves a list of futures to a list of the promised values.
//    private fun <T> List<Future<T>>.waitForAll(): List<T> = map { it.getOrThrow() }
//
//    // Starts multiple nodes simultaneously, then waits for them all to be ready.
//    private fun DriverDSL.startNodes(vararg identities: TestIdentity) = identities
//            .map { startNode(providedName = it.name) }
//            .waitForAll()
    }
}
