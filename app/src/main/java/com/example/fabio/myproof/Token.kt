package com.example.fabio.myproof

import android.text.TextUtils

import java.util.ArrayList
import java.util.HashSet

import com.example.fabio.myproof.MainActivity.Companion.store

/**
 * Created by fabio on 13/03/2017.
 */

class Token : ArrayList<Command> {
    var index: Int = 0

    internal val isComplete: Boolean
        get() = true
    internal// Check if this token contains a generic command.
    val isGeneric: Boolean
        get() {
            for (item in this)
                if (item.isGeneric)
                    return true
            return false
        }

    val isBlank: Boolean
        get() = get(0).isBlank
    val isError: Boolean
        get() {
            for (item in this)
                if (item.name == "error")
                    return true
            return false
        }
    internal val laTeXCode: String
        get() = getLaTeXCode(false)

    constructor() {
        add(store.BLANK)
        index = 0
    }

    constructor(list: List<Command>) {
        if (list.isEmpty())
            add(store.BLANK)
        else
            addAll(list)
        index = 0
        resize()
    }

    constructor(command: Command, vararg arg: Token) {
        add(command)
        for (item in arg) addAll(item)
        index = 0
        resize()
    }

    constructor(commandName: String, vararg arg: Token) : this(store.get(commandName), *arg) {}
    constructor(source: String) {
        if (source.isEmpty())
            super.add(store.BLANK)
        else
            for (item in source.split(" ".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()) {
                super.add(store.get(item))
            }
        index = 0
        resize()
    }

    constructor(list: HashSet<Token>) : this() {
        for (item in list) {
            if (item.isBlank)
            else if (isBlank)
                put(item)
            else
                app("comma", item)
        }
    }// Make list into a premise.

    private fun resize() {
        val n = next(0)
        if (n < size)
            removeRange(n, size)
        else
            while (n > size) super.add(store.BLANK)
    }

    internal fun arity(i: Int): Int = get(i)?.arity()

    internal fun next(i: Int): Int {
        var i = i
        var output = i + 1
        var steps = 1
        while (steps > 0) {
            val a = arity(i)
            output += a
            steps += a - 1
            i++
        }
        return output
    }

    private fun subToken(i: Int): Token {
        return Token(subList(i, next(i)))
    }

    fun root(): Command {
        return get(index)
    }

    internal fun getLeaf(i: Int): Array<Token> {
        val i = i + 1
        return Array(arity(i)) {Token(subList(i, next(i)))}
    }

    internal operator fun get(i: Int, n: Int): Command {
        return get(arg(i, n))
    }

    internal fun arg(i: Int, n: Int): Int {
        var i = i
        var n = n
        i++
        while (n-- > 0) i = next(i)
        return i
    }

    internal fun leaf(i: Int, n: Int): Token {
        return subToken(arg(i, n))
    }

    public override fun removeRange(i: Int, j: Int) {
        super.removeRange(i, j)
        if (index < i)
        else if (index < j)
            index = -1
        else
            index = index - j + i
    }

    override fun add(i: Int, command: Command) {
        super.add(i, command)
        if (index == -1)
            index = i
        else if (index >= i) index++
    }

    override fun addAll(i: Int, token: Collection<Command>): Boolean {
        if (index == -1)
            index = i
        else if (index >= i) index += token.size
        return super.addAll(i, token)
    }

    private fun cut(i: Int): Token {
        val j = next(i)
        val output = Token(subList(i, j))
        removeRange(i, j)
        return output
    }

    internal fun put(i: Int, token: Token) {
        removeRange(i, next(i))
        addAll(i, token)
    }

    private fun put(token: Token) {
        put(index, token)
    }

    internal fun put(source: String) {
        put(Token(source))
    }

    internal fun put(i: Int, source: String) {
        put(i, Token(source))
    }

    private fun put(i: Int, command: Command) {
        var i = i
        removeRange(i, next(i))
        add(i++, command)
        for (n in 0 until command.arity())
            add(i, store.BLANK)
    }

    internal fun put(command: Command) {
        put(index, command)
    }

    fun app(i: Int, command: Command, vararg arg: Token) {
        var i = i
        val arg0 = cut(i)
        add(i++, command)
        for (n in 0 until command.arity())
            if (n == 0) {
                addAll(i, arg0)
                i += arg0.size
            } else {
                addAll(i, arg[n - 1])
                i += arg[n - 1].size
            }
    }

    fun app(name: String, vararg arg: Token) {
        app(index, store.get(name), *arg)
    }

    fun app(i: Int, command: Command) {
        var i = i
        val arg0 = cut(i)
        add(i++, command)
        for (n in 0 until command.arity())
            if (n == 0) {
                addAll(i, arg0)
                i += arg0.size
            } else
                add(i, store.BLANK)
    }

    fun app(i: Int, name: String) {
        app(i, store.get(name))
    }

    fun app(command: Command) {
        app(index, command)
    }

    fun app(name: String) {
        app(index, store.get(name))
    }

    fun copy(): Token {
        val output = clone() as Token
        output.index = index
        return output
    }

    private fun putReference(i: Int, active: Token) {
        var j = 0
        if (i < 0 || i >= size) active.put(store.BLANK)
        while (j < size)
            if (j++ == i)
                return
            else {
                var k = 0
                var temp = i + 1
                while (temp <= i) {
                    temp = next(j)
                    j = temp
                    k++
                }
                active.app(Command("#$k"))
                //active.root().setOutput(get(j).output());   // deprecated
            }

    }

    internal fun putReference(active: Token) {
        var i = 0
        if (index >= size) active.put(store.BLANK)
        while (i < size)
            if (i++ == index)
                return
            else {
                var k = 0
                var temp = index + 1
                while (temp <= index) {
                    temp = next(i)
                    i = temp
                    k++
                }
                active.app(Command("#$k"))
                //active.root().setOutput(get(i).output());   // deprecated
            }

    }

    internal fun shiftReference(i: Int, t: Int) {
        // Shift references greater than i by t.
        for (item in this)
            if (item.name.startsWith("§")) {
                val j = Integer.parseInt(item.name.substring(1))
                if (t < 0 && j == i) item.setCommand(store.BLANK)
                if (j > i) item.setConstant("§" + (j + t))
            }
    }

    private fun compare(i: Int, j: Int): Boolean {
        // Compare the subtoken at i with that at j.
        val u = next(i)
        if (j + u - i != next(j)) return false
        for (k in i until u)
            if (!get(k).equals(get(j + k - i)))
                return false
        return true
    }

    private fun getBounderReference(i: Int): Token {
        // Return the reference of the command which bounds the token at i
        val temp = Token()
        var j = i
        while (j-- > 0)
            for (n in 0 until arity(j))
                if (get(j).type[n] == "Variable")
                    if (i < next(j) && compare(i, arg(j, n))) {
                        putReference(arg(j, n), temp)
                        return temp
                    }
        return temp
    }

    private fun reduce(reducedSteps: List<Token>) {
        for (i in size - 1 downTo 0)
            if (get(i).isReducible)
                if (index <= i || next(i) <= index)
                    put(i, get(i).applyTo(getLeaf(i), reducedSteps))
    }

    internal fun reducedCopy(reducedSteps: List<Token>): Token {
        val output = copy()
        output.reduce(reducedSteps)
        return output
    }

    internal fun fits(t: Token): Boolean {
        // Check if token t fits into this token.
        var i = 0
        var j = 0
        while (i < size) {
            var temp = getBounderReference(i)
            if (get(i).isGeneric)
            // The subtoken at i is generic
                if (t[j].check(get(i).output())) {
                    i = next(i)
                    j = t.next(j)
                } else
                    return false
            else if (!temp.isBlank)
            // The subtoken at i is bounded
                if (temp == t.getBounderReference(j)) {
                    i = next(i)
                    j = t.next(j)
                } else
                    return false
            else if (t[j].equals(get(i))) {
                i++
                j++
            } else
                return false
        }
        return true
    }

    private fun getBoundedVariables(i: Int): List<Token> {
        // Return the list of variables bounded by the command located at position i.
        val output = ArrayList<Token>()
        for (n in 0 until arity(i))
            if (get(i).type[n] == "Variable")
                output.add(leaf(i, n))
        return output
    }

    private fun bounds(i: Int, variable: Token): Boolean {
        // Check if the command at i bounds the given variable.
        for (n in 0 until arity(i))
            if (get(i).type[n] == "Variable")
                if (contains(arg(i, n), variable))
                    return true
        return false
    }

    internal fun hasFreeOccurrenceOf(i: Int, variable: Token): Boolean {
        var j = i
        val u = next(i)
        while (j < u) {
            if (bounds(j, variable))
                j = next(j)
            else if (contains(j, variable))
                return true
            else
                j++
        }
        return false
    }

    private fun generalSubstitution(i: Int, variable: Token, term: Token) {
        var j = i
        while (j < next(i))
            if (contains(j, variable)) {
                put(j, term)
                j += term.size
            } else if (!hasFreeOccurrenceOf(j, variable))
                j = next(j)
            else {
                val bounded = getBoundedVariables(j)
                for (item in bounded) {
                    val temp = item.copy()
                    while (term.hasFreeOccurrenceOf(0, temp)
                            || hasFreeOccurrenceOf(j, temp)
                            || temp != item && bounded.contains(temp))
                        temp.app(0, "next")
                    if (temp != item)
                        for (n in 0 until arity(j))
                            generalSubstitution(arg(j, n), item, temp)
                }
                j++
            }
    }

    internal fun substitution(variable: Token, term: Token): Token {
        val output = copy()
        output.generalSubstitution(0, variable, term)
        return output
    }

    private fun contains(i: Int, t: Token): Boolean {
        for (j in t.indices)
            if (i + j >= size)
                return false
            else if (!get(i + j).equals(t[j])) return false
        return true
    }

    private fun modulo(i: Int, x: Int): Int {
        var x = x
        if (i < 0 || i >= size) return 0
        val m = next(i) - i
        while (x < i) x += m
        return i + (x - i) % m
    }

    private fun getParent(i: Int): Int {
        var j = i
        while (j-- > 0)
            if (i < next(j))
                break
        return j
    }

    fun shift(t: Int) {
        index = modulo(0, index + t)
    }

    internal fun shiftLeaf(t: Int) {
        var t = t
        val i = getParent(index)
        val m = arity(i)
        if (m == 0) return
        while (t < 0)
            t += m
        t %= m
        while (t-- > 0) {
            index = modulo(i, next(index))
            if (index == i)
                index++
        }
        //index = modulo(index);
    }

    internal fun toHashSet(): HashSet<Token> {
        val output = HashSet<Token>()
        val temp = copy()
        val comma = store.get("comma")
        while (temp.contains(comma)) temp.remove(comma)
        while (!temp.isEmpty())
            output.add(temp.cut(0))
        return output
    }

    private fun toStringList(): ArrayList<String> {
        val output = ArrayList<String>()
        for (item in this)
            output.add(item.name)
        return output
    }

    private fun toIntegerStringList(): ArrayList<String> {
        val output = ArrayList<String>()
        for (item in this) {
            val i = store.names.indexOf(item.name)
            if (i < 0)
                output.add(item.name)
            else
                output.add(Integer.toString(i))
        }
        return output
    }

    override fun hashCode(): Int {
        return toString().hashCode()
    }

    override fun equals(o: Any?): Boolean {
        return hashCode() == o!!.hashCode()
    }

    override fun toString(): String {
        return TextUtils.join(" ", toStringList())
    }

    fun toIntegerString(): String {
        return TextUtils.join(" ", toIntegerStringList())
    }

    internal fun getLaTeXCode(active: Boolean): String {
        var temp = Array(size) {get(size-it-1).latex}
        for (i in size - 1 downTo 0) {
            var k = i + 1
            for (j in 0 until arity(i)) {
                if (get(i).brackets[j].check(get(k)))
                    temp[k] = "\\left(" + temp[k] + "\\right)"
                temp[i] = temp[i].replace("#${j+1}", temp[k])
                k = next(k)
            }
            if (active && i == index)
                if (get(i).name == "split") {
                    val list = temp[i]?.split("\\\\\\\\".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
                    for (u in list.indices)
                        list[u] = "\\color{Red}{" + list[u] + "}"
                    temp[i] = TextUtils.join("\\\\", list) + "|"
                } else if (!temp[i].contains("$$"))
                    temp[i] = "\\color{Red}{" + temp[i] + "}|"
        }
        return temp[0]
    }

    internal fun toggleSplitStyle() {
        for (i in 0 until size)
            when (get(i).name) {
                "sequent" -> set(i, store.get("therefore"))
                "therefore" -> set(i, store.get("sequent"))
                "comma" -> set(i, store.get("split"))
                "split" -> set(i, store.get("comma"))
            }
    }

    internal fun mergeStyle() {
        for (i in 0 until size)
            when (get(i).name) {
                "therefore" -> set(i, store.get("sequent"))
                "split" -> set(i, store.get("comma"))
            }
    }
}