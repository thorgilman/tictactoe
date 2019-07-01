package com.template.contracts

import com.template.states.BoardState
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.testing.contracts.DummyState
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Before
import org.junit.Test
import java.security.PublicKey

class BoardContractEndGameTests {
    private val ledgerServices = MockServices()

    class DummyCommand : TypeOnlyCommandData()

    lateinit var boardState: BoardState
    lateinit var publicKeys: List<PublicKey>
    lateinit var partyA: Party
    lateinit var partyB: Party

    @Before
    fun setup() {
        partyA = TestIdentity(CordaX500Name("PartyA","London","GB")).party
        partyB = TestIdentity(CordaX500Name("PartyB","New York","US")).party
        boardState = BoardState(partyA, partyB)
        publicKeys = boardState.participants.map {it.owningKey}
    }

    // TODO TODO TODO
    @Test
    fun mustIncludeEndGameCommand() {
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