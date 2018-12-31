package com.example.fabio.myproof

import java.util.ArrayList
import java.util.Arrays

/**
 * Created by fabio on 29/03/2017.
 */

internal class Bracket : ArrayList<String>() {
    fun set(source: String) {
        clear()
        if (source.isEmpty()) return
        addAll(Arrays.asList(*source.split(" ".toRegex()).dropLastWhile { it.isEmpty() } .toTypedArray()))
    }

    override fun toString(): String = joinToString(" ")

    fun check(command: Command): Boolean {
        var output = false
        val iter = listIterator()
        while(iter.hasNext()) {
            val item = iter.next()
            when (item.replace("-", "")) {
                "Term" -> iter.set(item + "Variable")
                "Premise" -> iter.set(item + "Statement")
                "Token" -> iter.set(item + "VariableTermStatementPremiseSequent")
            }
            if (item.startsWith("-")) {
                output = output and !item.replace("-", "").contains(command.output())
                output = output and (item.replace("-", "") != command.name)
            } else {
                output = output or item.contains(command.output())
                output = output or (item == command.name)
            }
        }
        return output
    }
}
