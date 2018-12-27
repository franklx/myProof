package com.example.fabio.myproof

import java.util.ArrayList
import java.util.Arrays

import android.text.TextUtils.join

/**
 * Created by fabio on 09/04/2017.
 */

internal class Steps : ArrayList<Token> {
    var reduced: ArrayList<Token>
    var active: Int = 0
    var select: Int = 0
    var last: Int = 0

    val isBlank: Boolean
        get() = size == 1 && get(0).isBlank
    val lastReducedStep: Token
        get() {
            val n = reduced.size
            return if (n < 1)
                Token()
            else
                reduced[n - 1]
        }

    constructor() {
        reduced = ArrayList()
        add(Token())
        active = 0
        select = -1
        last = 0
    }

    constructor(source: String) {
        // Provide an input structure on (a shallow copy, i.e. with same members of) steps.
        reduced = ArrayList()
        if (source.isEmpty()) add(Token())
        for (line in source.split("\\n".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray())
            add(Token(line))
        active = size - 1
        select = -1
        last = 0
    }

    constructor(definition: Array<Token>) {
        reduced = ArrayList()
        if (definition.size == 0)
            add(Token())
        else
            for (item in definition)
                add(item.copy())
        active = size - 1
        select = -1
        last = 0
    }

    constructor(definition: Array<Token>, arg: Array<Token>) {
        // Apply this definition to the list of (reduced) tokens arg.
        reduced = ArrayList()
        var n = 0
        if (definition.size == 0)
            add(Token())
        else
            for (step in definition)
                if (step.isGeneric) {
                    if (!step.reducedCopy(reduced).fits(arg[n]))
                        add(arg[n], Token("error"))
                    else
                        add(arg[n], arg[n])
                    n++
                } else
                    add(step.copy())
        active = size - 1
        select = -1
        last = 0
    }

    override fun add(step: Token): Boolean {
        step.index = 0
        super.add(step)
        reduced.add(step.reducedCopy(reduced))
        return lastReducedStep.isComplete //reduced.get(size()-1).get(0).name.equals("error");
    }

    fun add(step: Token, reducedStep: Token) {
        step.index = 0
        reducedStep.index = 0
        super.add(step)
        reduced.add(reducedStep)
    }

    override fun add(i: Int, step: Token) {
        step.index = 0
        super.add(i, step)
        reduced.add(i, step.reducedCopy(reduced))
    }

    override fun remove(i: Int): Token {
        reduced.removeAt(i)
        return super.removeAt(i)
    }

    override fun clear() {
        super.clear()
        reduced.clear()
    }

    fun set(source: String) {
        clear()
        reduced.clear()
        if (source.isEmpty()) return
        val list = ArrayList(Arrays.asList<String>(*source.split("\\n".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()))
        for (line in list)
            add(Token(line))
    }

    override fun set(i: Int, step: Token): Token {
        val output = super.set(i, step)
        reduced[i] = step.reducedCopy(reduced)
        return output
    }

    private fun seek(i: Int): Int {
        val a = get(i)
        val b = get(i).copy()
        reduced[i] = b
        a.index = 0
        b.index = 0
        for (j in b.indices.reversed()) {
            if (b.index == 0)
                for (n in 0 until b.arity(j))
                    if (!b.get(j, n).check(b[j].type[n])) {
                        a.index = a.arg(j, n)
                        b.index = b.arg(j, n)
                        break
                    }
            if (b[j].isReducible)
                if (b.index <= j || b.next(j) <= b.index)
                    b.put(j, b[j].applyTo(b.getLeaf(j), reduced))
        }
        return a.index
    }

    fun seek(): Int {
        return seek(active)
    }

    private fun reduce(i: Int = active) {
        reduced[i] = get(i).reducedCopy(reduced)
    }

    @JvmOverloads
    fun reduceFrom(i: Int = active) {
        for (j in i until size)
            reduce(j)
    }

    fun reduceAll() {
        reduceFrom(0)
    }

    fun activeStep(): Token {
        return get(active)
    }

    fun activeToken(i: Int): Command {
        return get(active)[i]
    }

    private fun selectStep(): Token {
        return reduced[select]
    }

    private fun selectToken(i: Int): Command {
        return reduced[select][i]
    }

    fun reduceActiveStep() {
        reduce(active)
    }

    fun set(command: Command) {
        activeStep().put(command)
    }

    private fun modulo(i: Int): Int {
        val mod: Int
        if (select < 0)
            mod = size
        else
            mod = active // During selection mode only steps above the active one are navigable.
        var output = i
        while (output < 0)
            output += mod
        output %= mod
        return output
    }

    fun setActiveStep(i: Int) {
        activeStep().index = 0
        reduceActiveStep()
        last = active
        active = modulo(i)
        seek()
    }

    fun shiftActiveStep(t: Int) {
        setActiveStep(active + t)
    }

    fun shiftActiveToken(t: Int) {
        activeStep().shift(t)
        reduceActiveStep()
    }

    fun shiftActiveLeaf(t: Int) {
        activeStep().shiftLeaf(t)
        reduceActiveStep()
    }

    // Selection mode methods.
    fun onSelect(): Boolean {
        return select >= 0
    }

    fun setSelectStep(i: Int) {
        if (select >= 0) selectStep().index = 0
        last = select
        select = modulo(i) // select is computed modulo active step index.
        selectStep().index = 0
        activeStep().put(Command("§$select"))
        //activeStep().root().setOutput(selectToken(0).output());
        reduceActiveStep()
    }

    fun shiftSelectStep(t: Int) {
        setSelectStep(select + t)
    }

    fun shiftSelectToken(t: Int) {
        selectStep().shift(t)
        activeStep().put(Command("§$select"))
        //activeStep().root().setOutput(selectToken(0).output());
        selectStep().putReference(activeStep())
        reduceActiveStep()
    }

    fun shiftSelectLeaf(t: Int) {
        selectStep().shiftLeaf(t)
        activeStep().put(Command("§$select"))
        selectStep().putReference(activeStep())
        reduceActiveStep()
    }

    fun offSelect() {
        last = select
        select = -1
    }

    fun shiftReference(t: Int) {
        for (i in active + 1 until size)
            get(i).shiftReference(active, t)    // Shift references greater than lastActive by t.
    }

    fun getLaTeXCodeStep(i: Int): String {
        return if (onSelect())
            reduced[i].getLaTeXCode(i == select)
        else
            reduced[i].getLaTeXCode(i == active)
    }

    override fun toString(): String {
        val list = ArrayList<String>()
        for (step in this)
            list.add(step.toString())
        return join("\n", list)
    }
}
