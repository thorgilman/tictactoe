package com.template.contracts

import com.template.states.BoardState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction


class BoardContract : Contract {
    companion object {
        const val ID = "com.template.contracts.BoardContract"
    }

    override fun verify(tx: LedgerTransaction) {

        val command = tx.commands.requireSingleCommand<BoardContract.Commands>()

        when(command.value) {

            is Commands.StartGame -> requireThat{
                "There should be no input state." using (tx.inputs.isEmpty())
                "There should be one output state." using (tx.outputs.size == 1)
                "The output state should be of type BoardState." using (tx.outputs[0].data is BoardState)

                val outputBoardState = tx.outputStates[0] as BoardState
                "You cannot play a game with yourself." using (outputBoardState.playerO != outputBoardState.playerX)

                "Both parties together only may sign a StartGame transaction." using (command.signers == outputBoardState.participants.map { it.owningKey })
                // TODO
            }

            is Commands.MakeMove -> requireThat{
                "There should be one input state." using (tx.inputs.size == 1)
                "There should be one output state." using (tx.outputs.size == 1)
                "The input state should be of type BoardState." using (tx.inputStates.single() is BoardState)
                "The output state should be of type BoardState." using (tx.outputStates.single() is BoardState)

                val inputBoardState = tx.inputStates.single() as BoardState
                val outputBoardState = tx.outputStates.single() as BoardState

                "It cannot be the same players turn both in the input board and the output board." using (inputBoardState.isPlayerXTurn xor outputBoardState.isPlayerXTurn)

                val playerChar = if (inputBoardState.isPlayerXTurn) 'X' else 'O'

                println("\n\n\n----------")
                inputBoardState.printBoard()
                outputBoardState.printBoard()
                println("----------\n\n\n")

                "Not valid board update." using checkIfValidBoardUpdate(inputBoardState.board, outputBoardState.board, playerChar)

            }

            is Commands.EndGame -> requireThat{
                "There should be one input state." using (tx.inputs.size == 1)
                "There should be no output state." using (tx.outputs.isEmpty())
                "The input state should be of type BoardState." using (tx.inputs[0].state.data is BoardState)

                val inputBoardState = tx.inputStates.single() as BoardState
                "The game must be over." using (inputBoardState.isGameOver())


                // TODO
            }

        }

    }


    fun checkIfValidBoardUpdate(inputBoard: Array<CharArray>, outputBoard: Array<CharArray>, playerChar: Char): Boolean {

//        inputBoard.forEach { it.forEach { print(it) } }
//        println()
//        outputBoard.forEach { it.forEach { print(it) } }

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

        if (numUpdates != 1) {
            println("MORE THAN ONE UPDATE!!!!!!" + numUpdates)
            return false // Board should only be updated in one place
        }
        return true
    }


    interface Commands : CommandData {
        class StartGame : Commands
        class MakeMove : Commands
        class EndGame : Commands
    }

}