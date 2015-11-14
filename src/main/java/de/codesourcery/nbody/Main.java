package de.codesourcery.nbody;

import java.awt.Button;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.TextField;
import java.awt.Toolkit;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.JPanel;

import de.codesourcery.nbody.Bodies.PointsVisitor;

/**
 * Initial code taken from http://physics.princeton.edu/~fpretori/Nbody.
 *
 * @author Frans Pretorius
 * @author tobias.gierke@voipfuture.com
 */
public class Main
{
    public static final Dimension INITIAL_CANVAS_SIZE = new Dimension(800,600);

    public static final double UNIVERSE_SIZE = 1e18;
    
    public static final BoundingBox UNIVERSE_BOUNDS = new BoundingBox(0,0,2*UNIVERSE_SIZE);

    public static final int FPS = 60;
    
    private static final Random rnd = new Random(0xdeadbeef);

    public final BHTree thetree = new BHTree(UNIVERSE_BOUNDS);
    
    public final Bodies bodies;
    public final int[] bodiesToProcess;
    
    private final int threadCount;
    private final ExecutorService threadPool;    
    
    public volatile boolean simulationRunning=true;

    public volatile int numBodies;
    public final TextField bodyCountInput = new TextField( Integer.toString(this.numBodies) ,5);
    
    protected final Object REPAINT_LOCK = new Object();

    private final JPanel canvas = new JPanel()
    {
        private int frameCount;
        private float totalElapsedSeconds=0;
        private long previous = System.currentTimeMillis();
        
        private double stepX,stepY;
        
        final PointsVisitor<Graphics> renderVisitor = new PointsVisitor<Graphics>()
        {
            public void visit(double x, double y,Graphics g) 
            {
                if ( UNIVERSE_BOUNDS.contains( x , y ) ) {
                    final int px = (int) Math.round( x * stepX );
                    final int py = (int) Math.round( y * stepY );
                    g.fillRect( px - 2 , py - 2 , 4,4 ); 
                }
            }
        };          
        
        @Override
        protected void paintComponent(Graphics g)
        {
            long renderStart= System.currentTimeMillis();
            
            super.paintComponent(g);
            
            final int centerX = getWidth()/2;
            final int centerY = getHeight()/2;
       
            stepX = centerX / UNIVERSE_SIZE;
            stepY = centerY / UNIVERSE_SIZE;
            
            g.setColor(Color.GREEN);
            
            try 
            {
                g.translate(centerX,centerY); // Originally the origin is in the top right. Put it in its normal place
                synchronized( bodies ) 
                {
                    bodies.visit( numBodies , renderVisitor ,g );
                }
            } finally {
                g.translate(-centerX,-centerY);
            }
            
            Toolkit.getDefaultToolkit().sync();
            
            final long time = System.currentTimeMillis();
            final long elapsed = time - previous;
            totalElapsedSeconds += elapsed / 1000f;
            previous = time;
            frameCount++;
            final int avgFps = (int) (frameCount/totalElapsedSeconds);
            if ( frameCount > 120 ) {
                frameCount = 0;
                totalElapsedSeconds = 0;
            }
            g.setColor(Color.BLACK);
            g.drawString("FPS: "+avgFps+" (rendering: "+(time-renderStart)+" ms)",15,15);
            
            synchronized ( REPAINT_LOCK ) {
                REPAINT_LOCK.notifyAll();
            }
        }
    };

    public static void main(String[] args) 
    {
        int threadCount = Runtime.getRuntime().availableProcessors();
        boolean benchmark = false;
        int bodies =30000;
        for ( int i = 0 ; i < args.length ; i++ ) {
            switch( args[i].toLowerCase() )
            {
                case "--benchmark":
                    benchmark = true;
                    break;
                case "--bodies":
                    bodies = Integer.parseInt( args[i+1] );
                    i++;
                    break;
                default:
                    if ( args[i].matches( "^[0-9]+$" ) ) {
                        threadCount = Integer.parseInt( args[i] );
                    }
            }
        }
        
        new Main( threadCount , bodies ).run( benchmark );
    }

