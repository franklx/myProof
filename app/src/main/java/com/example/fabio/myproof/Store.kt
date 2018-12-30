package com.example.fabio.myproof

import android.os.Environment
import android.util.Log

import org.apache.commons.io.FileUtils

import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Calendar
import java.util.Date
import java.util.HashMap

import android.text.TextUtils.join

/**
 * Created by fabio on 20/06/2017.
 */

class Store : HashMap<String, Command>() {
    var path: File
    var names: ArrayList<String>
    private val source: ArrayList<String>
    val BLANK = Command()

    init {
        path = File(Environment.getExternalStorageDirectory(), "myProof")
        names = ArrayList()
        source = ArrayList()
    }

    fun load(): Boolean {
        try {
            val cmdf = File(path, "commands.txt")
            var temp = ArrayList<String>()
            for(line in cmdf.readLines()) {
                if(!line.startsWith('@')) {
                    temp.add(line)
                    continue
                }
               put(line.substring(1), Command(line.substring(1), temp))
               temp = ArrayList<String>()
            }
            return true
        } catch (e: Exception) {
            Log.e("cmd", e.toString(), e)
            return false
        }

    }

    private fun getFileName(name: String): String {
        return name.replace(" ", "_") + ".txt"
    }

    private fun getCommandName(file: File): String {
        return file.name.replace(".txt", "")
    }

    fun add(command: Command): Boolean {
        if (containsKey(command.name)) return false
        put(command.name, command)
        return true
    }

    operator fun set(name: String, definition: Steps): Boolean {
        // Set definition to the command with that name
        if (get(name) == null)
            put(name, Command(name, definition))
        else
            get(name)?.setDefinition(definition)
        //save(get(i));
        return true
    }

    fun update(name: String) {
        val command = get(name)
        command?.loadDefinition()
    }

    override fun get(name: String): Command {
        if (name.isEmpty()) return BLANK
        if (isConstant(name)) return Command(name)
        var output: Command? = get(name)
        if (output == null) {
            output = Command(name)
            put(name, output)
            return output
        }
        output.loadDefinition()
        return output
    }

    private fun isConstant(name: String): Boolean {
        if (name.startsWith("\\")) return true
        if (name.startsWith("§")) return true
        return if (name.startsWith("#")) true else false
        //if (name.length()==1) return true;
    }

    fun update() {
        clear()
        names.clear()
        source.clear()
        load()
    }

    fun renameSource(oldName: String, newName: String) {
        val directory = path.listFiles() ?: return
        var line: String
        val words = ArrayList<String>()
        val lines = ArrayList<String>()
        for (child in directory)
            if (child.name == getFileName(oldName)) {
                child.renameTo(File(path, getFileName(newName)))
            } else
                try {
                    lines.clear()
                    for(line in child.readLines()) {
                        words.clear()
                        for (item in line.split(" ".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray())
                            if (item == oldName)
                                words.add(newName)
                            else
                                words.add(item)
                        lines.add(join(" ", words))
                    }
                    val writer = FileWriter(child)
                    writer.append(join("\n", lines))
                    writer.flush()
                    writer.close()
                } catch (e: Exception) {
                }

        update()
    }

    fun save(command: Command) {
        try {
            val file = File(path, getFileName(command.name))
            val writer = FileWriter(file)
            writer.append(command.getSource())
            writer.flush()
            writer.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    fun saveAll() {
        val file = File(path, "commands.txt")
        try {
            val writer = FileWriter(file)
            for (command in values) {
                writer.append(command.getSource())
                writer.append("\n")
                writer.append("@")
                writer.append(command.name)
                writer.append("\n")
            }
            writer.flush()
            writer.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    fun backup(): Boolean {
        val backup = File(path, "backup.txt")
        val source = File(path, "commands.txt")
        try {
            FileUtils.copyFile(source, backup)
            return true
        } catch (e: IOException) {
            val a = 5
            return false
        }

    }

    fun backup(name: String): Boolean {
        val backup = File(path.parent, "/Backup")
        val source = File(path, getFileName(name))
        val target = File(backup, getFileName(name))
        try {
            FileUtils.copyFile(source, target)
            return true
        } catch (e: Exception) {
            return false
        }

    }

    fun restore(): Boolean {
        val backup = File(path, "backup.txt")
        val target = File(path, "commands.txt")
        try {
            FileUtils.copyFile(backup, target)
            return true
        } catch (e: IOException) {
            return false
        }

    }

    /*boolean restore(String name) {
        File backup = new File(path.getParent(),"/Backup");
        File source = new File(backup,getFileName(name));
        File target = new File(path,getFileName(name));
        try {
            FileUtils.copyFile(source,target);
            if (!names.contains(name)) {
                names.add(name);
                super.add(load(name));
            }
            return true;
        } catch (Exception e) {return false;}
    }*/
    fun importPack(name: String): Boolean {
        val pack = File(path.parent, "/$name")
        try {
            FileUtils.copyDirectory(pack, path)
            update()
            return true
        } catch (e: Exception) {
            return false
        }

    }

    fun savePack(name: String): Boolean {
        val pack = File(path.parent, "/$name")
        try {
            FileUtils.copyDirectory(path, pack)
            return true
        } catch (e: Exception) {
            return false
        }

    }

    fun newPack(): Boolean {
        val df = SimpleDateFormat("yyyyMMdd")
        val today = Calendar.getInstance().time
        val reportDate = df.format(today)
        val backup = File(path.parent, "/Backup")
        val copy = File(path.parent, "/$reportDate Backup")
        try {
            FileUtils.copyDirectory(backup, copy)
            FileUtils.copyDirectory(path, backup)
            FileUtils.cleanDirectory(path)
            update()
            return true
        } catch (e: Exception) {
            return false
        }

    }
}
