package com.template

import com.template.contracts.BoardContract
import com.template.flows.EndGameFlow
import com.template.flows.Responder
import com.template.flows.StartGameFlow
import com.template.flows.SubmitTurnFlow
import com.template.states.BoardState
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.TestIdentity
import net.corda.testing.internal.chooseIdentityAndCert
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class EndGameFlowTests {

    private val mockNetwork = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
        TestCordapp.findCordapp("com.template.contracts"),
        TestCordapp.findCordapp("com.template.flows")
    )))

    private lateinit var nodeA: StartedMockNode
    private lateinit var nodeB: StartedMockNode
    private lateinit var partyA: Party
    private lateinit var partyB: Party

    @Before
    fun setup() {
        nodeA = mockNetwork.createNode()
        nodeB = mockNetwork.createNode()
        listOf(nodeA, nodeB).forEach {
            it.registerInitiatedFlow(Responder::class.java)
        }
        partyA = nodeA.info.chooseIdentityAndCert().party
        partyB = nodeB.info.chooseIdentityAndCert().party

        val future = nodeA.startFlow(StartGameFlow(partyB))
        mockNetwork.runNetwork()
        future.getOrThrow()
    }

    @After
    fun tearDown() = mockNetwork.stopNodes()


    fun formatBoardForEndGame() {
        nodeA.startFlow(SubmitTurnFlow(0,0))
        nodeB.startFlow(SubmitTurnFlow(1,0))
        nodeA.startFlow(SubmitTurnFlow(0,1))
        nodeB.startFlow(SubmitTurnFlow(0,2))
        nodeA.startFlow(SubmitTurnFlow(1,2))
        nodeB.startFlow(SubmitTurnFlow(1,1))
        nodeA.startFlow(SubmitTurnFlow(2,2))
        nodeB.startFlow(SubmitTurnFlow(2,1))
        nodeA.startFlow(SubmitTurnFlow(2,0))
    }


    @Test
    fun flowRunsNoMoreMovesAvailable() {
//        nodeA.startFlow(SubmitTurnFlow(0,0))
//        nodeB.startFlow(SubmitTurnFlow(0,1))
//        nodeA.startFlow(SubmitTurnFlow(0,2))
//        nodeB.startFlow(SubmitTurnFlow(1,0))
//        nodeA.startFlow(SubmitTurnFlow(1,2))
//        nodeB.startFlow(SubmitTurnFlow(1,0))
//        nodeA.startFlow(SubmitTurnFlow(2,0))
//        nodeB.startFlow(SubmitTurnFlow(2,1))
//        nodeA.startFlow(SubmitTurnFlow(2,2))

        ///////
    }


    @Test
    fun flowRunsWhenAPlayerGetsThreeInARow() {
//        nodeA.startFlow(SubmitTurnFlow(0,0))
//        nodeB.startFlow(SubmitTurnFlow(0,1))
//        nodeA.startFlow(SubmitTurnFlow(0,2))
//        nodeB.startFlow(SubmitTurnFlow(1,0))

        ///////
    }



    @Test
    fun flowReturnsCorrectlyFormedTransaction() {

        val criteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
        val results = nodeA.services.vaultService.queryBy<BoardState>(criteria)
        if (results.states.size != 1) {} // TODO err
        val state = results.states.single().state.data

//        val future = nodeA.startFlow(EndGameFlow(state.linearId))

        nodeA.startFlow(SubmitTurnFlow(0,0))
        nodeB.startFlow(SubmitTurnFlow(1,0))
        nodeA.startFlow(SubmitTurnFlow(0,1))
        nodeB.startFlow(SubmitTurnFlow(0,2))
        nodeA.startFlow(SubmitTurnFlow(1,2))
        nodeB.startFlow(SubmitTurnFlow(1,1))
        nodeA.startFlow(SubmitTurnFlow(2,2))
        nodeB.startFlow(SubmitTurnFlow(2,1))
        val future = nodeA.startFlow(SubmitTurnFlow(2,0))
        mockNetwork.runNetwork()
        val ptx: SignedTransaction = future.getOrThrow()

        assert(ptx.tx.inputs.isEmpty())
        assert(ptx.tx.outputs.size == 1)
        assert(ptx.tx.outputs.single().data is BoardState)
        assert(ptx.tx.commands.singleOrNull() != null)
        assert(ptx.tx.commands.single().value is BoardContract.Commands.EndGame)
        assert(ptx.tx.requiredSigningKeys.equals(setOf(partyA.owningKey, partyB.owningKey)))
    }

    @Test
    fun flowReturnsTransactionSignedByBothParties() {

    }

    @Test
    fun flowRecordsTheSameTransactionInBothPartyVaults() {

    }

}