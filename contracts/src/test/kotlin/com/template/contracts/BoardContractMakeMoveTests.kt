package com.template.contracts

import com.template.states.BoardState
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.identity.CordaX500Name
import net.corda.testing.contracts.DummyState
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class BoardContractMakeMoveTests {
    private val ledgerServices = MockServices()

    class DummyCommand : TypeOnlyCommandData()

    @Test
    fun mustIncludeMakeMoveCommand() {

        val partyA = TestIdentity(CordaX500Name("PartyA","London","GB")).party
        val partyB = TestIdentity(CordaX500Name("PartyB","New York","US")).party
        val boardState = BoardState(partyA, partyB)
        val publicKeys = boardState.participants.map {it.owningKey}

        ledgerServices.ledger {
            transaction {
                input(BoardContract.ID, boardState)
                //boardState.printBoard()
                val outputBoard = boardState.returnNewBoardAfterMove(Pair(0,0))
                output(BoardContract.ID, outputBoard)
                //outputBoard.printBoard()
                command(publicKeys, BoardContract.Commands.MakeMove())
                this.verifies()
            }
            transaction {
                input(BoardContract.ID, boardState)
                output(BoardContract.ID, boardState.returnNewBoardAfterMove(Pair(0,0)))
                command(publicKeys, DummyCommand())
                this.fails()
            }
        }
    }


//    @Test
//    fun makeMoveTransactionMustHaveOneInput() {
//        val partyA = TestIdentity(CordaX500Name("PartyA","London","GB")).party
//        val partyB = TestIdentity(CordaX500Name("PartyB","New York","US")).party
//        val boardState = BoardState(partyA, partyB)
//        val publicKeys = boardState.participants.map {it.owningKey}
//
//        ledgerServices.ledger {
//            transaction {
//                input(BoardContract.ID, boardState)
//                output(BoardContract.ID, boardState.returnNewBoardAfterMove(Pair(0,0)))
//                command(publicKeys, BoardContract.Commands.MakeMove())
//                this.verifies()
//            }
//            transaction {
//                input(BoardContract.ID, boardState)
//                input(BoardContract.ID, DummyState())
//                output(BoardContract.ID, boardState.returnNewBoardAfterMove(Pair(0,0)))
//                command(publicKeys, DummyCommand())
//                this `fails with` "There should be one input state."
//            }
//            transaction {
//                input(BoardContract.ID, boardState)
//                input(BoardContract.ID, boardState)
//                output(BoardContract.ID, boardState.returnNewBoardAfterMove(Pair(0,0)))
//                command(publicKeys, DummyCommand())
//                this `fails with` "There should be one input state."
//            }
//        }
//    }
//
//    @Test
//    fun makeMoveTransactionMustHaveOneOutput() {
//        val partyA = TestIdentity(CordaX500Name("PartyA","London","GB")).party
//        val partyB = TestIdentity(CordaX500Name("PartyB","New York","US")).party
//        val boardState = BoardState(partyA, partyB)
//        val publicKeys = boardState.participants.map {it.owningKey}
//
//        ledgerServices.ledger {
//            transaction {
//                input(BoardContract.ID, boardState)
//                output(BoardContract.ID, boardState.returnNewBoardAfterMove(Pair(0,0)))
//                command(publicKeys, BoardContract.Commands.MakeMove())
//                this.verifies()
//            }
//            transaction {
//                input(BoardContract.ID, boardState)
//                output(BoardContract.ID, boardState.returnNewBoardAfterMove(Pair(0,0)))
//                output(BoardContract.ID, boardState.returnNewBoardAfterMove(Pair(0,1)))
//                command(publicKeys, DummyCommand())
//                this `fails with` "There should be one input state."
//            }
//            transaction {
//                input(BoardContract.ID, boardState)
//                output(BoardContract.ID, boardState.returnNewBoardAfterMove(Pair(0,0)))
//                output(BoardContract.ID, DummyState())
//                command(publicKeys, DummyCommand())
//                this `fails with` "There should be one input state."
//            }
//        }
//    }





}