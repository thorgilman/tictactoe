package com.template.states

import com.template.contracts.BoardContract
import com.template.contracts.TemplateContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party

// *********
// * State *
// *********

@BelongsToContract(BoardContract::class)
data class BoardState(val playerO: Party,
                      val playerX: Party,
                      val isPlayerXTurn: Boolean = true,
                      val board: Array<CharArray> = Array(3, { charArrayOf('E', 'E', 'E')})): ContractState {

    override val participants: List<AbstractParty> = listOf(playerO, playerX)


    val potentialWins: List<Set<Pair<Int, Int>>> = listOf(
            // Colomns
            setOf(Pair(0,0), Pair(0,1), Pair(0,2)),
            setOf(Pair(1,0), Pair(1,1), Pair(1,2)),
            setOf(Pair(2,0), Pair(2,1), Pair(2,2)),

            // Rows
            setOf(Pair(0,0), Pair(1,0), Pair(2,0)),
            setOf(Pair(0,1), Pair(1,1), Pair(2,1)),
            setOf(Pair(0,2), Pair(1,2), Pair(2,2)),

            // Diagonal
            setOf(Pair(0,0), Pair(1,1), Pair(2,2)),
            setOf(Pair(2,0), Pair(1,1), Pair(0,2))
    )


    fun returnNewBoardWithMove(player: Party, x: Int, y: Int): BoardState {

        val newBoard = board.copyOf() // need copyOf()?

        if (player.name.commonName == "Player X") newBoard[x][y] = 'X'
        else if (player.name.commonName == "Player O") newBoard[x][y] = 'O'
        // else err

        // Check if game over?

        return BoardState(this.playerO, this.playerX, !this.isPlayerXTurn, newBoard)
    }


    fun isGameOver(playerChar: Char): Boolean {

        for (potentialWin in potentialWins) {

            var gameOver = true
            for ((x,y) in potentialWin) {
                if (board[x][y] != playerChar) gameOver = false
            }
            if (gameOver) return true

        }
        return false
    }

}

