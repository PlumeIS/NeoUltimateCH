package cn.plumc.ultimatech.section;

import cn.plumc.ultimatech.utils.IntCounter;
import net.minecraft.util.Mth;

import java.util.function.Consumer;

public class SectionCounter {
    private final int ticks;
    private final int seconds;
    private final boolean loop;

    private final IntCounter tickCounter;
    private final IntCounter triggerCounter;
    private final IntCounter secondCounter;

    private final Consumer<Integer> tickTask;
    private final Consumer<Integer> secondTask;

    private boolean running = false;

    public SectionCounter(int ticks, int seconds, boolean loop, Consumer<Integer> tickTask, Consumer<Integer> secondTask) {
        this.ticks = ticks;
        this.seconds = seconds;
        this.loop = loop;

        tickCounter = new IntCounter();
        triggerCounter = new IntCounter();
        secondCounter = new IntCounter();

        this.tickTask = tickTask;
        this.secondTask = secondTask;
    }

    public void start() {
        running = true;
    }

    public void stop() {
        running = false;
    }

    public void reset(){
        tickCounter.set(0);
        triggerCounter.set(0);
        secondCounter.set(0);
    }

    public void tick() {
        if (running) {
            tickTask.accept(tickCounter.get());
            tickCounter.add();
            if (tickCounter.get() > ticks) {
                if (loop) tickCounter.set(0);
                else {
                    tickCounter.set(0);
                    stop();
                }
            }
        }
        if (triggerCounter.get() % 20 == 0) {
            second();
            triggerCounter.set(0);
        };
        triggerCounter.add();

    }

    public void second() {
        if (running && seconds >= 0) {
            secondTask.accept(secondCounter.get());
            secondCounter.add();
            if (loop && secondCounter.get() > seconds) {
                secondCounter.set(0);
            }
        }
    }

    public boolean at(double time){
        return tickCounter.get() == toTicks(time);
    }

    public boolean in(double start, double end){
        return tickCounter.get() >= toTicks(start) && tickCounter.get() < toTicks(end);
    }

    public boolean contains(double start, double end){
        return tickCounter.get() >= toTicks(start) && tickCounter.get() <= toTicks(end);
    }

    public double progress(double start, double end){
        return (double) (tickCounter.get() - toTicks(start)) / (toTicks(end) - toTicks(start));
    }

    public static int toTicks(double seconds){
        return Mth.floor(seconds*20);
    }
}
