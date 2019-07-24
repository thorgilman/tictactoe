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

@CordaSerializable
enum class Status {
    GAME_IN_PROGRESS, GAME_OVER
}

@BelongsToContract(BoardContract::class)
@CordaSerializable
data class BoardState(val playerO: Party,
                      val playerX: Party,
                      val isPlayerXTurn: Boolean = false,
                      val board: Array<CharArray> = Array(3, { charArrayOf('E', 'E', 'E')}),
                      val status: Status = Status.GAME_IN_PROGRESS,
                      override val linearId: UniqueIdentifier = UniqueIdentifier()): LinearState {

    override val participants: List<AbstractParty> = listOf(playerO, playerX)

    // Returns the party of the current player
    fun getCurrentPlayerParty(): Party { return if (isPlayerXTurn) playerX else playerO }

    // Get deep copy of board
    private fun Array<CharArray>.copy() = Array(size) { get(it).clone() }

    // TODO: Also move to BoardContract?
    // Returns a copy of a BoardState object after a move at Pair<x,y>
    fun returnNewBoardAfterMove(pos: Pair<Int,Int>): BoardState {
        if (pos.first > 2 || pos.second > 2) throw IllegalStateException("Invalid board index.")
        val newBoard = board.copy()
        if (isPlayerXTurn) newBoard[pos.second][pos.first] = 'X'
        else newBoard[pos.second][pos.first] = 'O'

        val newBoardState = copy(board = newBoard, isPlayerXTurn = !isPlayerXTurn)
        if (BoardContract.BoardUtils.isGameOver(newBoardState)) return newBoardState.copy(status = Status.GAME_OVER)
        return newBoardState
    }

}

