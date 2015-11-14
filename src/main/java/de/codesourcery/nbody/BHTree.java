package de.codesourcery.nbody;

/**
 * Initial code taken from http://physics.princeton.edu/~fpretori/Nbody.
 *
 * @author Frans Pretorius
 * @author tobias.gierke@voipfuture.com
 */
public final class BHTree
{
    private final BoundingBox quad;     // square region that the tree represents

    private int body=-1;     // body or aggregate body stored in this node
    private BHTree NW=null;     // tree representing northwest quadrant
    private BHTree NE=null;     // tree representing northeast quadrant
    private BHTree SW=null;     // tree representing southwest quadrant
    private BHTree SE=null;     // tree representing southeast quadrant

    //Create and initialize a new bhtree. Initially, all nodes are null and will be filled by recursion
    //Each BHTree represents a quadrant and a body that represents all bodies inside the quadrant
    public BHTree(BoundingBox q) {
        this.quad=q;
    }

    public BHTree() {
        this.quad = null;
    }

    public void clear() {
        this.body=-1;
        this.NW=null;
        this.NE=null;
        this.SW=null;
        this.SE=null;
    }

    //If all nodes of the BHTree are null, then the quadrant represents a single body and it is "external"
    public boolean isExternalNode()
    {
        return this.NW==null && this.NE==null && this.SW ==null && this.SE==null;
    }

    //We have to populate the tree with bodies. We start at the current tree and recursively travel through the branches
    public void insert(int b,Bodies bodies)
    {
        //If there's not a body there already, put the body there.
        if (this.body==-1) {
            this.body=b;
            return;
        }
        //If there's already a body there, but it's not an external node
        //combine the two bodies and figure out which quadrant of the
        //tree it should be located in. Then recursively update the nodes below it.
        if ( ! isExternalNode() )
        {
            this.body=bodies.sumBodies(this.body,b);
            if ( bodies.nwContains( b , this.quad ) ) {
                if (this.NW==null) {
                    this.NW= new BHTree(this.quad.NW());
                }
                this.NW.insert(b,bodies);
            }
            else
            {
                if ( bodies.neContains( b , this.quad ) )
                {
                    if (this.NE==null) {
                        this.NE= new BHTree(this.quad.NE());
                    }
                    this.NE.insert(b,bodies);
                }
                else
                {
                    if ( bodies.seContains( b , this.quad ) ) {
                        if (this.SE==null) {
                            this.SE= new BHTree(this.quad.SE());
                        }
                        this.SE.insert(b,bodies);
                    }
                    else
                    {
                        if(this.SW==null) {
                            this.SW= new BHTree(this.quad.SW());
                        }
                        this.SW.insert(b,bodies);
                    }
                }
            }
            return;
        }
        
        //If the node is external and contains another body, create BHTrees
        //where the bodies should go, update the node, and end
        //(do not do anything recursively)
        final int c = this.body;
        if ( bodies.nwContains(c , this.quad ) )
        {
            if (this.NW==null) {
                this.NW= new BHTree(this.quad.NW());
            }
            this.NW.insert(c,bodies);
        }
        else
        {
            if ( bodies.neContains( c , this.quad ) )
            {
                if (this.NE==null) {
                    this.NE= new BHTree(this.quad.NE());
                }
                this.NE.insert(c,bodies);
            }
            else
            {
                if ( bodies.seContains( c , this.quad ) )
                {
                    if (this.SE==null) {
                        this.SE= new BHTree(this.quad.SE());
                    }
                    this.SE.insert(c,bodies);
                }
                else {
                    if(this.SW==null) {
                        this.SW= new BHTree(this.quad.SW());
                    }
                    this.SW.insert(c,bodies);
                }
            }
        }
        this.insert(b,bodies);
    }
    
    //Start at the main node of the tree. Then, recursively go each branch
    //Until either we reach an external node or we reach a node that is sufficiently
    //far away that the external nodes would not matter much.
    public void updateForce(int b,Bodies bodies)
    {
        if ( isExternalNode() )
        {
            if (this.body!=b) {
                bodies.addForce( b , this.body );
            }
            return;
        }
        if ( this.quad.sizeSqrd() / ( bodies.distanceToSqrd( b ,  this.body ) ) < 2*2)
        {
            bodies.addForce( b , this.body );
            return;
        }
        
        if ( this.NW != null ) {
            this.NW.updateForce(b,bodies);
        }

        if ( this.SW != null ) {
            this.SW.updateForce(b,bodies);
        }

        if ( this.SE != null ) {
            this.SE.updateForce(b,bodies);
        }

        if ( this.NE != null ) {
            this.NE.updateForce(b,bodies);
        }    
    }    
}