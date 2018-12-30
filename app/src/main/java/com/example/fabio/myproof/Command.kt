package com.example.fabio.myproof

import java.io.Serializable
import java.util.ArrayList
import java.util.HashSet

import android.text.TextUtils.join
import com.example.fabio.myproof.MainActivity.Companion.store

/**
 * Created by fabio on 13/03/2017.
 */

class Command : Serializable {
    lateinit var name: String
    lateinit internal var description: String
    lateinit var type: Array<String>
    lateinit internal var latex: String
    internal var brackets = emptyArray<Bracket>()
    internal var definition = emptyArray<Token>()
    internal var source = emptyArray<String>()
    internal val isBlank: Boolean
        get() = name == "blank"
    internal val isError: Boolean
        get() = name == "error"
    internal val isGeneric: Boolean
        get() {
            when (name) {
                "display_premise", "premise", "display_statement", "statement", "display_sentence", "sentence", "display_term", "term", "display_constant", "constant", "display_variable", "variable" -> return true
            }
            return false
        }
    internal val isReducible: Boolean
        get() = source.size > 0 || arity() > 0 || name.startsWith("§")
    val integerSource: String
        get() {
            val temp = ArrayList<String>()
            temp.add("@" + store.names.indexOf(name) + ":" + name)
            temp.add(description)
            temp.add(getType())
            temp.add(latex)
            temp.add(getBrackets())
            for (step in definition)
                temp.add(step.toIntegerString())
            return join("\n", temp)
        }

    internal constructor() { //Construct a blank command
        name = "blank"
        description = ""
        type = arrayOf("Premise")
        latex = ""
    }

    internal constructor(constant: String) {
        setConstant(constant)
    }

    internal constructor(commandName: String, commandDefinition: Steps) {
        name = commandName
        setDefinition(commandDefinition)
    }

