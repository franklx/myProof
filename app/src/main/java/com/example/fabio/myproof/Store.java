package com.example.fabio.myproof;

import android.os.Environment;

import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import static android.text.TextUtils.join;

/**
 * Created by fabio on 20/06/2017.
 */

class Store extends HashMap<String,Command> {
    protected File path;
    //boolean loaded;
    ArrayList<String> names;
    private ArrayList<String> source;
    final Command BLANK = new Command();

    Store() {
        path = new File(Environment.getExternalStorageDirectory(), "myProof");
        names = new ArrayList<String>();
        source = new ArrayList<String>();
        //loaded=false;
    }

    boolean load() {
        try {
            File file = new File(path,"commands.txt");
            FileReader reader = new FileReader(file);
            BufferedReader buffer = new BufferedReader(reader);
            String line;
            while ((line = buffer.readLine())!=null) {
                ArrayList<String> temp=new ArrayList<String>();
                while (!line.matches("@\\w+")) {
                    temp.add(line);
                    line = buffer.readLine();
                }
                String name = line.substring(1);
                put(name,new Command(name,temp));
                temp.clear();
            }
        } catch (Exception e) {
            return false;
        }
        for (Command command:values().toArray(new Command[0]))
            command.loadDefinition();
        return true;
    }

    private String getFileName(String name) {return name.replace(" ","_")+".txt";}
    private String getCommandName(File file) {return file.getName().replace(".txt","");}

    public boolean add(Command command) {
        if (containsKey(command.name)) return false;
        put(command.name,command);
        return true;
    }
    public boolean set(String name,Steps definition) {
        // Set definition to the command with that name
        if (super.get(name)==null)
            put(name,new Command(name,definition));
        else
            super.get(name).setDefinition(definition);
        //save(get(i));
        return true;
    }
    public String set(int i,String name,ArrayList<String> source) {
        get(i).name=name;
        get(i).set(source);
        return names.set(i,name);
    }
    void update(String name) {}
    public Command get(String name) {
        if (name.isEmpty()) return super.get("blank");
        if (isConstant(name)) return new Command(name);
        Command output = super.get(name);
        if (output==null) {
            output = new Command(name);
            if (name.matches("\\w+"))
                put(name,output);
            return output;
        }
        return output;
    }

    private int indexOf(String id) {
        if (id.matches("[0-9]+")) {
            return Integer.parseInt(id);
        } else {
            return names.indexOf(id);
        }
    }

    private boolean isConstant(String name) {
        if (name.startsWith("\\")) return true;
        if (name.startsWith("§")) return true;
        if (name.startsWith("#")) return true;
        //if (name.length()==1) return true;
        return false;
    }

    void update() {
        clear();
        load();
    }
    void renameSource(String oldName,String newName) {
        File[] directory = path.listFiles();
        if (directory == null) return;
        String line;
        ArrayList<String> words = new ArrayList<>();
        ArrayList<String> lines = new ArrayList<>();
        for (File child : directory)
            if (child.getName().equals(getFileName(oldName))) {
                child.renameTo(new File(path,getFileName(newName)));
            } else try {
                FileReader reader = new FileReader(child);
                BufferedReader buffer = new BufferedReader(reader);
                lines.clear();
                while ((line = buffer.readLine()) != null) {
                    words.clear();
                    for (String item : line.split(" "))
                        if (item.equals(oldName))
                            words.add(newName);
                        else words.add(item);
                    lines.add(join(" ",words));
                }
                buffer.close();
                FileWriter writer = new FileWriter(child);
                writer.append(join("\n",lines));//for (String item : lines) writer.append(item+"\n");
                writer.flush();
                writer.close();
            } catch (Exception e) {}
        update();
    }

    private ArrayList<String> loadSource(String name) {
        ArrayList<String> source = new ArrayList<>();
        try {
            File file = new File(path,getFileName(name));
            FileReader reader = new FileReader(file);
            BufferedReader buffer = new BufferedReader(reader);
            String line;
            while ((line = buffer.readLine()) != null)
                source.add(line);
        } catch (Exception e) {}
        while (source.size()<4) source.add("");
        return source;
    }
    private Command load(String name) {
        ArrayList<String> source = loadSource(name);
        if (name.isEmpty()) return get("blank");
        else return new Command(name,source);
    }
    void save(Command command) {
        try {
            File file = new File(path,getFileName(command.name));
            FileWriter writer = new FileWriter(file);
            writer.append(command.getSource());
            writer.flush();
            writer.close();
        } catch (IOException e) {e.printStackTrace();}
    }
    void saveAll() {
        File file = new File(path,"commands.txt");
        try {
            FileWriter writer = new FileWriter(file);
            for (Command command:values()) {
                if (command.output().equals("Variable") && command.name.equals(command.latex))
                    continue;
                writer.append(command.getSource());
                writer.append("\n");
                writer.append("@");
                writer.append(command.name);
                writer.append("\n");
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    void loadAll() {
        ArrayList<String> source = new ArrayList<>();
        try {
            File file = new File(path,"commands.txt");
            FileReader reader = new FileReader(file);
            BufferedReader buffer = new BufferedReader(reader);
            int i=-1;
            String name="";
            while (true) {
                String line = buffer.readLine();
                if (line==null||line.matches("@\\d+:.+")) {
                    if (i >= 0) {
                        while (source.size() < 4) source.add("");
                        set(i, name, source);
                        source.clear();
                    }
                    if (line!=null) {
                        int dot = line.indexOf(":");
                        if (dot < 0) continue;
                        i = Integer.parseInt(line.substring(1, dot));
                        name = line.substring(dot + 1);
                    } else {
                        //loaded=true;
                        return;
                    }
                } else source.add(line);
            }
        } catch (Exception e) {
            //loaded=false;
        }
    }
    boolean backup() {
        File backup = new File(path,"backup.txt");
        File source = new File(path,"commands.txt");
        try {
            FileUtils.copyFile(source,backup);
            return true;
        } catch (IOException e) {
            int a=5;
            return false;
        }
    }
    boolean backup(String name) {
        File backup = new File(path.getParent(),"/Backup");
        File source = new File(path,getFileName(name));
        File target = new File(backup,getFileName(name));
        try {
            FileUtils.copyFile(source,target);
            return true;
        } catch (Exception e) {return false;}
    }
    boolean restore() {
        File backup = new File(path,"backup.txt");
        File target = new File(path,"commands.txt");
        try {
            FileUtils.copyFile(backup,target);
            return true;
        } catch (IOException e) {return false;}
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
    boolean importPack(String name) {
        File pack = new File(path.getParent(),"/"+name);
        try {
            FileUtils.copyDirectory(pack,path);
            update();
            return true;
        } catch (Exception e) {return false;}
    }
    boolean savePack(String name) {
        File pack = new File(path.getParent(),"/"+name);
        try {
            FileUtils.copyDirectory(path,pack);
            return true;
        } catch (Exception e) {return false;}
    }
    boolean newPack() {
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
        Date today = Calendar.getInstance().getTime();
        String reportDate = df.format(today);
        File backup = new File(path.getParent(),"/Backup");
        File copy = new File(path.getParent(),"/"+reportDate+" Backup");
        try {
            FileUtils.copyDirectory(backup,copy);
            FileUtils.copyDirectory(path,backup);
            FileUtils.cleanDirectory(path);
            update();
            return true;
        } catch (Exception e) {return false;}
    }
}
