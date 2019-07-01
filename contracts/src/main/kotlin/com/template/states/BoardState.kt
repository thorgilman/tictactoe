package com.template.states

import com.template.contracts.BoardContract
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.QueryableState
import net.corda.core.serialization.CordaSerializable
import kotlin.IllegalStateException

// *********
// * State *
// *********

@BelongsToContract(BoardContract::class)
@CordaSerializable
data class BoardState(val playerO: Party,
                      val playerX: Party,
                      val isPlayerXTurn: Boolean = false,
                      val board: Array<CharArray> = Array(3, { charArrayOf('E', 'E', 'E')}),
                      override val linearId: UniqueIdentifier = UniqueIdentifier()): LinearState {

    override val participants: List<AbstractParty> = listOf(playerO, playerX)

    fun getCurrentPlayerParty(): Party { return if (isPlayerXTurn) playerX else playerO }

    // Get deep copy of board
    private fun Array<CharArray>.copy() = Array(size) { get(it).clone() }

    fun returnNewBoardAfterMove(pos: Pair<Int,Int>): BoardState {

        if (pos.first > 2 || pos.second > 2) throw IllegalStateException() // TODO ???

        val newBoard = board.copy()
        if (isPlayerXTurn) newBoard[pos.second][pos.first] = 'X'
        else newBoard[pos.second][pos.first] = 'O'
        return copy(board = newBoard, isPlayerXTurn = !isPlayerXTurn)
    }

    fun printBoard() {
        println("  1 2 3")
        var i = 1
        for (charArray in board) {
            print(i)
            print(" ")
            for (c in charArray) {
                print(c + " ")
            }
            println()
            i++
        }
    }

}

