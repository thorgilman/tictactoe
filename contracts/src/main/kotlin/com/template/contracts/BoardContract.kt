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
            is Commands.Place -> requireThat{
                "There should be one input state." using (tx.inputs.size == 1)
                "There should be one output state." using (tx.outputs.size == 1)
                "The input state should be of type BoardState." using (tx.inputs.get(0).state.data is BoardState)
                "The output state should be of type BoardState." using (tx.outputs.get(0).data is BoardState)

                val inputBoardState = tx.inputStates.get(0) as BoardState
                val outputBoardState = tx.inputStates.get(0) as BoardState

                "It cannot be the same players turn both in the input board and the output board" using (inputBoardState.isPlayerXTurn != outputBoardState.isPlayerXTurn)

                val playerChar = if (inputBoardState.isPlayerXTurn) 'X' else 'O'
                "Not valid board update." using checkIfValidBoardUpdate(inputBoardState.board, outputBoardState.board, playerChar)


            }
        }

    }


    fun checkIfValidBoardUpdate(inputBoard: Array<CharArray>, outputBoard: Array<CharArray>, playerChar: Char): Boolean {
        var numUpdates = 0
        for (x in (0..2)) {
            for (y in (0..2)) {

                val inputVal = inputBoard[x][y]
                val outputVal = outputBoard[x][y]

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



    interface Commands : CommandData {
        class Place : Commands
    }





}