    public Main(int threadCount,int bodyCount) 
    {
        this.threadCount = threadCount;
        this.numBodies = bodyCount;
        bodyCountInput.setText( Integer.toString( bodyCount ) );
        
        bodies = new Bodies( bodyCount );
        bodiesToProcess = new int[ bodyCount ];
        
        final BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>( threadCount+1);

        final ThreadFactory threadFactory = new ThreadFactory() {

            @Override
            public Thread newThread(Runnable r) {
                final Thread t = new Thread(r);
                t.setDaemon( true );
                return t;
            }
        };
        threadPool = new ThreadPoolExecutor(threadCount, threadCount , 10 , TimeUnit.MINUTES, workQueue, threadFactory, new ThreadPoolExecutor.CallerRunsPolicy() );
    }

    public void run(boolean benchmark)
    {
        final JFrame frame = new JFrame("n-body");
        frame.setPreferredSize( INITIAL_CANVAS_SIZE );
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );

        createBodies(this.numBodies);

        if ( benchmark )
        {
            System.out.println("Running benchmark with "+threadCount+" threads and "+numBodies+" bodies ...");
            final long start = System.currentTimeMillis();
            final int loops = 1000;
            for ( int i = 0 ; i < loops ; i++ ) {
                advanceSimulation();
            }
            long now = System.currentTimeMillis();
            long elapsed = now - start;
            System.out.println("Time: "+elapsed);
            System.exit(0);;
        }

        this.bodyCountInput.addActionListener( ev -> setup() );
        final Button restartButton=new Button("Restart");
        restartButton.addActionListener(ev -> 
        {
            setup();
            simulationRunning=true;
        });

        final Button stopButton =new Button("Stop");
        stopButton.addActionListener( ev -> 
        {
            stopButton.setLabel( simulationRunning ? "Run" : "Stop" );
            simulationRunning = !simulationRunning;
        });

        final JPanel toolbar = new JPanel();
        toolbar.setLayout( new FlowLayout() );
        toolbar.add(new Label("Number of bodies:"));
        toolbar.add(bodyCountInput);
        toolbar.add(restartButton);
        toolbar.add(stopButton);

        frame.getContentPane().setLayout( new GridBagLayout() );

        GridBagConstraints cnstrs = new GridBagConstraints();
        cnstrs.gridx = 0; cnstrs.gridy = 0;
        cnstrs.gridwidth = 1 ; cnstrs.gridheight = 1;
        cnstrs.weightx=1; cnstrs.weighty=0;
        cnstrs.fill = GridBagConstraints.HORIZONTAL;

        frame.getContentPane().add( toolbar , cnstrs );

        cnstrs = new GridBagConstraints();
        cnstrs.gridx = 0; cnstrs.gridy = 1;
        cnstrs.gridwidth = 1 ; cnstrs.gridheight = 1;
        cnstrs.weightx=1; cnstrs.weighty=1;
        cnstrs.fill = GridBagConstraints.BOTH;        
        frame.getContentPane().add( canvas , cnstrs );

        frame.pack();
        frame.setVisible( true );        

