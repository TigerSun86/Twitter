package common;

import java.awt.geom.Point2D;
import java.util.ArrayList;

import util.MyMath;

/**
 * FileName: Region.java
 * @Description:
 * 
 * @author Xunhu(Tiger) Sun
 *         email: TigerSun86@gmail.com
 * @date Jun 17, 2014 5:19:31 PM
 */
public interface Region {
    public boolean isInside (Point2D.Double p);

    /**
     * Circle region (include the boundary)
     * c: center. r: radius.
     * */
    public static class Circle implements Region {
        public final Point2D.Double c;
        public final double r;

        public Circle(Point2D.Double c, double r) {
            this.c = c;
            this.r = r;
        }

        @Override
        public boolean isInside (Point2D.Double p) {
            if (Double.compare(MyMath.distance(c, p), r) <= 0) {
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Ribbon region between two lines, include the boundary.
     * y = kx + b1
     * y = kx + b2
     * For vertical lines,
     * x = b1
     * x = b2
     * 
     * Return true if P(x, y) is
     * y-kx-b1<=0 and y-kx-b2>=0 or for vertical lines,
     * x - b1 <= 0 and x - b2 >= 0.
     * */
    public static class Ribbon implements Region {
        public final double k; // Slope.
        public final double b1; // Higher one.
        public final double b2; // Lower one.

        public Ribbon(double k, double b1, double b2) {
            this.k = k;
            if (Double.compare(b1, b2) >= 0) {
                this.b1 = b1;
                this.b2 = b2;
            } else {
                this.b1 = b2;
                this.b2 = b1;
            }
        }

        /** This constructor for line x = b. */
        public Ribbon(double b1, double b2) {
            this.k = Double.POSITIVE_INFINITY;
            if (Double.compare(b1, b2) >= 0) {
                this.b1 = b1;
                this.b2 = b2;
            } else {
                this.b1 = b2;
                this.b2 = b1;
            }
        }

        @Override
        public boolean isInside (Point2D.Double p) {
            if (!Double.isInfinite(k)) {
                if ((Double.compare(p.y - k * p.x - b1, 0) <= 0)
                        && (Double.compare(p.y - k * p.x - b2, 0) >= 0)) {
                    return true;
                } else {
                    return false;
                }
            } else {
                // x = b.
                if ((Double.compare(p.x - b1, 0) <= 0)
                        && (Double.compare(p.x - b2, 0) >= 0)) {
                    return true;
                } else {
                    return false;
                }
            }
        }

        @Override
        public String toString () {
            if (!Double.isInfinite(k)) {
                return String.format("[y = %.2fx + %.2f, y = %.2fx + %.2f]", k,
                        b1, k, b2);
            } else {
                return String.format("[x = %.2f, x = %.2f]", b1, b2);
            }
        }
    }

    /**
     * Parallelogram region between 2 ribbons, include the boundary.
     * 
     * Return true if P(x, y) is inside both of 2 ribbons.
     * */
    public static class Parallelogram implements Region {
        public final Ribbon rib1;
        public final Ribbon rib2;

        public Parallelogram(Ribbon rib1, Ribbon rib2) {
            this.rib1 = rib1;
            this.rib2 = rib2;
        }

        @Override
        public boolean isInside (Point2D.Double p) {
            if (rib1.isInside(p) && rib2.isInside(p)) {
                return true;
            } else {
                return false;
            }
        }

        @Override
        public String toString () {
            return "[" + rib1.toString() + rib2.toString() + "]";
        }
    }

    /**
     * Region list contains many regions.
     * 
     * Return true if P(x, y) is inside any one of regions.
     * */
    public static class RegionList extends ArrayList<Region> implements Region {

        @Override
        public boolean isInside (java.awt.geom.Point2D.Double p) {
            if (this.isEmpty()) {
                return false;
            }

            for (Region r : this) {
                if (r.isInside(p)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Region everywhere except inside the given reg.
     * 
     * Return true if p is outside the reg.
     * */
    public static class NotRegion implements Region {
        private final Region reg;

        public NotRegion(final Region reg) {
            this.reg = reg;
        }

        @Override
        public boolean isInside (java.awt.geom.Point2D.Double p) {
            return !reg.isInside(p);
        }
    }

    /**
     * Region interval reg1 and reg2.
     * 
     * Return true if p is inside both reg1 and reg2.
     * */
    public static class AndRegion implements Region {
        private final Region reg1;
        private final Region reg2;

        public AndRegion(final Region reg1, final Region reg2) {
            this.reg1 = reg1;
            this.reg2 = reg2;
        }

        @Override
        public boolean isInside (java.awt.geom.Point2D.Double p) {
            return reg1.isInside(p) && reg2.isInside(p);
        }
    }
    
    /**
     * Region union of reg1 and reg2.
     * 
     * Return true if p is inside either reg1 or reg2.
     * */
    public static class OrRegion implements Region {
        private final Region reg1;
        private final Region reg2;

        public OrRegion(final Region reg1, final Region reg2) {
            this.reg1 = reg1;
            this.reg2 = reg2;
        }

        @Override
        public boolean isInside (java.awt.geom.Point2D.Double p) {
            return reg1.isInside(p) || reg2.isInside(p);
        }
    }
}
