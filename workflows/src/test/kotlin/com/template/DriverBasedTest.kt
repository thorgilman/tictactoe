package com.template

import com.template.flows.StartGameFlow
import com.template.states.BoardState
import junit.framework.Assert.assertEquals
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import net.corda.core.messaging.vaultTrackBy
import net.corda.core.node.services.Vault
import net.corda.core.node.services.Vault.Update
import net.corda.core.utilities.getOrThrow
import net.corda.node.services.Permissions.Companion.invokeRpc
import net.corda.node.services.Permissions.Companion.startFlow
import net.corda.testing.core.TestIdentity
import net.corda.testing.core.expect
import net.corda.testing.core.expectEvents
import net.corda.testing.core.singleIdentity
import net.corda.testing.driver.DriverDSL
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.NodeHandle
import net.corda.testing.driver.driver
import net.corda.testing.node.TestCordapp
import net.corda.testing.node.User
import net.corda.testing.node.internal.startNode
import org.junit.Test
import java.util.*
import java.util.concurrent.Future

class DriverBasedTest {
//    private val playerX = TestIdentity(CordaX500Name("PlayerX", "", "US"))
//    private val playerO = TestIdentity(CordaX500Name("PlayerO", "", "US"))


    @Test
    fun test () {
        driver(DriverParameters(startNodesInProcess = true, cordappsForAllNodes = listOf(
                TestCordapp.findCordapp("com.template.contracts"),
                TestCordapp.findCordapp("com.template.flows")))) {
            val userX = User("playerX", "testPassword1", permissions = setOf(
                    startFlow<StartGameFlow>(),
                    // TODO: other
                    invokeRpc("vaultTrackBy")
            ))

            val userO = User("playerO", "testPassword2", permissions = setOf(
                    startFlow<StartGameFlow>(),
                    invokeRpc("vaultTrackBy")
            ))

            val (playerX, playerO) = listOf(
                    startNode(providedName = CordaX500Name("PlayerX", "New York", "US"), rpcUsers = listOf(userX)),
                    startNode(providedName = CordaX500Name("PlayerO", "New York", "US"), rpcUsers = listOf(userO))
            ).map { it.getOrThrow() }
            // END 1

            // START 2
            val playerXClient = CordaRPCClient(playerX.rpcAddress)
            val playerXCProxy: CordaRPCOps = playerXClient.start("playerX", "testPassword1").proxy

            val playerOClient = CordaRPCClient(playerO.rpcAddress)
            val playerOProxy: CordaRPCOps = playerOClient.start("playerO", "testPassword2").proxy
            // END 2

            // START 3
            val playerXVaultUpdates = playerXCProxy.vaultTrackBy<BoardState>().updates
            val playerOVaultUpdates = playerOProxy.vaultTrackBy<BoardState>().updates
            // END 3

            // START 4
            //val issueRef = OpaqueBytes.of(0)
            playerXCProxy.startFlow(::StartGameFlow,
                    playerO.nodeInfo.singleIdentity()
            ).returnValue.getOrThrow()

            playerXVaultUpdates.expectEvents {
                expect { update ->
                    //println("Bob got vault update of $update")
                    assertEquals(update.produced.first().state.data.isPlayerXTurn, false)
                    //val amount: Amount<Issued<Currency>> = update.produced.first().state.data.amount
                    //assertEquals(1000.DOLLARS, amount.withoutIssuer())
                }
            }
            // END 4

//            // START 5
//            bobProxy.startFlow(::CashPaymentFlow, 1000.DOLLARS, alice.nodeInfo.singleIdentity()).returnValue.getOrThrow()
//
//            aliceVaultUpdates.expectEvents {
//                expect { update ->
//                    println("Alice got vault update of $update")
//                    val amount: Amount<Issued<Currency>> = update.produced.first().state.data.amount
//                    assertEquals(1000.DOLLARS, amount.withoutIssuer())
//                }
//            }
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