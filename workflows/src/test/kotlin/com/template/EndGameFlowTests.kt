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

class EndGameFlowTests {

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

    }

    @Test
    fun flowRecordsTheSameTransactionInBothPartyVaults() {

    }

}