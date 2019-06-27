package com.template.contracts

import com.template.states.BoardState
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.identity.CordaX500Name
import net.corda.testing.contracts.DummyState
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class BoardContractStartGameTests {
    private val ledgerServices = MockServices()

    class DummyCommand : TypeOnlyCommandData()

    @Test
    fun mustIncludeStartGameCommand() {

        val partyA = TestIdentity(CordaX500Name("PartyA","London","GB")).party
        val partyB = TestIdentity(CordaX500Name("PartyB","New York","US")).party
        val boardState = BoardState(partyA, partyB)
        val publicKeys = boardState.participants.map {it.owningKey}

        ledgerServices.ledger {
            transaction {
                output(BoardContract.ID, boardState)
                command(publicKeys, BoardContract.Commands.StartGame())
                this.verifies()
            }
            transaction {
                output(BoardContract.ID, boardState)
                command(publicKeys, DummyCommand())
                this.fails()
            }
        }
    }


    @Test
    fun startGameTransactionMustHaveNoInputs() {
        val partyA = TestIdentity(CordaX500Name("PartyA","London","GB")).party
        val partyB = TestIdentity(CordaX500Name("PartyB","New York","US")).party
        val boardState = BoardState(partyA, partyB)
        val publicKeys = boardState.participants.map {it.owningKey}

        ledgerServices.ledger {
            transaction {
                output(BoardContract.ID, boardState)
                command(publicKeys, BoardContract.Commands.StartGame())
                this.verifies()
            }
            transaction {
                input(BoardContract.ID, DummyState())
                output(BoardContract.ID, boardState)
                command(publicKeys, BoardContract.Commands.StartGame())
                this `fails with` "There should be no input state."
            }
        }
    }

    @Test
    fun startGameTransactionMustHaveOneOutput() {
        val partyA = TestIdentity(CordaX500Name("PartyA","London","GB")).party
        val partyB = TestIdentity(CordaX500Name("PartyB","New York","US")).party
        val boardState = BoardState(partyA, partyB)
        val publicKeys = boardState.participants.map {it.owningKey}

        ledgerServices.ledger {
            transaction {
                output(BoardContract.ID, boardState)
                command(publicKeys, BoardContract.Commands.StartGame())
                this.verifies()
            }
            transaction {
                output(BoardContract.ID, boardState)
                output(BoardContract.ID, DummyState())
                command(publicKeys, BoardContract.Commands.StartGame())
                this `fails with` "There should be one output state."
            }
        }
    }


    @Test
    fun cannotStartGameWithYourself() {
        val partyA = TestIdentity(CordaX500Name("PartyA","London","GB")).party
        val partyB = TestIdentity(CordaX500Name("PartyB","New York","US")).party
        val boardState = BoardState(partyA, partyB)
        val publicKeys = boardState.participants.map {it.owningKey}
        val boardStateSameParty = BoardState(partyA, partyA)

        ledgerServices.ledger {
            transaction {
                output(BoardContract.ID, boardState)
                command(listOf(partyA.owningKey, partyB.owningKey), BoardContract.Commands.StartGame())
                this.verifies()
            }
            transaction {
                output(BoardContract.ID, boardStateSameParty)
                command(listOf(partyA.owningKey, partyA.owningKey), BoardContract.Commands.StartGame())
                this `fails with` "You cannot play a game with yourself."
            }
        }
    }

    @Test
    fun bothPlayersMustSignStartGameTransaction() {
        val partyA = TestIdentity(CordaX500Name("PartyA","London","GB")).party
        val partyB = TestIdentity(CordaX500Name("PartyB","New York","US")).party
        val partyC = TestIdentity(CordaX500Name("PartyC","New York","US")).party
        val boardState = BoardState(partyA, partyB)

        ledgerServices.ledger {
            transaction {
                command(listOf(partyA.owningKey, partyB.owningKey), BoardContract.Commands.StartGame())
                output(BoardContract.ID, boardState)
                this.verifies()
            }
            transaction {
                command(partyA.owningKey, BoardContract.Commands.StartGame())
                output(BoardContract.ID, boardState)
                this `fails with` "Both parties together only may sign a StartGame transaction."
            }
            transaction {
                command(partyC.owningKey, BoardContract.Commands.StartGame())
                output(BoardContract.ID, boardState)
                this `fails with` "Both parties together only may sign a StartGame transaction."
            }
            transaction {
                command(listOf(partyC.owningKey, partyA.owningKey, partyB.owningKey), BoardContract.Commands.StartGame())
                output(BoardContract.ID, boardState)
                this `fails with` "Both parties together only may sign a StartGame transaction."
            }
        }
    }

}