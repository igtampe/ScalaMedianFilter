# ScalaMedianFilter
An implementation of a MedianFilter using Akka actors and Scala Collections.
This is part of the CIIC4030 Programming Languages Course

The program compares the run time of running a Median Filter sequentially and concurrently (by dividing the image into 10 vertical columns). It uses a 10x10 window to execute the median filter. A more detailed report is available on the [included report](https://github.com/igtampe/ScalaMedianFilter/blob/master/A4%20Report.pdf).

Here's another example of the resutls:
|Original|Sequential|Concurrent
|-|-|-|
|![Einstein](https://raw.githubusercontent.com/igtampe/ScalaMedianFilter/master/Einstein.jpg)|![Sequential](https://raw.githubusercontent.com/igtampe/ScalaMedianFilter/master/SequentialResult.png)|![Concurrent](https://raw.githubusercontent.com/igtampe/ScalaMedianFilter/master/ConcurrentResult.png)

I can't find a source of the provided image of Albert Einstein, it was provided by a group chat for the course. Removal upon request.
The picture on the report is an attempt to follow a Bob Ross episode on MS Paint. Its my own work. 
