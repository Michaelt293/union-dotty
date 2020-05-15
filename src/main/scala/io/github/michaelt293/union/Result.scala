package io.github.michaelt293.union

import scala.language.implicitConversions
import scala.util.{Failure => TFailure, Success => TSuccess, Try}

sealed trait Result[E, +A] extends Product with Serializable {
  import Result._

  def fold[B](fe: E => B, fa: A => B): B =
    this match
      case Success(v) => fa(v)
      case Failure(err) => fe(err)

  def foreach[U](f: A => U): Unit =
    this match
      case Success(v) => f(v)
      case _ => ()

  def getOrElse[A1 >: A](or: => A1): A1 =
    this match
      case Success(v) => v
      case _ => or

  def orElse[E0, A1 >: A](or: => Result[E0, A1]): Result[E | E0, A1] =
    this match
      case Success(v) => Success(v)
      case _ => or match
        case Success(orV) => Success(orV)
        case Failure(orErr) => Failure(orErr)

  def exists(p: A => Boolean): Boolean =
    this match
      case Success(v) => p(v)
      case _ => false

  def contains[A1 >: A](a: A1): Boolean =
    this.exists(_ == a)

  def forall(p: A => Boolean): Boolean =
    this match
      case Success(v) => p(v)
      case _ => true

  def map[A1 >: A, B](f: A1 => B): Result[E, B] =
    this match
      case Success(v) => Success(f(v))
      case Failure(err) => Failure(err)

  def flatMap[E0, A1 >: A, B](f: A1 => Result[E0, B]): Result[E0 | E, B] =
    this.map(f) match
      case Success(Success(v)) => Success(v)
      case Success(Failure(err)) => Failure(err)
      case Failure(err) => Failure(err)

  def filter[A1 >: A](p: A1 => Boolean): Result[E | PredicateFalseError[A1], A1] =
    this match
      case Success(v) => if (p(v)) Success(v) else Failure(PredicateFalseError(v))
      case Failure(err) => Failure(err)

  def withFilter[A1 >: A](p: A1 => Boolean): WithFilter[A1] =
    new WithFilter(p)

  final class WithFilter[A1 >: A](p: A1 => Boolean) {
    def map[B](f: A1 => B): Result[E | PredicateFalseError[A1], B] =
      Result.this.filter(p).map(f)

    def flatMap[E0, B](f: A1 => Result[E0, B]): Result[E0 | E | PredicateFalseError[A1], B] =
      Result.this.filter(p).flatMap(f)

    def foreach[U](f: A1 => U): Unit =
      Result.this.filter(p).foreach(f)

    def withFilter(q: A1 => Boolean): WithFilter[A1] =
      new WithFilter(x => p(x) && q(x))
  }

  def existsError(p: E => Boolean): Boolean =
    this match
      case Failure(err) => p(err)
      case _ => false

  def containsError(err: E): Boolean =
    this.existsError(_ == err)

  def forallError(p: E => Boolean): Boolean =
    this match
      case Failure(err) => p(err)
      case _ => true

  def handleError[A1 >: A](f: E => A1): A1 =
    this match
      case Success(v) => v
      case Failure(err) => f(err)

  def handleSome[E0 <: E, E1 <: E, E2, A1 >: A](
    f: E0 | E1 => Result[E1 | E2, A1]
    )(implicit ev: E =:= (E0 | E1)): Result[E1 | E2, A1] =
    this match
      case Success(v) => Success(v)
      case Failure(err) => f(err)

  def mapError[E0](f: E => E0): Result[E0, A] =
    this match
      case Success(v) => Success(v)
      case Failure(err) => Failure(f(err))

  def toOption: Option[A] =
    this match
      case Success(v) => Some(v)
      case _ => None

  def toSeq: Seq[A] =
    this match
      case Success(v) => Seq(v)
      case _ => Seq.empty

  def toEither: Either[E, A] =
    this match
      case Success(v) => Right(v)
      case Failure(err) => Left(err)

  def toTry(ev: E <:< Throwable): Try[A] =
    this match
      case Success(v) => TSuccess(v)
      case Failure(err) => TFailure(err.asInstanceOf[Throwable])

  def isSuccess: Boolean

  def isFailure: Boolean
}

object Result {
  def apply[A](a: => A): Result[Throwable, A] =
    fromTry(Try(a))

  final case class Success[E, A](value: A) extends Result[E, A] {

    def isSuccess: Boolean = true

    def isFailure: Boolean = false
  }

  final case class Failure[E, A](error: E) extends Result[E, A] {

    def isSuccess: Boolean = false

    def isFailure: Boolean = true
  }

  final case class PredicateFalseError[A](value: A) extends AnyVal {
    def message: String = s"Predicate was false for: $value"
  }

  def cond[E, A](test: Boolean, success: => A, failure: => E): Result[E, A] =
    if (test) Success(success) else Failure(failure)

  def traverse[E, A, B](seq: Seq[A])(f: A => Result[E, B]): Result[E, Seq[B]] =
    seq.foldLeft(Success(Seq.empty): Result[E, Seq[B]]) { (acc, succ) => 
      f(succ) match
        case Success(v) => acc.map(_ :+ v)
        case Failure(err) => Failure(err)
    }

  def traverse[E, A, B](option: Option[A])(f: A => Result[E, B]): Result[E, Option[B]] =
    option match
      case None => Success(None)
      case Some(v) => f(v).map(Some(_))

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
}
