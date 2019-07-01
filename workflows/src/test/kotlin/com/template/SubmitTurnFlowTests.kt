package com.template

import com.template.contracts.BoardContract
import com.template.flows.Responder
import com.template.flows.StartGameFlow
import com.template.flows.SubmitTurnFlow
import com.template.states.BoardState
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
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

class SubmitTurnFlowTests {

    private val mockNetwork = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
        TestCordapp.findCordapp("com.template.contracts"),
        TestCordapp.findCordapp("com.template.flows")
    )))

    private lateinit var nodeA: StartedMockNode
    private lateinit var nodeB: StartedMockNode
    private lateinit var partyA: Party
    private lateinit var partyB: Party

    init {
        // ???
    }

    @Before
    fun setup() {
        nodeA = mockNetwork.createNode()
        nodeB = mockNetwork.createNode()
        partyA = nodeA.info.chooseIdentityAndCert().party
        partyB = nodeB.info.chooseIdentityAndCert().party
        listOf(nodeA, nodeB).forEach {
            it.registerInitiatedFlow(Responder::class.java)
        }
        nodeA.startFlow(StartGameFlow(partyB))
        mockNetwork.runNetwork()


    }

    @After
    fun tearDown() = mockNetwork.stopNodes()


    @Test
    fun flowReturnsCorrectlyFormedTransaction() {
        val future = nodeA.startFlow(SubmitTurnFlow(0,0))
        mockNetwork.runNetwork()
        val ptx: SignedTransaction = future.getOrThrow()

        assert(ptx.tx.inputs.size == 1)
        assert(ptx.tx.getOutput(0) is BoardState) // ???
        assert(ptx.tx.outputs.size == 1)
        assert(ptx.tx.outputs[0].data is BoardState)
        assert(ptx.tx.commands.singleOrNull() != null)
        assert(ptx.tx.commands.single().value is BoardContract.Commands.SubmitTurn)
        assert(ptx.tx.commands[0].signers.equals(listOf(partyA.owningKey, partyB.owningKey)))
        ptx.verifyRequiredSignatures()
    }


    @Test
    fun flowReturnsTransactionSignedByBothParties() {
        val future = nodeA.startFlow(SubmitTurnFlow(0,0))
        mockNetwork.runNetwork() // ???
        val stx = future.getOrThrow()
        stx.verifyRequiredSignatures()
    }

    @Test
    fun flowRecordsTheSameTransactionInBothPartyVaults() {
        val future = nodeA.startFlow(SubmitTurnFlow(0,0))
        mockNetwork.runNetwork() // ???
        val stx = future.getOrThrow()

        listOf(nodeA, nodeB).map {
            it.services.validatedTransactions.getTransaction(stx.id)
        }.forEach {
            val txHash = (it as SignedTransaction).id
            assertEquals(txHash, stx.id)
        }
    }

}