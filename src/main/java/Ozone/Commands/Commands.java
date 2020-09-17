package Ozone.Commands;

import Atom.Time.Countdown;
import arc.util.Log;
import mindustry.Vars;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Commands {

    public static final HashMap<String, Command> commandsList = new HashMap<>();
    private static boolean init = false;

    public static void init() {
        if (init) return;
        init = true;
        commandsList.put("help", new Command(Commands::help, "Help desk"));
        commandsList.put("chaos-kick", new Command(Commands::DeceptionKick, "make hell lose with votekick"));
        commandsList.put("task-move", new Command(Commands::move, "Move like an AI"));
        commandsList.put("info-pos", new Command(Commands::pos, "Get current pos"));
        Log.infoTag("Ozone", "Commands Center Initialized");
    }

    public static boolean call(String message) {
        //if (!message.startsWith(Settings.commandsPrefix)) return false;
        String[] arg = message.replaceFirst(",", "").split(" ");
        if (!commandsList.containsKey(arg[0])) {
            tellUser("Commands not found");
            help(new ArrayList<>());
            return false;
        }
        Command comm = commandsList.get(arg[0]);
        ArrayList<String> args;
        if (message.contains(" ")) {
            message = message.replaceFirst(arg[0], "").replaceFirst(" ", "");
            arg = message.split(" ");
            args = new ArrayList<>(Arrays.asList(arg));
        } else {
            args = new ArrayList<>();
        }
        comm.method.accept(args);
        return true;
    }

    public static void pos(ArrayList<String> a) {
        tellUser("Player x,y: " + Vars.player.x + ", " + Vars.player.y);
        tellUser("Player tile x,y: " + Vars.player.tileX() + ", " + Vars.player.tileY());
    }

    public static void help(ArrayList<String> a) {
        StringBuilder sb = new StringBuilder();
        //sb.append("\n").append("Prefix: ").append(Settings.commandsPrefix).append("\n");
        sb.append("Available Commands:").append("\n");
        for (Map.Entry<String, Command> s : commandsList.entrySet()) {
            sb.append(s.getKey()).append(": ").append(s.getValue().description).append("\n");
        }
        tellUser(sb.toString());
    }

    public static void move(ArrayList<String> s) {
        if (s.size() < 2) {
            tellUser("Not enough arguments");
            tellUser("usage: " + "task-move x(coordinate) y(coordinate)");
            return;
        }
        try {
            int x = Integer.parseInt(s.get(0));
            int y = Integer.parseInt(s.get(1));
            if (Vars.world.tile(x, y) == null) {
                tellUser("Non existent tiles");
                return;
            }
            long start = System.currentTimeMillis();
            PlayerInterface.moveTo(x, y, a -> tellUser("Reached in " + Countdown.result(start, TimeUnit.SECONDS)));
        } catch (NumberFormatException f) {
            tellUser("Failed to parse integer, are you sure that argument was integer ?");
            Vars.ui.showException(f);
        }

    }

    //Nexity shitcode revised by Itzbenz
    private volatile static boolean falseVote = false;
    public static void DeceptionKick(ArrayList<String > unused) {
        falseVote = !falseVote;
        if (falseVote) {
            Thread s1 = new Thread(() -> {
                while (Vars.net.active() && falseVote)
                    for (Player target : Groups.player) {
                        if (!target.name.equals(Vars.player.name)) {
                            Call.sendChatMessage("/votekick " + target.name);
                            try { Thread.sleep(200); } catch (Throwable ignored) {}
                        }
                    }
            });
            s1.start();
            tellUser("kicking started");
        } else {
            tellUser("kicking ended");
        }
    }


    public static void tellUser(String s) {
        if(Vars.ui.scriptfrag.shown())
            Log.infoTag("Ozone", s);
        else
            Vars.ui.chatfrag.addMessage("[white][[royal]Ozone[white]]: " + s, null);
    }

    public static class Command {
        public final Consumer<ArrayList<String>> method;
        public final String description;

        public Command(Consumer<ArrayList<String>> method, String description) {
            this.method = method;
            this.description = description;
        }
    }
}
