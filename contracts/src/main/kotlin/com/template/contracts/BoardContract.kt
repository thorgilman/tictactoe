package com.template.contracts

import com.template.states.BoardState
import com.template.states.Status
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.identity.Party
import net.corda.core.transactions.LedgerTransaction

class BoardContract : Contract {
    companion object {
        const val ID = "com.template.contracts.BoardContract"
    }

    interface Commands : CommandData {
        class StartGame : Commands
        class SubmitTurn : Commands
        class EndGame : Commands
    }

    override fun verify(tx: LedgerTransaction) {

        val command = tx.commands.requireSingleCommand<Commands>()
        when(command.value) {

            is Commands.StartGame -> requireThat{
                "There should be no input state." using (tx.inputs.isEmpty())
                "There should be one output state." using (tx.outputs.size == 1)
                "The output state should be of type BoardState." using (tx.outputs[0].data is BoardState)

                val outputBoardState = tx.outputStates[0] as BoardState
                "Output board must have status GAME_IN_PROGRESS" using (outputBoardState.status == Status.GAME_IN_PROGRESS)
                "You cannot play a game with yourself." using (outputBoardState.playerO != outputBoardState.playerX)
                "Both parties together only may sign a StartGame transaction." using (command.signers == outputBoardState.participants.map { it.owningKey })
            }

            is Commands.SubmitTurn -> requireThat{
                "There should be one input state." using (tx.inputs.size == 1)
                "There should be one output state." using (tx.outputs.size == 1)
                "The input state should be of type BoardState." using (tx.inputStates.single() is BoardState)
                "The output state should be of type BoardState." using (tx.outputStates.single() is BoardState)

                val inputBoardState = tx.inputStates.single() as BoardState
                val outputBoardState = tx.outputStates.single() as BoardState
                "Input board must have status GAME_IN_PROGRESS." using (inputBoardState.status == Status.GAME_IN_PROGRESS)
                "Participants should not change." using (inputBoardState.participants == outputBoardState.participants)
                "It cannot be the same players turn both in the input board and the output board." using (inputBoardState.isPlayerXTurn xor outputBoardState.isPlayerXTurn)

                val playerChar = if (inputBoardState.isPlayerXTurn) 'X' else 'O'
                "Not valid board update." using BoardUtils.checkIfValidBoardUpdate(inputBoardState.board, outputBoardState.board, playerChar)
            }
            is Commands.EndGame -> requireThat{
                "There should be one input state." using (tx.inputs.size == 1)
                "There should be no output state." using (tx.outputs.isEmpty())
                "The input state should be of type BoardState." using (tx.inputs[0].state.data is BoardState)

                val inputBoardState = tx.inputStates.single() as BoardState
                "Input board must have status GAME_OVER." using (inputBoardState.status == Status.GAME_OVER)
                "The game must be over." using (BoardUtils.isGameOver(inputBoardState))
            }
        }
    }


    class BoardUtils {
        companion object {

            val potentialWins: List<Set<Pair<Int, Int>>> = listOf(
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

            fun checkIfValidBoardUpdate(inputBoard: Array<CharArray>, outputBoard: Array<CharArray>, playerChar: Char): Boolean {
                var numUpdates = 0
                for (x in (0..2)) {
                    for (y in (0..2)) {
                        val inputVal = inputBoard[y][x]
                        val outputVal = outputBoard[y][x]
                        if (inputVal == 'E') { // Space on board was empty
                            if (outputVal != 'E') { // Space on board isn't empty anymore
                                if (outputVal != playerChar) return false // Board was updated with the wrong players char
                                numUpdates++
                            }
                        }
                        else { // Space on board wasn't empty
                            if (inputVal != outputVal) return false // If non empty space was overridden, invalid board
                        }
                    }
                }
                if (numUpdates != 1) return false // Board should only be updated in one place
                return true
            }

            fun isGameOver(boardState: BoardState): Boolean {
                val board: Array<CharArray> = boardState.board
                if (board.flatMap{ it.asList() }.indexOf('E') == -1) return true

                for (potentialWin in potentialWins) {
                    val c = potentialWin.map { (x,y) -> board[x][y] }.distinct().singleOrNull()
                    if (c != null && (c == 'O' || c == 'X')) return true
                }
                return false
            }

            fun getWinner(boardState: BoardState): Party? {
                val board: Array<CharArray> = boardState.board
                if (board.flatMap { it.asList() }.indexOf('E') == -1) return null

                for (potentialWin in potentialWins) {
                    val c = potentialWin.map { (x,y) -> board[x][y] }.distinct().singleOrNull()
                    if (c == null) continue
                    if (c == 'O') return boardState.playerO
                    if (c == 'X') return boardState.playerX
                }
                return null
            }

        }
    }
}