    internal constructor(commandName: String, fileSource: MutableList<String>) {
        name = commandName
        while (fileSource.size < 4)
            fileSource.add("")
        description = fileSource[0]
        type = fileSource[1].split("->".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
        latex = fileSource[2]
        setBrackets(fileSource[3])
        source = Array(fileSource.size - 4) {fileSource[it + 4]}
    }

    internal fun loadDefinition() {
        if (definition.isEmpty())
            definition = Array(source.size) {Token(source[it])}
    }

    fun set(fileSource: List<String>) {
        description = fileSource[0].replace("  ", " blank ")
        type = fileSource[1].split("->".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
        latex = fileSource[2]
        setBrackets(fileSource[3])
        source = Array(fileSource.size - 4) {fileSource[it + 4]}
        definition = Array(fileSource.size - 4) {Token(fileSource[it + 4])}
    }

    fun setCommand(command: Command) {
        name = command.name
        description = command.description
        type = command.type
        latex = command.latex
        brackets = command.brackets
        definition = command.definition
        source = command.source
    }

    internal fun setConstant(s: String) {
        name = s //name = s.replace("\\","");
        description = ""
        if (s.startsWith("#")) {
            type = arrayOf("Token", "Token")
            latex = "\\sharp " + s.substring(1) + "(#1)"
        } else if (s.startsWith("§")) {
            type = arrayOf("Token")
            latex = s.replace("§", "")
        } else {
            type = arrayOf("Variable")
            latex = s
        }
        setBrackets()
    }

    internal fun setDefinition(steps: Steps) {
        definition = steps.toTypedArray()
        source = Array(steps.size) {steps[it].toString()}
        val typeList = ArrayList<String>()
        val argsList = ArrayList<String>()
        val seqsList = ArrayList<String>()
        var output: String
        var code: String
        for (i in steps.indices) {
            if (steps[i].isGeneric) {
                output = steps.reduced[i][0].output()
                code = steps.reduced[i].laTeXCode
                typeList.add(output)
                argsList.add(code + ":#" + typeList.size)
                if (output == "Sequent")
                    seqsList.add(code)
            }
        }
        val conclusion = steps.lastReducedStep
        val args = ("\\(\\begin{array}{l}"
                + join("\\\\ ", argsList)
                + "\\end{array}\\)")
        if (typeList.isEmpty())
            latex = conclusion.laTeXCode
        else
            latex = ("$$\\frac{"
                    + join("\\quad ", seqsList)
                    + "}{"
                    + conclusion.laTeXCode
                    + "}\\text{("
                    + name.replace("_", " ")
                    + ")}$$"
                    + args)
        typeList.add(conclusion[0].output())
        type = typeList.toTypedArray<String>()
        setBrackets()
        if (argsList.isEmpty()) description = conclusion.toString()
    }

    private fun setBrackets(n: Int = arity()) {
        if (brackets.size == n) return
        brackets = Array(n) {Bracket()}
    }

    internal fun setBrackets(source: String) {
        setBrackets()
        val array = source.split(",".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
        var n = array.size
        if (brackets.size < n) n = brackets.size
        for (i in 0 until n)
            brackets[i].set(array[i])
    }

    internal fun getBrackets(): String {
        val list = arrayOfNulls<String>(brackets.size)
        for ((i, b) in brackets.withIndex())
            list[i] = b.toString()
        return join(",", list)
    }

    internal fun setOutput(output: String) {
        type[arity()] = output
    }

    internal fun getType(): String {
        return join("->", type)
    }

    internal fun arity(): Int {
        return type.size - 1
    }

    internal fun output(): String {
        return type[arity()]
    }

    internal fun check(required: String): Boolean {
        var required = required
        // Check if this command has the required output.
        //if (output().equals("Token")) return true;  // Non-reduced token.
        when (required) {
            "Term" -> required += "Variable"
            "Premise" -> required += "Statement"
            "Token" -> return true
        }
        return required.contains(output())
    }

    internal fun equals(command: Command): Boolean {
        return name == command.name
    }

    private fun applyDefinitionTo(arg: Array<Token>): Token {
        // Apply these steps to the list of (reduced) tokens arg.
        val reduced = ArrayList<Token>()
        if (arg.size == 0 && !description.isEmpty()) return Token(description)
        var n = 0
        for (step in definition!!)
            if (step.isGeneric) {
                if (!step.reducedCopy(reduced).fits(arg[n]))
                    return Token("error")
                else
                    reduced.add(arg[n++])
            } else {
                reduced.add(step.reducedCopy(reduced))
                if (reduced[reduced.size - 1].isError)
                    return Token("error")
            }

        if (arg.size == 0) {
            description = reduced[reduced.size - 1].toString()
            store.save(this)
        }
        return reduced[reduced.size - 1]
    }

    internal fun applyTo(arg: Array<Token>, reducedSteps: List<Token>): Token {
        // Apply this command to the (reduced) token in arg.
        // Explicits references over reducedSteps.
        // Check argument type.
        for (n in arg.indices)
            if (!arg[n].root().check(type[n]))
                return Token("error")
        try {
            // root has definition.
            if (definition!!.size > 0)
                return applyDefinitionTo(arg)
            // root is a reference.
            if (name.startsWith("§")) {
                val i = Integer.parseInt(name.substring(1))
                val temp = reducedSteps[i].copy()
                temp.mergeStyle()
                return temp
            }
            // root is an argument.
            if (name.startsWith("#")) {
                val n = Integer.parseInt(name.substring(1))
                return arg[0].leaf(0, n)
            }
            //
            if (name.startsWith("display_"))
                return Token(name.replace("display_", ""), *arg)
            // root is a primitive command.
            when (name) {
                "comma"   // Merge two premises.
                -> {
                    var premise = arg[0].toHashSet()
                    premise.addAll(arg[1].toHashSet())
                    return Token(premise)
                }
                "drop"   // Drop the statement from the premise.
                -> {
                    var premise = arg[0].toHashSet()
                    premise.remove(arg[1])
                    return Token(premise)
                }
                "new" -> {
                    val temp = arg[0].copy()
                    while (arg[1].hasFreeOccurrenceOf(0, temp))
                    //arg[1].toTokenList().contains(temp)
                        temp.app("next")
                    return temp
                }
                "free" -> if (!arg[0].hasFreeOccurrenceOf(0, arg[1]))
                    // Reduces if arg[0] doesn't contains free occurrence of arg[1].
                        return arg[0]
                "substitute" -> return arg[0].substitution(arg[1], arg[2])
            }
        } catch (e: Exception) {
            return Token("error")
        }

        return Token(this, *arg)
    }

    override fun hashCode(): Int {
        return toString().hashCode()
    }

    override fun toString(): String {
        return name.replace(" ", "_")
    }

    fun getSource(): String {
        val a = arrayOf(description, getType(), latex, getBrackets()) + definition.map {it.toString()}
        return a.joinToString("\n")
    }

    internal fun rename(newName: String) {
        latex = latex.replace(name.replace("_", " "), newName.replace("_", " "))
        name = newName
    }
}