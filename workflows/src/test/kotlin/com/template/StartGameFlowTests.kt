package com.template

import com.template.flows.Responder
import com.template.flows.StartGameFlow
import com.template.states.BoardState
import net.corda.core.identity.CordaX500Name
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

class StartGameFlowTests {

    private val mockNetwork = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
        TestCordapp.findCordapp("com.template.contracts"),
        TestCordapp.findCordapp("com.template.flows")
    )))

    private val nodeA = mockNetwork.createNode()
    private val nodeB = mockNetwork.createNode()
    init {
        listOf(nodeA, nodeB).forEach {
            it.registerInitiatedFlow(Responder::class.java)
        }
    }

//    val partyA = TestIdentity(CordaX500Name("PartyA","London","GB")).party
//    val partyB = TestIdentity(CordaX500Name("PartyB","New York","US")).party

    @Before
    fun setup() = mockNetwork.runNetwork()

    @After
    fun tearDown() = mockNetwork.stopNodes()

    @Test
    fun flowReturnsTransactionSignedByBothParties() {
        val partyA = nodeA.info.chooseIdentityAndCert().party
        val partyB = nodeB.info.chooseIdentityAndCert().party
        //val boardState = BoardState(partyA, partyB)

        val flow = StartGameFlow(partyB)
        val future = nodeA.startFlow(flow)
        mockNetwork.runNetwork() // ???
        val stx = future.getOrThrow()
        stx.verifyRequiredSignatures()
    }

    @Test
    fun flowRecordsTheSameTransactionInBothPartyVaults() {
        val partyA = nodeA.info.chooseIdentityAndCert().party
        val partyB = nodeB.info.chooseIdentityAndCert().party
        //val boardState = BoardState(partyA, partyB)

        val flow = StartGameFlow(partyB)
        val future = nodeA.startFlow(flow)
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