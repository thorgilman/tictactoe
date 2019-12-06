package com.template.states

import com.template.contracts.BoardContract
import com.template.schemas.BoardStateSchemaV1
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.serialization.CordaSerializable
import org.apache.commons.lang.ObjectUtils
import kotlin.IllegalStateException

@CordaSerializable
enum class Status {
    GAME_IN_PROGRESS, GAME_OVER
}

@BelongsToContract(BoardContract::class)
@CordaSerializable
data class BoardState(val playerO: Party,
                      val playerX: Party,
                      val isPlayerXTurn: java.lang.Boolean = java.lang.Boolean(false),
                      val board: Array<CharArray> = Array(3, {charArrayOf('E', 'E', 'E')} ),
                      val status: Status = Status.GAME_IN_PROGRESS,
                      val observer: Party? = null,
                      override val linearId: UniqueIdentifier = UniqueIdentifier()): LinearState, QueryableState {

    override val participants: List<AbstractParty> = listOfNotNull(playerO, playerX, observer)

    // Returns the party of the current player
    fun getCurrentPlayerParty(): Party { return if (isPlayerXTurn.booleanValue()) playerX else playerO }

    // Get deep copy of board
    private fun Array<CharArray>.copy() = Array(size) { get(it).clone() }

    // Returns a copy of a BoardState object after a move at Pair<x,y>
    fun returnNewBoardAfterMove(pos: Pair<Int,Int>): BoardState {
        if (pos.first > 2 || pos.second > 2) throw IllegalStateException("Invalid board index.")
        val newBoard = board.copy()
        if (isPlayerXTurn.booleanValue()) newBoard[pos.second][pos.first] = 'X'
        else newBoard[pos.second][pos.first] = 'O'

        val newBoardState = copy(board = newBoard, isPlayerXTurn = java.lang.Boolean(!isPlayerXTurn.booleanValue()))
        if (BoardContract.BoardUtils.isGameOver(newBoardState)) return newBoardState.copy(status = Status.GAME_OVER)
        return newBoardState
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(BoardStateSchemaV1)
    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        if (!(schema is BoardStateSchemaV1)) throw Exception()
        return BoardStateSchemaV1.PersistentBoardState(playerO, playerX, isPlayerXTurn, board, status, linearId.id)
    }

}

