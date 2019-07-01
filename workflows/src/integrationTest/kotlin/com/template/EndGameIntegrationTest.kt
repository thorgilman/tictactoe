package com.template

import com.template.contracts.BoardContract
import com.template.flows.Responder
import com.template.flows.StartGameFlow
import com.template.flows.SubmitTurnFlow
import com.template.states.BoardState
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import net.corda.core.messaging.vaultTrackBy
import net.corda.core.node.services.Vault
import net.corda.core.node.services.Vault.Update
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.finance.contracts.asset.Cash
import net.corda.finance.flows.CashIssueAndPaymentFlow
import net.corda.finance.flows.CashPaymentFlow
import net.corda.node.services.Permissions.Companion.invokeRpc
import net.corda.node.services.Permissions.Companion.startFlow
import net.corda.testing.core.ALICE_NAME
import net.corda.testing.core.TestIdentity
import net.corda.testing.core.expect
import net.corda.testing.core.expectEvents
import net.corda.testing.driver.DriverDSL
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.NodeHandle
import net.corda.testing.driver.driver
import net.corda.testing.internal.chooseIdentity
import net.corda.testing.internal.chooseIdentityAndCert
import net.corda.testing.node.*
import org.bouncycastle.pqc.crypto.newhope.NHOtherInfoGenerator
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.*
import java.util.concurrent.Future
import kotlin.test.assert
import kotlin.test.assertEquals
import kotlin.test.expect

class EndGameIntegrationTest {

    private val mockNetwork = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
            TestCordapp.findCordapp("com.template.contracts"),
            TestCordapp.findCordapp("com.template.flows")
    )))
    private lateinit var nodeA: StartedMockNode
    private lateinit var nodeB: StartedMockNode


    @Before
    fun setup() {
        nodeA = mockNetwork.createNode(MockNodeParameters())
        nodeB = mockNetwork.createNode(MockNodeParameters())
        listOf(nodeA, nodeB).forEach {
            it.registerInitiatedFlow(Responder::class.java)
        }
    }

    @After
    fun tearDown() = mockNetwork.stopNodes()



    @Test
    fun `node test`()  {

        val partyA = nodeA.info.chooseIdentity()
        val partyB = nodeB.info.chooseIdentity()

        // Setup Game
        val futureWithGameState = nodeA.startFlow(StartGameFlow(partyB))
        mockNetwork.runNetwork()
        var boardState = getBoardState(futureWithGameState.getOrThrow())
        assertEquals(boardState.playerO, partyA)
        assertEquals(boardState.playerX, partyB)
        assertEquals(partyA, boardState.getCurrentPlayerParty())
        assert(!BoardContract.BoardUtils.isGameOver(boardState))

        // Move #1
        boardState = makeMoveAndGetNewBoardState(nodeA, 0,0)
        assertEquals(partyB, boardState.getCurrentPlayerParty())
        assert(!BoardContract.BoardUtils.isGameOver(boardState))

        // Move #2
        boardState = makeMoveAndGetNewBoardState(nodeB, 1,0)
        assertEquals(partyA, boardState.getCurrentPlayerParty())
        assert(!BoardContract.BoardUtils.isGameOver(boardState))

        // Move #3
        boardState = makeMoveAndGetNewBoardState(nodeA, 0,1)
        assertEquals(partyB, boardState.getCurrentPlayerParty())
        assert(!BoardContract.BoardUtils.isGameOver(boardState))

        // Move #4
        boardState = makeMoveAndGetNewBoardState(nodeB, 2,1)
        assertEquals(partyA, boardState.getCurrentPlayerParty())
        assert(!BoardContract.BoardUtils.isGameOver(boardState))

        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(boardState.linearId))
        val boardStateNodeA = nodeA.services.vaultService.queryBy<BoardState>(queryCriteria).states.single()
        val boardStateNodeB = nodeB.services.vaultService.queryBy<BoardState>(queryCriteria).states.single()
        assertEquals(boardStateNodeA.state.data.linearId, boardStateNodeB.state.data.linearId)

        // Move #5
        boardState = makeMoveAndGetNewBoardState(nodeA, 0,2)
        assertEquals(partyB, boardState.getCurrentPlayerParty())
        assert(BoardContract.BoardUtils.isGameOver(boardState))

        assert(nodeA.services.vaultService.queryBy<BoardState>(queryCriteria).states.isEmpty())
        assert(nodeB.services.vaultService.queryBy<BoardState>(queryCriteria).states.isEmpty())
        
    }


    fun makeMoveAndGetNewBoardState(node: StartedMockNode, x: Int, y: Int): BoardState {
        val futureWithGameState = node.startFlow(SubmitTurnFlow(x, y))
        mockNetwork.runNetwork()
        return getBoardState(futureWithGameState.getOrThrow())
    }

    fun getBoardState(tx: SignedTransaction): BoardState = tx.coreTransaction.outputsOfType<BoardState>().single()

/*
    // Runs a test inside the Driver DSL, which provides useful functions for starting nodes, etc.
    private fun withDriver(test: DriverDSL.() -> Unit) = driver(
        DriverParameters(isDebug = true, startNodesInProcess = true)
    ) {
        /////
    }

    // Makes an RPC call to retrieve another node's name from the network map.
    private fun NodeHandle.resolveName(name: CordaX500Name) = rpc.wellKnownPartyFromX500Name(name)!!.name

    // Resolves a list of futures to a list of the promised values.
    private fun <T> List<Future<T>>.waitForAll(): List<T> = map { it.getOrThrow() }

    // Starts multiple nodes simultaneously, then waits for them all to be ready.
    private fun DriverDSL.startNodes(vararg identities: TestIdentity) = identities
        .map { startNode(providedName = it.name) }
        .waitForAll()

 */

}