# nbody
This is a tiny Swing application that renders a n-body simulation. The implementation of the actual n-body simulation algorithm 
is from a Java Applet done by Frans Pretorius (http://physics.princeton.edu/~fpretori/Nbody) ; I converted this into a Swing application
and then started tweaking it for performance ; mostly as an experiment to see how far I could get with doing micro-optimizations.

![screenshot](https://raw.github.com/toby1984/nbody/master/screenshot.png)

Changes done to the original code (in no particular order):

- restructured code/adopted Java naming conventions
- turned magic values into constants
- reduced number of object allocations
- turned "Body" class inside out to be more cache friendly
- Calculating forces is now done in parallel (main bottleneck still seems to be the BH tree creation which is hard to parallelize without losing all the performance gains to locking)
- replaced some code that used Math.sqrt() to use the squared value instead
- made sure animation runs at ~60fps (will ofc be slower if your CPU can't keep up)

# Requirements

JDK >= 1.8

# Building

    mvn clean package

This will create a self-executable target/nbody.jar 

# Running

    java -jar tar/nbody.jar [--benchmark] [--bodies <count>] [thread count]

