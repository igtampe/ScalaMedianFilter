import java.awt.image.BufferedImage
import java.io.File

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import javax.imageio.ImageIO

import scala.collection.parallel.immutable.ParVector

//-[MedianRange]-------------------------------------------------------------------------------------
class MedianRange(Start : Int, End : Int){
  var start : Int = Start
  var end: Int = End
  def getStart: Int=start
  def getEnd: Int=end
}

//-[ChopoActor]-------------------------------------------------------------------------------------

/**
 * ChopoActor that includes all the stuff for Median Filter stuff
 * @author Chopo
 */
abstract class ChopoActor extends Actor{

  protected var PrePicture : BufferedImage = _
  protected var ReturnPicture : BufferedImage =_

  protected var t1 : Long =0

  def StartTime() {t1=System.nanoTime()}
  def StopTime(): Double ={(System.nanoTime()-t1)/1000000000.0} //returns in seconds because we're normal people

  /**
   * Executes a median filter on PrePicture and outputs to ReturnPicture
   * @param Range MedianRange to specify XStart and XEnd
   * @author Chopo
   */
  def MedianFilter(Range : MedianRange): Unit ={
    if(Range.start>Range.getEnd){throw new IllegalArgumentException("XStart was after XEnd")}
    if(Range.start<0 || Range.getEnd<0) {throw new IllegalArgumentException("XStart or XEnd was below 0")}
    if(Range.getEnd>PrePicture.getWidth()){throw new IllegalArgumentException("XEnd was larger than prePicture")}

    println("Starting to process range " + Range.start + " to " + Range.`end`)

    //Create an array of window width x window height
    val Ventana: Array[Int] = new Array[Int](101)

    //For X from EdgeX( AKA WindowWidth/2 AKA 5) to Image Width - EdgeX do
    for( x <- 5 + Range.start to Range.getEnd-5){
      //For Y from EdgeY to Image Height - EdgeY do
      for( y <- 5 to PrePicture.getHeight()-5){
        //    i=0
        var i : Integer = 0
        //    For FX From 0 to WindowWidth do
        for( fx <- 0 to 9){
          //    For FY from 0 to Window Height do
          for( fy <- 0 to 9){
            //        Window[i] = InputPixelValue[X+Fx-Edgex][Y+FY-EdgeY] //Basically get the value from everywhere
            Ventana(i) = PrePicture.getRGB(x+fx-5,y+fy-5)
            //        i++
            i+=1
          }

        }
        //    sort(Window) //Sort to find the average
        Ventana.sortInPlace()
        //    OutputPixelValue[X][Y] = Window[(WindowWidth * WindowHeight)/2] //Get the average
        ReturnPicture.setRGB(x,y,Ventana(50))
      }
    }
  }

}

//-[ClientActor]-------------------------------------------------------------------------------------

/**
 * Client actor that sends esta cosa
 * @author Chopo
 */
class ClientActor extends Actor {

  var mySystem : ActorSystem= _

  var Responses : Integer = 0
  var Concurrent : ActorRef = _
  var Sequential : ActorRef = _

  def receive: PartialFunction[Any, Unit] = {
    case filename : String =>
      println("Loading file")
      val img: BufferedImage = ImageIO.read(new File(filename)) //We have ourselves an image!
      println("Time to start working on " + filename)

      Concurrent = mySystem.actorOf(Props[ConcurrentActor], name = "Conqy")
      Sequential = mySystem.actorOf(Props[SequentialActor], name = "Sequy")

      Sequential ! img
      Concurrent ! img

    case processedPicture : BufferedImage =>
      //received the processed picture

      Responses+=1

      var WhereWereSavingThis : File = null

      if(sender().equals(Concurrent)){WhereWereSavingThis = new File("ConcurrentResult.png");}
      else if(sender().equals(Sequential)){WhereWereSavingThis = new File("SequentialResult.png");}
      else {
        println("I've received an image, but I don't know where it came from. Someone please send for help")
        WhereWereSavingThis = new File("A Result from somewhere.png")
      }

      //Save it
      println("Saving " + WhereWereSavingThis.getAbsolutePath)
      ImageIO.write(processedPicture,"png",WhereWereSavingThis)

      if(Responses==2){mySystem.terminate()} //time to die if this is over.

    case time : Double =>
      //received one of the times
      if(sender().equals(Concurrent)){
        println("Time for Concurrent: " + time + "s")
      } else if(sender().equals(Sequential)){
        println("Time for Sequential: " + time + "s")
      } else {println("I've received a time, but I don't know where it came from. Someone please send for help")}


    case system : ActorSystem =>
      mySystem=system
      println("I have received a system")
    case _ => println("NO")
  }

}

