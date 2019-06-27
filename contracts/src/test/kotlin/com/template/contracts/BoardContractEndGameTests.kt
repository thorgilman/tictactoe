package com.template.contracts

import com.template.states.BoardState
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.identity.CordaX500Name
import net.corda.testing.contracts.DummyState
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class BoardContractEndGameTests {
    private val ledgerServices = MockServices()

    class DummyCommand : TypeOnlyCommandData()

    @Test
    fun mustIncludeEndGameCommand() {

//        val partyA = TestIdentity(CordaX500Name("PartyA","London","GB")).party
//        val partyB = TestIdentity(CordaX500Name("PartyB","New York","US")).party
//        val boardState = BoardState(partyA, partyB)
//        val publicKeys = boardState.participants.map {it.owningKey}
//
//        ledgerServices.ledger {
//            transaction {
//                input(BoardContract.ID, boardState)
//                command(publicKeys, BoardContract.Commands.EndGame())
//                this.verifies()
//            }
//            transaction {
//                input(BoardContract.ID, boardState)
//                command(publicKeys, DummyCommand())
//                this.fails()
//            }
//        }
    }


    @Test
    fun endGameTransactionMustHaveOneInputs() {

    }

    @Test
    fun endGameTransactionMustHaveOneOutput() {

    }


}