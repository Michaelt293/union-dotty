# Error handling with union types in Dotty

## Introduction

In Scala, there are several common approaches to error handling, these include -

* *Throwing exceptions*: Using this approach, Scala's type system cannot be used to catch bugs due to lack of error handling. To see if a method/function is capable of throwing an exception, documentation or the inspection of source code is required.

* *Using the `Option` type*: `Option` can be used for computations that can fail. Unfortunately, `Option` does not provide information on why the computation failed. For simple methods/functions with only one possible source of failure, `Option` is a suitable choice. However, whenever there are multiple possible sources of failure, more information is required.

* *Using a type with a `Throwable` error channel*: An example from the standard library is `Try`. An expression with such a type can either succeed with a value of type `A` or fail with a value of type `Throwable`. This provides far more information than using Option since the precise cause of the failure can be captured at runtime. The problem, however, is that `Throwable` as a type is too broad. Consequently, `Throwable` does not communicate possible failure modes to the programmer.

* *Using a type with a typed error channel*: An example from the standard library is `Either`. An expression with such a type can either succeed with a value of type `A` or fail with a value of type `E`. Often, applications/libraries will define an enum (ADT) datatype describing all possible failure modes for the error type.

It may seem that using a type with a typed error channel, for example `Either`, is the clear winner. The use of `Either`, however, is not without its downsides. Notably, error types have to be the same when composing two `Either`s. If the error types are different, the resulting error type will be the common super type of the two error types. This can be seen below where the resulting error type from composing `Either`s of type `Either[MissingArgError, Int]` and `ither[FileNotFoundError, Int]` is `Object & Product & Serializable`!

```scala
scala>val eitherMissingArg: Either[MissingArgError, Int] = Right(1)
     |val eitherFileNotFound: Either[FileNotFoundError, Int] = Right(1)
val eitherFileNotFound: Either[Errors.FileNotFoundError, Int] = Right(1)
val eitherMissingArg: Either[Errors.MissingArgError, Int] = Right(1)

scala> eitherMissingArg.flatMap(_ => eitherFileNotFound)
val res1: Either[Object & Product & Serializable, Int] = Right(1)
```

This issue can be resolved by defining an application/library specific error type (see example below). While certainly much more informative than having a `Throwable` error type, it is worth noting that there is still some loss in precision. For example, a function could fail with only a `FileNotFoundError` but will be forced to return a `ProgramErrorADT`.

```scala
scala> enum ProgramErrorADT {
     |   case MissingArgError
     |   case AccessControlError
     |   case FileNotFoundError
     |   case ArithmeticError
     |   case NumberFormatError
     | }
// defined class ProgramError
scala> val eitherMissingArg: Either[ProgramError, Int] = Right(1)
     | val eitherFileNotFound: Either[ProgramError, Int] = Right(1)
val eitherFileNotFound: Either[ProgramError, Int] = Right(1)
val eitherMissingArg: Either[ProgramError, Int] = Right(1)
scala> eitherMissingArg.flatMap(_ => eitherFileNotFound)
val res8: Either[ProgramError, Int] = Right(1)
```

## Union types for errors

In Dotty (Scala 3), union types have been introduced. A union type has the form of `A | B` and indicates that an expression can have type `A` *OR* `B`. To experiment with using union types for errors, the type `Result` was defined. This type can succeed with a value of type `A` or fail with an error of type `E`.

```scala
sealed trait Result[E, A]

object Result
  final case class Success[E, A](value: A) extends Result[E, A]

  final case class Failure[E, A](error: E) extends Result[E, A]
```

As demonstrated previously, composing two `Either`s with different error types results in the error type having the common super type. For `Result`, `flatMap` was defined to return a `Result` with the union of the two error types.

```scala
def flatMap[E0, B](f: A => Result[E0, B]): Result[E0 | E, B] =
  this.map(f) match
    case Success(Success(v)) => Success(v)
    case Success(Failure(err)) => Failure(err)
    case Failure(err) => Failure(err)
```

Defining `flatMap` in this way allows two `Result`s with different error types to be composed without losing typing information.

```scala
scala>val resultMissingArg: Result[MissingArgError, Int] = Success(1)      
     |val resultFileNotFound: Result[FileNotFoundError, Int] = Success(1)
val resultFileNotFound: Result[Errors.FileNotFoundError, Int] = Success(1)
val resultMissingArg: Result[Errors.MissingArgError, Int] = Success(1)
scala> resultMissingArg.flatMap(_ => resultFileNotFound)
val res6: Result[Errors.FileNotFoundError | Errors.MissingArgError, Int] = Success(1)
```

## Partial error handling

In addition, it is also possible to have partial error-handling tracked by the type system. For example, the method `handleSome` takes a function of type `E0 | E1 => Result[E0, A]` (where `E0` and`E1` are subtypes of `E` and `E0 | E1` is the same type as `E`) and returns a `Result[E0, A]`. This indicates that errors of `E1` have been successfully handled.

```scala
def handleSome[E0 <: E, E1 <: E](
  f: E0 | E1 => Result[E0, A]
  )(implicit ev: E =:= (E0 | E1)): Result[E0, A] =
  this match
    case Success(v) => Success(v)
    case Failure(err) => f(err)
```

To give a concrete example, below the `FileNotFoundError` and `ArithmeticError` are handled and therefore no longer present in the error union type.

```scala
val resultWithErrorHandling: 
  Result[MissingArgError | AccessControlError | NumberFormatError, Int] = 
    result.handleSome { err =>
      err match
        case FileNotFoundError() => Success(0)
        case ArithmeticError() => Success(0)
        case err @ MissingArgError() => Failure(err)
        case err @ AccessControlError() => Failure(err)
        case err @ NumberFormatError() => Failure(err)
    }
```

The Scala compiler can also detect if a pattern match is not exhaustive. For example, if the `MissingArgError` case was missing above, we would get the following warning -
```text
[warn] -- [E029] Pattern Match Exhaustivity Warning: /src/union-dotty/src/main/scala/Main.s
cala:16:10 
[warn] 16 |          err match
[warn]    |          ^^^
[warn]    |          match may not be exhaustive.
[warn]    |
[warn]    |          It would fail on pattern case: Errors.MissingArgError()
```

## Filter and guards

An advantage of using union types as errors is that it is possible to define `filter` and `withFilter`. A `PredicateFalseError` is returned in the case where the predicate evaluates to `false`. Defining `withFilter` allows guards to be used in for comprehensions with `Result`.

```scala
val result: Result[ProgramError | PredicateFalseError[Int], Int] =
  for {
    lines <- getLines(args)
    ave <- getAverage(lines)
    if ave > 0
  } yield ave
```

## Conclusion

The use of union types for errors seems very promising. Being able to introduce and eliminate error types is incredibly powerful for the fine-grained tracking of errors.