//-[SequentialActor]-------------------------------------------------------------------------------------

/**
 * Executes a Median Filter Sequentially
 * @author Chopo
 */
class SequentialActor extends ChopoActor {

  def receive: PartialFunction[Any, Unit] = {
    case picture: BufferedImage =>
      StartTime()

      //Create new image
      PrePicture = picture
      ReturnPicture = new BufferedImage(picture.getWidth,picture.getHeight(),BufferedImage.TYPE_INT_RGB)

      MedianFilter(new MedianRange(0,picture.getWidth())) //Run the median filter for the entire thing

      println("Beep boop, Sequential is done:")
      sender () ! StopTime()
      sender () ! ReturnPicture

    case _ =>
      println("Sequential actor did not receive an image")
  }
}

//-[ConcurrentActor]-------------------------------------------------------------------------------------

/**
 * Concurrently does a Median filter by splitting it into 10 regions
 * @author Chopo
 */
class ConcurrentActor extends ChopoActor {

  def receive: PartialFunction[Any, Unit] = {

    case picture: BufferedImage =>
      StartTime()
      PrePicture=picture
      ReturnPicture = new BufferedImage(picture.getWidth, picture.getHeight(), BufferedImage.TYPE_INT_RGB)

      //We're going to get a set of ranges
      val Regions : scala.collection.parallel.immutable.ParSeq[MedianRange] = ParVector(
        new MedianRange(0*(picture.getWidth()/10),1*(picture.getWidth()/10)+5), //Split the task into 10 equally sized areas
        new MedianRange(1*(picture.getWidth()/10)-5,2*(picture.getWidth()/10)+5),
        new MedianRange(2*(picture.getWidth()/10)-5,3*(picture.getWidth()/10)+5),
        new MedianRange(3*(picture.getWidth()/10)-5,4*(picture.getWidth()/10)+5), //+5 and -5 due to the window size. Without this adjustment, the
        new MedianRange(4*(picture.getWidth()/10)-5,5*(picture.getWidth()/10)+5), //picture shows up with 10 pixel wide bars
        new MedianRange(5*(picture.getWidth()/10)-5,6*(picture.getWidth()/10)+5),
        new MedianRange(6*(picture.getWidth()/10)-5,7*(picture.getWidth()/10)+5),
        new MedianRange(7*(picture.getWidth()/10)-5,8*(picture.getWidth()/10)+5), //This should really be a for loop but oh well
        new MedianRange(8*(picture.getWidth()/10)-5,9*(picture.getWidth()/10)+5),
        new MedianRange(9*(picture.getWidth()/10)-5,picture.getWidth())
      )
      Regions.map(MedianFilter(_))
      
      println("Beep boop, Concurrent is done:")
      sender() ! StopTime()
      sender() ! ReturnPicture


    case _ =>
      println("Concurrent actor did not receive an image")
  }

}

//-[DoTheMedian]-------------------------------------------------------------------------------------

/**
 * Does the Median
 * @author Chopo
 */
object Example extends App {
  val system = ActorSystem("MedianSimulation")
  val TheClient = system.actorOf(Props[ClientActor], name = "Wilson")

  println("Wilson has been created. He will request this picture to be processed")

  TheClient ! system

  println("he received the system. Now he better do this.")
  TheClient ! "Einstein.jpg" //CHANGE THIS LINE OF TEXT TO CHANGE WHAT PICTURE TO USE

}