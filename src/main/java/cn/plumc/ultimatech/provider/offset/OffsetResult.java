package cn.plumc.ultimatech.provider.offset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OffsetResult{
    public double slippingOffset;
    public HashMap<Double, Double> droppingOffset;

    public final int ping;

    public OffsetResult(int ping){
        this.ping = ping;
        droppingOffset = new HashMap<>();
    }

    private double withBasic(double offset) {
        return OffsetProvider.OffsetTesting.BASE_OFFSET + offset;
    }

    public double getSlippingOffset() {
        return withBasic(slippingOffset);
    }

    public double getDroppingOffset(double speed) {
        if (droppingOffset == null || droppingOffset.isEmpty()) {
            return withBasic(slippingOffset);
        }

        List<Map.Entry<Double, Double>> points = new ArrayList<>(droppingOffset.entrySet());
        points.sort(Map.Entry.comparingByKey());

        if (points.size() == 1) {
            return withBasic(points.getFirst().getValue());
        }

        if (speed <= points.getFirst().getKey()) {
            return withBasic(points.getFirst().getValue());
        }

        if (speed >= points.getLast().getKey()) {
            Map.Entry<Double, Double> p1 = points.get(points.size() - 2);
            Map.Entry<Double, Double> p2 = points.getLast();
            return withBasic(interpolate(speed, p1.getKey(), p1.getValue(), p2.getKey(), p2.getValue()));
        }

        for (int i = 0; i < points.size() - 1; i++) {
            Map.Entry<Double, Double> p1 = points.get(i);
            Map.Entry<Double, Double> p2 = points.get(i + 1);

            if (speed >= p1.getKey() && speed <= p2.getKey()) {
                return withBasic(interpolate(speed, p1.getKey(), p1.getValue(), p2.getKey(), p2.getValue()));
            }
        }

        return withBasic(slippingOffset);
    }

    private double interpolate(double x, double x1, double y1, double x2, double y2) {
        if (x1 == x2) return y1;
        return y1 + (x - x1) * (y2 - y1) / (x2 - x1);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("OffsetResult[ping=%d,".formatted(ping))
                .append(" slipping=%.2f".formatted(slippingOffset))
                .append(" dropping={\n");
        droppingOffset.forEach((key, value) -> builder.append(key).append(":").append(value).append("\n"));
        builder.append("}");
        return builder.toString();
    }
}