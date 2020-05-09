package io.github.michaelt293.union

import java.io.FileNotFoundException
import java.lang.{ArithmeticException, NumberFormatException}
import java.security.AccessControlException

import scala.io.Source

import org.junit.Test
import org.junit.Assert._

class TestProgram {
  final case class MissingArgError()
  final case class FileNotFoundError()
  final case class ArithmeticError()
  final case class NumberFormatError()
  final case class AccessControlError()

  type ProgramError =
    MissingArgError | 
    AccessControlError | 
    FileNotFoundError | 
    ArithmeticError | 
    NumberFormatError

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

  def program(args: Array[String]): Unit =
    val result: Result[ProgramError | Result.PredicateFalseError[Int], Int] =
      for {
        path <- readArgs(args)
        lines <- readLines(path)
        ints <- Result.traverse(lines)(parseInt)
        ave <- average(ints.toList)
        if ave > 0
      } yield ave

    val resultWithErrorHandling: 
      Result[MissingArgError | AccessControlError | NumberFormatError, Int] = 
        result.handleSome { err =>
          err match
            case FileNotFoundError() => Result.Success(0)
            case ArithmeticError() => Result.Success(0)
            case Result.PredicateFalseError(_) => Result.Success(0)
            case err @ MissingArgError() => Result.Failure(err)
            case err @ AccessControlError() => Result.Failure(err)
            case err @ NumberFormatError() => Result.Failure(err)
        }

    assertEquals(result, Result.Failure(FileNotFoundError()))
    assertEquals(resultWithErrorHandling, Result.Success(0))

  @Test def testProgram(): Unit = {
    program(Array("not a file"))
  }
}