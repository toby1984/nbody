package de.codesourcery.nbody;

/**
 * Axis-aligned bounding box.
 * 
 * @author tobias.gierke@voipfuture.com
 */
public final class BoundingBox {

    private final double xmin;
    private final double xmax;
    private final double ymax;
    private final double ymin;
    private final double xmid;
    private final double ymid;

    public BoundingBox(double xmid, double ymid, double size) {

        this.xmin = xmid - size/2.0;
        this.ymin = ymid - size/2.0;

        this.xmax = xmid + size/2.0;
        this.ymax = ymid + size/2.0;

        this.xmid = xmid;
        this.ymid = ymid;
    }

    public BoundingBox(double xmin,double ymin,double xmax,double ymax) {
        this.xmin = xmin;
        this.xmax = xmax;
        this.ymin = ymin;
        this.ymax = ymax;

        this.xmid = (xmin + xmax ) / 2.0;
        this.ymid = (ymin + ymax ) / 2.0;
    }

    public double size() {
        return xmax-xmin;
    }

    public double sizeSqrd() {
        return size()*size();
    }

    public boolean contains(double x, double y)
    {
        return x >= xmin && x <= xmax && y >= ymin && y <= ymax;
    }

    public BoundingBox NW() {
        return new BoundingBox(this.xmin , this.ymin , xmid , ymid );
    }

    public boolean nwContains(double x,double y)
    {
        double xmin = this.xmin;
        double ymin = this.ymin;
        double xmax = this.xmid;
        double ymax = this.ymid;
        return x >= xmin && x <= xmax && y >= ymin && y <= ymax;
    }

    public BoundingBox NE() {
        return new BoundingBox(this.xmid, ymin , this.xmax , ymid );
    }

    public boolean neContains(double x,double y) {
        double xmin = this.xmid;
        double ymin = this.ymin;
        double xmax = this.xmax;
        double ymax = this.ymid;
        return x >= xmin && x <= xmax && y >= ymin && y <= ymax;
    }

    public BoundingBox SW() {
        return new BoundingBox(this.xmin , ymid , xmid , ymax );
    }

    public boolean swContains(double x,double y)
    {
        double xmin = this.xmin;
        double ymin = this.ymid;
        double xmax = this.xmid;
        double ymax = this.ymax;
        return x >= xmin && x <= xmax && y >= ymin && y <= ymax;
    }

    public BoundingBox SE() {
        return new BoundingBox(xmid,ymid,xmax,ymax);
    }

    public boolean seContains(double x,double y) {
        double xmin = this.xmid;
        double ymin = this.ymid;
        double xmax = this.xmax;
        double ymax = this.ymax;
        return x >= xmin && x <= xmax && y >= ymin && y <= ymax;
    }
}