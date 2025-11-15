package cn.plumc.ultimatech.utils;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

public class TickUtil {
    private static final Queue<Runnable> tasks = new LinkedList<>();
    private static final List<Runnable> recurringTasks = new LinkedList<>(); ;
    private static final Map<Runnable, IntCounter> delayTask = new ConcurrentHashMap<>();

    public static void tick() {
        if (!tasks.isEmpty()) {
            tasks.poll().run();
        }
        if (!delayTask.isEmpty()) {
            for (Map.Entry<Runnable, IntCounter> entry : delayTask.entrySet()) {
                entry.getValue().set(entry.getValue().get()-1);
                if (entry.getValue().get() <= 0){
                    entry.getKey().run();
                    delayTask.remove(entry.getKey());
                }
            }
        }
        for (Runnable task:recurringTasks){
            task.run();
        }
    }

    public static void tickRun(Runnable task){
        TickUtil.tasks.add(task);
    }

    public static void runAfterTick(Runnable task, double delay){
        int tick = (int) (delay * 20);
        delayTask.put(task, new IntCounter(tick));
    }

    public static int addTask(Runnable task){
        recurringTasks.add(task);
        return recurringTasks.size() - 1;
    }

    public static void removeTask(int index){
        recurringTasks.remove(index);
    }

    public static void cancelDelayTask(){delayTask.clear();}
}
