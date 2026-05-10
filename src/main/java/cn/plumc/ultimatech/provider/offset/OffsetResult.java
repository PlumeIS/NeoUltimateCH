package cn.plumc.ultimatech.provider.offset;

import java.util.HashMap;

public class OffsetResult{
    public double slippingOffset;
    public HashMap<Double, Double> droppingOffset;

    public final int ping;

    public OffsetResult(int ping){
        this.ping = ping;
        droppingOffset = new HashMap<>();
    }

    public double getSlippingOffset() {
        return slippingOffset;
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