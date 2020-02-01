import scala.language.implicitConversions
import scala.util.{Failure => TFailure, Success => TSuccess, Try}

sealed trait Result[E, A]
  import Result._

  def map[B](f: A => B): Result[E, B] =
    this match
      case Success(v) => Success(f(v))
      case Failure(err) => Failure(err)

  def flatMap[E0, B](f: A => Result[E0, B]): Result[E0 | E, B] =
    this.map(f) match
      case Success(Success(v)) => Success(v)
      case Success(Failure(err)) => Failure(err)
      case Failure(err) => Failure(err)

  def handleErrors(f: E => A): A =
    this match
      case Success(v) => v
      case Failure(err) => f(err)

  def handleSome[E0 <: E, E1 <: E](
    f: E0 | E1 => Result[E0, A]
    )(implicit ev: E =:= (E0 | E1)): Result[E0, A] =
    this match
      case Success(v) => Success(v)
      case Failure(err) => f(err)

  def mapErrors[E0](f: E => E0): Result[E0, A] =
    this match
      case Success(v) => Success(v)
      case Failure(err) => Failure(f(err))

object Result
  final case class Success[E, A](value: A) extends Result[E, A]

  final case class Failure[E, A](error: E) extends Result[E, A]

  def traverse[E, A, B](l: List[A])(f: A => Result[E, B]): Result[E, List[B]] =
    l.foldLeft(Success(List.empty): Result[E, List[B]]) { (acc, succ) => 
      f(succ) match
        case Success(v) => acc.map(_ :+ v)
        case Failure(err) => Failure(err)
    }

  def fromOption[A](option: Option[A]): Result[Unit, A] =
    option match
      case None => Failure(())
      case Some(v) => Success(v)

  def fromEither[E, A](either: Either[E, A]): Result[E, A] =
    either match
      case Left(err) => Failure(err)
      case Right(v) => Success(v)

  def fromTry[A](`try`: Try[A]): Result[Throwable, A] =
    `try` match
      case TFailure(err) => Failure(err)
      case TSuccess(v) => Success(v)

  def catchExceptions[A](expression: => A): Result[Throwable, A] =
    fromTry(Try(expression))
