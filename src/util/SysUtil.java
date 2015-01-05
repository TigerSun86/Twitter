package util;

/*
   Phil Chan
   Feb 2010

   Methods for measuring wall clock, CPU, memory/heap usage.

   Since garbage collection takes a bit of time, 
   measure memory/heap usage outside time usage

   startHeap
   startWallClock
   startCpu

   ...

   endCpu
   endWallClock
   endHeap

*/


import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

public class SysUtil
{
    // http://nadeausoftware.com/articles/2008/03/java_tip_how_get_cpu_and_user_time_benchmarking

    // Wall clock time in nanoseconds
    public static long getWallClockTime()
    {
	return System.nanoTime();
    }

    // CPU time in nanoseconds
    public static long getCpuTime() 
    {
	ThreadMXBean bean = ManagementFactory.getThreadMXBean();
	return bean.isCurrentThreadCpuTimeSupported() ?
	    bean.getCurrentThreadCpuTime() : 0L;
    }


    // http://www.roseindia.net/javatutorials/determining_memory_usage_in_java.shtml

    // Memory usage on the heap in bytes
    public static long getHeapUsage()  
    {
	System.gc();  // garbage collection
        Runtime s_runtime = Runtime.getRuntime();
	return s_runtime.totalMemory() - s_runtime.freeMemory();
    }

}