        mainLoop();
    }

    private void mainLoop() 
    {
        float elapsedMillis = 1000/FPS;
        long previous = System.nanoTime();
        while ( true ) 
        {
            final long now = System.nanoTime();
            elapsedMillis += (now - previous)/1000000f;
            previous = now;
            if ( elapsedMillis >= 1000/FPS ) 
            {
                elapsedMillis = 0;
                synchronized( bodies ) // hold lock to avoid having the Swing EDT interfere while we're updating the simulation
                {                
                    if (simulationRunning) 
                    {
                        advanceSimulation();
                    }
                }     
                synchronized ( REPAINT_LOCK ) // wait for repaint() to finish , otherwise we might fill-up the Swing event queue and cause sluggish UI behaviour 
                {
                    try 
                    {
                        canvas.repaint();
                        REPAINT_LOCK.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }                
            }
        }
    }
    
    private void setup() 
    {
        synchronized( bodies ) 
        {
            final int value = Integer.parseInt(bodyCountInput.getText());
            if (value > bodies.maxBodies ) {
                bodyCountInput.setText( Integer.toString( bodies.maxBodies ) );
            } else {
                numBodies = value;
            }
            createBodies( numBodies );
        }
    }

    //Initialize N bodies with random positions and circular velocities
    public void createBodies(int count)
    {
        for (int i = 0; i < count; i++)
        {
            final double px = UNIVERSE_SIZE*exp(-1.8)*(.5-rnd.nextDouble());
            final double py = UNIVERSE_SIZE*exp(-1.8)*(.5-rnd.nextDouble());
            final double magv = circlev(px,py);

            final double absangle = Math.atan(Math.abs(py/px));
            final double thetav= Math.PI/2-absangle;
            double vx   = -1*Math.signum(py)*Math.cos(thetav)*magv;
            double vy   = Math.signum(px)*Math.sin(thetav)*magv;
            
            if (rnd.nextDouble() <=.5) { // randomly flip rotation (clock-wise/counter-clock wise) 
                vx=-vx;
                vy=-vy;
            }

            final double mass = 1e20 + Math.random()*Bodies.SOLAR_MASS*10;
            this.bodies.set(i,px, py, vx, vy, mass);
        }

        // Put the central mass in
        this.bodies.set(0,0,0,0,0,1e6*Bodies.SOLAR_MASS); // put a heavy body in the center
    }

    //the bodies are initialized in circular orbits around the central mass.
    //This is just some physics to do that
    public static double circlev(double rx, double ry)
    {
        final double distToCenter=Math.sqrt(rx*rx+ry*ry);
        final double numerator=(6.67e-11)*1e6*Bodies.SOLAR_MASS;
        return Math.sqrt(numerator/distToCenter);
    }    

    public static double exp(double lambda) {
        return -Math.log(1 - rnd.nextDouble()) / lambda;
    }    

    // BH algorithm
    public int advanceSimulation()
    {
        bodies.resetTempBuffer();
        thetree.clear();

        // gather bodies still on screen and add them to the tree
        final int[] toProcess = this.bodiesToProcess;
        int toProcessCount = 0;
        for (int i = 0; i < numBodies; i++)
        {
            if ( bodies.isBodyInRegion( i , UNIVERSE_BOUNDS ) )
            {
                thetree.insert( i , this.bodies );
                toProcess[toProcessCount++] = i;
            }
        }

        // update the forces, traveling recursively through the tree   
        final int bodiesPerSlice = toProcessCount / threadCount;

        final CountDownLatch latch = new CountDownLatch( threadCount );
        for ( int sliceIdx = 0 ; sliceIdx < threadCount ; sliceIdx++ )
        {
            final int start= sliceIdx * bodiesPerSlice;
            final int end = sliceIdx == threadCount-1 ? toProcessCount : (sliceIdx+1) * bodiesPerSlice;

            threadPool.execute( () -> 
            {
                try 
                {
                    for (int idx = start ; idx < end ; idx++)
                    {
                        final int i = toProcess[idx];
                        synchronized(bodies.LOCKS[i]) 
                        {
                            bodies.resetForce( i );
                            thetree.updateForce(i,bodies);
                        }
                    }    
                } finally {
                    latch.countDown();
                }
            });
        }
        try {
            latch.await(); // wait for all threads to finish before updating the positions
        } 
        catch (InterruptedException e) { e.printStackTrace(); }

        //Calculate the new positions on a time step dt (1e11 here)
        for ( int i = 0 ; i < toProcessCount ; i++ ) {
            final int idx = toProcess[i]; 
            bodies.updatePosition( idx , 1e11 );
        }
        return toProcessCount;
    }    
}