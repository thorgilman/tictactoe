package com.template.states

import com.template.contracts.BoardContract
import com.template.contracts.TemplateContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.internal.checkMinimumPlatformVersion
import net.corda.core.serialization.ConstructorForDeserialization
import net.corda.core.serialization.CordaSerializable

// *********
// * State *
// *********

@BelongsToContract(BoardContract::class)
@CordaSerializable
data class BoardState(val playerO: Party,
                      val playerX: Party,
                      val isPlayerXTurn: Boolean = false,
                      val board: Array<CharArray> = Array(3, { charArrayOf('E', 'E', 'E')}),
                      val linearId: UniqueIdentifier = UniqueIdentifier()): ContractState {


    override val participants: List<AbstractParty> = listOf(playerO, playerX)


    private val potentialWins: List<Set<Pair<Int, Int>>> = listOf(
            // Columns
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


    fun getCurrentPlayerParty(): Party {
        return if (isPlayerXTurn) playerX else playerO
    }

    fun returnNewBoardAfterMove(pos: Pair<Int,Int>): BoardState {

        val newBoard = board.copyOf()

        if (isPlayerXTurn) newBoard[pos.second][pos.first] = 'X'
        else newBoard[pos.second][pos.first] = 'O'

        //if (isGameOver()) System.out.println("GAME OVER!!!")

        return copy(board = newBoard, isPlayerXTurn = !isPlayerXTurn)
    }

    fun printBoard() {

        System.out.println("  A B C")

        var i = 1
        for (charArray in board) {
            System.out.print(i)
            System.out.print(" ")
            for (c in charArray) {
                System.out.print(c + " ")
            }
            System.out.println()
            i++
        }
    }


    fun isGameOver(): Boolean {

        if (board.flatMap{ it.asList() }.indexOf('E') == -1) return true

        for (potentialWin in potentialWins) {
            var gameOver = true
            for ((x,y) in potentialWin) {
                if (board[x][y] != 'O') gameOver = false
            }
            if (gameOver) return true
        }
        for (potentialWin in potentialWins) {
            var gameOver = true
            for ((x,y) in potentialWin) {
                if (board[x][y] != 'X') gameOver = false
            }
            if (gameOver) return true
        }
        return false
    }


    fun getWinner(): Party? {

        if (board.flatMap { it.asList() }.indexOf('E') == -1) return null

        for (potentialWin in potentialWins) {
            var oWin = true
            for ((x,y) in potentialWin) {
                if (board[x][y] != 'O') oWin = false
            }
            if (oWin) return playerO
        }
        for (potentialWin in potentialWins) {
            var xWin = true
            for ((x,y) in potentialWin) {
                if (board[x][y] != 'X') xWin = false
            }
            if (xWin) return playerX
        }

        return null
    }

}

