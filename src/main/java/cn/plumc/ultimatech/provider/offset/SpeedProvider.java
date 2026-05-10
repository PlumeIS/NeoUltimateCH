package cn.plumc.ultimatech.provider.offset;

import cn.plumc.ultimatech.utils.IntCounter;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedList;

public class SpeedProvider{
        private final LinkedList<Double> lastY = new LinkedList<>(){
            @Override
            public void addFirst(Double aDouble) {
                super.addFirst(aDouble);
                if (size() > 200) removeLast();
            }
        };

        private final ServerPlayer player;

    private final IntCounter testCounter = new IntCounter();
        private double currentSpeed = 0;
        private double lastSpeed = 0;
        private double testSpeed = 0;

        private boolean waiting;
        private boolean finished;
        private int testTicks;
        private double testTime;

        public SpeedProvider(ServerPlayer player){
            this.player = player;
        }

        public void tick(){
            lastY.addFirst(player.position().y);
            testCounter.add(1);

            if (lastY.size() > 5) {
                lastSpeed = currentSpeed;
                currentSpeed = (lastY.get(5) - lastY.getFirst()) / 0.25;
            }

            if (waiting && testCounter.get() > testTicks) {
                testSpeed = (lastY.get(testTicks) - lastY.getFirst()) / testTime;
                waiting = false;
                finished = true;
            }

            if (currentSpeed < 0) {
                currentSpeed = 0;
                lastY.clear();
            }
            if (testSpeed < 0) testSpeed = 0;
        }

        public void test(double sec){
            if (waiting) return;
            testTicks = (int)(sec/0.05);
            if (testTicks > 100) return;
            waiting = true;
            testTime = testTicks * 0.05;
            testCounter.set(0);
        }

        public Double getCurrentSpeed(){
            return currentSpeed;
        }

        public Double getSmoothSpeed(){
            double alpha = 0.3;
            return currentSpeed * alpha + lastSpeed * (1 - alpha);
        }

        public Double getTestSpeed(){
            if (waiting) return null;
            return testSpeed;
        }

        public boolean finished(){
            return finished;
        }

        public void reset() {
            lastY.clear();
            currentSpeed = 0;
            lastSpeed = 0;
            testSpeed = 0;
            waiting = false;
            finished = false;
        }
    }