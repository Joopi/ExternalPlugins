package net.runelite.api.model;

import lombok.Value;
import net.runelite.api.Point;

@Value
public class Triangle2D {
    private Point a;
    private Point b;
    private Point c;

    public Point random()
    {
        double r1 = Math.random();
        double r2 = Math.random();

        double sq1 = Math.sqrt(r1);

        int x = (int) ((1 - sq1) * a.getX() + (sq1 * (1 - r2)) * b.getX() + (sq1 * r2) * c.getX());
        int y = (int) ((1 - sq1) * a.getY() + (sq1 * (1 - r2)) * b.getY() + (sq1 * r2) * c.getY());

        return new Point(x,y);
    }
}
