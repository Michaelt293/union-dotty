import java.io.FileNotFoundException
import java.lang.{ArithmeticException, NumberFormatException}
import java.security.AccessControlException

import scala.io.Source

object Functions
  import Errors._
  import Result._

  def readArgs(args: Array[String]): Result[MissingArgError, String] =
    Result
      .fromOption(args.headOption)
      .mapErrors(_ => MissingArgError())

  def readLines(path: String):
    Result[AccessControlError | FileNotFoundError, List[String]] =
      Result(Source.fromFile(path))
        .map(_.getLines.toList)
        .mapErrors {
          case err: AccessControlException => AccessControlError()
          case err: FileNotFoundException => FileNotFoundError()
          case err => throw err
        }

  def parseInt(str: String): Result[NumberFormatError, Int] =
    Result(str.toInt)
      .mapErrors {
        case err: NumberFormatException => NumberFormatError()
        case err => throw err
      }

  def average(ints: List[Int]): Result[ArithmeticError, Int] =
    Result(ints.sum / ints.length).mapErrors {
      case err: ArithmeticException => ArithmeticError()
      case err => throw err
    }

  def getLines(args: Array[String]): 
    Result[MissingArgError | AccessControlError | FileNotFoundError, List[String]] =
      for {
        path <- readArgs(args)
        lines <- readLines(path)
      } yield lines

  def getAverage(lines: List[String]): 
    Result[ArithmeticError | NumberFormatError, Int] =
      for {
        ints <- Result.traverse(lines)(parseInt)
        ave <- average(ints.toList)
      } yield ave
