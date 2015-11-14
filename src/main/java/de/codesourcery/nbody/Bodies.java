package de.codesourcery.nbody;

/**
 * Initial code taken from http://physics.princeton.edu/~fpretori/Nbody.
 *
 * @author Frans Pretorius
 * @author tobias.gierke@voipfuture.com
 */
public final class Bodies
{
    public static final double G = 6.673e-11;   // gravitational constant
    
    public static final double SOLAR_MASS=1.98892e30;

    public static final int TMP_BODIES_FACTOR = 15; // how much space to reserve for temporary bodies created while constructing the BH tree
    
    private static final int PART_COUNT = 7;

    private static final int X  = 0;
    private static final int Y  = 1;
    private static final int VX = 2;
    private static final int VY = 3;
    private static final int FX = 4;
    private static final int FY = 5;
    private static final int MASS = 6;

    private final double[] parts;
    public final int maxBodies;

    protected int tmpBufferIdx;

    public final Object[] LOCKS;
    
    public Bodies(int count)
    {
    	this.maxBodies = count;
        this.tmpBufferIdx = count;
        
        final int totalCount = count*TMP_BODIES_FACTOR;
        this.parts = new double[ totalCount * PART_COUNT ];
        final int size = (totalCount*PART_COUNT*8)/1024/1024;
        System.out.println("Bodies occupy "+size+" MB");
        this.LOCKS = new Object[ totalCount ];
        for ( int i = 0 ; i < totalCount ; i++ ) {
            LOCKS[i] = new Object();
        }
    }

    public static abstract class PointsVisitor<T>
    {
        public abstract void visit(double x,double y,T data);
    }

    public void resetTempBuffer()
    {
        tmpBufferIdx = maxBodies;
    }

    public <T> void visit(int count,PointsVisitor<T> v,T data) {

        for ( int offset = 0 , i = 0 ; i <count ; i++, offset += PART_COUNT )
        {
            v.visit( parts[ offset + X ] , parts[ offset + Y ] , data );
        }
    }

    public boolean isBodyInRegion(int body,BoundingBox q)
    {
        final int offset = body*PART_COUNT;
        return q.contains( parts[ offset + X ] , parts[ offset + Y ] );
    }

    public void set(int index,double px,double  py,double  vx,double  vy,double  mass) {
        final int idx = index * PART_COUNT;
        parts[idx + X ] = px;
        parts[idx + Y ] = py;
        parts[idx + VX ] = vx;
        parts[idx + VY ] = vy;
        parts[idx + FX ] = 0;
        parts[idx + FY ] = 0;
        parts[idx + MASS ] = mass;
    }

    public int sumBodies(int a,int b)
    {
        final int offsetA = a*PART_COUNT;
        final int offsetB = b*PART_COUNT;

        final double bodyAx = parts[ offsetA + X ];
        final double bodyBx = parts[ offsetB + X ];

        final double bodyAy = parts[ offsetA + Y ];
        final double bodyBy = parts[ offsetB + Y ];

        final double bodyAmass = parts[ offsetA + MASS ];
        final double bodyBmass = parts[ offsetB + MASS ];

        double mass = bodyAmass + bodyBmass;

        double rx = (bodyAx * bodyAmass + bodyBx * bodyBmass) / mass;
        double ry = (bodyAy * bodyAmass + bodyBy * bodyBmass) / mass;
        return add( rx , ry , 0 , 0 , mass );
    }

    public int add(double px,double  py,double  vx,double  vy,double  mass)
    {
        final int idx = tmpBufferIdx;
        final int offset = idx* PART_COUNT;
        tmpBufferIdx++;
        try {
            parts[offset + X ] = px;
            parts[offset + Y ] = py;
            parts[offset + VX ] = vx;
            parts[offset + VY ] = vy;
            parts[offset + FX ] = 0;
            parts[offset + FY ] = 0;
            parts[offset + MASS ] = mass;
        } catch(ArrayIndexOutOfBoundsException e) {
            throw new ArrayIndexOutOfBoundsException("Out of space for temporary Body instances, increase TMP_BODIES_FACTOR: "+e.getMessage());
        }
        return idx;
    }

    public void resetForce(int index) 
    {
        synchronized (LOCKS[index]) {
            final int offset = index * PART_COUNT;
            parts[ offset + FX ] = 0;
            parts[ offset + FY ] = 0;
        }
    }

    public void resetForces(int maxIndex) {

        for ( int i = 0 , offset = 0 ; i < maxIndex ; i++ , offset += PART_COUNT )
        {
            parts[ offset + FX ] = 0;
            parts[ offset + FY ] = 0;
        }
    }

    public void updatePosition(int index , double dt)
    {
        final int offset = index * PART_COUNT;

        parts[ offset + VX ] += dt * parts[ offset + FX ] / parts[ offset + MASS ];
        parts[ offset + VY ] += dt * parts[ offset + FY ] / parts[ offset + MASS ];

        parts[ offset + X ] += dt * parts[ offset + VX ];
        parts[ offset + Y ] += dt * parts[ offset + VY ];
    }

    public void addForce(int bodyToAddTo,int bodyToAdd)
    {
        final double EPS = 3E4;      // softening parameter (just to avoid infinities)

        final int offsetB = bodyToAdd * PART_COUNT;
        final int offsetThis = bodyToAddTo * PART_COUNT;

        double dx = parts[ offsetB + X ] - parts[ offsetThis + X ];
        double dy = parts[ offsetB + Y ] - parts[ offsetThis + Y ];
        double dist = Math.sqrt(dx*dx + dy*dy);
        double F = (G * parts[ offsetThis + MASS ] * parts[ offsetB + MASS ] ) / (dist*dist + EPS*EPS);

        parts[ offsetThis + FX ] += F * dx/dist;
        parts[ offsetThis + FY ] += F * dy/dist;
    }

    public double distanceToSqrd(int b,int thisIndex)
    {
        final int offsetB = b * PART_COUNT;
        final int offsetThis = thisIndex * PART_COUNT;

        double dx = parts[ offsetThis + X ] - parts[ offsetB + X ];
        double dy = parts[ offsetThis + Y ] - parts[ offsetB + Y ];

        return dx*dx + dy*dy;
    }

    public boolean neContains(int b , BoundingBox q)
    {
        final int offset = b*PART_COUNT;
        return q.neContains( parts[ offset +X ] , parts[ offset + Y ] );
    }

    public boolean nwContains(int b , BoundingBox q)
    {
        final int offset = b*PART_COUNT;
        return q.nwContains( parts[ offset +X ] , parts[ offset + Y ] );
    }

    public boolean seContains(int b , BoundingBox q)
    {
        final int offset = b*PART_COUNT;
        return q.seContains( parts[ offset + X ] , parts[ offset + Y ] );
    }

    public boolean swContains(int b , BoundingBox q)
    {
        final int offset = b*PART_COUNT;
        return q.swContains( parts[ offset +X ] , parts[ offset + Y ] );
    }
}
