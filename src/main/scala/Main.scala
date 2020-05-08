object Main {
  import Errors._
  import Functions._
  import Result._

  def main(args: Array[String]): Unit =
    val result: Result[ProgramError | PredicateFalseError[Int], Int] =
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
            case FileNotFoundError() => Success(0)
            case ArithmeticError() => Success(0)
            case PredicateFalseError(_) => Success(0)
            case err @ MissingArgError() => Failure(err)
            case err @ AccessControlError() => Failure(err)
            case err @ NumberFormatError() => Failure(err)
        }

    println(s"Result: $result")

    println(s"Result after partial error handling: $resultWithErrorHandling")
}
