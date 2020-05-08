import java.io.FileNotFoundException
import java.lang.{ArithmeticException, NumberFormatException}
import java.security.AccessControlException

import scala.io.Source

object Functions {
  import Errors._
  import Result._

  def readArgs(args: Array[String]): Result[MissingArgError, String] =
    Result
      .fromOption(args.headOption)
      .mapError(_ => MissingArgError())

  def readLines(path: String):
    Result[AccessControlError | FileNotFoundError, List[String]] =
      Result(Source.fromFile(path))
        .map(_.getLines.toList)
        .mapError {
          case err: AccessControlException => AccessControlError()
          case err: FileNotFoundException => FileNotFoundError()
          case err => throw err
        }

  def parseInt(str: String): Result[NumberFormatError, Int] =
    Result(str.toInt)
      .mapError {
        case err: NumberFormatException => NumberFormatError()
        case err => throw err
      }

  def average(ints: List[Int]): Result[ArithmeticError, Int] =
    Result(ints.sum / ints.length).mapError {
      case err: ArithmeticException => ArithmeticError()
      case err => throw err
    }
}
